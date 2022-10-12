package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers

/**
 * JsonDeserializer
 */
class JsonDeserializer : Deserializer<Any> {
    override fun deserialize(data: ByteArray, topic: String?, headers: Headers?): Any {
        return ""
    }
}