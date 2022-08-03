package com.icemachined.kafka.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class StopWatch {
    private var startTime:Instant? = null;
    fun start(){
        startTime = Clock.System.now()
    }
    fun stop() = Clock.System.now().minus(startTime!!)
}
