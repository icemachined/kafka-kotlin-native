package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers

/**
 * An interface for converting bytes to objects.
 *
 * A class that implements this interface is expected to have a constructor with no parameters.
 *
 *
 * @param <T> Type to be deserialized into.
</T> */
interface Deserializer<T> {
    /**
     * Configure this class.
     *
     * @param configs configs in key/value pairs
     * @param isKey whether is for key or value
     */
    fun configure(configs: Map<String?, *>?, isKey: Boolean) {
        // intentionally left blank
    }

    /**
     * Deserialize a record value from a byte array into a value or object.
     *
     * @param topic topic associated with the data
     * @param headers headers associated with the record; may be empty.
     * @param data serialized bytes; may be null; implementations are recommended to handle null by returning a value or null rather than throwing an exception.
     * @return deserialized typed data; may be null
     */
    fun deserialize(
        data: ByteArray,
        topic: String? = null,
        headers: Headers? = null
    ): T?
}
