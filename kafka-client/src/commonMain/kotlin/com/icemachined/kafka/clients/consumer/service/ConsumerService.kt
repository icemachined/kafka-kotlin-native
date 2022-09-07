package com.icemachined.kafka.clients.consumer.service

/**
 * Consumer Service
 */
interface ConsumerService {
    suspend fun start()
    suspend fun stop()
}
