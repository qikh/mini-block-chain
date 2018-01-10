package mbc.core

import mbc.config.BlockChainConfig
import mbc.storage.BlockInfo
import mbc.storage.Repository
import mbc.util.CodecUtil
import mbc.util.CryptoUtil
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

/**
 * 区块链(BlockChain)，一个BlockChain实例就代表一个链。
 */
class BlockChain(val config: BlockChainConfig) {

  private val logger = LoggerFactory.getLogger(javaClass)

  private var bestBlock: Block = config.getGenesisBlock()

  /**
   * 数据的存储。
   */
  val repository = Repository.getInstance(config)

  /**
   * 交易处理实例。
   */
  val transactionExecutor = TransactionExecutor(repository)

  init {
    // 检查NodeId，如果不存在就自动生成NodeId。
    config.getNodeId()

    loadBestBlock()
  }

  /**
   * 读取BestBlock
   */
  private fun loadBestBlock(): Block {
    bestBlock = repository.getBestBlock() ?: config.getGenesisBlock()
    logger.debug("Best block is:" + bestBlock)
    return bestBlock
  }

  fun getBestBlock() = bestBlock

  fun updateBestBlock(newBlock: Block) {
    logger.debug("Updating best block to ${Hex.toHexString(newBlock.hash)}")
    repository.updateBestBlock(newBlock)
    this.bestBlock = newBlock
  }

  /**
   * 构造新的区块，要素信息为：区块高度(height)，父区块的哈希值(parentHash), 交易记录(transactions)，时间戳(time)。
   * 新的区块不会影响当前区块链的状态。
   */
  fun generateNewBlock(transactions: List<Transaction>): Block {
    val parent = bestBlock

    val coinbaseTransaction = generateCoinBaseTransaction()
    val transactionsToInclude = listOf(coinbaseTransaction) + transactions

    val block = Block(config.getPeerVersion(), parent.height + 1, parent.hash,
        config.getMinerCoinbase(), DateTime(), 0, 0, parent.totalDifficulty,
        CryptoUtil.merkleRoot(emptyList()), CryptoUtil.merkleRoot(transactions),
        transactionsToInclude)
    return block
  }

  /**
   * 构造CoinBase Transaction (https://bitcoin.org/en/glossary/coinbase-transaction)，矿工的收益。
   */
  private fun generateCoinBaseTransaction(): Transaction {
    return Transaction(COINBASE_SENDER_ADDRESS,
        config.getMinerCoinbase(), BigInteger.valueOf(25), DateTime(), config.getNodePubKey()!!)
  }

  /**
   * 执行区块的交易数据，会影响当前区块链的状态。
   *
   * TODO: 费用的计算和分配。
   */
  fun processBlock(block: Block): Block {
    transactionExecutor.executeCoinbaseTransaction(block.transactions[0])

    for (trx in block.transactions.drop(1)) {
      transactionExecutor.execute(trx)
    }

    return Block(block.version, block.height, block.parentHash, block.coinBase,
        block.time, block.difficulty, block.nonce, block.totalDifficulty,
        repository.getAccountStateStore()?.rootHash ?: ByteArray(0), block.trxTrieRoot,
        block.transactions)
  }

  /**
   * Import区块数据的结果
   */
  enum class ImportResult {

    BEST_BLOCK,
    NON_BEST_BLOCK,
    EXIST,
    NO_PARENT,
    INVALID_BLOCK
  }

