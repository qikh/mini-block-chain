package mbc.core

import mbc.core.TransactionExecutor.applyTrx
import org.joda.time.DateTime

/**
 * 区块链(BlockChain)管理类，负责区块的生成、发布、计算等功能。
 */
class BlockChain(val minerAddress: String) {

  /**
   * 构造新的区块，要素信息为：区块高度(height)，父区块的哈希值(parentHash), 交易记录(transactions)，时间戳(time)
   */
  fun createNewBlock(parent: Block, transactions: List<Transaction>): Block {
    val block = Block(parent.height + 1, parent.hash, minerAddress, transactions, DateTime())

    for (trx in transactions) {
      applyTrx(trx)
    }

    return block
  }
}
