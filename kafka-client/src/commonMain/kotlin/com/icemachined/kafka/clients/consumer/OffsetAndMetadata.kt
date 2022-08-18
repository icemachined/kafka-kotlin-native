package com.icemachined.kafka.clients.consumer

/**
 * @property offset
 * @property leaderEpoch
 * @property metadata
 */
data class OffsetAndMetadata(
    val offset: ULong,
    val leaderEpoch: Int? = null,
    val metadata: String? = ""
)
