package com.icemachined.kafka.clients.producer

import kotlinx.coroutines.flow.StateFlow
import org.apache.kafka.common.PartitionInfo
import kotlin.time.Duration

data class SendResult (
    val isOk:Boolean,
    val errorMessage:String? = null
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
     */
    fun send(record: ProducerRecord<K, V>): StateFlow<SendResult>


    /**
     * See [KafkaProducer.flush]
     */
    fun flush()

    /**
     * See [KafkaProducer.partitionsFor]
     */
    fun partitionsFor(topic: String): List<PartitionInfo>

    /**
     * See [KafkaProducer.close]
     */
    fun close()

    /**
     * See [KafkaProducer.close]
     */
    fun close(timeout: Duration)
}