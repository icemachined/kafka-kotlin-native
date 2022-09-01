package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.common.header.Header

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Suppress("AVOID_USING_UTILITY_CLASS")
object SerializeUtils {
    fun retrieveHeaderAsString(headers: List<Header>, headerName: String): String? {
        val header = headers.lastHeader(headerName)
        return header?.value?.decodeToString()
    }

    fun getExceptionFromHeader(
        record: ConsumerRecord<*, *>,
        headerName: String
    ): DeserializationException? {
        val header = record.headers?.lastHeader(headerName)
        return header?.value?.let {
            val ex = Json.decodeFromString<DeserializationExceptionData>(it.decodeToString()).getException()
            val headers = record.headers.filter { it.key != headerName }
            ex.headers = headers
            return ex
        }
    }
}
fun List<Header>.lastHeader(name: String) = this.findLast { it.key == name }
