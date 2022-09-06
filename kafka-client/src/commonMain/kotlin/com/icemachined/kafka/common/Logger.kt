/**
 * Stub for logging
 */

package com.icemachined.kafka.common

import kotlin.native.concurrent.AtomicReference

/**
 * Log a message to the [stream] with timestamp and specific [level]
 *
 * @param level log level
 * @param msg a message string
 * @param stream output stream (file, stdout, stderr)
 */

interface KafkaClientLogger {
    fun logMessage(
        level: Int,
        facility: String?,
        message: String?
    )
}

@SharedImmutable
val kafkaLogger: AtomicReference<KafkaClientLogger?> = AtomicReference(null)

/**
 * Log a message with info level
 *
 * @param msg a message string
 */
inline fun <reified T> T.logInfo(msg: String) {
    kafkaLogger.value?.logMessage(6, T::class.toString(), msg)
}

/**
 * Log a message with error level
 *
 * @param msg a message string
 */
inline fun <reified T> T.logError(msg: String) {
    kafkaLogger.value?.logMessage(3, T::class.toString(), msg)
}

/**
 * Log a message with warn level
 *
 * @param msg a message string
 */
inline fun <reified T> T.logWarn(msg: String) {
    kafkaLogger.value?.logMessage(4, T::class.toString(), msg)
}

/**
 * Log a message with debug level
 *
 * @param msg a message string
 */
inline fun <reified T> T.logDebug(msg: String) {
    kafkaLogger.value?.logMessage(7, T::class.toString(), msg)
}

/**
 * Log a message with trace level
 *
 * @param msg a message string
 */
inline fun <reified T> T.logTrace(msg: String) {
    kafkaLogger.value?.logMessage(8, T::class.toString(), msg)
}
