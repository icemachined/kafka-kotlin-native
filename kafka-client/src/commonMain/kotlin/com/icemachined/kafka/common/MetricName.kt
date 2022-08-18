package com.icemachined.kafka.common

/**
 * @property name
 * @property group
 * @property description
 * @property tags
 */
data class MetricName(
    val name: String,
    val group: String,
    val description: String,
    val tags: Map<String, String>
)
