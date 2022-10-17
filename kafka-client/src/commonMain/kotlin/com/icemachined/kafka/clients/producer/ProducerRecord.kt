package com.icemachined.kafka.clients.producer

import com.icemachined.kafka.clients.consumer.Headers

/**
 * @property topic
 * @property value
 * @property key
 * @property partition
 * @property timestamp
 * @property headers
 */
data class ProducerRecord<K, V>(
    val topic: String,
    val value: V,
    val key: K? = null,
    val partition: UInt? = null,
    val timestamp: ULong? = null,
    val headers: Headers? = null
)
