package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.clients.KafkaUtils
import com.icemachined.kafka.clients.producer.isPollingActive
import com.icemachined.kafka.common.Metric
import com.icemachined.kafka.common.MetricName
import com.icemachined.kafka.common.StopWatch
import com.icemachined.kafka.common.TopicPartition
import com.icemachined.kafka.common.record.TimestampType
import com.icemachined.kafka.common.serialization.Deserializer
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import librdkafka.*
import org.apache.kafka.common.PartitionInfo
import platform.posix.size_t
import kotlin.coroutines.cancellation.CancellationException
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.time.Duration

class KafkaConsumer<K, V>(
    private val producerConfig: Map<String, String>,
    private val keySerializer: Deserializer<K>,
    private val valueSerializer: Deserializer<V>,
    private val topics: List<String>,
    private val kafkaPollingIntervalMs: Long = 100
) : Consumer<K, V> {
    private var kafkaPollingJobFuture: Any
    private var worker: Worker
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
        val err = rd_kafka_subscribe(consumerHandle, subscription)
        if (err != 0) {
            rd_kafka_topic_partition_list_destroy(subscription)
            rd_kafka_destroy(rk)
            throw RuntimeException("Failed to subscribe to ${topics} topics: ${rd_kafka_err2str(err)?.toKString()}")
        }

        println("Subscribed to ${topics} topic(s), waiting for rebalance and messages...")
        rd_kafka_topic_partition_list_destroy(subscription)
    }

    fun startConsume(){
        worker = Worker.start(true, "kafka-polling-consumer-worker")
        kafkaPollingJobFuture = worker.execute(TransferMode.SAFE, {kafkaPollingIntervalMs to consumerHandle}){ param ->
            runBlocking {
                launch(Dispatchers.Default) {
                    try {
                        val watch = StopWatch()
                        while (isPollingActive.value) {
                            watch.start()
                            val elapsedTime = watch.stop().inWholeMilliseconds
                            println("poll happened in $elapsedTime millis")
                            var timeLeft = param.first - elapsedTime
                            if(timeLeft>0)
                                delay(timeLeft)
                        }
                    } catch (e: CancellationException) {
                        println("poll cancelled it's ok")
                    }
                    catch (e: Throwable) {
                        println("Unexpected exception in kafka polling job:")
                        e.printStackTrace()
                    } finally {
                        println("exiting poll ")
                    }
                }
            }
        }

    }
    private fun consume(rkmessage: CPointer<rd_kafka_message_t>): Iterable<ConsumerRecord<K, V>> {
        if (rkmessage.pointed.err != 0) {
            if (rkmessage.pointed.err == RD_KAFKA_RESP_ERR__PARTITION_EOF) {
                println(
                    "Consumer reached end of ${rd_kafka_topic_name(rkmessage.pointed.rkt)?.toKString()} " +
                            "[${rkmessage.pointed.partition}] message queue " +
                            "at offset ${rkmessage.pointed.offset}"
                )

                return emptyList()
            }
            val errorMessage = if (rkmessage.pointed.rkt == null) {
                    "Consumer error: ${rd_kafka_err2str(rkmessage.pointed.err)}: ${rd_kafka_message_errstr(rkmessage)}"
            } else {
                    "Consume error for topic \"${rd_kafka_topic_name(rkmessage.pointed.rkt)?.toKString()}\" [${rkmessage.pointed.partition}] " +
                            "offset ${rkmessage.pointed.offset}: " +
                            "${rd_kafka_message_errstr(rkmessage)}"
            }
            println(errorMessage)
            if (rkmessage.pointed.err === RD_KAFKA_RESP_ERR__UNKNOWN_PARTITION ||
                rkmessage.pointed.err === RD_KAFKA_RESP_ERR__UNKNOWN_TOPIC) {
                throw RuntimeException(errorMessage)
            }
            return emptyList()
        }
        val key = rkmessage.pointed.key?.let{keySerializer.deserialize(it.readBytes(rkmessage.pointed.key_len.toInt()) )}
        val value = rkmessage.pointed.payload?.let{valueSerializer.deserialize(it.readBytes(rkmessage.pointed.len.toInt()) )}
        return listOf(
            ConsumerRecord(
                rd_kafka_topic_name(rkmessage.pointed.rkt)?.toKString(),
                rkmessage.pointed.partition,
                rkmessage.pointed.offset,
                0, TimestampType.NO_TIMESTAMP_TYPE,
                rkmessage.pointed.key_len.toInt(),
                rkmessage.pointed.len.toInt(),
                key, value, null, null))
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

    override fun poll(timeout:Duration?): Iterable<ConsumerRecord<K, V>> {
        val rkmessage = rd_kafka_consumer_poll(consumerHandle, timeout?.inWholeMilliseconds?.toInt()?:0) // non-blocking poll
        rkmessage?.let{
            val records = consume(rkmessage)
            rd_kafka_message_destroy(rkmessage)
            return records
        }
        return emptyList()
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