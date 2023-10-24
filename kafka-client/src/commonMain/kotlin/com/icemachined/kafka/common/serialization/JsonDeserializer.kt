package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.Header
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.header.RecordHeader

import kotlinx.cinterop.toKString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * JsonDeserializer
 */
class JsonDeserializer<T>(
    private val typeResolver: TypeResolver,
    private val typeHeaderName: String = KafkaHeaders.KTYPE_ID
) : Deserializer<T> {
    override fun deserialize(
        data: ByteArray,
        topic: String?,
        headers: Headers?
    ): T? {
        try {
            return tryDeserialize(data, topic, headers)
        } catch (ex: Exception) {
            val exceptionJson =
                    Json.encodeToString(
                        DeserializationExceptionData("Can't deserialize: ${ex.message}", data, ex)
                    )
            headers?.add(RecordHeader(KafkaHeaders.DESERIALIZER_EXCEPTION_VALUE, exceptionJson.encodeToByteArray()))
        }
        return null
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun tryDeserialize(
        data: ByteArray,
        topic: String?,
        headers: MutableList<Header>?
    ): T {
        val typeCode = headers?.lastHeader(typeHeaderName)?.value?.toKString()
        val targetType = typeResolver.resolve(typeCode, topic)
        return Json.decodeFromString(serializer(targetType), data.toKString()) as T
    }
}
