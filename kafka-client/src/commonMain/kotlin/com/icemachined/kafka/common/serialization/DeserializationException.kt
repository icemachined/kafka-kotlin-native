/**
 *  Deserualization exception with it's dto
 */

package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.clients.consumer.Headers
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @property message
 * @property data
 * @property cause
 */
@Serializable
data class DeserializationExceptionData(
    val message: String?,
    val data: ByteArray,
    @Contextual
    val cause: Throwable?
) {
    fun getException() = DeserializationException(message, data, cause)
}

/**
 * Deserialization Exception
 */
class DeserializationException(
    message: String?,
    private val data: ByteArray,
    cause: Throwable?
) : RuntimeException(message, cause) {
    @Transient
    lateinit var headers: Headers

    @Transient
    lateinit var record: ConsumerRecord<Any, Any>

    fun getSerializable() = DeserializationExceptionData(message, data, cause)
}
