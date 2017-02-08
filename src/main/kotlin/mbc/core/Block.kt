package mbc.core

import mbc.util.CryptoUtil
import org.joda.time.DateTime

/**
 * 区块(Block)类，包含了区块高度(height)，上一个区块哈希值(parentHash)，旷工账户地址(minerAddress)，交易列表(transactions)和时间戳(time)。
 */
class Block(val height: Long, val parentHash: ByteArray, val minerAddress: String, val transactions: List<Transaction>,
            val time: DateTime) {

  val version: Int = 1

  val merkleRoot: ByteArray
    get() = CryptoUtil.merkleRoot(transactions)

  var difficulty: Int = 0

  var nonce: Int = 0

  /**
   * 区块(Block)的哈希值(KECCAK-256)
   */
  val hash: ByteArray
    get() = CryptoUtil.hashBlock(this)

}
