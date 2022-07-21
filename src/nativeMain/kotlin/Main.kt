import kotlinx.cinterop.*
import librdkafka.*

fun dr_msg_cb(rk:kotlinx.cinterop.CPointer<librdkafka.rd_kafka_t /* = cnames.structs.rd_kafka_s */>?,
rkmessage:kotlinx.cinterop.CPointer<librdkafka.rd_kafka_message_t /* = librdkafka.rd_kafka_message_s */>?,
opaque:kotlinx.cinterop.COpaquePointer? /* = kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>? */) : kotlin.Unit {
    if (rkmessage?.pointed?.err != 0) {
        println("Message delivery failed: ${rd_kafka_err2str(rkmessage?.pointed?.err?:0)}")
    } else {
        println("Message delivered ( ${rkmessage?.pointed?.len} bytes, partition ${rkmessage?.pointed?.partition}")
    }
}

fun main(args: Array<String>) {
//    val brokers = args[1];
//    val topic   = args[2];


    /*
     * Create Kafka client configuration place-holder
     */
    val conf = rd_kafka_conf_new();

    /* Set bootstrap broker(s) as a comma-separated list of
     * host or host:port (default port 9092).
     * librdkafka will use the bootstrap brokers to acquire the full
     * set of brokers from the cluster. */
    val buf = ByteArray(512)
    if (buf.usePinned {
        rd_kafka_conf_set(
                conf, "bootstrap.servers", "localhost:9092", it.addressOf(0),
                (buf.size - 1).toULong()

        ) } != RD_KAFKA_CONF_OK ) {
            throw RuntimeException("Error setting bootstrap.servers property: ${buf.decodeToString()}")
    }
    rd_kafka_conf_set_dr_msg_cb(conf, staticCFunction(::dr_msg_cb))
}