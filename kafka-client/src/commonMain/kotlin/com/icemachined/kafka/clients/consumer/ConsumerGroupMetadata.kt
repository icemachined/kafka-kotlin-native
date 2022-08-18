package com.icemachined.kafka.clients.consumer

/**
 * @property groupId
 * @property generationId
 * @property memberId
 * @property groupInstanceId
 */
data class ConsumerGroupMetadata(
    val groupId: String,
    val generationId: Int = -1,
    val memberId: String = "",
    val groupInstanceId: String? = null
)
