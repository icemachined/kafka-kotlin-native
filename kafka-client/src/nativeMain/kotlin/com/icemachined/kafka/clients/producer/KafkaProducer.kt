/**
 *  Kafka producer
 */

package com.icemachined.kafka.clients.producer

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.KafkaNativeProperties
import com.icemachined.kafka.clients.setupKafkaConfig
import com.icemachined.kafka.common.PartitionInfo
import com.icemachined.kafka.common.serialization.Serializer

import librdkafka.*
import mu.KotlinLogging
import platform.posix.size_t

import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@SharedImmutable
internal val isPollingActive = atomic(true)

/**
 * Kafka producer polling job, for polling kafka ack or reject on send
 */
@Suppress("DEBUG_PRINT")
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
                            // non-blocking
                            rd_kafka_poll(producerHandle, 0)
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

/**
 * Kafka producer implements standard interface wrapping to native library calls
 */
@Suppress(
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "DEBUG_PRINT",
    "MAGIC_NUMBER"
)
class KafkaProducer<K, V>(
    private val producerConfig: KafkaNativeProperties,
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
        val configHandle = setupKafkaConfig(producerConfig)
        rd_kafka_conf_set_dr_msg_cb(configHandle, staticCFunction(::messageDeliveryCallback))

        val buf = ByteArray(512)
        val strSize: size_t = (buf.size - 1).convert()
        val rk =
                buf.usePinned { rd_kafka_new(rd_kafka_type_t.RD_KAFKA_PRODUCER, configHandle, it.addressOf(0), strSize) }
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

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    override fun send(record: ProducerRecord<K, V>): SharedFlow<SendResult> {
        val key = record.key?.let { keySerializer.serialize(it, record.topic, record.headers) }
        val keySize: size_t = (key?.size ?: 0).convert()
        val value = record.value?.let { valueSerializer.serialize(it, record.topic, record.headers) }
        val valueSize: size_t = (value?.size ?: 0).convert()
        val flow = MutableSharedFlow<SendResult>()
        val pinnedKey = key?.pin()
        val pinnedValue = value?.pin()
        try {
            val keyPointer = pinnedKey?.addressOf(0)
            val valuePointer = pinnedValue?.addressOf(0)
            val flowPointer = StableRef.create(flow.freeze()).asCPointer()
            println("flowPointer = $flowPointer")
            val headersPointer = getNativeHeaders(record)
            trySend(record.topic, keyPointer, keySize, valuePointer, valueSize, headersPointer, flowPointer)
        } finally {
            pinnedKey?.unpin()
            pinnedValue?.unpin()
        }
        return flow
    }

    @Suppress("TOO_MANY_PARAMETERS")
    private fun trySend(
        topic: String,
        keyPointer: CPointer<ByteVar>?,
        keySize: size_t,
        valuePointer: CPointer<ByteVar>?,
        valueSize: size_t,
        headersPointer: CPointer<rd_kafka_headers_t>?,
        flowPointer: COpaquePointer
    ) {
        do {
            val err = kafka_send(
                producerHandle,
                topic,
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
                println("Enqueued message ($valueSize bytes) for topic $topic")
            } else {
                log.error { "Failed to produce to topic $topic: ${rd_kafka_err2str(err)?.toKString()}" }
                if (err == RD_KAFKA_RESP_ERR__QUEUE_FULL) {
                    rd_kafka_poll(producerHandle, 1000)
                }
            }
        } while (err == RD_KAFKA_RESP_ERR__QUEUE_FULL)
    }

    private fun getNativeHeaders(record: ProducerRecord<K, V>) =
            record.headers?.let {headers ->
                val nativeHeaders = rd_kafka_headers_new(headers.size.convert())
                headers.forEach { header ->
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
        doFlush(flushTimeoutMs)
    }

    private fun doFlush(timeout: Int) {
        rd_kafka_flush(producerHandle, timeout)
    }

    override fun partitionsFor(topic: String): List<PartitionInfo> {
        TODO("Not yet implemented")
    }

    override fun close(timeout: Duration) {
        println("flushing on close")

        /* 1) Make sure all outstanding requests are transmitted and handled. */
        doFlush(timeout.toInt(DurationUnit.MILLISECONDS))

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

/**
 * message delivery callback for kafka producer
 *
 * @param rk
 * @param rkMessage
 * @param opaque
 */
@Suppress("DEBUG_PRINT")
internal fun messageDeliveryCallback(
    rk: CPointer<rd_kafka_t>?,
    rkMessage: CPointer<rd_kafka_message_t>?,
    opaque: COpaquePointer?
) {
    println("got msg_opaque=${rkMessage?.pointed?._private}")
    val flow = rkMessage?.pointed?._private?.asStableRef<MutableSharedFlow<SendResult>>()
        ?.get()
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
