package com.icemachined.kafka.clients.consumer

/**
 * The consumer configuration keys
 */
object ConsumerConfigNames  {
        const val MAX_POLL_RECORDS_CONFIG = "max.poll.records"
        const val ENABLE_AUTO_COMMIT_CONFIG = "enable.auto.commit"
        const val AUTO_COMMIT_INTERVAL_MS_CONFIG = "auto.commit.interval.ms"
        const val PARTITION_ASSIGNMENT_STRATEGY_CONFIG = "partition.assignment.strategy"
        const val AUTO_OFFSET_RESET_CONFIG = "auto.offset.reset"
        const val FETCH_MIN_BYTES_CONFIG = "fetch.min.bytes"
        const val FETCH_MAX_BYTES_CONFIG = "fetch.max.bytes"
        const val DEFAULT_FETCH_MAX_BYTES = 50 * 1024 * 1024
        const val FETCH_MAX_WAIT_MS_CONFIG = "fetch.max.wait.ms"
        const val MAX_PARTITION_FETCH_BYTES_CONFIG = "max.partition.fetch.bytes"
        const val DEFAULT_MAX_PARTITION_FETCH_BYTES = 1 * 1024 * 1024
        const val CHECK_CRCS_CONFIG = "check.crcs"
        const val INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes"
        const val EXCLUDE_INTERNAL_TOPICS_CONFIG = "exclude.internal.topics"
        const val ISOLATION_LEVEL_CONFIG = "isolation.level"
        const val ALLOW_AUTO_CREATE_TOPICS_CONFIG = "allow.auto.create.topics"
}