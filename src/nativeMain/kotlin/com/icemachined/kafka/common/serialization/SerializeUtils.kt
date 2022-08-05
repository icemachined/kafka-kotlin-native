package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.common.header.Header

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