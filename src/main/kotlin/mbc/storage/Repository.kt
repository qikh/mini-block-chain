package mbc.storage

import mbc.config.BlockChainConfig
import mbc.core.AccountState
import mbc.core.Block
import mbc.core.Transaction
import mbc.serialization.AccountStateSerialize
import mbc.serialization.BlockInfosSerialize
import mbc.serialization.BlockSerialize
import mbc.serialization.TransactionSerialize
import mbc.util.CodecUtil
import java.math.BigInteger

/**
 * 管理区块链状态的管理类(Account State, Blocks和Transactions)，不同的BlockChainConfig应该使用不同的Repository。
 */
class Repository {

  var config: BlockChainConfig

  /**
   * 不允许直接构造Repository。
   */
  private constructor(config: BlockChainConfig) {
    this.config = config
  }

  companion object {
    /**
     * Repository索引表。
     */
    val repositoryMap = mutableMapOf<BlockChainConfig, Repository>()

    /**
     * 如果反复构造Repository会造成底层数据库重复初始化而出错（例如LevelDb），因此不同的BlockChainConfig应该使用不同的Repository实例。
     */
    fun getInstance(config: BlockChainConfig): Repository {
      if (repositoryMap[config] == null) {
        val rep = Repository(config)
        repositoryMap.put(config, rep)
        return rep
      } else {
        return repositoryMap[config]!!
      }
    }
  }

  /**
   * Account State Db.
   */
  private var accountDs: ObjectStore<AccountState>? = null

  /**
   * Blocks Db.
   */
  private var blockDs: ObjectStore<Block>? = null

  /**
   * Block index db.
   */
  private var blockIndexDs: ObjectStore<List<BlockInfo>>? = null

  /**
   * Transactions Db.
   */
  private var transactionDs: ObjectStore<Transaction>? = null

  /**
   * Account State的存储类组装。
   */
  fun getAccountStateStore(): ObjectStore<AccountState>? {
    if (accountDs != null) return accountDs

    val dbName = "accounts"
    var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
    if (config.getDatabaseType().equals(BlockChainConfig.DATABASE_TYPE.LEVELDB.name, true)) {
      ds = LevelDbDataSource(dbName, config.getDatabaseDir())
    }
    ds.init()
    accountDs = ObjectStore(ds, AccountStateSerialize())
    return accountDs
  }

  /**
   * Block的存储类组装。
   */
  fun getBlockStore(): ObjectStore<Block>? {
    if (blockDs != null) return blockDs

    val dbName = "blocks"
    var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
    if (config.getDatabaseType().equals(BlockChainConfig.DATABASE_TYPE.LEVELDB.name, true)) {
      ds = LevelDbDataSource(dbName, config.getDatabaseDir())
    }
    ds.init()
    blockDs = ObjectStore(ds, BlockSerialize())
    return blockDs
  }

  /**
   * Transaction的存储类组装。
   */
  fun getTransactionStore(): ObjectStore<Transaction>? {
    if (transactionDs != null) return transactionDs

    val dbName = "transactions"
    var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
    if (config.getDatabaseType().equals(BlockChainConfig.DATABASE_TYPE.LEVELDB.name, true)) {
      ds = LevelDbDataSource(dbName, config.getDatabaseDir())
    }
    ds.init()
    transactionDs = ObjectStore(ds, TransactionSerialize())
    return transactionDs
  }

  /**
   * Block Index的存储类组装。
   */
  fun getBlockIndexStore(): ObjectStore<List<BlockInfo>>? {
    if (blockIndexDs != null) return blockIndexDs

    val dbName = "blockIndex"
    var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
    if (config.getDatabaseType().equals(BlockChainConfig.DATABASE_TYPE.LEVELDB.name, true)) {
      ds = LevelDbDataSource(dbName, config.getDatabaseDir())
    }
    ds.init()
    blockIndexDs = ObjectStore(ds, BlockInfosSerialize())
    return blockIndexDs
  }

  /**
   * 读取账户余额。
   */
  fun getBalance(address: ByteArray): BigInteger {
    return getAccountStateStore()?.get(address)?.balance ?: BigInteger.ZERO
  }

  /**
   * 账户的Nonce+1
   */
  fun increaseNonce(address: ByteArray) {
    val accountState = getOrCreateAccountState(address)
    getAccountStateStore()?.put(address, accountState.increaseNonce())
  }

  /**
   * 增加账户余额。
   */
  fun addBalance(address: ByteArray, amount: BigInteger) {
    val accountState = getOrCreateAccountState(address)
    getAccountStateStore()?.put(address, accountState.increaseBalance(amount))
  }

  fun getBlockInfos(height: Long): List<BlockInfo>? {
    return getBlockIndexStore()?.get(CodecUtil.longToByteArray(height))
  }

  fun getBlock(hash: ByteArray): Block? {
    return getBlockStore()?.get(hash)
  }

  /**
   * 新建账户。
   */
  private fun createAccountState(address: ByteArray): AccountState {
    val state = AccountState(BigInteger.ZERO, BigInteger.ZERO)
    getAccountStateStore()?.put(address, state)
    return state
  }

  /**
   * 判断账户状态Account State是不是存在，如果不存在就新建账户。
   */
  private fun getOrCreateAccountState(address: ByteArray): AccountState {
    var ret = getAccountStateStore()?.get(address)
    if (ret == null) {
      ret = createAccountState(address)
    }
    return ret
  }

}
