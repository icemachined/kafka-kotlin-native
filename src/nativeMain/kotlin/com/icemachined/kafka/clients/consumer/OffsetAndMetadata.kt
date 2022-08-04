package com.icemachined.kafka.clients.consumer

data class OffsetAndMetadata(
    val offset:ULong,
    val leaderEpoch:Int? = null,
    val metadata:String? = ""
)
