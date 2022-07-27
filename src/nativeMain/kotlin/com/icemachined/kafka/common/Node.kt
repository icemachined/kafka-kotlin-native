/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.icemachined.kafka.common

/**
 * Information about a Kafka node
 */
data class Node(
    /**
     * The node id of this node
     */
    val id: Int,
    /**
     * The host name for this node
     */
    val host: String?,
    /**
     * The port for this node
     */
    val port: Int,
    /**
     * The rack for this node
     */
    val rack: String? = null) {


    /**
     * Check whether this node is empty, which may be the case if noNode() is used as a placeholder
     * in a response payload with an error.
     * @return true if it is, false otherwise
     */
    val isEmpty: Boolean
        get() = host.isNullOrEmpty() || port < 0

    companion object {
        private val NO_NODE = Node(-1, "", -1)
        fun noNode(): Node {
            return NO_NODE
        }
    }
}