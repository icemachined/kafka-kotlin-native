package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.clients.setupKafkaConfig
import com.icemachined.kafka.common.Metric
import com.icemachined.kafka.common.MetricName
import com.icemachined.kafka.common.PartitionInfo
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.header.Header
import com.icemachined.kafka.common.header.RecordHeader
import com.icemachined.kafka.common.record.TimestampType
import com.icemachined.kafka.common.serialization.Deserializer

import librdkafka.*
import platform.posix.size_t
import platform.posix.size_tVar

import kotlin.time.Duration
import kotlinx.cinterop.*

/**
 * @property kafkaConsumerProperties
 * @property keyDeserializer
 * @property valueDeserializer
 */
@Suppress("MAGIC_NUMBER", "DEBUG_PRINT")
class KafkaConsumer<K, V>(
    val kafkaConsumerProperties: Map<String, String>,
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
) : Consumer<K, V> {
    private var consumerHandle: CPointer<rd_kafka_t>

    init {
        val configHandle = setupKafkaConfig(kafkaConsumerProperties.entries)
        val buf = ByteArray(512)
        val strBufSize: size_t = (buf.size - 1).convert()

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
        rd_kafka_poll_set_consumer(consumerHandle)
    }

    private fun shouldReturnOnErrors(rkmessage: CPointer<rd_kafka_message_t>): Boolean {
        val kafkaMessage = rkmessage.pointed
        if (kafkaMessage.err != 0) {
            if (kafkaMessage.err == RD_KAFKA_RESP_ERR__PARTITION_EOF) {
                println(
                    "Consumer reached end of ${rd_kafka_topic_name(kafkaMessage.rkt)?.toKString()} " +
                            "[${kafkaMessage.partition}] message queue " +
                            "at offset ${kafkaMessage.offset}"
                )
                return true
            }
            val errorMessage = kafkaMessage.rkt?.let {
                "Consume error for topic \"${rd_kafka_topic_name(kafkaMessage.rkt)?.toKString()}\" [${kafkaMessage.partition}] " +
                        "offset ${kafkaMessage.offset}: " +
                        "${rd_kafka_message_errstr(rkmessage)}"
            } ?: "Consumer error: ${rd_kafka_err2str(kafkaMessage.err)}: ${rd_kafka_message_errstr(rkmessage)}"
            println(errorMessage)
            if (kafkaMessage.err === RD_KAFKA_RESP_ERR__UNKNOWN_PARTITION ||
                    kafkaMessage.err === RD_KAFKA_RESP_ERR__UNKNOWN_TOPIC
            ) {
                throw RuntimeException(errorMessage)
            }
            return true
        }
        return false
    }

    private fun consume(rkmessage: CPointer<rd_kafka_message_t>): Headers<K, V> {
        if (shouldReturnOnErrors(rkmessage)) {
            return emptyList()
        }
        val kafkaMessage = rkmessage.pointed
        val key = kafkaMessage.key?.let {
            keyDeserializer.deserialize(it.readBytes(kafkaMessage.key_len.toInt()))
        }
        val value = kafkaMessage.payload?.let {
            valueDeserializer.deserialize(it.readBytes(kafkaMessage.len.toInt()))
        }
        val headers = extractHeaders(rkmessage)
        return listOf(
            ConsumerRecord(
                rd_kafka_topic_name(kafkaMessage.rkt)!!.toKString(),
                kafkaMessage.partition,
                kafkaMessage.offset.toULong(),
                0, TimestampType.NO_TIMESTAMP_TYPE,
                kafkaMessage.key_len.toInt(),
                kafkaMessage.len.toInt(),
                key, value, headers, null
            )
        )
    }

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private fun extractHeaders(rkmessage: CPointer<rd_kafka_message_t>): List<Header> {
        val headers = mutableListOf<Header>()
        memScoped {
            val header = allocPointerTo<rd_kafka_headers_t>()
            println("get headers from message")
            if (rd_kafka_message_headers(rkmessage, header.ptr) == 0) {
                val valRef: COpaquePointerVar = alloc()
                val sizeRef: size_tVar = alloc()
                val nameRef: CPointerVar<ByteVar> = alloc()

                println("start getting headers")
                var idx = 0
                while (rd_kafka_header_get_all(
                    header.value,
                    idx.convert(),
                    nameRef.ptr,
                    valRef.ptr,
                    sizeRef.ptr
                ) == 0
                ) {
                    println("getting header $idx")
                    headers.add(
                        RecordHeader(
                            nameRef.value?.toKString(),
                            valRef.value?.readBytes(sizeRef.value.toInt())
                        )
                    )
                    println("got header ${nameRef.value?.toKString()}, ${sizeRef.value}")
                    idx++
                }
                println("finish getting headers")
            }
        }
        return headers
    }

    override fun assignment(): Set<TopicPartition> {
        TODO("Not yet implemented")
    }

    override fun subscription(): Set<String> {
        TODO("Not yet implemented")
    }

    override fun subscribe(topics: Collection<String>) {
        /* Convert the list of topics to a format suitable for librdkafka */
        val subscription = rd_kafka_topic_partition_list_new(topics.size)
        topics.forEach {
            rd_kafka_topic_partition_list_add(
                subscription, it,
                /* the partition is ignored
                 * by subscribe() */
                RD_KAFKA_PARTITION_UA
            )
        }
        /* Subscribe to the list of topics */
        val err = rd_kafka_subscribe(consumerHandle, subscription)
        if (err != 0) {
            rd_kafka_topic_partition_list_destroy(subscription)
            throw RuntimeException("Failed to subscribe to $topics topics: ${rd_kafka_err2str(err)?.toKString()}")
        }

        println("Subscribed to $topics topic(s), waiting for rebalance and messages...")
        rd_kafka_topic_partition_list_destroy(subscription)
    }

    override fun assign(partitions: Collection<TopicPartition>) {
        TODO("Not yet implemented")
    }

    override fun unsubscribe() {
        val err = rd_kafka_unsubscribe(consumerHandle)
        if (err != 0) {
            throw RuntimeException("Failed to unsubscribe: ${rd_kafka_err2str(err)?.toKString()}")
        }
    }

    override fun poll(timeout: Duration?): Headers<K, V> {
        val rkmessage =
                rd_kafka_consumer_poll(consumerHandle, timeout?.inWholeMilliseconds?.toInt() ?: 0)  // non-blocking poll
        rkmessage?.let {
            val records = consume(rkmessage)
            rd_kafka_message_destroy(rkmessage)
            return records
        }
        return emptyList()
    }

    override fun commitSync(offsets: Map<TopicPartition, OffsetAndMetadata>) {
        val partitionsOffsetsList = rd_kafka_topic_partition_list_new(offsets.size)
        offsets.entries.forEach {
            rd_kafka_topic_partition_list_set_offset(
                partitionsOffsetsList,
                it.key.topic,
                it.key.partition,
                it.value.offset.toLong()
            )
        }
        rd_kafka_commit(consumerHandle, partitionsOffsetsList, 0)
        rd_kafka_topic_partition_list_destroy(partitionsOffsetsList)
        println("Sync committed: $offsets")
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

    override fun listTopics(timeout: Duration?): TopicsList {
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
        /* Close the consumer: commit final offsets and leave the group. */
        println("Closing consumer")
        rd_kafka_consumer_close(consumerHandle)

        /* Destroy the consumer */
        rd_kafka_destroy(consumerHandle)
    }

    override fun wakeup() {
        TODO("Not yet implemented")
    }
}
