package com.icemachined.kafka.clients.consumer.service

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.KafkaConsumer
import kotlinx.atomicfu.AtomicRef

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.native.concurrent.*

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
    //private val worker: Worker = Worker.start(true, "kafka-consumer-$clientId")
    private val consumer: KafkaConsumer<K, V> = KafkaConsumer(config.kafkaConsumerProperties, config.keyDeserializer, config.valueDeserializer)
    private val isConsumerPollingActive = MutableStateFlow(true)
    private val isStopped = MutableStateFlow(false)
    private val coroutineJob: AtomicReference<Job?> = AtomicReference(null)

    fun isStopped(): Boolean = !(kafkaPollingJobFuture?.result?.isActive ?: false)
    override suspend fun start() {
//        kafkaPollingJobFuture = worker.execute(TransferMode.SAFE,
//            {
//                KafkaConsumerJob(config, consumer, isConsumerPollingActive, isStopped,
//                    newSingleThreadContext("kafka-consumer-context-$clientId")).freeze()
//            }) {
//            it.pollingCycle()
//        }
        val consumerJob = KafkaConsumerJob(config, consumer, isConsumerPollingActive, isStopped, coroutineDispatcher).freeze()
        coroutineJob.value = consumerJob.pollingCycle()
    }

    override suspend fun stop() {
//        runBlocking(coroutineDispatcher) {
//            kafkaPollingJobFuture?.let { stopJob(it) } ?: run {
//                throw RuntimeException("Polling job haven't been started yet")
//            }
//        }
        coroutineJob.value?.cancelAndJoin()
        if (!isStopped.value) {
            consumer.close()
        }
    }

    private suspend fun stopJob(jobFuture: Future<Job>) {
        println("stop consumer polling")
        isConsumerPollingActive.emit(false)
        println("cancel and wait")
        jobFuture.result.cancelAndJoin()
        //worker.requestTermination().result
        println("closing kafka producer")
        if (!isStopped.value) {
            consumer.close()
        }
    }
}
