package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.KafkaHeaders
import kotlinx.cinterop.toKString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

@Serializable
data class Test (
    val text:String = ""
)

/**
 * JsonDeserializer
 */
class JsonDeserializer<T>(private val typeMap: Map<String, KType>) : Deserializer<T> {

    override fun deserialize(data: ByteArray, topic: String?, headers: Headers?): T? {
        val typeId = headers?.lastHeader(KafkaHeaders.KType_ID)?.value?.toKString()
        return typeId?.let {
            typeMap[typeId]?.let {targetType ->
                Json.decodeFromString(serializer(targetType), data.toKString())
            }
        } as T
    }
}