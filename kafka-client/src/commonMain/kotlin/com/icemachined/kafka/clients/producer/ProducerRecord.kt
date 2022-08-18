/*
    
*/

package com.icemachined.kafka.clients.producer

import com.icemachined.kafka.common.header.Header

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
    val headers: List<Header>? = null
)
