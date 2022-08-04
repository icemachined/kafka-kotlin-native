package com.db.tf.messaging.consumer

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newSingleThreadContext

/**
 * Starts or stops consuming into provided callbacks.
 */
class KafkaParallelGroupsConsumer<K, V>(
        private val config: ConsumerConfig<K, V>,
        private val executorService: CoroutineDispatcher = newSingleThreadContext(""),
        private val numberOfWorkers: Int
) : ConsumerService {

    private val clientId: String
//    private val log: Logger = org.slf4j.LoggerFactory.getLogger(this.javaClass)
    val consumerKafkaProperties: Map<String, Any>

    init {
        assert(numberOfWorkers > 0)
        consumerKafkaProperties = config.kafkaConsumerProperties.toMutableMap()
        clientId = consumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG]!!.toString()
        // we override important properties
        consumerKafkaProperties[CommonConfigNames.ENABLE_AUTO_COMMIT_CONFIG] = "false"
    }

    private var jobs: ArrayList<KafkaConsumerJob> = ArrayList()

    @Synchronized
    override fun start() {
        log.info(
                "Starting consumer group {} with {} parallel workers",
                consumerKafkaProperties[ConsumerConfig.GROUP_ID_CONFIG],
                numberOfWorkers
        )
        val upperBound = numberOfWorkers - 1
        if (jobs.isEmpty()) {
            for (workerId in 0..upperBound)
                jobs.add(createConsumerJob(workerId))
        } else {
            for (workerId in 0..upperBound)
                if (jobs[workerId].isStopped()) {
                    jobs[workerId] = createConsumerJob(workerId)
                } else
                    throw IllegalStateException("Consumer is still running. You need to stop first")
        }
        for (workerId in 0..upperBound) {
            val job = jobs[workerId]
            executorService.execute(job::pollingCycle)
        }
    }

    private fun createConsumerJob(consumerIndex: Int): KafkaConsumerJob {
        val jobConsumerKafkaProperties = consumerKafkaProperties.toMutableMap()
        val jobClientId = join(
                "-",
                jobConsumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG] as String,
                consumerIndex.toString()
        )

        jobConsumerKafkaProperties[CommonConfigNames.CLIENT_ID_CONFIG] = jobClientId as Object


        return KafkaConsumerJob(jobConsumerKafkaProperties, jobClientId, config, recordHandler)
    }

    @Synchronized
    override fun stop() {
        log.info(
                "Stopping kafka consumer {} for topics={}",
                clientId,
                config.topicNames
        )
        if (jobs.isEmpty()) {
            throw IllegalStateException("Consumer ${clientId} is not initialized yet")
        } else {
            for (workerId in 0..numberOfWorkers - 1)
                if (jobs[workerId].isStopped()) {
                    log.info(
                            "Consumer {} for topics={} is already stopped",
                            clientId,
                            config.topicNames
                    )
                } else {
                    jobs[workerId].stop()
                }
        }
    }
}

