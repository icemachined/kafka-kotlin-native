package com.icemachined.kafka.clients.producer

import org.apache.kafka.common.PartitionInfo

import kotlin.time.Duration
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
     * @param record
     * @return
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
     * @return
     */
    fun partitionsFor(topic: String): List<PartitionInfo>

    /**
     * See [KafkaProducer.close]
     */
    fun close()

    /**
     * See [KafkaProducer.close]
     *
     * @param timeout
     */
    fun close(timeout: Duration)
}
