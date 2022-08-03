package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.common.Metric
import com.icemachined.kafka.common.MetricName
import com.icemachined.kafka.common.TopicPartition
import org.apache.kafka.common.PartitionInfo
import kotlin.time.Duration


/**
 * @see KafkaConsumer
 *
 * @see MockConsumer
 */
interface Consumer<K, V> {
    /**
     * @see KafkaConsumer.assignment
     */
    fun assignment(): Set<TopicPartition>

    /**
     * @see KafkaConsumer.subscription
     */
    fun subscription(): Set<String>

    /**
     * @see KafkaConsumer.subscribe
     */
    fun subscribe(topics: Collection<String>)

    /**
     * @see KafkaConsumer.assign
     */
    fun assign(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.unsubscribe
     */
    fun unsubscribe()

    /**
     * @see KafkaConsumer.poll
     */
    fun poll(timeout: Duration? = null): Iterable<ConsumerRecord<K, V>>

    /**
     * @see KafkaConsumer.commitSync
     */
    fun commitSync(offsets: Map<TopicPartition, OffsetAndMetadata>? = null, timeout: Duration? = null)

    /**
     * @see KafkaConsumer.commitAsync
     */
    fun commitAsync(offsets: Map<TopicPartition, OffsetAndMetadata>? = null, callback: OffsetCommitCallback? = null)

    /**
     * @see KafkaConsumer.seek
     */
    fun seek(partition: TopicPartition, offset: Long)

    /**
     * @see KafkaConsumer.seek
     */
    fun seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata)

    /**
     * @see KafkaConsumer.seekToBeginning
     */
    fun seekToBeginning(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.seekToEnd
     */
    fun seekToEnd(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.position
     */
    fun position(partition: TopicPartition, timeout: Duration? = null): Long

    /**
     * @see KafkaConsumer.committed
     */
    fun committed(partitions: Set<TopicPartition>, timeout: Duration? = null): Map<TopicPartition, OffsetAndMetadata>

    /**
     * @see KafkaConsumer.metrics
     */
    fun metrics(): Map<MetricName, Metric>

    /**
     * @see KafkaConsumer.partitionsFor
     */
    fun partitionsFor(topic: String, timeout: Duration? = null): List<PartitionInfo>

    /**
     * @see KafkaConsumer.listTopics
     */
    fun listTopics(timeout: Duration? = null): Map<String, List<PartitionInfo>>

    /**
     * @see KafkaConsumer.paused
     */
    fun paused(): Set<TopicPartition>

    /**
     * @see KafkaConsumer.pause
     */
    fun pause(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.resume
     */
    fun resume(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.offsetsForTimes
     */
    fun offsetsForTimes(
        timestampsToSearch: Map<TopicPartition, Long>,
        timeout: Duration? = null
    ): Map<TopicPartition, OffsetAndTimestamp>

    /**
     * @see KafkaConsumer.beginningOffsets
     */
    fun beginningOffsets(partitions: Collection<TopicPartition>, timeout: Duration? = null): Map<TopicPartition, Long>

    /**
     * @see KafkaConsumer.endOffsets
     */
    fun endOffsets(partitions: Collection<TopicPartition>, timeout: Duration? = null): Map<TopicPartition, Long>

    /**
     * @see KafkaConsumer.currentLag
     */
    fun currentLag(topicPartition: TopicPartition): Long

    /**
     * @see KafkaConsumer.groupMetadata
     */
    fun groupMetadata(): ConsumerGroupMetadata

    /**
     * @see KafkaConsumer.enforceRebalance
     */
    fun enforceRebalance(reason: String? = null)

    /**
     * @see KafkaConsumer.close
     */
    fun close(timeout: Duration? = null)

    /**
     * @see KafkaConsumer.wakeup
     */
    fun wakeup()
}