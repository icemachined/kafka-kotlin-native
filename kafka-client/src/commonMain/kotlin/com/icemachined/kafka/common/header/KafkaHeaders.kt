package com.icemachined.kafka.common.header

object KafkaHeaders {
    const val PREFIX: String = "_kafka_"
    const val DLT_ORIGINAL_TOPIC: String = PREFIX + "dlt-original-topic"
    const val DLT_ORIGINAL_PARTITION: String = PREFIX + "dlt-original-partition"
    const val DLT_ORIGINAL_OFFSET: String = PREFIX + "dlt-original-offset"
    const val DLT_ORIGINAL_TIMESTAMP: String = PREFIX + "dlt-original-timestamp"
    const val DLT_ORIGINAL_TIMESTAMP_TYPE: String = PREFIX + "dlt-original-timestamp-type"
    const val DLT_EXCEPTION_FQCN: String = PREFIX + "dlt-exception-fqcn"
    const val DLT_EXCEPTION_MESSAGE: String = PREFIX + "dlt-exception-message"
    const val API_VERSION = "__KafkaApiVersion__"
    const val KTYPE_ID = "__KTypeId__"
    const val MESSAGE_ID = "__KafkaMessageId__"
    const val PRODUCER_ID: String = "__ProducerId__"
    const val DESERIALIZER_EXCEPTION_VALUE = "DeserializerExceptionValue"
}
