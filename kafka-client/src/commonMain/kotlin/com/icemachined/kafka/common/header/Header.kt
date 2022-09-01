package com.icemachined.kafka.common.header

/**
 * Header interface
 */
interface Header {
    val key: String?
    val value: ByteArray?
}
