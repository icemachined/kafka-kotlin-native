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
    val producerConfig = mapOf(
        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to "d00665536.local:9092",
        CommonConfigNames.CLIENT_ID_CONFIG to "test-consumer",
        CommonConfigNames.LOG_LEVEL_NATIVE to "7"
    )
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
                        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to "d00665536.local:9092",
                        CommonConfigNames.CLIENT_ID_CONFIG to "test-consumer",
                        CommonConfigNames.GROUP_ID_CONFIG to "test-consumer-group",
                        CommonConfigNames.LOG_LEVEL_NATIVE to "7",
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
