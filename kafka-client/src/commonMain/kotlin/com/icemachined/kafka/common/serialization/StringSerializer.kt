package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers

/**
 * StringSerializer
 */
class StringSerializer : Serializer<String> {
    override fun serialize(
        data: String,
        topic: String?,
        headers: Headers?
    ): ByteArray? =
            data.encodeToByteArray()
}
