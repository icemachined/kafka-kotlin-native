/*
    
*/

package com.icemachined.kafka.common

/**
 * Information about a Kafka node
 * @property id The node id of this node
 * @property host The host name for this node
 * @property port The port for this node
 * @property rack The rack for this node
 */
data class Node(
    val id: Int,
    val host: String?,
    val port: Int,
    val rack: String? = null
) {
    /**
     * Check whether this node is empty, which may be the case if noNode() is used as a placeholder
     * in a response payload with an error.
     *
     * @return true if it is, false otherwise
     */
    val isEmpty: Boolean
        get() = host.isNullOrEmpty() || port < 0

    companion object {
        private val noNode = Node(-1, "", -1)
        fun noNode(): Node = noNode
    }
}
