package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.common.Metric
import com.icemachined.kafka.common.MetricName
import com.icemachined.kafka.common.PartitionInfo
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.header.Header

import kotlin.time.Duration

typealias Headers = List<Header>

typealias ConsumerRecords<K, V> = List<ConsumerRecord<K, V>>

typealias TopicsList = Map<String, List<PartitionInfo>>

@Suppress(
    "KDOC_CONTAINS_DATE_OR_AUTHOR",
    "KDOC_EMPTY_KDOC",
    "KDOC_NEWLINES_BEFORE_BASIC_TAGS",
    "KDOC_NO_DEPRECATED_TAG",
    "KDOC_NO_EMPTY_TAGS",
    "KDOC_NO_NEWLINES_BETWEEN_BASIC_TAGS",
    "KDOC_NO_NEWLINE_AFTER_SPECIAL_TAGS",
    "KDOC_WRONG_SPACES_AFTER_TAG",
    "KDOC_WRONG_TAGS_ORDER"
)
/**
 * @see KafkaConsumer
 * @see MockConsumer
 */
interface Consumer<K, V> {
    /**
     * @see KafkaConsumer.assignment
     * @return
     */
    fun assignment(): Set<TopicPartition>

    /**
     * @see KafkaConsumer.subscription
     * @return
     */
    fun subscription(): Set<String>

    /**
     * @see KafkaConsumer.subscribe
     * @param topics
     */
    fun subscribe(topics: Collection<String>)

    /**
     * @see KafkaConsumer.assign
     * @param partitions
     */
    fun assign(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.unsubscribe
     */
    fun unsubscribe()

    /**
     * @see KafkaConsumer.poll
     * @param timeout
     * @return
     */
    fun poll(timeout: Duration? = null): ConsumerRecords<K, V>

    /**
     * @see KafkaConsumer.commitSync
     * @param offsets
     */
    fun commitSync(offsets: Map<TopicPartition, OffsetAndMetadata>)

    /**
     * @see KafkaConsumer.commitAsync
     * @param offsets
     * @param callback
     */
    fun commitAsync(offsets: Map<TopicPartition, OffsetAndMetadata>? = null, callback: OffsetCommitCallback? = null)

    /**
     * @see KafkaConsumer.seek
     * @param partition
     * @param offset
     */
    fun seek(partition: TopicPartition, offset: Long)

    /**
     * @see KafkaConsumer.seek
     * @param partition
     * @param offsetAndMetadata
     */
    fun seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata)

    /**
     * @see KafkaConsumer.seekToBeginning
     * @param partitions
     */
    fun seekToBeginning(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.seekToEnd
     * @param partitions
     */
    fun seekToEnd(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.position
     * @param partition
     * @param timeout
     * @return
     */
    fun position(partition: TopicPartition, timeout: Duration? = null): Long

    /**
     * @see KafkaConsumer.committed
     * @param partitions
     * @param timeout
     * @return
     */
    fun committed(partitions: Set<TopicPartition>, timeout: Duration? = null): Map<TopicPartition, OffsetAndMetadata>

    /**
     * @see KafkaConsumer.metrics
     * @return
     */
    fun metrics(): Map<MetricName, Metric>

    /**
     * @see KafkaConsumer.partitionsFor
     * @param topic
     * @param timeout
     * @return
     */
    fun partitionsFor(topic: String, timeout: Duration? = null): List<PartitionInfo>

    /**
     * @see KafkaConsumer.listTopics
     * @param timeout
     * @return
     */
    @Suppress("TYPE_ALIAS")
    fun listTopics(timeout: Duration? = null): TopicsList

    /**
     * @see KafkaConsumer.paused
     * @return
     */
    fun paused(): Set<TopicPartition>

    /**
     * @see KafkaConsumer.pause
     * @param partitions
     */
    fun pause(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.resume
     * @param partitions
     */
    fun resume(partitions: Collection<TopicPartition>)

    /**
     * @see KafkaConsumer.offsetsForTimes
     * @param timestampsToSearch
     * @param timeout
     * @return
     */
    fun offsetsForTimes(
        timestampsToSearch: Map<TopicPartition, Long>,
        timeout: Duration? = null
    ): Map<TopicPartition, OffsetAndTimestamp>

    /**
     * @see KafkaConsumer.beginningOffsets
     * @param partitions
     * @param timeout
     * @return
     */
    fun beginningOffsets(partitions: Collection<TopicPartition>, timeout: Duration? = null): Map<TopicPartition, Long>

    /**
     * @see KafkaConsumer.endOffsets
     * @param partitions
     * @param timeout
     * @return
     */
    fun endOffsets(partitions: Collection<TopicPartition>, timeout: Duration? = null): Map<TopicPartition, Long>

    /**
     * @see KafkaConsumer.currentLag
     * @param topicPartition
     * @return
     */
    fun currentLag(topicPartition: TopicPartition): Long

    /**
     * @see KafkaConsumer.groupMetadata
     * @return
     */
    fun groupMetadata(): ConsumerGroupMetadata

    /**
     * @see KafkaConsumer.enforceRebalance
     * @param reason
     */
    fun enforceRebalance(reason: String? = null)

    /**
     * @see KafkaConsumer.close
     * @param timeout
     */
    fun close(timeout: Duration? = null)

    /**
     * @see KafkaConsumer.wakeup
     */
    fun wakeup()
}
