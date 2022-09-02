package com.icemachined.kafka.clients.consumer.service

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.ConsumerConfigNames
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Starts or stops consuming into provided callbacks.
 */
@Suppress("TYPE_ALIAS", "DEBUG_PRINT")
class KafkaParallelGroupsConsumerService<K, V>(
    private val config: ConsumerConfig<K, V>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val numberOfWorkers: Int
) : ConsumerService {
    private val clientId: String
    private val consumerKafkaProperties: Map<String, String>
    private var jobs: ArrayList<KafkaConsumerService<K, V>> = ArrayList()

    init {
        assert(numberOfWorkers > 0)
        consumerKafkaProperties = config.kafkaConsumerProperties.toMutableMap()
        clientId = consumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!.toString()
        // we override important properties
        consumerKafkaProperties[ConsumerConfigNames.ENABLE_AUTO_COMMIT_CONFIG] = "false"
    }

    override fun start() {
        println(
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

    private fun createConsumerJob(consumerIndex: Int): KafkaConsumerService<K, V> {
        val jobConsumerKafkaProperties = consumerKafkaProperties.toMutableMap()
        val jobClientId = listOf(
            jobConsumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG] as String,
            consumerIndex.toString()
        ).joinToString("-")

        jobConsumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG] = jobClientId

        return KafkaConsumerService(config.copy(kafkaConsumerProperties = jobConsumerKafkaProperties.toMap()), coroutineDispatcher)
    }

    override fun stop() {
        println(
            "Stopping kafka consumer {clientId} for topics={config.topicNames}"
        )
        if (jobs.isEmpty()) {
            throw IllegalStateException("Consumer $clientId is not initialized yet")
        } else {
            for (workerId in 0..numberOfWorkers - 1) {
                if (jobs[workerId].isStopped()) {
                    println(
                        "Consumer $clientId for topics=${config.topicNames} is already stopped"
                    )
                } else {
                    jobs[workerId].stop()
                }
            }
        }
    }
}
