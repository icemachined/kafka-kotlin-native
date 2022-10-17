package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.clients.consumer.Headers

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Suppress("AVOID_USING_UTILITY_CLASS")
object SerializeUtils {
    fun retrieveHeaderAsString(headers: Headers, headerName: String): String? {
        val header = headers.lastHeader(headerName)
        return header?.value?.decodeToString()
    }

    fun getExceptionFromHeader(
        record: ConsumerRecord<*, *>,
        headerName: String
    ): DeserializationException? {
        val header = record.headers?.lastHeader(headerName)
        return header?.value?.let {value ->
            val ex = Json.decodeFromString<DeserializationExceptionData>(value.decodeToString()).getException()
            val headers = record.headers.filter { it.key != headerName }.toMutableList()
            ex.headers = headers
            return ex
        }
    }
}

/**
 * find last header with a given name
 *
 * @param name
 * @return last header with a given name or null
 */
fun Headers.lastHeader(name: String) = this.findLast { it.key == name }
