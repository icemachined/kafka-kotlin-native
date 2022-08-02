/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.icemachined.kafka.common.serialization

import com.icemachined.kafka.common.header.Header

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
    fun serialize(data: T, topic: String? = null, headers: Iterable<Header>? = null): ByteArray?
}