package com.icemachined.kafka.common.header

interface Header {
    val key: String
    val value: ByteArray
}