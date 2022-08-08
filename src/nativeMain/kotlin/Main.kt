import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.*
import com.icemachined.kafka.clients.producer.KafkaProducer
import com.icemachined.kafka.clients.producer.ProducerRecord
import com.icemachined.kafka.common.header.Header
import com.icemachined.kafka.common.serialization.Serializer
import com.icemachined.kafka.common.serialization.Deserializer
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import librdkafka.*
import platform.posix.size_t

//import platform.posix.size_t

fun dr_msg_cb(
    rk: kotlinx.cinterop.CPointer<librdkafka.rd_kafka_t /* = cnames.structs.rd_kafka_s */>?,
    rkmessage: kotlinx.cinterop.CPointer<librdkafka.rd_kafka_message_t /* = librdkafka.rd_kafka_message_s */>?,
    opaque: kotlinx.cinterop.COpaquePointer? /* = kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>? */
): kotlin.Unit {
    if (rkmessage?.pointed?.err != 0) {
        println("Message delivery failed: ${rd_kafka_err2str(rkmessage?.pointed?.err ?: 0)}")
    } else {
        println("Message delivered ( ${rkmessage?.pointed?.len} bytes, partition ${rkmessage?.pointed?.partition}")
    }
}

fun main(args: Array<String>) {
    val producerConfig = mapOf(CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to "DESKTOP-E1SB1S9.local:9092")
    val producer = KafkaProducer(producerConfig, object : Serializer<String> {
        override fun serialize(data: String, topic: String?, headers: Iterable<Header>?): ByteArray? =
            data.encodeToByteArray()
    }, object : Serializer<String> {
        override fun serialize(data: String, topic: String?, headers: Iterable<Header>?): ByteArray? =
            data.encodeToByteArray()
    })
    runBlocking {
        launch(Dispatchers.Default) {
            println("Start consume")
            val consumerService = KafkaConsumerService(
                ConsumerConfig(
                    listOf("kkn-test"),
                    mapOf(
                        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to "DESKTOP-E1SB1S9.local:9092",
                        CommonConfigNames.CLIENT_ID_CONFIG to "test-consumer",
                        CommonConfigNames.GROUP_ID_CONFIG to "test-consumer-group",
                        ConsumerConfigNames.ENABLE_AUTO_COMMIT_CONFIG to "false",
                        ConsumerConfigNames.AUTO_OFFSET_RESET_CONFIG to "earliest"
                    ),
                    object : Deserializer<String> {
                        override fun deserialize(data: ByteArray, topic: String?, headers: Iterable<Header>?): String {
                            return data.decodeToString()
                        }
                    },
                    object : Deserializer<String> {
                        override fun deserialize(data: ByteArray, topic: String?, headers: Iterable<Header>?): String {
                            return data.decodeToString()
                        }
                    },
                    object : ConsumerRecordHandler<String, String> {
                        override fun handle(record: ConsumerRecord<String, String>) {
                            println("Key : ${record.key}, Value : ${record.value}")
                        }
                    }
                )
            )
            consumerService.start()
            println("Start delay")
            yield()
            delay(1000)
            println("Sending messages")
            val flow = producer.send(ProducerRecord("kkn-test", "new producer test", "test key"))
            println("Got result ${flow.first()}")
            val flow1 = producer.send(ProducerRecord("kkn-test", "new producer test 1", "test key"))
            println("Got result ${flow1.first()}")
            producer.close()
            yield()
            delay(10000)
            println("End delay")
            consumerService.stop()
        }
    }
}

fun produceExample() {
    val brokers = "d00665536.local:9092" /*args[1]*/;
    val topic = "kkn-test" /*args[2]*/;
    val payload = "some text" /*args[3]*/;

    /*
     * Create Kafka client configuration place-holder
     */
    val conf = rd_kafka_conf_new();

    /* Set bootstrap broker(s) as a comma-separated list of
     * host or host:port (default port 9092).
     * librdkafka will use the bootstrap brokers to acquire the full
     * set of brokers from the cluster. */
    val buf = ByteArray(512)
    val strBufSize = (buf.size - 1).toULong()
    if (buf.usePinned {
            rd_kafka_conf_set(
                conf, "bootstrap.servers", brokers, it.addressOf(0), strBufSize
            )
            rd_kafka_conf_set(
                conf, "log_level", "7", it.addressOf(0), strBufSize
            )
        } != RD_KAFKA_CONF_OK) {
        throw RuntimeException("Error setting bootstrap.servers property: ${buf.decodeToString()}")
    }
    rd_kafka_conf_set_dr_msg_cb(conf, staticCFunction(::dr_msg_cb))

    /*
     * Create producer instance.
     *
     * NOTE: rd_kafka_new() takes ownership of the conf object
     *       and the application must not reference it again after
     *       this call.
     */
    val rk = buf.usePinned { rd_kafka_new(rd_kafka_type_t.RD_KAFKA_PRODUCER, conf, it.addressOf(0), strBufSize) }
    rk ?: run {
        println("Failed to create new producer: ${buf.decodeToString()}")
    }
    val maxRetryCount = 10
    var retryCount = 0
    var err = 0
    do {
        err =
            rd_kafka_producev(
                /* Producer handle */
                rk,
                /* Topic name */
                rd_kafka_vtype_t.RD_KAFKA_VTYPE_TOPIC, topic.cstr,
                /* Make a copy of the payload. */
                rd_kafka_vtype_t.RD_KAFKA_VTYPE_MSGFLAGS, RD_KAFKA_MSG_F_COPY,
                /* Message value and length */
                rd_kafka_vtype_t.RD_KAFKA_VTYPE_VALUE, payload.cstr, payload.cstr.size.convert<size_t>(),
                /* Per-Message opaque, provided in
 * delivery report callback as
 * msg_opaque. */
                rd_kafka_vtype_t.RD_KAFKA_VTYPE_OPAQUE, null,
                /* End sentinel */
                RD_KAFKA_V_END
            )
        if (err != 0) {
            println("Failed to produce to topic $topic: ${rd_kafka_err2str(err)}")
            rd_kafka_poll(
                rk,
                1000 /*block for max 1000ms*/
            )
        }
    } while (err != 0 && retryCount < maxRetryCount)
    if (retryCount >= maxRetryCount && err != 0) {
        println("Failed to produce to topic $topic. Stop retrying")
    } else if (err == 0) {
        println("%% Enqueued message ($strBufSize bytes) for topic $topic")
    }

    rd_kafka_poll(rk, 0 /*non-blocking*/);

    println("Flushing final messages..");
    rd_kafka_flush(rk, 10 * 1000 /* wait for max 10 seconds */);

    /* If the output queue is still not empty there is an issue
     * with producing messages to the clusters. */
    if (rd_kafka_outq_len(rk) > 0)
        println("${rd_kafka_outq_len(rk)} message(s) were not delivered");

    /* Destroy the producer instance */
    rd_kafka_destroy(rk);
}
