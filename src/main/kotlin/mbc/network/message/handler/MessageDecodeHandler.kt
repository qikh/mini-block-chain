package mbc.network.message.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import mbc.core.Block
import mbc.core.BlockChainManager
import mbc.core.Node
import mbc.network.Peer
import mbc.network.message.*
import mbc.storage.Repository
import org.slf4j.LoggerFactory

/**
 * 解码消息，然后分发给各个消息的处理类。
 */
class MessageDecodeHandler(val peer: Peer) : ByteToMessageDecoder() {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val manager: BlockChainManager = peer.manager

  private val repository: Repository = peer.manager.blockChain.repository

  lateinit var ctx: ChannelHandlerContext

  override fun decode(ctx: ChannelHandlerContext, data: ByteBuf, out: MutableList<Any>) {
    this.ctx = ctx

    val code = data.readByte()

    val buffer = ByteArray(data.readableBytes())
    data.readBytes(buffer)

    try {
      when (code) {
        MessageCodes.DISCONNECT.code -> processDisconnect()
        MessageCodes.STATUS.code -> processStatus(StatusMessage.decode(buffer))
        MessageCodes.GET_NODES.code -> processGetNodes(GetNodesMessage.decode(buffer))
        MessageCodes.NODES.code -> processNodes(NodesMessage.decode(buffer))
        MessageCodes.NEW_TRANSACTIONS.code -> processNewTransactions(NewTransactionsMessage.decode(buffer))
        MessageCodes.NEW_BLOCK.code -> processNewBlock(NewBlockMessage.decode(buffer))
        MessageCodes.GET_BLOCKS.code -> processGetBlocks(GetBlocksMessage.decode(buffer))
        MessageCodes.BLOCKS.code -> processBlocks(BlocksMessage.decode(buffer))
      }
    } catch (e: Exception) {
      logger.error("Process message packet failed!")
    }

  }

  private fun processGetNodes(msg: GetNodesMessage?) {

    logger.debug("Processing GetNodesMessage")

    if (msg == null) {
      throw MessageDecodeException("GetNodesMessage decode failed.")
    }

    /**
     * Do not include the connected peer to the node list.
     */
    val peers = manager.peers.minus(peer)

    val nodeList = mutableListOf<Node>()
    peers.forEach { nodeList.add(it.node) }

    peer.sendPeers(nodeList)
  }

  /**
   * Peers handler.
   *
   * TODO: 发现逻辑。
   */
  private fun processNodes(msg: NodesMessage?) {
    logger.debug("Processing NodesMessage")

    if (msg == null) {
      throw MessageDecodeException("NodesMessage decode failed.")
    }

    manager.discoveryNodes.addAll(msg.nodes)
  }

  private fun processGetBlocks(msg: GetBlocksMessage?) {

    logger.debug("Processing GetBlocksMessage")

    if (msg == null) {
      throw MessageDecodeException("GetBlocksMessage decode failed.")
    }

    val fromHeight = msg.fromHeight
    val numOfBlocks = msg.numOfBlocks

    val blockHashs = mutableListOf<ByteArray>()
    if (repository != null) {
      for (i in fromHeight..fromHeight + numOfBlocks) {
        val blockInfos = repository.getBlockInfos(i)

        blockInfos?.forEach { if (it.isMain) blockHashs.add(it.hash) }
      }
    }

    val blocks = mutableListOf<Block>()
    blockHashs.forEach { repository.getBlock(it)?.let { blocks.add(it) } }

    peer.sendBlocks(blocks)
  }

  /**
   * Blocks handler.
   */
  private fun processBlocks(msg: BlocksMessage?) {
    logger.debug("Processing BlocksMessage")

    if (msg == null) {
      throw MessageDecodeException("BlocksMessage decode failed.")
    }

    manager.processPeerBlocks(peer, msg.blocks)
  }

  /**
   * NewBlock handler
   */
  private fun processNewBlock(msg: NewBlockMessage?) {
    logger.debug("Processing NewBlockMessage")

    if (msg == null) {
      throw MessageDecodeException("NewBlockMessage decode failed.")
    }

    manager.processPeerNewBlock(msg.block, peer)
  }

  /**
   * NewTransactionsMessage handler
   */
  private fun processNewTransactions(msg: NewTransactionsMessage?) {
    logger.debug("Processing NewTransactionsMessage")

    if (msg == null) {
      throw MessageDecodeException("NewTransactionsMessage decode failed.")
    }

    manager.addPendingTransactions(msg.transactions)
  }

  /**
   * DisconnectMessage handler
   */
  private fun processDisconnect() {
    peer.close()
  }

  /**
   * StatusMessage handler
   */
  private fun processStatus(msg: StatusMessage?) {
    if (msg == null) {
      throw MessageDecodeException("StatusMessage decode failed.")
    }

    peer.protocolVersion = msg.protocolVersion
    peer.networkId = msg.networkId
    peer.totalDifficulty = msg.totalDifficulty
    peer.bestHash = msg.bestHash
    peer.genesisHash = msg.genesisHash

    val bestBlock = manager.blockChain.getBestBlock()
    val myTotalDifficulty = bestBlock.totalDifficulty
    val peerTotalDifficulty = peer.totalDifficulty
    if (peerTotalDifficulty != null && peerTotalDifficulty > myTotalDifficulty) {
      logger.debug("Peer total difficulty was greater than mine.")

      manager.stopMining()

      manager.startSync(peer)
    } else {
      manager.startMining()
    }

    logger.debug(
        "Peer status { Protocol Version:${msg.protocolVersion} NetworkId:${msg.networkId} Total Difficulty:${msg.totalDifficulty} }")
  }

}

/**
 * 消息解码异常。
 */
class MessageDecodeException(s: String) : Throwable(s)
