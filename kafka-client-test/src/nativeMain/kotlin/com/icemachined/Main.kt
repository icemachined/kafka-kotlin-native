/**
 * Example of producer and consumer in subproject
 */

package com.icemachined

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.*
import com.icemachined.kafka.clients.consumer.service.*
import com.icemachined.kafka.clients.initKafkaLoggerDefault
import com.icemachined.kafka.clients.producer.KafkaProducer
import com.icemachined.kafka.clients.producer.ProducerRecord
import com.icemachined.kafka.clients.producer.SendResult
import com.icemachined.kafka.common.LogLevel
import com.icemachined.kafka.common.header.RecordHeader
import com.icemachined.kafka.common.logInfo
import com.icemachined.kafka.common.serialization.JsonDeserializer
import com.icemachined.kafka.common.serialization.JsonSerializer
import com.icemachined.kafka.common.serialization.StringDeserializer
import com.icemachined.kafka.common.serialization.StringSerializer

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

abstract class TaskCore() {
    abstract var id: Int
}
@Serializable
class DiktatSuite(): TaskCore() {
    override var id: Int = 0
    var diktatFeature: Int = 0
    constructor(diktatFeature: Int, id: Int):this() {
        this.diktatFeature = diktatFeature
        this.id = id
    }
}
@Serializable
class DetectSuite(): TaskCore() {
    override var id: Int = 0
    var detectFeature: Int = 0
    constructor(detectFeature: Int, id: Int):this() {
        this.detectFeature = detectFeature
        this.id = id
    }
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "DEBUG_PRINT",
    "MAGIC_NUMBER",
    "TYPE_ALIAS"
)
fun main(args: Array<String>) {
    initKafkaLoggerDefault(LogLevel.DEBUG)
    val producerConfig = mapOf(
        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to "localhost:29092",
        CommonConfigNames.CLIENT_ID_CONFIG to "test-consumer",
        CommonConfigNames.LOG_LEVEL_NATIVE to "7"
    )
    val typesMap = mapOf(DiktatSuite!!::class.qualifiedName!! to typeOf<DiktatSuite>(),
        DetectSuite!!::class.qualifiedName!! to typeOf<DetectSuite>())
    val producer = KafkaProducer(producerConfig, StringSerializer(), JsonSerializer<TaskCore>(typesMap))
    runBlocking {
        launch {
            println("Start consume")
            val consumerService = KafkaParallelGroupsConsumerService(
                ConsumerConfig(
                    listOf("kkn-serialized-test"),
                    mapOf(
                        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to "localhost:29092",
                        CommonConfigNames.CLIENT_ID_CONFIG to "test-consumer",
                        CommonConfigNames.GROUP_ID_CONFIG to "test-consumer-group",
                        CommonConfigNames.LOG_LEVEL_NATIVE to "7",
                        ConsumerConfigNames.ENABLE_AUTO_COMMIT_CONFIG to "false",
                        ConsumerConfigNames.AUTO_OFFSET_RESET_CONFIG to "earliest"

                    ),
                    StringDeserializer(),
                    JsonDeserializer(typesMap),
                    object : ConsumerRecordHandler<String, TaskCore> {
                        override fun handle(record: ConsumerRecord<String, TaskCore>) {
                            println("Key : ${record.key}, Value : ${record.value}, Headers: ${record.headers}")
                        }
                    }
                ),
                12
            )
            consumerService.start()
            println("Start delay")
            yield()
            delay(1000)
            logInfo("main", "Sending messages")
            val flows: ArrayList<SharedFlow<SendResult>> = ArrayList(10)
            for (i in 0..10) {
                val payload = if (i % 2 == 0) DiktatSuite(i + 1, i) else DetectSuite(i - 1, i)
                val flow = producer.send(
                    ProducerRecord(
                        "kkn-serialized-test", payload, "test key$i",
                        headers = mutableListOf(RecordHeader("test.header.name", "test header value".encodeToByteArray()))
                    )
                )
                flows.add(flow)
            }
            for (i in 0..10) {
                logInfo("main", "Start waiting $i")
                logInfo("main", "Got $i result ${flows[i].first()}")
            }
            producer.close()
            yield()
            delay(10000)
            println("End delay")
            consumerService.stop()
        }
    }
}
