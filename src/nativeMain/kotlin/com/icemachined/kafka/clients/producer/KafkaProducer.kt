package com.icemachined.kafka.clients.producer

import com.icemachined.kafka.common.serialization.Serializer
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

class KafkaProducer<K, V>(
    private val producerConfig: Map<String, String>,
    private val keySerializer: Serializer<K>,
    private val valueSerializer: Serializer<V>,
    private val flushTimeoutMs: Int = 10 * 1000,
    private val kafkaPollingIntervalMs: Long = 200
) : Producer<K, V> {
    private var kafkaPollingJobFuture: Future<Job>
    private val log = KotlinLogging.logger {}
    private val producerHandle: CPointer<rd_kafka_t>
    private val configHandle: CPointer<rd_kafka_conf_t>
    private val worker: Worker

    init {
        val conf = rd_kafka_conf_new()
        configHandle = conf ?: run {
            throw RuntimeException("Failed to create configuration for producer")
        }
        val buf = ByteArray(512)
        val strBufSize = (buf.size - 1).convert<size_t>()
        val errors = ArrayList<String>()
        buf.usePinned { punnedBuf ->
            val bufPointer = punnedBuf.addressOf(0)
            producerConfig.entries.forEach {
                val error = rd_kafka_conf_set(
                    configHandle, it.key, it.value, bufPointer, strBufSize
                )
                if (error != RD_KAFKA_CONF_OK) {
                    errors.add(punnedBuf.get().decodeToString())
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw RuntimeException("Error setting producer configuration: ${errors.joinToString(", ")}")
        }
        rd_kafka_conf_set_dr_msg_cb(configHandle, staticCFunction(::messageDeliveryCallback))

        val rk =
            buf.usePinned { rd_kafka_new(rd_kafka_type_t.RD_KAFKA_PRODUCER, configHandle, it.addressOf(0), strBufSize) }
        producerHandle = rk ?: run {
            throw RuntimeException("Failed to create new producer: ${buf.decodeToString()}")
        }
        worker = Worker.start()
        kafkaPollingJobFuture = worker.execute(TransferMode.SAFE, {kafkaPollingIntervalMs to producerHandle}){ param ->
            runBlocking {
                launch {
                    while (this.isActive) {
                        delay(param.first)
                        rd_kafka_poll(param.second, 0 /*non-blocking*/);
                        //println("poll happened")
                    }
                }
            }
        }
    }

    override fun send(record: ProducerRecord<K, V>): SharedFlow<SendResult> {
        val key = record.key?.let { keySerializer.serialize(record.topic, record.headers, it) }
        val keySize = (key?.size?:0).convert<size_t>()
        val value = record.value?.let { valueSerializer.serialize(record.topic, record.headers, it) }
        val valueSize = (value?.size?:0).convert<size_t>()
        var pKey: Pinned<ByteArray>? = null
        var pValue: Pinned<ByteArray>? = null
        val flow = MutableSharedFlow<SendResult>()
        try {
            pKey = key?.pin()
            val pKeyPointer = pKey?.addressOf(0)
            pValue = value?.pin()
            val pValuePointer = pValue?.addressOf(0)
            val flowPointer = StableRef.create(flow.freeze()).asCPointer()
            println("flowPointer = $flowPointer")
            do {
                val err =
                    rd_kafka_producev(
                        /* Producer handle */
                        producerHandle,
                        /* Topic name */
                        rd_kafka_vtype_t.RD_KAFKA_VTYPE_TOPIC, record.topic.cstr,
                        /* Make a copy of the payload. */
                        rd_kafka_vtype_t.RD_KAFKA_VTYPE_MSGFLAGS, RD_KAFKA_MSG_F_COPY,
                        /* Message key and length */
                        rd_kafka_vtype_t.RD_KAFKA_VTYPE_KEY, pKeyPointer, keySize,
                        /* Message value and length */
                        rd_kafka_vtype_t.RD_KAFKA_VTYPE_VALUE, pValuePointer, valueSize,
                        /* Per-Message opaque, provided in
                         * delivery report callback as
                         * msg_opaque.
                         * */
                        rd_kafka_vtype_t.RD_KAFKA_VTYPE_OPAQUE, flowPointer,
                        /* End sentinel */
                        RD_KAFKA_V_END
                    )
                if (err == 0) {
                    println("%% Enqueued message ($valueSize bytes) for topic ${record.topic}")
                } else {
                    log.error { "Failed to produce to topic ${record.topic}: ${rd_kafka_err2str(err)?.toKString()}" }
                    if (err == RD_KAFKA_RESP_ERR__QUEUE_FULL) {
                        rd_kafka_poll(
                            producerHandle,
                            1000 /*block for max 1000ms*/
                        )
                    }
                }
            } while (err == RD_KAFKA_RESP_ERR__QUEUE_FULL)
        } finally {
            pKey?.unpin()
            pValue?.unpin()
        }
        return flow
    }

    override fun flush() {
        rd_kafka_flush(producerHandle, flushTimeoutMs);
    }

    override fun partitionsFor(topic: String): List<PartitionInfo> {
        TODO("Not yet implemented")
    }

    override fun close() {
        close(1.toDuration(DurationUnit.MINUTES))
        runBlocking { kafkaPollingJobFuture.result.cancelAndJoin() }
    }

    override fun close(timeout: Duration) {
        /* 1) Make sure all outstanding requests are transmitted and handled. */
        rd_kafka_flush(producerHandle, timeout.toInt(DurationUnit.MILLISECONDS)); /* One minute timeout */

        /* If the output queue is still not empty there is an issue
         * with producing messages to the clusters. */
        if (rd_kafka_outq_len(producerHandle) > 0)
            println("${rd_kafka_outq_len(producerHandle)} message(s) were not delivered");

        /* 2) Destroy the topic and handle objects */
        rd_kafka_destroy(producerHandle);
    }
}