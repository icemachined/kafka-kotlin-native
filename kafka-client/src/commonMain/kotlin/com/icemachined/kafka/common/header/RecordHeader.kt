package com.icemachined.kafka.common.header

/**
 * Record header data
 * @property key
 * @property value
 */
data class RecordHeader(
    override val key: String?,
    override val value: ByteArray?
) : Header
