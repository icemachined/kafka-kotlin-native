package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.clients.CommonConfigNames

import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class KafkaConsumerService<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ConsumerService {
    private var kafkaPollingJobFuture: Future<Job>? = null
    private val clientId = config.kafkaConsumerProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!
    private val worker: Worker = Worker.start(true, "kafka-consumer-$clientId")

    // private var dltProducer: TfDeadletterPublisher?
    // private val log: Logger = org.slf4j.LoggerFactory.getLogger(this.javaClass)
    private val consumer: KafkaConsumer<K, V>
    val _isConsumerPollingActive = MutableStateFlow(true)
    private val isStopped = MutableStateFlow(false)

    init {
        consumer = KafkaConsumer(config.kafkaConsumerProperties, config.keyDeserializer, config.valueDeserializer)
        // dltProducer = config.dltProducerProperties?.let {
        // TfDeadletterPublisher(it, consumerKafkaProperties[CommonClientConfigs.CLIENT_ID_CONFIG] as String)
        // }
    }

    fun isStopped(): Boolean = !(kafkaPollingJobFuture?.result?.isActive ?: false)
    override fun start() {
        kafkaPollingJobFuture = worker.execute(TransferMode.SAFE,
            {
                KafkaConsumerJob(config, consumer, _isConsumerPollingActive, isStopped,
                    newSingleThreadContext("kafka-consumer-context-$clientId")).freeze()
            }) {
            it.pollingCycle()
        }
    }

    override fun stop() {
        runBlocking(coroutineDispatcher) {
            kafkaPollingJobFuture?.let {
                println("stop consumer polling")
                _isConsumerPollingActive.emit(false)
                println("cancel and wait")
                it.result.cancelAndJoin()
                worker.requestTermination().result
                println("closing kafka producer")
                if (!isStopped.value) {
                    consumer.close()
                }
            } ?: run {
                throw RuntimeException("Polling job haven't been started yet")
            }
        }
    }
}