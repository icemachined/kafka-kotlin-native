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
package com.icemachined.kafka.clients.producer

class ProducerRecord<K, V>(
    topic: String,
    partition: Int?,
    timestamp: Long?,
    key: K?,
    value: V,
    headers: Iterable<Header>?
) {
    private val topic: String?
    private val partition: Int?
    private val headers: Headers?
    private val key: K?
    private val value: V?
    private val timestamp: Long?

    /**
     * Creates a record with a specified timestamp to be sent to a specified topic and partition
     *
     * @param topic The topic the record will be appended to
     * @param partition The partition to which the record should be sent
     * @param timestamp The timestamp of the record, in milliseconds since epoch. If null, the producer will assign
     * the timestamp using System.currentTimeMillis().
     * @param key The key that will be included in the record
     * @param value The record contents
     * @param headers the headers that will be included in the record
     */
    init {
        requireNotNull(topic) { "Topic cannot be null." }
        if (timestamp != null && timestamp < 0) throw IllegalArgumentException(
            String.format("Invalid timestamp: %d. Timestamp should always be non-negative or null.", timestamp)
        )
        if (partition != null && partition < 0) throw IllegalArgumentException(
            String.format("Invalid partition: %d. Partition number should always be non-negative or null.", partition)
        )
        this.topic = topic
        this.partition = partition
        this.key = key
        this.value = value
        this.timestamp = timestamp
        this.headers = RecordHeaders(headers)
    }

    /**
     * Creates a record with a specified timestamp to be sent to a specified topic and partition
     *
     * @param topic The topic the record will be appended to
     * @param partition The partition to which the record should be sent
     * @param timestamp The timestamp of the record, in milliseconds since epoch. If null, the producer will assign the
     * timestamp using System.currentTimeMillis().
     * @param key The key that will be included in the record
     * @param value The record contents
     */
    constructor(topic: String?, partition: Integer?, timestamp: Long?, key: K, value: V) : this(
        topic,
        partition,
        timestamp,
        key,
        value,
        null
    ) {
    }

    /**
     * Creates a record to be sent to a specified topic and partition
     *
     * @param topic The topic the record will be appended to
     * @param partition The partition to which the record should be sent
     * @param key The key that will be included in the record
     * @param value The record contents
     * @param headers The headers that will be included in the record
     */
    constructor(topic: String?, partition: Integer?, key: K, value: V, headers: Iterable<Header?>?) : this(
        topic,
        partition,
        null,
        key,
        value,
        headers
    ) {
    }

    /**
     * Creates a record to be sent to a specified topic and partition
     *
     * @param topic The topic the record will be appended to
     * @param partition The partition to which the record should be sent
     * @param key The key that will be included in the record
     * @param value The record contents
     */
    constructor(topic: String?, partition: Integer?, key: K, value: V) : this(
        topic,
        partition,
        null,
        key,
        value,
        null
    ) {
    }

    /**
     * Create a record to be sent to Kafka
     *
     * @param topic The topic the record will be appended to
     * @param key The key that will be included in the record
     * @param value The record contents
     */
    constructor(topic: String?, key: K, value: V) : this(topic, null, null, key, value, null) {}

    /**
     * Create a record with no key
     *
     * @param topic The topic this record should be sent to
     * @param value The record contents
     */
    constructor(topic: String?, value: V) : this(topic, null, null, null, value, null) {}

    /**
     * @return The topic this record is being sent to
     */
    fun topic(): String? {
        return topic
    }

    /**
     * @return The headers
     */
    fun headers(): Headers? {
        return headers
    }

    /**
     * @return The key (or null if no key is specified)
     */
    fun key(): K? {
        return key
    }

    /**
     * @return The value
     */
    fun value(): V? {
        return value
    }

    /**
     * @return The timestamp, which is in milliseconds since epoch.
     */
    fun timestamp(): Long? {
        return timestamp
    }

    /**
     * @return The partition to which the record will be sent (or null if no partition was specified)
     */
    fun partition(): Integer? {
        return partition
    }

    @Override
    override fun toString(): String {
        val headers = if (headers == null) "null" else headers.toString()
        val key = if (key == null) "null" else key.toString()
        val value = if (value == null) "null" else value.toString()
        val timestamp = if (timestamp == null) "null" else timestamp.toString()
        return "ProducerRecord(topic=" + topic + ", partition=" + partition + ", headers=" + headers + ", key=" + key + ", value=" + value +
                ", timestamp=" + timestamp + ")"
    }

    @Override
    override fun equals(o: Object): Boolean {
        if (this === o) return true else if (o !is ProducerRecord<*, *>) return false
        val that = o as ProducerRecord<*, *>
        return Objects.equals(key, that.key) &&
                Objects.equals(partition, that.partition) &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(value, that.value) &&
                Objects.equals(timestamp, that.timestamp)
    }

    @Override
    override fun hashCode(): Int {
        var result = topic?.hashCode() ?: 0
        result = 31 * result + if (partition != null) partition.hashCode() else 0
        result = 31 * result + if (headers != null) headers.hashCode() else 0
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }
}