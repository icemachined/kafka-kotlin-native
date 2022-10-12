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
import com.icemachined.kafka.common.header.Header
import com.icemachined.kafka.common.header.RecordHeader
import com.icemachined.kafka.common.logInfo
import com.icemachined.kafka.common.serialization.Deserializer
import com.icemachined.kafka.common.serialization.Serializer

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

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
    val producer = KafkaProducer(producerConfig, object : Serializer<String> {
        override fun serialize(
            data: String,
            topic: String?,
            headers: Headers?
        ): ByteArray? =
                data.encodeToByteArray()
    }, object : Serializer<String> {
        override fun serialize(
            data: String,
            topic: String?,
            headers: Headers?
        ): ByteArray? =
                data.encodeToByteArray()
    })
    runBlocking {
        launch {
            println("Start consume")
            val consumerService = KafkaParallelGroupsConsumerService(
                ConsumerConfig(
                    listOf("kkn-parallel-test"),
                    mapOf(
                        CommonConfigNames.BOOTSTRAP_SERVERS_CONFIG to "localhost:29092",
                        CommonConfigNames.CLIENT_ID_CONFIG to "test-consumer",
                        CommonConfigNames.GROUP_ID_CONFIG to "test-consumer-group",
                        CommonConfigNames.LOG_LEVEL_NATIVE to "7",
                        ConsumerConfigNames.ENABLE_AUTO_COMMIT_CONFIG to "false",
                        ConsumerConfigNames.AUTO_OFFSET_RESET_CONFIG to "earliest"

                    ),
                    object : Deserializer<String> {
                        override fun deserialize(data: ByteArray, topic: String?, headers: Headers?): String =
                                data.decodeToString()
                    },
                    object : Deserializer<String> {
                        override fun deserialize(data: ByteArray, topic: String?, headers: Headers?): String =
                                data.decodeToString()
                    },
                    object : ConsumerRecordHandler<String, String> {
                        override fun handle(record: ConsumerRecord<String, String>) {
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
                val flow = producer.send(
                    ProducerRecord(
                        "kkn-parallel-test", "new producer test$i", "test key$i",
                        headers = listOf(RecordHeader("test.header.name", "test header value".encodeToByteArray()))
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
