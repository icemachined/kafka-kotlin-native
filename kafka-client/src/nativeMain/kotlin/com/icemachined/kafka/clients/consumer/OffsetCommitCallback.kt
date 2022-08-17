package com.icemachined.kafka.clients.consumer

import com.icemachined.kafka.common.TopicPartition

/**
 * A callback interface that the user can implement to trigger custom actions when a commit request completes. The callback
 * may be executed in any thread calling [poll()][Consumer.poll].
 */
interface OffsetCommitCallback {
    /**
     * A callback method the user can implement to provide asynchronous handling of commit request completion.
     * This method will be called when the commit request sent to the server has been acknowledged.
     *
     * @param offsets A map of the offsets and associated metadata that this callback applies to
     * @param exception The exception thrown during processing of the request, or null if the commit completed successfully
     *
     */
    fun onComplete(offsets: Map<TopicPartition, OffsetAndMetadata>, exception: Exception?)
}
