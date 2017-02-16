package mbc.core

/**
 * 区块链Node信息mnode://nodeId@ip:port. nodeId可以为空(byte[0])
 */
data class Node(val nodeId: String, val ip: String, val port: Int)
