package com.icemachined.kafka.clients

/**
 * Configurations shared by Kafka client applications: producer, consumer, connect, etc.
 */
object CommonConfigNames {
    const val BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers"
    const val LOG_LEVEL_NATIVE = "log_level"
    const val CLIENT_DNS_LOOKUP_CONFIG = "client.dns.lookup"
    const val METADATA_MAX_AGE_CONFIG = "metadata.max.age.ms"
    const val SEND_BUFFER_CONFIG = "send.buffer.bytes"
    const val SEND_BUFFER_LOWER_BOUND = -1
    const val RECEIVE_BUFFER_CONFIG = "receive.buffer.bytes"
    const val RECEIVE_BUFFER_LOWER_BOUND = -1
    const val CLIENT_ID_CONFIG = "client.id"
    const val CLIENT_RACK_CONFIG = "client.rack"
    const val RECONNECT_BACKOFF_MS_CONFIG = "reconnect.backoff.ms"
    const val RECONNECT_BACKOFF_MAX_MS_CONFIG = "reconnect.backoff.max.ms"
    const val RETRIES_CONFIG = "retries"
    const val RETRY_BACKOFF_MS_CONFIG = "retry.backoff.ms"
    const val METRICS_SAMPLE_WINDOW_MS_CONFIG = "metrics.sample.window.ms"
    const val METRICS_NUM_SAMPLES_CONFIG = "metrics.num.samples"
    const val METRICS_RECORDING_LEVEL_CONFIG = "metrics.recording.level"
    const val METRIC_REPORTER_CLASSES_CONFIG = "metric.reporters"
    const val METRICS_CONTEXT_PREFIX = "metrics.context."
    const val SECURITY_PROTOCOL_CONFIG = "security.protocol"
    const val DEFAULT_SECURITY_PROTOCOL = "PLAINTEXT"
    const val SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG = "socket.connection.setup.timeout.ms"
    const val DEFAULT_SOCKET_CONNECTION_SETUP_TIMEOUT_MS = 10 * 1000L
    const val SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG = "socket.connection.setup.timeout.max.ms"
    const val DEFAULT_SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS = 30 * 1000L
    const val CONNECTIONS_MAX_IDLE_MS_CONFIG = "connections.max.idle.ms"
    const val REQUEST_TIMEOUT_MS_CONFIG = "request.timeout.ms"
    const val DEFAULT_LIST_KEY_SERDE_INNER_CLASS = "default.list.key.serde.inner"
    const val DEFAULT_LIST_VALUE_SERDE_INNER_CLASS = "default.list.value.serde.inner"
    const val DEFAULT_LIST_KEY_SERDE_TYPE_CLASS = "default.list.key.serde.type"
    const val DEFAULT_LIST_VALUE_SERDE_TYPE_CLASS = "default.list.value.serde.type"
    const val GROUP_ID_CONFIG = "group.id"
    const val GROUP_INSTANCE_ID_CONFIG = "group.instance.id"
    const val MAX_POLL_INTERVAL_MS_CONFIG = "max.poll.interval.ms"
    const val REBALANCE_TIMEOUT_MS_CONFIG = "rebalance.timeout.ms"
    const val SESSION_TIMEOUT_MS_CONFIG = "session.timeout.ms"
    const val HEARTBEAT_INTERVAL_MS_CONFIG = "heartbeat.interval.ms"
    const val DEFAULT_API_TIMEOUT_MS_CONFIG = "default.api.timeout.ms"
}
