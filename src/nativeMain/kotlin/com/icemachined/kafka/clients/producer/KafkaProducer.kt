package com.icemachined.kafka.clients.producer

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import librdkafka.*
import org.apache.kafka.common.PartitionInfo
import platform.posix.size_t
import kotlin.time.Duration


class SendResultCallback(private val flow: MutableStateFlow<SendResult>) {
    fun dr_msg_cb(
        rk: kotlinx.cinterop.CPointer<librdkafka.rd_kafka_t /* = cnames.structs.rd_kafka_s */>?,
        rkmessage: kotlinx.cinterop.CPointer<librdkafka.rd_kafka_message_t /* = librdkafka.rd_kafka_message_s */>?,
        opaque: kotlinx.cinterop.COpaquePointer? /* = kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>? */
    ): kotlin.Unit {
        if (rkmessage?.pointed?.err != 0) {
            val errorMessage = rd_kafka_err2str(rkmessage?.pointed?.err ?: 0)?.toKString()
            println("Message delivery failed: $errorMessage")
            flow.value = SendResult(false, errorMessage)
        } else {
            println("Message delivered ( ${rkmessage?.pointed?.len} bytes, partition ${rkmessage?.pointed?.partition}")
            flow.value = SendResult(true)
        }
    }
}


class KafkaProducer<K, V>(
    private val producerConfig: Map<String, String>
) : Producer<K, V> {
    private val conf: CPointer<rd_kafka_conf_t>
    private val flow = MutableStateFlow(SendResult(false))

    init {
        conf = rd_kafka_conf_new()!!;
        val buf = ByteArray(512)
        val strBufSize = (buf.size - 1).convert<size_t>()
        val errors = ArrayList<String>()
        buf.usePinned { punnedBuf ->
            val bufPointer = punnedBuf.addressOf(0)
            producerConfig.entries.forEach {
                val error = rd_kafka_conf_set(
                    conf, it.key, it.value, bufPointer, strBufSize
                )
                if (error != RD_KAFKA_CONF_OK) {
                    errors.add(punnedBuf.get().decodeToString())
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw RuntimeException("Error setting producer configuration: ${errors.joinToString(", ")}")
        }
        rd_kafka_conf_set_dr_msg_cb(conf, staticCFunction(::dr_msg_cb))

    }

    fun dr_msg_cb(
        rk: kotlinx.cinterop.CPointer<librdkafka.rd_kafka_t /* = cnames.structs.rd_kafka_s */>?,
        rkmessage: kotlinx.cinterop.CPointer<librdkafka.rd_kafka_message_t /* = librdkafka.rd_kafka_message_s */>?,
        opaque: kotlinx.cinterop.COpaquePointer? /* = kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>? */
    ): kotlin.Unit {
        if (rkmessage?.pointed?.err != 0) {
            val errorMessage = rd_kafka_err2str(rkmessage?.pointed?.err ?: 0)?.toKString()
            println("Message delivery failed: $errorMessage")
            flow.value = SendResult(false, errorMessage)
        } else {
            println("Message delivered ( ${rkmessage?.pointed?.len} bytes, partition ${rkmessage?.pointed?.partition}")
            flow.value = SendResult(true)
        }
    }

    override fun send(record: ProducerRecord<K, V>?): StateFlow<SendResult> {
        return flow
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun partitionsFor(topic: String): List<PartitionInfo>? {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun close(timeout: Duration) {
        TODO("Not yet implemented")
    }
}