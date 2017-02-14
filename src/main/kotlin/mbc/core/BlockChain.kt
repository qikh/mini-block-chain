package mbc.core

import mbc.config.BlockChainConfig
import mbc.core.TransactionExecutor.execute
import mbc.util.CryptoUtil
import org.joda.time.DateTime

/**
 * 区块链(BlockChain)管理类，负责区块的生成、发布、计算等功能。
 */
class BlockChain {

  /**
   * 构造新的区块，要素信息为：区块高度(height)，父区块的哈希值(parentHash), 交易记录(transactions)，时间戳(time)
   */
  fun createNewBlock(version: Int, parent: Block, transactions: List<Transaction>): Block {
    val block = Block(version, parent.height + 1, parent.hash, CryptoUtil.merkleRoot(transactions),
                      BlockChainConfig.getMinerCoinbase(),
                      transactions, DateTime(), 0, 0)

    for (trx in transactions) {
      execute(trx)
    }

    return block
  }
}