  /**
   * 保存区块数据。
   *
   * TODO: 实现AccountState的Merkle Patricia Tree存储。
   */
  fun importBlock(block: Block): ImportResult {
    // Validate Block
    if (!block.isValid) {
      return ImportResult.INVALID_BLOCK
    }

    if (isNextBlock(block)) {
      logger.debug("Push block $block to end of chain.")
      val blockToSave = processBlock(block)

      repository.saveBlock(blockToSave)

      updateMainBlockInfo(blockToSave)

      logger.debug("Block hash: ${Hex.toHexString(blockToSave.hash)}")

      updateBestBlock(blockToSave)

      return ImportResult.BEST_BLOCK
    } else {
      if (block.height < bestBlock.height) {
        if (repository.getBlock(block.hash) != null) { // Already exist
          logger.debug(
              "Block already exist. hash: ${Hex.toHexString(block.hash)}, height: ${block.height}")

          return ImportResult.EXIST
        } else if (repository.getBlock(block.parentHash) != null) { // Branch fork
          logger.debug("Import branch block $block.")

          return importBranchBlock(block)
        } else {
          logger.debug("Import block without parent block $block.")

          return importBranchBlock(block)
        }
      } else if (block.height == bestBlock.height) {
        if (repository.getBlock(block.hash) != null) { // Already exist
          logger.debug(
              "Block already exist. hash: ${Hex.toHexString(block.hash)}, height: ${block.height}")

          return ImportResult.EXIST
        } else if (repository.getBlock(block.parentHash) != null) { // Fork
          logger.debug("Fork block $block.")

          return forkBlock(block)
        } else {
          logger.debug("Import block without parent block $block.")

          return importBranchBlock(block)
        }
      } else {
        logger.debug("Import block without parent block $block.")

        return importBranchBlock(block)
      }
    }

  }

  private fun forkBlock(block: Block): ImportResult {
    repository.saveBlock(block)
    updateMainBlockInfo(block)

    val parentBlockHash = block.parentHash
    var parentBlock = repository.getBlock(parentBlockHash)

    while (parentBlock != null) {
      val parentBlockInfo = repository.getBlockInfo(parentBlockHash)

      if (parentBlockInfo == null) {
        break
      }

      if (parentBlockInfo.isMain) {
        break
      } else {
        updateMainBlockInfo(parentBlock)
      }

      parentBlock = repository.getBlock(parentBlock.parentHash)
    }
    return ImportResult.BEST_BLOCK
  }

  private fun importBranchBlock(block: Block): ImportResult {
    repository.saveBlock(block)
    updateBranchBlockInfo(block)

    return ImportResult.NON_BEST_BLOCK
  }

  private fun updateMainBlockInfo(block: Block) {
    val isMain = true
    val blockInfo = BlockInfo(block.hash, isMain, block.totalDifficulty)

    val blockIndexStore = repository.getBlockIndexStore()
    val k = CodecUtil.longToByteArray(block.height)
    val blockInfoList = blockIndexStore?.get(k)
    if (blockInfoList != null) {
      val filtered = blockInfoList.dropWhile { it.hash.contentEquals(blockInfo.hash) }
      val converted = filtered.map { BlockInfo(it.hash, false, it.totalDifficulty) }
      blockIndexStore.put(CodecUtil.longToByteArray(block.height), converted.plus(blockInfo))
    } else {
      blockIndexStore?.put(CodecUtil.longToByteArray(block.height), listOf(blockInfo))
    }
  }

  private fun updateBranchBlockInfo(block: Block) {
    val isMain = false
    val blockInfo = BlockInfo(block.hash, isMain, block.totalDifficulty)

    val blockIndexStore = repository.getBlockIndexStore()
    val k = CodecUtil.longToByteArray(block.height)
    val blockInfoList = blockIndexStore?.get(k)
    if (blockInfoList != null) {
      val filtered = blockInfoList.dropWhile { it.hash.contentEquals(blockInfo.hash) }
      blockIndexStore.put(CodecUtil.longToByteArray(block.height), filtered.plus(blockInfo))
    } else {
      blockIndexStore?.put(CodecUtil.longToByteArray(block.height), listOf(blockInfo))
    }
  }

  private fun isNextBlock(block: Block): Boolean {
    println("Best block Hash:" + Hex.toHexString(bestBlock.hash))
    println("Parent block Hash:" + Hex.toHexString(block.parentHash))
    return Arrays.equals(bestBlock.hash, block.parentHash)
  }

}
