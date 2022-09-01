/**
 *  Producer interface
 */

package com.icemachined.kafka.clients.producer

import com.icemachined.kafka.common.PartitionInfo

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.flow.SharedFlow

/**
 * @property isOk
 * @property errorMessage
 */
data class SendResult(
    val isOk: Boolean,
    val errorMessage: String? = null
)

/**
 * The interface for the [KafkaProducer]
 * @see KafkaProducer
 *
 * @see MockProducer
 */
interface Producer<K, V> {
    /**
     * See [KafkaProducer.send]
     *
     * @param record - ProducerRecord to send
     * @return shared flow of send results
     */
    fun send(record: ProducerRecord<K, V>): SharedFlow<SendResult>

    /**
     * See [KafkaProducer.flush]
     */
    fun flush()

    /**
     * See [KafkaProducer.partitionsFor]
     *
     * @param topic
     * @return - list of partitions
     */
    fun partitionsFor(topic: String): List<PartitionInfo>

    /**
     * See [KafkaProducer.close]
     *
     * @param timeout
     */
    fun close(timeout: Duration = 1.toDuration(DurationUnit.MINUTES))
}
