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
    val errstr = ArrayList<Byte>()
    errstr.ensureCapacity(512)
    if (rd_kafka_conf_set(conf, "bootstrap.servers", brokers, errstr.toByteArray(),
            errstr.size) != RD_KAFKA_CONF_OK) {
        println(errstr.toByteArray().toString());
    }
}