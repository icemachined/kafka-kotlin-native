package com.icemachined.kafka.common

/**
 * Timeout exception for waiting kafka processes to finish
 */
class KafkaTimeoutException(message: String) : Exception(message)
