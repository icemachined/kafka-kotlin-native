import kotlinx.cinterop.*
import librdkafka.*

fun main(args: Array<String>) {
    val brokers = args[1];
    val topic   = args[2];


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
                conf, "bootstrap.servers", brokers, it.addressOf(0),
                (buf.size - 1).toULong()

        ) } != RD_KAFKA_CONF_OK ) {
        println("Error setting bootstrap.servers property: ${buf.decodeToString()}")
    }
    println("Ok setting bootstrap.servers property")
}