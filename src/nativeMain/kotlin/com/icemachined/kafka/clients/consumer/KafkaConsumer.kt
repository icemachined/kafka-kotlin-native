package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.common.Metric
import com.icemachined.kafka.common.MetricName
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.serialization.Deserializer
import com.icemachined.kafka.common.serialization.Serializer
import org.apache.kafka.common.PartitionInfo
import kotlin.time.Duration

class KafkaConsumer<K, V>(
    private val producerConfig: Map<String, String>,
    private val keySerializer: Deserializer<K>,
    private val valueSerializer: Deserializer<V>
) : Consumer<K, V> {
    override fun assignment(): Set<TopicPartition> {
        TODO("Not yet implemented")
    }

    override fun subscription(): Set<String> {
        TODO("Not yet implemented")
    }

    override fun subscribe(topics: Collection<String>) {
        TODO("Not yet implemented")
    }

    override fun assign(partitions: Collection<TopicPartition>) {
        TODO("Not yet implemented")
    }

    override fun unsubscribe() {
        TODO("Not yet implemented")
    }

    override fun poll(timeout: Duration): Iterable<ConsumerRecord<K, V>> {
        TODO("Not yet implemented")
    }

    override fun commitSync(offsets: Map<TopicPartition, OffsetAndMetadata>?, timeout: Duration?) {
        TODO("Not yet implemented")
    }

    override fun commitAsync(offsets: Map<TopicPartition, OffsetAndMetadata>?, callback: OffsetCommitCallback?) {
        TODO("Not yet implemented")
    }

    override fun seek(partition: TopicPartition, offset: Long) {
        TODO("Not yet implemented")
    }

    override fun seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata) {
        TODO("Not yet implemented")
    }

    override fun seekToBeginning(partitions: Collection<TopicPartition>) {
        TODO("Not yet implemented")
    }

    override fun seekToEnd(partitions: Collection<TopicPartition>) {
        TODO("Not yet implemented")
    }

    override fun position(partition: TopicPartition, timeout: Duration?): Long {
        TODO("Not yet implemented")
    }

    override fun committed(
        partitions: Set<TopicPartition>,
        timeout: Duration?
    ): Map<TopicPartition, OffsetAndMetadata> {
        TODO("Not yet implemented")
    }

    override fun metrics(): Map<MetricName, Metric> {
        TODO("Not yet implemented")
    }

    override fun partitionsFor(topic: String, timeout: Duration?): List<PartitionInfo> {
        TODO("Not yet implemented")
    }

    override fun listTopics(timeout: Duration?): Map<String, List<PartitionInfo>> {
        TODO("Not yet implemented")
    }

    override fun paused(): Set<TopicPartition> {
        TODO("Not yet implemented")
    }

    override fun pause(partitions: Collection<TopicPartition>) {
        TODO("Not yet implemented")
    }

    override fun resume(partitions: Collection<TopicPartition>) {
        TODO("Not yet implemented")
    }

    override fun offsetsForTimes(
        timestampsToSearch: Map<TopicPartition, Long>,
        timeout: Duration?
    ): Map<TopicPartition, OffsetAndTimestamp> {
        TODO("Not yet implemented")
    }

    override fun beginningOffsets(
        partitions: Collection<TopicPartition>,
        timeout: Duration?
    ): Map<TopicPartition, Long> {
        TODO("Not yet implemented")
    }

    override fun endOffsets(partitions: Collection<TopicPartition>, timeout: Duration?): Map<TopicPartition, Long> {
        TODO("Not yet implemented")
    }

    override fun currentLag(topicPartition: TopicPartition): Long {
        TODO("Not yet implemented")
    }

    override fun groupMetadata(): ConsumerGroupMetadata {
        TODO("Not yet implemented")
    }

    override fun enforceRebalance(reason: String?) {
        TODO("Not yet implemented")
    }

    override fun close(timeout: Duration?) {
        TODO("Not yet implemented")
    }

    override fun wakeup() {
        TODO("Not yet implemented")
    }
}