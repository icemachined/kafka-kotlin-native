package com.icemachined.kafka.clients.consumer

import com.db.tf.messaging.consumer.ConsumerService
import com.icemachined.kafka.common.StopWatch
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.serialization.DeserializationException
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.join
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


class KafkaConsumerService<K, V>(
    private val clientId: String,
    private val config: ConsumerConfig<K, V>
) : ConsumerService {
    private var kafkaPollingJobFuture: Future<Job>? = null
    private val worker = Worker.start(true, "kafka-polling-consumer-worker")

    //    private var dltProducer: TfDeadletterPublisher?
//    private val log: Logger = org.slf4j.LoggerFactory.getLogger(this.javaClass)
    private val consumer: KafkaConsumer<K, V>
    val _isConsumerPollingActive = MutableStateFlow(true)
    private val _isStopped = MutableStateFlow(false)

    init {
        consumer = KafkaConsumer(config.kafkaConsumerProperties, config.keyDeserializer, config.valueDeserializer)
//        dltProducer = config.dltProducerProperties?.let {
//            TfDeadletterPublisher(it, consumerKafkaProperties[CommonClientConfigs.CLIENT_ID_CONFIG] as String)
//        }
    }

    class KafkaConsumerJob<K, V>(
        private val config: ConsumerConfig<K, V>,
        private val consumer: KafkaConsumer<K, V>,
        private val isPollingActive: StateFlow<Boolean>,
        private val isPollingStopped: StateFlow<Boolean>,
        private val clientId: String
    ) {
        fun pollingCycle() =
            runBlocking {
                launch(Dispatchers.Default) {
                    println("Starting consumer:[$clientId], for topics=${config.topicNames}")
                    try {
                        consumer.subscribe(config.topicNames)
                        println("Consumer:[${clientId}], subscribed to topics=${config.topicNames}")

                        val watch = StopWatch()
                        while (isPollingActive.value) {
                            watch.start()
                            try {
                                val records = consumer.poll()

                                records.forEach { handleRecord(it) };
                                val elapsedTime = watch.stop().inWholeMilliseconds
                                if (records.count() > 0) {
                                    println(
                                        "Message batch with ${records.count()} msg(s) processed in ${elapsedTime} ms"
                                    )
                                }
                                var timeLeft = config.kafkaPollingIntervalMs - elapsedTime
                                if (timeLeft > 0)
                                    delay(timeLeft)
                            } catch (ex: DeserializationException) {
                                handleSerializationException(clientId, ex)
                            }
                        }
                    } catch (e: CancellationException) {
                        println("poll cancelled it's ok")
                    } catch (e: Throwable) {
                        println("Unexpected exception in kafka polling job:")
                        e.printStackTrace()
                    } finally {
                        println("exiting poll ")
                    }
                }
            }
        private fun handleSerializationException(clientId: String, ex: DeserializationException) {
            println("Deserialization exception: ${ex.message}")
            // dltProducer?.publish(clientId, ex)
            println("Committing failed message for consumer:$clientId.")
            commitSync(ex.record)
        }

        private fun handleRecord(record: ConsumerRecord<K, V>) {
            checkDeser(record, KafkaHeaders.DESERIALIZER_EXCEPTION_VALUE)

            val messageId: String = transportMessageId(
                record.topic,
                record.partition,
                record.offset
            )
            println(
                "Consumer:[${clientId}] Record with key='${record.key}' and value's transportId='${messageId}' has been read."
            )
            config.recordHandler.handle(record)
            commitSync(record)
            println(
                "Consumer:[{}] message with transportId='{}' consumed successfully.",
                clientId,
                messageId
            )
        }

        fun checkDeser(record: ConsumerRecord<K, V>, headerName: String) {
            val exception = TfSerializeUtils.getExceptionFromHeader(record, headerName, log)
            if (exception != null) {
                exception.record = record
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

        private fun transportMessageId(topic: String, partition: Int, offset: ULong): String =
            listOf(topic, partition.toString(), offset.toString()).joinToString("_")

        private fun useRecoveryStrategy(clientId: String) {
            TODO("Not yet implemented")
        }

        private fun handleInterruptException(clientId: String, ex: CancellationException) {
            println("Consumer:[${clientId}] has been interrupted.")
            println("Consumer:[${clientId}] has been interrupted. ${ex.message}")
        }
    }

    fun isStopped(): Boolean = _isStopped.value
    fun startConsume() {
        kafkaPollingJobFuture = worker.execute(TransferMode.SAFE,
            { KafkaConsumerJob(config, consumer, _isConsumerPollingActive.asStateFlow(), _isStopped.asStateFlow()) })
        {
            it.pollingCycle()
        }

    }

    fun pollingCycle() {
        println("Starting consumer:[$clientId], for topics=${config.topicNames}")
        try {
            consumer.subscribe(config.topicNames)
            println("Consumer:[${clientId}], subscribed to topics=${config.topicNames}")

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
                } catch (ex: DeserializationException) {
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


    fun stop() {
        pollCycleStopped.set(true)
    }
}