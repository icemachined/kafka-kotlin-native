package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.header.RecordHeader

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * JsonSerializer
 */
class JsonSerializer<T>(
    private val typeResolver: TypeResolver,
    private val typeCodeResolver: TypeCodeResolver<Any> = defaultTypeCodeResolver(),
    private val typeHeaderName: String = KafkaHeaders.KTYPE_ID
) : Serializer<T> {
    override fun serialize(
        data: T,
        topic: String?,
        headers: Headers?
    ): ByteArray? =
            data?.let {
                val typeCode = typeCodeResolver.resolve(data)
                headers?.add(RecordHeader(typeHeaderName, typeCode.encodeToByteArray()))
                return Json.encodeToString(serializer(typeResolver.resolve(typeCode, topic)), data).encodeToByteArray()
            }
}
