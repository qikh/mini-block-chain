package mbc.core

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import mbc.miner.BlockMiner
import mbc.miner.MineResult
import mbc.network.Peer
import mbc.network.client.PeerClient
import org.slf4j.LoggerFactory
import java.net.URI

class BlockChainManager(val blockChain: BlockChain) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * 等待加入区块的交易数据。
   */
  val pendingTransactions = mutableListOf<Transaction>()

  /**
   * 当前连接的Peer。
   */
  var peers = mutableListOf<Peer>()

  /**
   * 等待发现的Node。
   */
  var discoveryNodes = mutableListOf<Node>()

  /**
   * 是否正在挖矿中。
   */
  var mining: Boolean = false

  /**
   * 正在挖矿中的区块。
   */
  var miningBlock: Block? = null

  /**
   * 是否正在同步区块中。
   */
  var synching: Boolean = false

  /**
   * 将Transaction加入到Pending List。
   */
  fun addPendingTransaction(trx: Transaction) {
    if (trx.isValid) {
      pendingTransactions.add(trx)
    } else {
      logger.debug("Invalid transaction $trx was ignored.")
    }
  }

  /**
   * 批量将Transaction加入到Pending List。
   */
  fun addPendingTransactions(transactions: List<Transaction>) {
    logger.debug("Appending ${transactions.size} transactions to pending transactions.")

    transactions.map { addPendingTransaction(it) }
  }

  /**
   * 增加Peer连接。
   */
  fun addPeer(peer: Peer): Int {
    logger.debug("Peer connected: $peer")

    if (peers.none { it.node.nodeId == peer.node.nodeId }) {
      peers.add(peer)
    } else {
      logger.debug("Peer $peer.node.nodeId already exists in connected peer list")
      return -1
    }

    // 监听Peer的连接关闭事件
    peer.channel.closeFuture().addListener { notifyPeerClosed(peer) }

    return 0
  }

  private fun notifyPeerClosed(peer: Peer) {
    logger.debug("Peer closed: $peer.")
    peers.remove(peer)
  }

  /**
   * 开始异步Mining。
   */
  fun startMining() {
    mining = true

    mineBlock()
  }

  /**
   * 停止异步Mining。
   */
  fun stopMining() {

    mining = false
  }

  fun mineBlock() {
    logger.debug("Mine block.")
    miningBlock = blockChain.generateNewBlock(pendingTransactions)
    Flowable.fromCallable({ BlockMiner.mine(miningBlock!!) })
        .subscribeOn(Schedulers.computation())
        .observeOn(Schedulers.single())
        .subscribe({
          if (it.success) {
            processMinedBlock(it)
          }
          if (mining) { // continue mining.
            mineBlock()
          }
        })
  }

  /**
   * 开始同步区块。
   */
  fun startSync(peer: Peer) {
    synching = true

    requestPeerBlocks(peer)
  }

  /**
   * 向Peer请求区块数据。
   */
  fun requestPeerBlocks(peer: Peer) {
    peer.sendGetBlocks(blockChain.getBestBlock().height + 1, 10)
  }

  /**
   * 处理Peer同步的区块。
   */
  fun processPeerBlocks(peer: Peer, blocks: List<Block>) {
    /**
     * 同步区块中。。。
     */
    if (synching) {

      /**
       * 收到区块的数量大于0则保存区块，否则说明同步完成，停止区块同步。
       */
      if (blocks.size > 0) {
        blocks.forEach { blockChain.importBlock(it) }

        // 继续请求区块数据，直至同步完毕。
        requestPeerBlocks(peer)
      } else {
        stopSync()

        startMining()
      }
    }
  }

  fun stopSync() {
    synching = false
  }

  /**
   * Mining完成后把挖到的区块加入到区块链。
   */
  private fun processMinedBlock(result: MineResult) {
    val block = result.block
    logger.debug("Mined block: $block")

    if (blockChain.importBlock(block) == BlockChain.ImportResult.BEST_BLOCK) {
      val bestBlock = blockChain.getBestBlock()
      peers.forEach { it.sendNewBlock(bestBlock) }
    }
  }

  /**
   * 开始搜索可连接的Peer。
   *
   * TODO: 运行时更新Peer地址列表并刷新连接。
   */
  fun startPeerDiscovery() {
    val bootnodes = blockChain.config.getBootnodes()

    if (bootnodes.size > 0) {
      bootnodes.forEach {
        val uri = URI(it)
        if (uri.scheme == "mnode") {
          // Run PeerClient for nodes in bootnodes list. It will make async connection.
          PeerClient(this).connectAsync(Node(uri.userInfo, uri.host, uri.port))
        }
      }
    } else {
      startMining()
    }
  }

  /**
   * 停止对Peer的搜索。
   */
  fun stopPeerDiscovery() {

  }

  /**
   * 提交交易数据，会进入Pending Transactions并发布给已连接的客户端。
   */
  fun submitTransaction(trx: Transaction) {
    if (trx.isValid) {
      addPendingTransaction(trx)

      peers.map { it.sendTransaction(trx) }
    }
  }

  /**
   * 判断Peer是否已经在连接列表内
   */
  fun peerConnected(peer: Peer): Boolean {
    return peers.any { it.node.nodeId == peer.node.nodeId }
  }

  /**
   * 处理同步过来的区块数据，检索区块是否已经存在，只保存新增区块。
   */
  fun processPeerNewBlock(block: Block, peer: Peer) {
    val importResult = blockChain.importBlock(block)

    if (importResult == BlockChain.ImportResult.BEST_BLOCK) {
      if (miningBlock != null && block.height >= miningBlock!!.height) {
        BlockMiner.skip()
      }

      broadcastBlock(block, peer)
    }
  }

  private fun broadcastBlock(block: Block, skipPeer: Peer) {
    peers.filterNot { it == skipPeer }.forEach { it.sendNewBlock(block) }
  }
}
