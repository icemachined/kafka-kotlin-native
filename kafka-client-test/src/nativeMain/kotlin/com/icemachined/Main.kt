/**
 * Example of producer and consumer in subproject
 */

package com.icemachined

import com.icemachined.kafka.clients.CommonConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerConfig
import com.icemachined.kafka.clients.consumer.ConsumerConfigNames
import com.icemachined.kafka.clients.consumer.ConsumerRecord
import com.icemachined.kafka.clients.consumer.ConsumerRecordHandler
import com.icemachined.kafka.clients.consumer.service.KafkaParallelConsumerService
import com.icemachined.kafka.clients.initKafkaLoggerDefault
import com.icemachined.kafka.clients.producer.KafkaProducer
import com.icemachined.kafka.clients.producer.ProducerRecord
import com.icemachined.kafka.clients.producer.SendResult
import com.icemachined.kafka.common.LogLevel
import com.icemachined.kafka.common.header.RecordHeader
import com.icemachined.kafka.common.logDebug
import com.icemachined.kafka.common.logInfo
import com.icemachined.kafka.common.serialization.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable

@Suppress("MISSING_KDOC_TOP_LEVEL", "EMPTY_PRIMARY_CONSTRUCTOR")
abstract class TaskCore() {
    abstract var id: Int
}
@Suppress("MISSING_KDOC_TOP_LEVEL", "EMPTY_PRIMARY_CONSTRUCTOR")
@Serializable
class DiktatSuite() : TaskCore() {
    override var id: Int = 0
    var diktatFeature: Int = 0
    constructor(diktatFeature: Int, id: Int) : this() {
        this.diktatFeature = diktatFeature
        this.id = id
    }

    override fun toString(): String = "DiktatSuite(id=$id, diktatFeature=$diktatFeature)"
}
@Suppress("MISSING_KDOC_TOP_LEVEL", "EMPTY_PRIMARY_CONSTRUCTOR")
@Serializable
class DetectSuite() : TaskCore() {
    override var id: Int = 0
    var detectFeature: Int = 0
    constructor(detectFeature: Int, id: Int) : this() {
        this.detectFeature = detectFeature
        this.id = id
    }

    override fun toString(): String = "DetectSuite(id=$id, detectFeature=$detectFeature)"
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "DEBUG_PRINT",
    "MAGIC_NUMBER",
    "TYPE_ALIAS"
)
fun main(args: Array<String>) {
    initKafkaLoggerDefault(LogLevel.DEBUG)
    val bootstrapServers = if (args.isNotEmpty()) args[0] else "localhost:29092"
    val topicName = if (args.size > 1) args[1] else "kkn-serialized-test"
    logInfo("Main", "Starting test with bootstrapServers: $bootstrapServers and topic: $topicName")
    val producerConfig = mapOf(
        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        CommonConfigNames.CLIENT_ID_CONFIG to "test-consumer",
        CommonConfigNames.LOG_LEVEL_NATIVE to "7"
    )
    val typesMap = mapOf(serializeTypeOf<DiktatSuite>(), serializeTypeOf<DetectSuite>())
    logDebug("Main", "typesMap = $typesMap")
    val producer = KafkaProducer(producerConfig, StringSerializer(), JsonSerializer<TaskCore>(typesMap))
    runBlocking {
        launch {
            println("Start consume")
            val consumerService = KafkaParallelConsumerService(
                ConsumerConfig(
                    listOf(topicName),
                    mapOf(
                        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
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
                        topicName, payload, "test key$i",
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
