package mbc.core

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import mbc.miner.BlockMiner
import mbc.miner.MineResult
import mbc.network.Peer
import org.slf4j.LoggerFactory

class BlockChainManager(val blockChain: BlockChain) {

  private val logger = LoggerFactory.getLogger(BlockChainManager::class.java)

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
  fun addPeer(peer: Peer) {
    logger.debug("Peer connected: $peer")

    peers.add(peer)

    // 监听Peer的连接关闭事件
    peer.channel.closeFuture().addListener { notifyPeerClosed(peer) }
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

    BlockMiner.stop()
  }

  fun mineBlock() {
    val block = blockChain.generateNewBlock(pendingTransactions)
    Flowable.fromCallable({ BlockMiner.mine(block) })
        .subscribeOn(Schedulers.computation())
        .observeOn(Schedulers.single())
        .subscribe({
                     processNewBlock(it)
                     if (mining) {
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
    peer.sendGetBlocks(blockChain.bestBlock.height, 10)
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
        blocks.forEach { blockChain.pushBlock(it) }

        // 继续请求区块数据，直至同步完毕。
        requestPeerBlocks(peer)
      } else {
        stopSync()
      }
    }
  }

  fun stopSync() {
    synching = false
  }

  /**
   * Mining完成后把挖到的区块加入到区块链。
   */
  private fun processNewBlock(result: MineResult) {
    val block = result.block
    logger.debug("Mined block: $block")

    blockChain.pushBlock(block)

    peers.map { it.sendNewBlock(block) }
  }

  /**
   * 开始搜索可连接的Peer。
   */
  fun startPeerDiscovery() {

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
}
