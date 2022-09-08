package com.icemachined.kafka.clients.consumer.service

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.Consumer
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.clients.consumer.OffsetAndMetadata
import com.icemachined.kafka.common.StopWatch
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.serialization.DeserializationException
import com.icemachined.kafka.common.serialization.SerializeUtils

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.*

/**
 * Kafka Consumer Job for polling cycle
 */
@Suppress("TOO_LONG_FUNCTION", "DEBUG_PRINT")
class KafkaConsumerJob<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val consumer: Consumer<K, V>,
    private val consumerScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val clientId = config.kafkaConsumerProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!

    suspend fun pollingCycle() =
            consumerScope.launch {
                println("Starting consumer:[$clientId], for topics=${config.topicNames}")
                try {
                    consumer.subscribe(config.topicNames)
                    println("Consumer:[$clientId], subscribed to topics=${config.topicNames}")

                    val watch = StopWatch()
                    while (isActive) {
                        println("Start consumer poll cycle")
                        watch.start()
                        try {
                            val records = consumer.poll()

                            records.forEach { handleRecord(it) }
                            val elapsedTime = watch.stop().inWholeMilliseconds
                            if (records.count() > 0) {
                                println(
                                    "Message batch with ${records.count()} msg(s) processed in $elapsedTime ms"
                                )
                            }
                            var timeLeft = config.kafkaPollingIntervalMs - elapsedTime
                            if (timeLeft > 0) {
                                delay(timeLeft)
                            }
                        } catch (ex: DeserializationException) {
                            handleSerializationException(clientId, ex)
                        }
                    }
                } catch (e: CancellationException) {
                    println("poll cancelled it's ok")
                    handleInterruptException(clientId, e)
                } catch (e: Throwable) {
                    println("Consumer:[$clientId] Exception occurred during reading from kafka ${e.message}")
                    e.printStackTrace()
                    useRecoveryStrategy(clientId)
                } finally {
                    println(
                        "Consumer:[$clientId] for topics=${config.topicNames} is closing."
                    )
                    consumer.close()
                    // dltProducer?.close()
                    println(
                        "Consumer:[$clientId] for topics=${config.topicNames} has been closed."
                    )
                    println("exiting poll ")
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
            "Consumer:[$clientId] Record with key='${record.key}' and value's transportId='$messageId' has been read."
        )
        config.recordHandler.handle(record)
        commitSync(record as ConsumerRecord<Any, Any>)
        println(
            "Consumer:[$clientId] message with transportId='$messageId' consumed successfully."
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

    private fun handleInterruptException(clientId: String, ex: CancellationException) {
        println("Consumer:[$clientId] has been interrupted.")
        println("Consumer:[$clientId] has been interrupted. ${ex.message}")
    }
}
