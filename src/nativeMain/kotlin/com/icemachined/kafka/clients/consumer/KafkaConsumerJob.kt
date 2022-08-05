package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.common.StopWatch
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.serialization.DeserializationException
import com.icemachined.kafka.common.serialization.SerializeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.cancellation.CancellationException

class KafkaConsumerJob<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val consumer: KafkaConsumer<K, V>,
    private val isPollingActive: MutableStateFlow<Boolean>,
    private val isPollingStopped: MutableStateFlow<Boolean>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val clientId = config.kafkaConsumerProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!

    fun pollingCycle() =
        runBlocking {
            launch(coroutineDispatcher) {
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
                    handleInterruptException(clientId, e);
                } catch (e: Throwable) {
                    println("Consumer:[${clientId}] Exception occurred during reading from kafka ${e.message}")
                    e.printStackTrace()
                    useRecoveryStrategy(clientId)
                } finally {
                    println(
                        "Consumer:[${clientId}] for topics=${config.topicNames} is closing."
                    )
                    isPollingActive.emit(false)
                    consumer.close()
                    // dltProducer?.close()
                    isPollingStopped.emit(true)
                    println(
                        "Consumer:[${clientId}] for topics=${config.topicNames} has been closed."
                    )
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
        commitSync(record as ConsumerRecord<Any, Any>)
        println(
            "Consumer:[${clientId}] message with transportId='${messageId}' consumed successfully."
        )
    }

    fun checkDeser(record: ConsumerRecord<K, V>, headerName: String) {
        val exception = SerializeUtils.getExceptionFromHeader(record, headerName)
        if (exception != null) {
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