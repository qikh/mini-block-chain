package mbc.network

import io.netty.channel.Channel
import mbc.core.Block
import mbc.core.BlockChainManager
import mbc.core.Node
import mbc.core.Transaction
import mbc.network.message.*
import mbc.util.NetworkUtil
import org.slf4j.LoggerFactory
import java.math.BigInteger

/**
 * 管理P2P连接，可以使PeerClient发起的连接，也可以是PeerServer收到的连接。
 */
class Peer(val node: Node, val manager: BlockChainManager, val channel: Channel) {

  private val logger = LoggerFactory.getLogger(Peer::class.java)

  var protocolVersion: Int? = null
  var networkId: Int? = null
  var totalDifficulty: BigInteger? = null
  var bestHash: ByteArray? = null
  var genesisHash: ByteArray? = null

  var handshakeComplete = false

  /**
   * 关闭Peer。
   */
  fun close() {
    this.channel.close()
  }

  /**
   * 建立连接后应该首先握手(发送并接收HELLO消息)。
   */
  fun sendHelloMessage() {
    val config = manager.blockChain.config

    val msg = HelloMessage(config.getPeerVersion(), config.getClientId(), config.getPeerListenPort(),
                           config.getNodeId()!!)

    logger.debug("${node.nodeId} say HELLO to ${channel.remoteAddress()}")

    sendMessage(msg)
  }

  /**
   * 握手成功后应该首先向对方发送STATUS消息。
   */
  fun sendStatusMessage() {
    val config = manager.blockChain.config

    val bestBlock = manager.blockChain.bestBlock
    val genesisBlock = config.getGenesisBlock()
    val msg = StatusMessage(config.getPeerVersion(), config.getNetworkId(), bestBlock.totalDifficulty, bestBlock.hash,
                            genesisBlock.hash)

    logger.debug("${node.nodeId} send STATUS to ${channel.remoteAddress()}")

    sendMessage(msg)
  }

  /**
   * 发送交易数据。
   */
  fun sendTransaction(trx: Transaction) {
    val msg = NewTransactionsMessage(listOf(trx))

    sendMessage(msg)
  }

  /**
   * 发送新的区块。
   */
  fun sendNewBlock(block: Block) {
    val msg = NewBlockMessage(block)

    sendMessage(msg)
  }

  /**
   * 发送区块。
   */
  fun sendBlocks(blocks: List<Block>) {
    val msg = BlocksMessage(blocks)

    sendMessage(msg)
  }

  /**
   * 下载区块。
   */
  fun sendGetBlocks(fromHeight: Long, numOfBlocks: Int) {
    val msg = GetBlocksMessage(fromHeight, numOfBlocks)

    sendMessage(msg)
  }

  /**
   * 下载节点列表。
   */
  fun sendGetPeers() {
    val msg = GetNodesMessage()

    sendMessage(msg)
  }

  /**
   * 发送节点列表。
   */
  fun sendPeers(peers: List<Node>) {
    val msg = NodesMessage(peers)

    sendMessage(msg)
  }

  /**
   * 发送消息包。消息包的格式为[code(1 byte)][msg]
   */
  private fun sendMessage(msg: Message) {
    NetworkUtil.sendMessage(channel, msg)
  }

  override fun toString(): String {
    return "[Peer:${channel.remoteAddress()}]"
  }

}
