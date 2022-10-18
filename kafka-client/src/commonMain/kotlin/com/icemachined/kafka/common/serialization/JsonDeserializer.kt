package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.KafkaHeaders

import kotlin.reflect.KType
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * JsonDeserializer
 */
class JsonDeserializer<T>(
    private val typesMap: Map<String, KType>
) : Deserializer<T> {
    override fun deserialize(
        data: ByteArray,
        topic: String?,
        headers: Headers?
    ): T? {
        val typeId = headers?.lastHeader(KafkaHeaders.KTYPE_ID)?.value?.toKString()
        return typeId?.let {
            typesMap[typeId]?.let { targetType ->
                Json.decodeFromString(serializer(targetType), data.toKString())
            }
        } as T
    }
}
