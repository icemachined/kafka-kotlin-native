package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers

class StringDeserializer: Deserializer<String> {
    override fun deserialize(data: ByteArray, topic: String?, headers: Headers?): String =
        data.decodeToString()
}