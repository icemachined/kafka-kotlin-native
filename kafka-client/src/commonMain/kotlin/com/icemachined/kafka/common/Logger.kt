/**
 * Stub for logging
 */

@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "MAGIC_NUMBER",
    "DEBUG_PRINT"
)

package com.icemachined.kafka.common

import kotlin.native.concurrent.AtomicReference

@SharedImmutable
val kafkaLogger: AtomicReference<KafkaClientLogger?> = AtomicReference(null)

/**
 * log levels - corresonds to syslog levels
 */
enum class LogLevel {
    ALERT, CRITICAL, DEBUG, EMERGENCY, ERROR, INFO, NOTICE, TRACE, WARN
}

/**
 * Log a message to the [stream] with timestamp and specific [level]
 *
 * @param level log level
 * @param message a message string
 * @param stream output stream (file, stdout, stderr)
 */
interface KafkaClientLogger {
    fun logMessage(
        level: LogLevel,
        facility: String?,
        message: String?,
        exception: Throwable? = null
    )
}

/**
 * Log a message with info level
 *
 * @param message a message string
 * @param facility
 */
inline fun <reified T> T.logKafkaInfo(message: String, facility: String = (T::class).simpleName!!) {
    logMessage(LogLevel.INFO, facility, message)
}

/**
 * Log a message
 *
 * @param level
 */

/**
 * Log a message with error level
 *
 * @param message a message string
 * @param facility
 * @param exception
 */
inline fun <reified T> T.logKafkaError(
    message: String,
    facility: String = (T::class).simpleName!!,
    exception: Throwable? = null
) {
    logError(facility, message, exception)
}

/**
 * Log a message with warn level
 *
 * @param message a message string
 * @param facility
 */
inline fun <reified T> T.logKafkaWarn(message: String, facility: String = (T::class).simpleName!!) {
    logWarn(facility, message)
}

/**
 * Log a message with debug level
 *
 * @param message a message string
 * @param facility
 */
inline fun <reified T> T.logKafkaDebug(message: String, facility: String = (T::class).simpleName!!) {
    logDebug(facility, message)
}

/**
 * Log a message with trace level
 *
 * @param message a message string
 * @param facility
 */
inline fun <reified T> T.logKafkaTrace(message: String, facility: String = (T::class).simpleName!!) {
    logTrace(facility, message)
}

/**
 * Log a message
 *
 * @param level
 * @param facility
 * @param message
 * @param exception
 */
inline fun logMessage(
    level: LogLevel,
    facility: String?,
    message: String?,
    exception: Throwable? = null
) {
    kafkaLogger.value?.logMessage(level, facility, message, exception)
}

/**
 * logInfo
 *
 * @param facility
 * @param message
 */
inline fun logInfo(
    facility: String,
    message: String
) {
    logMessage(LogLevel.INFO, facility, message)
}

/**
 * logError
 *
 * @param facility
 * @param message
 * @param exception
 */
inline fun logError(
    facility: String,
    message: String,
    exception: Throwable? = null
) {
    logMessage(LogLevel.ERROR, facility, message, exception)
}

/**
 * logWarn
 *
 * @param facility
 * @param message
 */
inline fun logWarn(
    facility: String,
    message: String
) {
    logMessage(LogLevel.WARN, facility, message)
}

/**
 * logDebug
 *
 * @param facility
 * @param message
 */
inline fun logDebug(
    facility: String,
    message: String
) {
    logMessage(LogLevel.DEBUG, facility, message)
}

/**
 * logTrace
 *
 * @param facility
 * @param message
 */
inline fun logTrace(
    facility: String,
    message: String
) {
    logMessage(LogLevel.TRACE, facility, message)
}
