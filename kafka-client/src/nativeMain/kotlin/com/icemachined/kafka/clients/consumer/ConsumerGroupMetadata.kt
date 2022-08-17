package com.icemachined.kafka.clients.consumer

data class ConsumerGroupMetadata(
    val groupId: String,
    val generationId: Int = -1,
    val memberId: String = "",
    val groupInstanceId: String? = null
)
