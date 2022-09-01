package com.icemachined.kafka.common.record

/**
 * The timestamp type of the records.
 * @property id
 */
enum class TimestampType(val id: Int) {
    CREATE_TIME(0),
    LOG_APPEND_TIME(1),
    NO_TIMESTAMP_TYPE(-1),
    ;
}
