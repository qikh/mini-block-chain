package mbc.core

import mbc.config.BlockChainConfig
import mbc.storage.BlockInfo
import mbc.storage.Repository
import mbc.util.CodecUtil
import mbc.util.CryptoUtil
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.util.*

/**
 * 区块链(BlockChain)，一个BlockChain实例就代表一个链。
 */
class BlockChain(val config: BlockChainConfig) {

  private val logger = LoggerFactory.getLogger(BlockChain::class.java)

  var bestBlock: Block = loadBestBlock()

  /**
   * TODO: 加载BestBlock
   */
  private fun loadBestBlock(): Block {
    return config.getGenesisBlock()
  }

  /**
   * 数据的存储。
   */
  val repository = Repository.getInstance(config)

  /**
   * 交易处理实例。
   */
  val transactionExecutor = TransactionExecutor(repository)

  /**
   * 构造新的区块，要素信息为：区块高度(height)，父区块的哈希值(parentHash), 交易记录(transactions)，时间戳(time)。
   * 新的区块不会影响当前区块链的状态。
   */
  fun generateNewBlock(transactions: List<Transaction>): Block {
    val parent = bestBlock

    val block = Block(config.getPeerVersion(), parent.height + 1, parent.hash, CryptoUtil.merkleRoot(transactions),
                      config.getMinerCoinbase(), DateTime(), 0, 0, parent.totalDifficulty, transactions)
    return block
  }

  /**
   * 执行区块的交易数据，会影响当前区块链的状态。
   *
   * TODO: 费用的计算和分配。
   */
  fun processBlock(block: Block) {
    for (trx in block.transactions) {
      transactionExecutor.execute(trx)
    }
  }

  /**
   * 保存区块数据。
   *
   * TODO: 实现AccountState的Merkle Patricia Tree存储。
   */
  fun pushBlock(block: Block) {
    logger.debug("Push block $block to end of chain.")

    repository.getBlockStore()?.put(block.hash, block)

    processBlock(block)

    val isMain = true // TODO: 实现分叉逻辑
    val newBlockInfo = BlockInfo(block.hash, isMain, block.totalDifficulty)

    val blockIndexStore = repository.getBlockIndexStore()
    val k = CodecUtil.longToByteArray(block.height)
    val blockInfoList = blockIndexStore?.get(k)
    if (blockInfoList != null) {
      val filtered = blockInfoList.dropWhile { Arrays.equals(it.hash, newBlockInfo.hash) }
      blockIndexStore?.put(CodecUtil.longToByteArray(block.height), filtered.plus(newBlockInfo))
    } else {
      blockIndexStore?.put(CodecUtil.longToByteArray(block.height), listOf(newBlockInfo))
    }

    bestBlock = block
  }

}
