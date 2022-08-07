package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.common.header.Header
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DeserializationException(
        message: String,
        val data: ByteArray,
        cause: Throwable
) : RuntimeException(message, cause) {

    @Transient
    lateinit var headers: Iterable<Header>
    @Transient
    lateinit var record: ConsumerRecord<Any, Any>
}