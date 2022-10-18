package com.icemachined.kafka.clients.consumer.service

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.ConsumerConfigNames
import com.icemachined.kafka.common.logKafkaInfo
import kotlinx.coroutines.*

/**
 * Starts or stops multiple parallel kafka consumer services with the same group.
 */
@Suppress("TYPE_ALIAS")
class KafkaParallelConsumerService<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val numberOfWorkers: Int,
    private val coroutineDispatcher: CoroutineDispatcher? = null
) : ConsumerService {
    private val clientId: String
    private val consumerKafkaProperties: Map<String, String>
    private var jobs: ArrayList<KafkaConsumerService<K, V>> = ArrayList()
    private val consumerGroupScope: CoroutineScope = CoroutineScope(SupervisorJob())

    init {
        assert(numberOfWorkers > 0)
        consumerKafkaProperties = config.kafkaConsumerProperties.toMutableMap()
        clientId = consumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!.toString()
        // we override important properties
        consumerKafkaProperties[ConsumerConfigNames.ENABLE_AUTO_COMMIT_CONFIG] = "false"
    }

    override suspend fun start() {
        logKafkaInfo(
            "Starting consumer group ${consumerKafkaProperties[CommonConfigNames.GROUP_ID_CONFIG]} " +
                    "with $numberOfWorkers parallel workers"
        )
        val upperBound = numberOfWorkers - 1
        if (jobs.isEmpty()) {
            for (workerId in 0..upperBound) {
                jobs.add(createConsumerJob(workerId))
            }
        } else {
            for (workerId in 0..upperBound) {
                if (jobs[workerId].isStopped()) {
                    jobs[workerId] = createConsumerJob(workerId)
                } else {
                    throw IllegalStateException("Consumer is still running. You need to stop first")
                }
            }
        }
        for (workerId in 0..upperBound) {
            val job = jobs[workerId]
            job.start()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createConsumerJob(consumerIndex: Int): KafkaConsumerService<K, V> {
        val jobConsumerKafkaProperties = consumerKafkaProperties.toMutableMap()
        val jobClientId = listOf(
            jobConsumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG] as String,
            consumerIndex.toString()
        ).joinToString("-")

        jobConsumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG] = jobClientId

        return KafkaConsumerService(
            config.copy(kafkaConsumerProperties = jobConsumerKafkaProperties.toMap()),
            CoroutineScope(
                Job(consumerGroupScope.coroutineContext.job) +
                        (coroutineDispatcher ?: newSingleThreadContext("consumer-context-$clientId"))
            )
        )
    }

    override suspend fun stop() {
        logKafkaInfo(
            "Stopping kafka consumer $clientId for topics=${config.topicNames}"
        )
        if (jobs.isEmpty()) {
            throw IllegalStateException("Consumer $clientId is not initialized yet")
        } else {
            consumerGroupScope.cancel()
            for (workerId in 0..numberOfWorkers - 1) {
                if (jobs[workerId].isStopped()) {
                    logKafkaInfo(
                        "Consumer ${jobs[workerId].clientId} for topics=${config.topicNames} is already stopped"
                    )
                } else {
                    jobs[workerId].stop()
                }
            }
        }
    }
}
