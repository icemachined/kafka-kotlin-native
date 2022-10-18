package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers

/**
 * StringDeserializer
 */
class StringDeserializer : Deserializer<String> {
    override fun deserialize(
        data: ByteArray,
        topic: String?,
        headers: Headers?
    ): String =
            data.decodeToString()
}
