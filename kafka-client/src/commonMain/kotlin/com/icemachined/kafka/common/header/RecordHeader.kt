/*
    
*/

package com.icemachined.kafka.common.header

import kotlinx.cinterop.toKString

data class RecordHeader(
    override val key: String?,
    override val value: ByteArray?
) : Header {
    val valueStr: String?
        get() = value?.toKString()
}
