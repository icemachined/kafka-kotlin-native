package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.clients.consumer.Headers

/**
 * An interface for converting objects to bytes.
 *
 * A class that implements this interface is expected to have a constructor with no parameter.
 *
 * @param <T> Type to be serialized from.
</T> */
interface Serializer<T> {
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
     * Convert `data` into a byte array.
     *
     * @param topic topic associated with data
     * @param headers headers associated with the record
     * @param data typed data
     * @return serialized bytes
     */
    fun serialize(
        data: T,
        topic: String? = null,
        headers: Headers? = null
    ): ByteArray?
}
