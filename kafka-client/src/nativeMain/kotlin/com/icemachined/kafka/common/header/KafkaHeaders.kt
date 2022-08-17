package com.icemachined.kafka.common.header

object KafkaHeaders {
    val PREFIX: String = "_kafka_"
    val DLT_ORIGINAL_TOPIC: String = PREFIX + "dlt-original-topic"
    val DLT_ORIGINAL_PARTITION: String = PREFIX + "dlt-original-partition"
    val DLT_ORIGINAL_OFFSET: String = PREFIX + "dlt-original-offset"
    val DLT_ORIGINAL_TIMESTAMP: String = PREFIX + "dlt-original-timestamp"
    val DLT_ORIGINAL_TIMESTAMP_TYPE: String = PREFIX + "dlt-original-timestamp-type"
    val DLT_EXCEPTION_FQCN: String = PREFIX + "dlt-exception-fqcn"
    val DLT_EXCEPTION_MESSAGE: String = PREFIX + "dlt-exception-message"
    val API_VERSION = "__KafkaApiVersion__"
    val CLASS_ID = "__TypeId__"
    val MESSAGE_ID = "__KafkaMessageId__"
    val PRODUCER_ID: String = "__ProducerId__"
    val DESERIALIZER_EXCEPTION_VALUE = "DeserializerExceptionValue"
}
