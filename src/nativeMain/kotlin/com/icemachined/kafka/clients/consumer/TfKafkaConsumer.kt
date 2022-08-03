package com.db.tf.messaging.consumer

import com.db.tf.messaging.config.TfKafkaConsumerConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.Logger
import java.lang.String.join
import java.util.concurrent.ExecutorService

/**
 * Starts or stops consuming into provided callbacks.
 */
class TfKafkaConsumer(
        private val config: TfKafkaConsumerConfig,
        private val recordHandler: ConsumerRecordHandler,
        private val executorService: ExecutorService,
        private val numberOfWorkers: Int
) : Consumer {

    private val clientId: String
    private val log: Logger = org.slf4j.LoggerFactory.getLogger(this.javaClass)
    val consumerKafkaProperties: Map<String, Any>

    init {
        assert(numberOfWorkers > 0)
        consumerKafkaProperties = config.kafkaConsumerProperties.toMutableMap()
        clientId = consumerKafkaProperties[ConsumerConfig.CLIENT_ID_CONFIG]!!.toString()
        // we override important properties
        consumerKafkaProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
    }

    private var jobs: ArrayList<TfConsumerJob> = ArrayList()

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

    private fun createConsumerJob(consumerIndex: Int): TfConsumerJob {
        val jobConsumerKafkaProperties = consumerKafkaProperties.toMutableMap()
        val jobClientId = join(
                "-",
                jobConsumerKafkaProperties[CommonClientConfigs.CLIENT_ID_CONFIG] as String,
                consumerIndex.toString()
        )

        jobConsumerKafkaProperties[CommonClientConfigs.CLIENT_ID_CONFIG] = jobClientId as Object


        return TfConsumerJob(jobConsumerKafkaProperties, jobClientId, config, recordHandler)
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

