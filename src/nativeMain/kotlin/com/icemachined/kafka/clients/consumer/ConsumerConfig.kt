package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.common.serialization.Deserializer

data class ConsumerConfig<K, V> (
    val topicNames: Collection<String>,
    val kafkaConsumerProperties: Map<String, String>,
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
    val recordHandler: ConsumerRecordHandler<K, V>,
    val kafkaPollingIntervalMs: Long = 100
)

