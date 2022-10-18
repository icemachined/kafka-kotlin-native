package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.header.RecordHeader

import kotlin.reflect.KType
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * JsonSerializer
 */
class JsonSerializer<T>(
    private val typesMap: Map<String, KType>
) : Serializer<T> {
    override fun serialize(
        data: T,
        topic: String?,
        headers: Headers?
    ): ByteArray? {
        val typeName = data!!::class.qualifiedName
        return typesMap[typeName]?.let {
            headers?.add(RecordHeader(KafkaHeaders.KTYPE_ID, typeName?.encodeToByteArray()))
            Json.encodeToString(serializer(it), data).encodeToByteArray()
        }
    }
}
