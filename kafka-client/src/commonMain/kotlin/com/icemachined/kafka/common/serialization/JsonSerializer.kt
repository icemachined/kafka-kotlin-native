package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers
import com.icemachined.kafka.common.header.Header
import com.icemachined.kafka.common.header.KafkaHeaders
import com.icemachined.kafka.common.header.RecordHeader
import kotlinx.cinterop.toKString
import kotlinx.cinterop.typeOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType


/**
 * JsonSerializer
 */
inline class JsonSerializer<T>(
    private val typesMap:Map<String, KType>
    ) : Serializer<T> {

    override fun serialize(data: T, topic: String?, headers: Headers?): ByteArray? {
        val typeName = data!!::class.qualifiedName
        return typesMap[typeName]?.let{
            headers?.add(RecordHeader(KafkaHeaders.KType_ID, typeName?.encodeToByteArray()))
            Json.encodeToString(serializer( it ), data).encodeToByteArray()
        }
    }

}
