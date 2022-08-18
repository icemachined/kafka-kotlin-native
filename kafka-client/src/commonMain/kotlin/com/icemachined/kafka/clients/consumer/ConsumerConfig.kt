package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.common.serialization.Deserializer

/**
 * @property topicNames
 * @property kafkaConsumerProperties
 * @property keyDeserializer
 * @property valueDeserializer
 * @property recordHandler
 * @property kafkaPollingIntervalMs
 */
data class ConsumerConfig<K, V>(
    val topicNames: Collection<String>,
    val kafkaConsumerProperties: Map<String, String>,
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
    val recordHandler: ConsumerRecordHandler<K, V>,
    val kafkaPollingIntervalMs: Long = 100
)
