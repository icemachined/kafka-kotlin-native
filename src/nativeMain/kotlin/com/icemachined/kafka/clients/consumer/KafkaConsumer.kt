package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.clients.KafkaUtils
import com.icemachined.kafka.common.Metric
import com.icemachined.kafka.common.MetricName
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.serialization.Deserializer
import kotlinx.cinterop.*
import librdkafka.*
import org.apache.kafka.common.PartitionInfo
import platform.posix.size_t
import kotlin.time.Duration

class KafkaConsumer<K, V>(
    private val producerConfig: Map<String, String>,
    private val keySerializer: Deserializer<K>,
    private val valueSerializer: Deserializer<V>,
    private val topics: List<String>
) : Consumer<K, V> {
    private var consumerHandle: CPointer<rd_kafka_t>

    init {
        val configHandle = KafkaUtils.setupConfig(producerConfig.entries)
        val buf = ByteArray(512)
        val strBufSize = (buf.size - 1).convert<size_t>()

        /*
         * Create consumer instance.
         *
         * NOTE: rd_kafka_new() takes ownership of the conf object
         *       and the application must not reference it again after
         *       this call.
         */
        val rk =
            buf.usePinned { rd_kafka_new(rd_kafka_type_t.RD_KAFKA_CONSUMER, configHandle, it.addressOf(0), strBufSize) }
        consumerHandle = rk ?: run {
            throw RuntimeException("Failed to create new consumer: ${buf.decodeToString()}")
        }

        /* Redirect all messages from per-partition queues to
         * the main queue so that messages can be consumed with one
         * call from all assigned partitions.
         *
         * The alternative is to poll the main queue (for events)
         * and each partition queue separately, which requires setting
         * up a rebalance callback and keeping track of the assignment:
         * but that is more complex and typically not recommended. */
        rd_kafka_poll_set_consumer(consumerHandle);

        /* Convert the list of topics to a format suitable for librdkafka */

        val subscription =rd_kafka_topic_partition_list_new(topics.size)
        topics.forEach {
            rd_kafka_topic_partition_list_add(
                subscription, it,
                /* the partition is ignored
                 * by subscribe() */
                RD_KAFKA_PARTITION_UA
            )
        }
        /* Subscribe to the list of topics */
        val err = rd_kafka_subscribe(consumerHandle, subscription);
        if (err != 0) {
            rd_kafka_topic_partition_list_destroy(subscription);
            rd_kafka_destroy(rk);
            throw RuntimeException("Failed to subscribe to ${topics} topics: ${rd_kafka_err2str(err)?.toKString()}");
        }

        println("Subscribed to ${topics} topic(s), waiting for rebalance and messages...");
    }

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