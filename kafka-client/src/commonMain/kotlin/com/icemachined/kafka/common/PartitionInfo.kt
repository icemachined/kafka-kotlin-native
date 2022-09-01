package com.icemachined.kafka.common

/**
 * This is used to describe per-partition state in the MetadataResponse.
 * @property topic The topic name
 * @property partition The partition id
 * @property leader The node id of the node currently acting as a leader for this partition or null if there is no leader
 * @property replicas The complete set of replicas for this partition regardless of whether they are alive or up-to-date
 * @property inSyncReplicas The subset of the replicas that are in sync, that is caught-up to the leader and ready to take over as leader if
 * the leader should fail
 * @property offlineReplicas The subset of the replicas that are offline
 */
data class PartitionInfo(
    val topic: String?,
    val partition: Int,
    val leader: Node?,
    val replicas: Array<Node>?,
    val inSyncReplicas: Array<Node>?,
    val offlineReplicas: Array<Node> = emptyArray()
)
