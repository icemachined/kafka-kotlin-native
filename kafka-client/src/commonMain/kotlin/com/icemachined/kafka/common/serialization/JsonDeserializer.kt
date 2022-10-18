package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.logDebug
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

/**
 * JsonDeserializer
 */
class JsonDeserializer<T>(
    private val typesMap: Map<String, KType>
) : Deserializer<T> {

    override fun deserialize(data: ByteArray, topic: String?, headers: Headers?): T? {
        val typeId = headers?.lastHeader(KafkaHeaders.KType_ID)?.value?.toKString()
        logDebug("JsonDeserializer", "Extracted typeId header: $typeId")
        return typeId?.let {
            typesMap[typeId]?.let { targetType ->
                Json.decodeFromString(serializer(targetType), data.toKString())
            }
        } as T
    }
}