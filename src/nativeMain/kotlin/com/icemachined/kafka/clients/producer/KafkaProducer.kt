package com.icemachined.kafka.clients.producer

import com.icemachined.kafka.common.serialization.Serializer
import kotlinx.cinterop.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import librdkafka.*
import org.apache.kafka.common.PartitionInfo
import platform.posix.size_t
import kotlin.time.Duration


class KafkaProducer<K, V>(
    private val producerConfig: Map<String, String>,
    private val keySerializer: Serializer<K>,
    private val valueSerializer: Serializer<V>,
    private val maxRetryCount: Int = 10
) : Producer<K, V> {
    private val producerHandle: CPointer<rd_kafka_t>
    private val configHandle: CPointer<rd_kafka_conf_t>

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

    }

    private fun messageDeliveryCallback(
        rk: CPointer<rd_kafka_t>?,
        rkMessage: CPointer<rd_kafka_message_t>?,
        opaque: COpaquePointer?
    ) {
        val flow = opaque?.asStableRef<MutableStateFlow<SendResult>>()?.get()
        if (rkMessage?.pointed?.err != 0) {
            val errorMessage = rd_kafka_err2str(rkMessage?.pointed?.err ?: 0)?.toKString()
            println("Message delivery failed: $errorMessage")
            flow?.value = SendResult(false, errorMessage)
        } else {
            println("Message delivered ( ${rkMessage?.pointed?.len} bytes, partition ${rkMessage?.pointed?.partition}")
            flow?.value = SendResult(true)
        }
    }

    override fun send(record: ProducerRecord<K, V>): StateFlow<SendResult> {
        val key = record.key?.let { keySerializer.serialize(record.topic, record.headers, it) }
        val keySize = key?.size?.convert<size_t>() ?: 0
        val value = record.value?.let { valueSerializer.serialize(record.topic, record.headers, it) }
        val valueSize = value?.size?.convert<size_t>() ?: 0
        var retryCount = 0
        var err = 0
        var pKey: Pinned<ByteArray>? = null
        var pValue: Pinned<ByteArray>? = null
        val flow = MutableStateFlow<SendResult?>(null)
        try {
            pKey = key?.pin()
            val pKeyPointer = pKey?.addressOf(0)
            pValue = value?.pin()
            val pValuePointer = pValue?.addressOf(0)
            val flowPointer = StableRef.create(flow).asCPointer()
            do {
                ++retryCount
                err =
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
                if (err != 0) {
                    println("Failed to produce to topic ${record.topic}: ${rd_kafka_err2str(err)?.toKString()}")
                    rd_kafka_poll(
                        producerHandle,
                        1000 /*block for max 1000ms*/
                    )
                }
            } while (err != 0 && retryCount < maxRetryCount)
        } finally {
            pKey?.unpin()
            pValue?.unpin()
        }
        if (retryCount >= maxRetryCount && err != 0) {
            println("Failed to produce to topic $topic. Stop retrying")
        } else if (err == 0) {
            println("%% Enqueued message ($strBufSize bytes) for topic $topic")
        }
        return flow
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun partitionsFor(topic: String): List<PartitionInfo> {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun close(timeout: Duration) {
        TODO("Not yet implemented")
    }
}