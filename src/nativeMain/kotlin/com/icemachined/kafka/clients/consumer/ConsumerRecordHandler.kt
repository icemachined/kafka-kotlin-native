package com.db.tf.messaging.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord

interface ConsumerRecordHandler {
    fun handle(record: ConsumerRecord<String, Object>)
}
