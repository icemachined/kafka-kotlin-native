package com.icemachined.kafka.common

/**
 * @property topic
 * @property partition
 */
data class TopicPartition(
    val topic: String,
    val partition: Int
)
