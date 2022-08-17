package com.icemachined.kafka.common

data class MetricName(
    val name: String,
    val group: String,
    val description: String,
    val tags: Map<String, String>
)
