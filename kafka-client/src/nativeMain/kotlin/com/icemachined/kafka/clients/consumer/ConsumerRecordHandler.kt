package com.icemachined.kafka.clients.consumer

interface ConsumerRecordHandler<K,V> {
    fun handle(record: ConsumerRecord<K, V>)
}
