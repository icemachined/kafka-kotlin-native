package com.icemachined.kafka.clients.consumer.service

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.KafkaConsumer

import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Kafka consumer service
 */
@Suppress("DEBUG_PRINT")
class KafkaConsumerService<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ConsumerService {
    private var kafkaPollingJobFuture: Future<Job>? = null
    private val clientId = config.kafkaConsumerProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!
    private val worker: Worker = Worker.start(true, "kafka-consumer-$clientId")
    private val consumer: KafkaConsumer<K, V> = KafkaConsumer(config.kafkaConsumerProperties, config.keyDeserializer, config.valueDeserializer)
    private val isConsumerPollingActive = MutableStateFlow(true)
    private val isStopped = MutableStateFlow(false)

    fun isStopped(): Boolean = !(kafkaPollingJobFuture?.result?.isActive ?: false)
    override fun start() {
        kafkaPollingJobFuture = worker.execute(TransferMode.SAFE,
            {
                KafkaConsumerJob(config, consumer, isConsumerPollingActive, isStopped,
                    newSingleThreadContext("kafka-consumer-context-$clientId")).freeze()
            }) {
            it.pollingCycle()
        }
    }

    override fun stop() {
        runBlocking(coroutineDispatcher) {
            kafkaPollingJobFuture?.let { stopJob(it) } ?: run {
                throw RuntimeException("Polling job haven't been started yet")
            }
        }
    }

    private suspend fun stopJob(jobFuture: Future<Job>) {
        println("stop consumer polling")
        isConsumerPollingActive.emit(false)
        println("cancel and wait")
        jobFuture.result.cancelAndJoin()
        worker.requestTermination().result
        println("closing kafka producer")
        if (!isStopped.value) {
            consumer.close()
        }
    }
}
