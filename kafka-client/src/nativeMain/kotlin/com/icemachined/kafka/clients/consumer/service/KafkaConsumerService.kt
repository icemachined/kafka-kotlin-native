package com.icemachined.kafka.clients.consumer.service

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.KafkaConsumer

import kotlin.native.concurrent.*
import kotlinx.coroutines.*

/**
 * Kafka consumer service
 */
@Suppress("DEBUG_PRINT")
class KafkaConsumerService<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val consumerScope: CoroutineScope = CoroutineScope(Job())
) : ConsumerService {
    private val clientId = config.kafkaConsumerProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!
    private val consumer: KafkaConsumer<K, V> = KafkaConsumer(config.kafkaConsumerProperties, config.keyDeserializer, config.valueDeserializer)
    private val coroutineJob: AtomicReference<Job?> = AtomicReference(null)

    fun isStopped(): Boolean = (coroutineJob.value?.isCompleted ?: false)

    override suspend fun start() {
        println("Starting consumer $clientId polling")
        coroutineJob.value = KafkaConsumerJob(config, consumer, consumerScope).pollingCycle()
    }

    override suspend fun stop() {
        println("stop consumer polling")
        coroutineJob.value?.cancelAndJoin()
    }
}
