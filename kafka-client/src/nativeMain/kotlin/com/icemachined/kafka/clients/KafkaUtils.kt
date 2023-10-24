/**
 * Kafka utility functions
 */

@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "MAGIC_NUMBER"
)

package com.icemachined.kafka.clients

import com.icemachined.kafka.common.*

import librdkafka.*
import platform.posix.size_t
import platform.posix.stdout

import kotlin.native.concurrent.freeze
import kotlinx.cinterop.*
import kotlinx.coroutines.delay

typealias KafkaNativeProperties = Map<String, String>

/**
 *  DefaultKafkaLogger
 */
@Suppress("DEBUG_PRINT")
class DefaultKafkaLogger(private val maxLogLevel: LogLevel) : KafkaClientLogger {
    override fun logMessage(
        level: LogLevel,
        facility: String?,
        message: String?,
        exception: Throwable?
    ) {
        if (level <= maxLogLevel) {
            println("${level.name} [$facility]: $message")
            exception?.printStackTrace()
        }
    }
}

/**
 * construct output string for rd_kafka_topic_partition_list_t data
 *
 * @return rd_kafka_topic_partition_list_t string presentation for log
 */
@OptIn(ExperimentalForeignApi::class)
fun rd_kafka_topic_partition_list_t.toLogString(): String {
    val partitions: Array<String?> = arrayOfNulls(this.size)
    for (i in 0 until this.size) {
        val elem = this.elems!![i]
        partitions[i] = "${elem.topic?.toKString()} [${elem.partition}] offset ${elem.offset}"
    }
    return partitions.joinToString()
}

/**
 * create and setup native kafka konfiguration
 *
 * @param kafkaProperties
 * @return pointer to native kafka config structure
 * @throws RuntimeException
 */
@OptIn(ExperimentalForeignApi::class)
@Suppress("MAGIC_NUMBER", "GENERIC_VARIABLE_WRONG_DECLARATION")
fun setupKafkaConfig(kafkaProperties: KafkaNativeProperties): CPointer<rd_kafka_conf_t> {
    val buf = ByteArray(512)
    val strBufSize: size_t = (buf.size - 1).convert()
    val conf = rd_kafka_conf_new()
    val resultConfHandle = conf ?: run {
        throw RuntimeException("Failed to create configuration for producer")
    }
    val errors = ArrayList<String>()
    buf.usePinned { punnedBuf ->
        val bufPointer = punnedBuf.addressOf(0)
        kafkaProperties.forEach {
            val error = rd_kafka_conf_set(
                resultConfHandle, it.key, it.value, bufPointer, strBufSize
            )
            if (error != RD_KAFKA_CONF_OK) {
                errors.add(punnedBuf.get().decodeToString())
            }
        }
    }
    rd_kafka_conf_set_log_cb(resultConfHandle, log_cb = staticCFunction(::kafkaLogCallback))

    if (errors.isNotEmpty()) {
        rd_kafka_conf_destroy(conf)
        throw RuntimeException("Error setting producer configuration: ${errors.joinToString(", ")}")
    }
    return resultConfHandle
}

/**
 *  wait for kafka being destroyed
 *
 * @param timeout
 * @param repeats
 * @throws KafkaTimeoutException
 */
suspend fun waitKafkaDestroyed(timeout: Long, repeats: Int) {
    var run = repeats
    while (run > 0 && rd_kafka_wait_destroyed(0) == -1) {
        logInfo("waitKafkaDestroyed", "Waiting for librdkafka to destroy")
        delay(timeout)
        run = run.dec()
    }
    if (run <= 0) {
        throw KafkaTimeoutException("Kafka haven't been destroyed in ${timeout * repeats} millis")
    }
}

/**
 * kafka native library dump
 *
 * @param kafkaInstance
 */
@OptIn(ExperimentalForeignApi::class)
fun kafkaDump(kafkaInstance: CValuesRef<rd_kafka_t>) {
    rd_kafka_dump(stdout?.reinterpret(), kafkaInstance)
}

/**
 * callback for native library logging
 *
 * @param kafkaInstance
 * @param level
 * @param fac
 * @param buf
 */
@OptIn(ExperimentalForeignApi::class)
fun kafkaLogCallback(
    kafkaInstance: CPointer<rd_kafka_t>?,
    level: Int,
    fac: CPointer<ByteVar>?,
    buf: CPointer<ByteVar>?
) {
    kafkaLogger.value?.logMessage(LogLevel.values()[level], fac?.toKString(), buf?.toKString())
}

/**
 * init kafka logger with default logger
 *
 * @param maxLogLevel
 */
fun initKafkaLoggerDefault(maxLogLevel: LogLevel) {
    kafkaLogger.value = DefaultKafkaLogger(maxLogLevel)
}
