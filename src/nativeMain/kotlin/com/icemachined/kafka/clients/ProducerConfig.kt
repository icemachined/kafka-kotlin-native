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
package com.icemachined.kafka.clients


/**
 * Configuration for the Kafka Producer. Documentation for these configurations can be found in the [Kafka documentation](http://kafka.apache.org/documentation.html#producerconfigs)
 */
object ProducerConfig {
    const val METADATA_MAX_IDLE_CONFIG = "metadata.max.idle.ms"
    const val BATCH_SIZE_CONFIG = "batch.size"
    const val PARTITIONER_ADPATIVE_PARTITIONING_ENABLE_CONFIG = "partitioner.adaptive.partitioning.enable"
    const val PARTITIONER_AVAILABILITY_TIMEOUT_MS_CONFIG = "partitioner.availability.timeout.ms"
    const val PARTITIONER_IGNORE_KEYS_CONFIG = "partitioner.ignore.keys"
    const val ACKS_CONFIG = "acks"
    const val LINGER_MS_CONFIG = "linger.ms"
    const val DELIVERY_TIMEOUT_MS_CONFIG = "delivery.timeout.ms"
    const val MAX_REQUEST_SIZE_CONFIG = "max.request.size"
    const val MAX_BLOCK_MS_CONFIG = "max.block.ms"
    const val BUFFER_MEMORY_CONFIG = "buffer.memory"
    const val COMPRESSION_TYPE_CONFIG = "compression.type"
    const val MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = "max.in.flight.requests.per.connection"
    const val KEY_SERIALIZER_CLASS_CONFIG = "key.serializer"
    const val VALUE_SERIALIZER_CLASS_CONFIG = "value.serializer"
    const val PARTITIONER_CLASS_CONFIG = "partitioner.class"
    const val INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes"
    const val ENABLE_IDEMPOTENCE_CONFIG = "enable.idempotence"
    const val TRANSACTION_TIMEOUT_CONFIG = "transaction.timeout.ms"
    const val TRANSACTIONAL_ID_CONFIG = "transactional.id"
}