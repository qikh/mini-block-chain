package mbc.miner

import mbc.core.Block
import mbc.util.CryptoUtil
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer


/**
 * 挖矿的管理类，算法可以参照https://en.bitcoin.it/wiki/Block_hashing_algorithm。
 */
object BlockMiner {
  private val logger = LoggerFactory.getLogger(BlockMiner.javaClass)

  var mining = false

  var currentDifficulty = 0x1e00ffff // 比特币的最小(初始)难度为0x1d00ffff，为测试方便我们降低难度为0x1e00ffff

  /**
   * 挖矿，返回nonce值和target值。目前采用阻塞模型，后期修改为更合理的异步模型。
   */
  fun mine(block: Block): MineResult {
    logger.debug("Start mining block ...")

    mining = true

    val startTime = System.currentTimeMillis()

    val ver = block.version
    val parentHash = block.parentHash
    val merkleRoot = block.merkleRoot
    val time = (block.time.millis / 1000).toInt() // Current timestamp as seconds since 1970-01-01T00:00 UTC
    val difficulty = currentDifficulty // difficulty

    // 挖矿难度的算法：https://en.bitcoin.it/wiki/Difficulty
    val exp = difficulty shr 24
    val mant = difficulty and 0xffffff
    val target = BigInteger.valueOf(mant.toLong()).multiply(BigInteger.valueOf(2).pow(8 * (exp - 3)))
    val targetStr = "%064x".format(target)

    var nonce = 0
    while (mining && nonce < 0x100000000) {

      val headerBuffer = ByteBuffer.allocate(4 + 32 + 32 + 4 + 4 + 4)
      headerBuffer.put(ByteBuffer.allocate(4).putInt(ver).array()) // version
      headerBuffer.put(parentHash) // parentHash
      headerBuffer.put(merkleRoot) // merkleRoot
      headerBuffer.put(ByteBuffer.allocate(4).putInt(time).array()) // time
      headerBuffer.put(ByteBuffer.allocate(4).putInt(difficulty).array()) // difficulty(current difficulty)
      headerBuffer.put(ByteBuffer.allocate(4).putInt(nonce).array()) // nonce

      val header = headerBuffer.array()
      val hit = Hex.toHexString(CryptoUtil.sha256(CryptoUtil.sha256(header)))

      if (hit < targetStr) {
        break
      }
      nonce += 1
    }

    val endTime = System.currentTimeMillis()

    val interval = (endTime - startTime) / 1000

    logger.debug("Mining block finished in $interval seconds.")

    val totalDifficulty = block.totalDifficulty + BigInteger.valueOf(difficulty.toLong())

    val newBlock = Block(block.version, block.height, block.parentHash, block.merkleRoot, block.coinBase,
                         DateTime(), difficulty, nonce, totalDifficulty, block.transactions)
    val result = MineResult(difficulty, nonce, newBlock)

    mining = false

    return result
  }

  fun stop() {
    mining = false
  }
}
