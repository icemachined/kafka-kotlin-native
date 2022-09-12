package com.icemachined.kafka.clients.consumer.service

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.Consumer
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.clients.consumer.OffsetAndMetadata
import com.icemachined.kafka.common.*
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.serialization.DeserializationException
import com.icemachined.kafka.common.serialization.SerializeUtils

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.*

/**
 * Kafka Consumer Job for polling cycle
 */
@Suppress("TOO_LONG_FUNCTION")
class KafkaConsumerJob<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val consumer: Consumer<K, V>,
    private val consumerScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val clientId = config.kafkaConsumerProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!

    suspend fun pollingCycle() =
            consumerScope.launch {
                logInfo(clientId, "Starting consumer")
                try {
                    consumer.subscribe(config.topicNames)
                    logInfo(clientId, "Subscribed to topics=${config.topicNames}")

                    val watch = StopWatch()
                    while (isActive) {
                        logTrace(clientId, "Start consumer poll")
                        watch.start()
                        try {
                            val records = consumer.poll()

                            records.forEach { handleRecord(it) }
                            val elapsedTime = watch.stop().inWholeMilliseconds
                            if (records.count() > 0) {
                                logInfo(clientId,
                                    "Message batch with ${records.count()} msg(s) processed in $elapsedTime ms by $clientId"
                                )
                            }
                            val timeLeft = config.kafkaPollingIntervalMs - elapsedTime
                            if (timeLeft > 0) {
                                delay(timeLeft)
                            }
                        } catch (ex: DeserializationException) {
                            handleSerializationException(clientId, ex)
                        }
                    }
                } catch (e: CancellationException) {
                    logDebug(clientId, "Poll cancelled it's ok")
                } catch (e: Throwable) {
                    logError(clientId, "Exception occurred during reading from kafka", e)
                    useRecoveryStrategy(clientId)
                } finally {
                    logInfo(clientId,
                        "Consumer is closing."
                    )
                    consumer.close()
                    // dltProducer?.close()
                    logInfo(clientId,
                        "Consumer has been closed."
                    )
                    logInfo(clientId, "Exiting poll")
                }
            }
    private fun handleSerializationException(clientId: String, ex: DeserializationException) {
        logError(clientId, "Deserialization exception.", ex)
        // dltProducer?.publish(clientId, ex)
        logInfo(clientId, "Committing failed message for consumer.")
        commitSync(ex.record)
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleRecord(record: ConsumerRecord<K, V>) {
        checkDeser(record, KafkaHeaders.DESERIALIZER_EXCEPTION_VALUE)

        val messageId: String = transportMessageId(
            record.topic,
            record.partition,
            record.offset
        )
        logInfo(clientId,
            "Record with key='${record.key}' and value's transportId='$messageId' has been read."
        )
        config.recordHandler.handle(record)
        commitSync(record as ConsumerRecord<Any, Any>)
        logInfo(clientId,
            "Message with transportId='$messageId' consumed successfully."
        )
    }

    fun checkDeser(record: ConsumerRecord<K, V>, headerName: String) {
        val exception = SerializeUtils.getExceptionFromHeader(record, headerName)
        exception?.let {
            exception.record = record as ConsumerRecord<Any, Any>
            throw exception
        }
    }

    private fun commitSync(record: ConsumerRecord<Any, Any>) {
        consumer.commitSync(
            mapOf(
                TopicPartition(record.topic, record.partition) to OffsetAndMetadata(record.offset + 1U)
            )
        )
    }

    private fun transportMessageId(
        topic: String,
        partition: Int,
        offset: ULong
    ): String =
            listOf(topic, partition.toString(), offset.toString()).joinToString("_")

    private fun useRecoveryStrategy(clientId: String) {
        TODO("Not yet implemented")
    }
}
