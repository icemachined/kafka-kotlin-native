package com.icemachined.kafka.clients.consumer

data class OffsetAndMetadata(
    val offset:UInt,
    val leaderEpoch:Int? = null,
    val metadata:String? = ""
)
