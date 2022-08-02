package com.icemachined.kafka.clients

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import librdkafka.*
import platform.posix.size_t

object KafkaUtils {
    fun setupConfig(entries: Set<Map.Entry<String, String>>): CPointer<rd_kafka_conf_t> {
        val buf = ByteArray(512)
        val strBufSize = (buf.size - 1).convert<size_t>()
        val conf = rd_kafka_conf_new()
        val resultConfHandle = conf ?: run {
            throw RuntimeException("Failed to create configuration for producer")
        }
        val errors = ArrayList<String>()
        buf.usePinned { punnedBuf ->
            val bufPointer = punnedBuf.addressOf(0)
            entries.forEach {
                val error = rd_kafka_conf_set(
                    resultConfHandle, it.key, it.value, bufPointer, strBufSize
                )
                if (error != RD_KAFKA_CONF_OK) {
                    errors.add(punnedBuf.get().decodeToString())
                }
            }
        }
        if (errors.isNotEmpty()) {
            rd_kafka_conf_destroy(conf)
            throw RuntimeException("Error setting producer configuration: ${errors.joinToString(", ")}")
        }
        return resultConfHandle
    }
}