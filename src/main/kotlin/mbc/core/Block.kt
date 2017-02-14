package mbc.core

import mbc.util.CodecUtil
import mbc.util.CryptoUtil
import org.joda.time.DateTime

/**
 * 区块(Block)类，包含了区块高度(height)，上一个区块哈希值(parentHash)，旷工账户地址(coinBase)，交易列表(transactions)和时间戳(time)。
 */
class Block(val version: Int, val height: Long, val parentHash: ByteArray, val merkleRoot: ByteArray,
            val coinBase: ByteArray, val transactions: List<Transaction>, val time: DateTime, val difficulty: Int,
            val nonce: Int) {

  /**
   * 区块(Block)的哈希值(KECCAK-256)
   */
  val hash: ByteArray
    get() = CryptoUtil.hashBlock(this)

  fun encode(): ByteArray {
    return CodecUtil.encodeBlock(this)
  }

}
