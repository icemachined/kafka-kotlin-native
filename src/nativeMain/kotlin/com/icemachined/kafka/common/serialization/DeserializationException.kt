package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.common.header.Header
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient



@Serializable
data class DeserializationExceptionData(
    val message: String?,
    val data: ByteArray,
    @Contextual
    val cause: Throwable?
) {
    fun getException() = DeserializationException(message, data, cause)
}


class DeserializationException(
        message: String?,
        private val  data: ByteArray,
        cause: Throwable?
) : RuntimeException(message, cause) {

    @Transient
    lateinit var headers: Iterable<Header>
    @Transient
    lateinit var record: ConsumerRecord<Any, Any>

    fun getSerializable() = DeserializationExceptionData(message, data, cause)
}