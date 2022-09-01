package com.icemachined.kafka.clients

import librdkafka.*
import platform.posix.size_t
import platform.posix.stdout

import kotlinx.cinterop.*
import kotlinx.coroutines.delay

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
        rd_kafka_conf_set_log_cb(resultConfHandle, log_cb = staticCFunction(::logCallback))

        if (errors.isNotEmpty()) {
            rd_kafka_conf_destroy(conf)
            throw RuntimeException("Error setting producer configuration: ${errors.joinToString(", ")}")
        }
        return resultConfHandle
    }

    suspend fun waitKafkaDestroyed(timeout: Long, repeats: Int): Boolean {
        var run = repeats
        while (run.dec() > 0 && rd_kafka_wait_destroyed(0) == -1) {
            println("Waiting for librdkafka to decommission")
            delay(timeout)
        }
        return run <= 0
    }

    fun kafkaDump(rk: CValuesRef<rd_kafka_t>) {
        rd_kafka_dump(stdout?.reinterpret(), rk)
    }
}

fun logCallback(
    rk: CPointer<rd_kafka_t>?,
    level: Int,
    fac: CPointer<ByteVar>?,
    buf: CPointer<ByteVar>?
) {
    println("level=$level , facility=${fac?.toKString()}, message=${buf?.toKString()}")
}
