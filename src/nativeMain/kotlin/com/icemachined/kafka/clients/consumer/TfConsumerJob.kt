package com.db.tf.messaging.consumer

import com.db.tf.messaging.config.TfKafkaConsumerConfig
import com.db.tf.messaging.serializer.TfDeserializationException
import com.db.tf.messaging.serializer.TfKafkaHeaders
import com.db.tf.messaging.serializer.TfSerializeUtils
import org.apache.commons.lang3.time.StopWatch
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.InterruptException
import org.slf4j.Logger
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class TfConsumerJob(
        private val consumerKafkaProperties: Map<String, Any>,
        private val clientId: String,
        private val config: TfKafkaConsumerConfig,
        private val recordHandler: ConsumerRecordHandler
) {

    private var dltProducer: TfDeadletterPublisher?
    private val log: Logger = org.slf4j.LoggerFactory.getLogger(this.javaClass)
    private val consumer: KafkaConsumer<String, Object>
    private val pollCycleStopped = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    init {
        consumer = KafkaConsumer<String, Object>(consumerKafkaProperties)
        dltProducer = config.dltProducerProperties?.let {
            TfDeadletterPublisher(it, consumerKafkaProperties[CommonClientConfigs.CLIENT_ID_CONFIG] as String)
        }
    }

    fun isStopped(): Boolean = isStopped.get()

    fun pollingCycle() {
        log.info(
                "Starting consumer:[{}], for topics={}",
                clientId,
                config.topicNames
        )
        try {
            consumer.subscribe(config.topicNames)
            log.info(
                    "Consumer:[{}], subscribed to topics={}",
                    clientId,
                    config.topicNames
            )
            val watch = StopWatch()
            while (!pollCycleStopped.get()) {
                watch.reset()
                watch.start()
                try {
                    val consumerRecords = consumer.poll(config.pollingTimeout)

                    if (!consumerRecords.isEmpty) {
                        consumerRecords.forEach { handleRecord(it) };
                        log.info(
                                "Message batch with {}msg(s) processed in {}ms",
                                consumerRecords.count(),
                                watch.time
                        )
                    }
                } catch (ex: TfDeserializationException) {
                    handleSerializationException(clientId, ex)
                }
            }
        } catch (ex: InterruptException) {
            handleInterruptException(clientId, ex);
        } catch (throwable: Throwable) {
            log.error("Consumer:[{}] Exception occurred during reading from kafka", clientId, throwable);
            useRecoveryStrategy(clientId);
        } finally {
            log.info(
                    "Consumer:[{}] for topics={} is closing.",
                    clientId,
                    config.topicNames
            )
            pollCycleStopped.set(true)
            consumer.close()
            dltProducer?.close()
            isStopped.set(true)
            log.info(
                    "Consumer:[{}] for topics={} has been closed.",
                    clientId,
                    config.topicNames
            )
        }
    }

    private fun handleSerializationException(clientId: String, ex: TfDeserializationException) {
        log.error("Deserialization exception:", ex)
        dltProducer?.publish(clientId, ex)
        log.info("Committing failed message for consumer:$clientId.")
        commitSync(ex.record)
    }

    private fun handleRecord(record: ConsumerRecord<String, Object>) {
        checkDeser(record, TfKafkaHeaders.DESERIALIZER_EXCEPTION_VALUE)

        val messageId: String = transportMessageId(
                record.topic(),
                record.partition(),
                record.offset()
        )
        log.info(
                "Consumer:[{}] Record with key='{}' and value's transportId='{}' has been read.",
                clientId,
                record.key(),
                messageId
        )
        recordHandler.handle(record)
        commitSync(record)
        log.info(
                "Consumer:[{}] message with transportId='{}' consumed successfully.",
                clientId,
                messageId
        )
    }

    fun checkDeser(record: ConsumerRecord<String, Object>, headerName: String) {
        val exception = TfSerializeUtils.getExceptionFromHeader(record, headerName, log)
        if (exception != null) {
            exception.record = record
            throw exception
        }
    }

    private fun commitSync(record: ConsumerRecord<String, Object>) {
        consumer.commitSync(
                Collections.singletonMap(
                        TopicPartition(record.topic(), record.partition()),
                        OffsetAndMetadata(record.offset() + 1)
                )
        )
    }

    private fun transportMessageId(topic: String, partition: Int, offset: Long): String =
            java.lang.String.join("_", topic, partition.toString(), offset.toString())

    private fun useRecoveryStrategy(clientId: String) {
        TODO("Not yet implemented")
    }

    private fun handleInterruptException(clientId: String, ex: InterruptException) {
        log.info("Consumer:[{}] has been interrupted.", clientId)
        log.debug("Consumer:[{}] has been interrupted.", clientId, ex)
    }

    fun stop() {
        pollCycleStopped.set(true)
    }
}