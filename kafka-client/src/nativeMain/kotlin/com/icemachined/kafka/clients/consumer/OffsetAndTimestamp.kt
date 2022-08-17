package com.icemachined.kafka.clients.consumer

/**
 * A container class for offset and timestamp.
 */
data class OffsetAndTimestamp(
    private val timestamp: ULong,
    private val offset: UInt,
    private val leaderEpoch: Int? = null
)
