package com.icemachined.kafka.clients.consumer

/**
 * Consumer Callback
 */
interface ConsumerRecordHandler<K, V> {
    fun handle(record: ConsumerRecord<K, V>)
}
