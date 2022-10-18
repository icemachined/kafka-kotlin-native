package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.header.RecordHeader
import com.icemachined.kafka.common.logDebug
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

/**
 * JsonSerializer
 */
class JsonSerializer<T>(
    private val typesMap: Map<String, KType>
) : Serializer<T> {

    override fun serialize(data: T, topic: String?, headers: Headers?): ByteArray? {
        val typeName = data!!::class.qualifiedName
        logDebug("JsonSerializer", "typeName=$typeName")
        return typesMap[typeName]?.let {
            logDebug("JsonSerializer", "adding type: $typeName to ${KafkaHeaders.KType_ID} header")
            headers?.add(RecordHeader(KafkaHeaders.KType_ID, typeName?.encodeToByteArray()))
            Json.encodeToString(serializer(it), data).encodeToByteArray()
        }
    }
}