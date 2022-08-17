package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.common.header.Header
import com.icemachined.kafka.common.record.TimestampType

/**
 * A key/value pair to be received from Kafka. This also consists of a topic name and
 * a partition number from which the record is being received, an offset that points
 * to the record in a Kafka partition, and a timestamp as marked by the corresponding ProducerRecord.
 * @property topic
 * @property partition
 * @property offset
 * @property timestamp
 * @property timestampType
 * @property serializedKeySize
 * @property serializedValueSize
 * @property key
 * @property value
 * @property headers
 * @property leaderEpoch
 */
data class ConsumerRecord<K, V>(
    val topic: String,
    val partition: Int,
    val offset: ULong,
    val timestamp: Long,
    val timestampType: TimestampType,
    val serializedKeySize: Int,
    val serializedValueSize: Int,
    val key: K?,
    val value: V?,
    val headers: List<Header>?,
    val leaderEpoch: Int?
)
