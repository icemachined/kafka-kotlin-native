package com.icemachined.kafka.clients.producer

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.KafkaUtils
import com.icemachined.kafka.common.serialization.Serializer

import librdkafka.*
import mu.KotlinLogging
import org.apache.kafka.common.PartitionInfo
import platform.posix.size_t

import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@SharedImmutable
internal val isPollingActive = atomic(true)

class KafkaProducerPollingJob(
    private val producerHandle: CPointer<rd_kafka_t>,
    private val kafkaPollingIntervalMs: Long,
    private val coroutineDispatcher: CoroutineDispatcher
) {
    fun pollingCycle() =
            runBlocking {
                launch(coroutineDispatcher) {
                    try {
                        while (isPollingActive.value) {
                            delay(kafkaPollingIntervalMs)
                            rd_kafka_poll(producerHandle, 0 /* non-blocking*/)
                            println("producer poll happened")
                        }
                    } catch (e: CancellationException) {
                        println("poll cancelled it's ok")
                    } catch (e: Throwable) {
                        println("Unexpected exception in kafka polling job:")
                        e.printStackTrace()
                    } finally {
                        println("exiting poll ")
                    }
                }
            }
}

class KafkaProducer<K, V>(
    private val producerConfig: Map<String, String>,
    private val keySerializer: Serializer<K>,
    private val valueSerializer: Serializer<V>,
    private val flushTimeoutMs: Int = 10 * 1000,
    private val kafkaPollingIntervalMs: Long = 200
) : Producer<K, V> {
    private val log = KotlinLogging.logger {}
    private val pollingJobDispatcher: CloseableCoroutineDispatcher
    private val kafkaPollingJobFuture: Future<Job>
    private val producerHandle: CPointer<rd_kafka_t>
    private val worker: Worker
    private val clientId = producerConfig[CommonConfigNames.CLIENT_ID_CONFIG]!!

    init {
        val configHandle = KafkaUtils.setupConfig(producerConfig.entries)
        rd_kafka_conf_set_dr_msg_cb(configHandle, staticCFunction(::messageDeliveryCallback))

        val buf = ByteArray(512)
        val strBufSize = (buf.size - 1).convert<size_t>()
        val rk =
                buf.usePinned { rd_kafka_new(rd_kafka_type_t.RD_KAFKA_PRODUCER, configHandle, it.addressOf(0), strBufSize) }
        producerHandle = rk ?: run {
            throw RuntimeException("Failed to create new producer: ${buf.decodeToString()}")
        }
        worker = Worker.start(true, "kafka-producer-polling-worker-$clientId")
        pollingJobDispatcher = newSingleThreadContext("kafka-producer-polling-context-$clientId")
        kafkaPollingJobFuture =
                worker.execute(TransferMode.SAFE,
                    {
                        KafkaProducerPollingJob(
                            producerHandle, kafkaPollingIntervalMs,
                            pollingJobDispatcher
                        ).freeze()
                    }) { it.pollingCycle() }
    }

    override fun send(record: ProducerRecord<K, V>): SharedFlow<SendResult> {
        val key = record.key?.let { keySerializer.serialize(it, record.topic, record.headers) }
        val keySize = (key?.size ?: 0).convert<size_t>()
        val value = record.value?.let { valueSerializer.serialize(it, record.topic, record.headers) }
        val valueSize = (value?.size ?: 0).convert<size_t>()
        var pinnedKey: Pinned<ByteArray>? = null
        var pinnedValue: Pinned<ByteArray>? = null
        val flow = MutableSharedFlow<SendResult>()
        try {
            pinnedKey = key?.pin()
            val keyPointer = pinnedKey?.addressOf(0)
            pinnedValue = value?.pin()
            val valuePointer = pinnedValue?.addressOf(0)
            val flowPointer = StableRef.create(flow.freeze()).asCPointer()
            println("flowPointer = $flowPointer")
            val headersPointer = getNativeHeaders(record)
            do {
                val err = kafka_send(
                    producerHandle,
                    record.topic,
                    RD_KAFKA_PARTITION_UA,
                    RD_KAFKA_MSG_F_COPY,
                    keyPointer,
                    keySize,
                    valuePointer,
                    valueSize,
                    headersPointer,
                    flowPointer
                )
                if (err == 0) {
                    println("Enqueued message ($valueSize bytes) for topic ${record.topic}")
                } else {
                    log.error { "Failed to produce to topic ${record.topic}: ${rd_kafka_err2str(err)?.toKString()}" }
                    if (err == RD_KAFKA_RESP_ERR__QUEUE_FULL) {
                        rd_kafka_poll(
                            producerHandle,
                            1000 /* block for max 1000ms*/
                        )
                    }
                }
            } while (err == RD_KAFKA_RESP_ERR__QUEUE_FULL)
        } finally {
            pinnedKey?.unpin()
            pinnedValue?.unpin()
        }
        return flow
    }

    private fun getNativeHeaders(record: ProducerRecord<K, V>) =
            record.headers?.let {
                val nativeHeaders = rd_kafka_headers_new(it.size.convert())
                it.forEach { header ->
                    header.value?.usePinned { value ->
                        rd_kafka_header_add(
                            nativeHeaders, header.key, -1,
                            value.addressOf(0), header.value?.size?.convert() ?: 0
                        )
                    }
                }
                nativeHeaders
            }

    override fun flush() {
        flush(flushTimeoutMs)
    }

    private fun flush(timeout: Int) {
        rd_kafka_flush(producerHandle, timeout)
    }

    override fun partitionsFor(topic: String): List<PartitionInfo> {
        TODO("Not yet implemented")
    }

    override fun close() {
        close(1.toDuration(DurationUnit.MINUTES))
    }

    override fun close(timeout: Duration) {
        println("flushing on close")

        /* 1) Make sure all outstanding requests are transmitted and handled. */
        flush(timeout.toInt(DurationUnit.MILLISECONDS))

        /* If the output queue is still not empty there is an issue
         * with producing messages to the clusters. */
        if (rd_kafka_outq_len(producerHandle) > 0) {
            println("${rd_kafka_outq_len(producerHandle)} message(s) were not delivered")
        }

        stopWorker()

        /* 2) Destroy the topic and handle objects */
        rd_kafka_destroy(producerHandle)
    }

    private fun stopWorker() {
        println("stop polling")
        while (!isPollingActive.compareAndSet(true, false)) {
        }
        println("cancel and wait")
        runBlocking {
            kafkaPollingJobFuture.result.cancelAndJoin()
        }
        worker.requestTermination().result
        println("closing kafka producer")
    }
}

internal fun messageDeliveryCallback(
    rk: CPointer<rd_kafka_t>?,
    rkMessage: CPointer<rd_kafka_message_t>?,
    opaque: COpaquePointer?
) {
    println("got msg_opaque=${rkMessage?.pointed?._private}")
    val flow = rkMessage?.pointed?._private?.asStableRef<MutableSharedFlow<SendResult>>()?.get()
    val result: SendResult
    if (rkMessage?.pointed?.err != 0) {
        val errorMessage = rd_kafka_err2str(rkMessage?.pointed?.err ?: 0)?.toKString()
        println("Message delivery failed: $errorMessage")
        result = SendResult(false, errorMessage)
    } else {
        println("Message delivered ( ${rkMessage?.pointed?.len} bytes, partition ${rkMessage?.pointed?.partition}")
        result = SendResult(true)
    }
    println("start emit to flow $flow")
    runBlocking { flow?.emit(result) }
    println("emitted to flow")
}
