package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.common.header.Header

class DeserializationException(
        message: String,
        val data: ByteArray,
        cause: Throwable
) : RuntimeException(message, cause) {

    lateinit var headers: Iterable<Header>
    lateinit var record: ConsumerRecord<Any, Any>
}