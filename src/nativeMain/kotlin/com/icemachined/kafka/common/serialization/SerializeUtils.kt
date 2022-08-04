package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.common.header.Header
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.nio.charset.StandardCharsets

object SerializeUtils {

    fun retrieveHeaderAsString(headers: Iterable<Header>, headerName: String): String? {
        val header = headers.lastHeader(headerName)
        return header?.value()?.let { String(it, StandardCharsets.UTF_8) }
    }

    fun getExceptionFromHeader(
        record: ConsumerRecord<*, *>,
        headerName: String?
    ): DeserializationException? {
        val header: Header? = record.headers().lastHeader(headerName)
        if (header != null) {
            try {
                val ex = ObjectInputStream(
                        ByteArrayInputStream(header.value)
                ).readObject() as TfDeserializationException
                val headers: List<Header> = ArrayList(record.headers)
                headers.remove(headerName)
                ex.headers = headers
                return ex
            } catch (e: IOException) {
                println("Failed to deserialize a deserialization exception", e)
            } catch (e: ClassNotFoundException) {
                println("Failed to deserialize a deserialization exception", e)
            } catch (e: ClassCastException) {
                println("Failed to deserialize a deserialization exception", e)
            }
        }
        return null
    }
}