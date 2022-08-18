package com.icemachined.kafka.common

import com.icemachined.kafka.common.Node

/**
 * This is used to describe per-partition state in the MetadataResponse.
 */
data class PartitionInfo(
    /**
     * The topic name
     */
    val topic: String?,
    /**
     * The partition id
     */
    val partition: Int,
    /**
     * The node id of the node currently acting as a leader for this partition or null if there is no leader
     */
    val leader: Node?,
    /**
     * The complete set of replicas for this partition regardless of whether they are alive or up-to-date
     */
    val replicas: Array<Node>?,
    /**
     * The subset of the replicas that are in sync, that is caught-up to the leader and ready to take over as leader if
     * the leader should fail
     */
    val inSyncReplicas: Array<Node>?,
    /**
     * The subset of the replicas that are offline
     */
    val offlineReplicas: Array<Node> = emptyArray()
)