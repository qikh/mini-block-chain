package mbc.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mbc.core.AccountState
import mbc.core.Block
import mbc.core.Transaction
import mbc.serialization.AccountStateSerialize
import mbc.serialization.BlockSerialize
import mbc.serialization.TransactionSerialize
import mbc.storage.DataSource
import mbc.storage.LevelDbDataSource
import mbc.storage.MemoryDataSource
import mbc.storage.ObjectStore
import org.spongycastle.util.encoders.Hex
import java.io.File

class BlockChainConfig {

  enum class DATABASE_TYPE {
    MEMORY, LEVELDB
  }

  companion object {
    var minerCoinBase: ByteArray = ByteArray(0)

    var defaultConfig: Config = ConfigFactory.empty()

    fun getConfig(): Config {
      if (defaultConfig.isEmpty) {
        val resourceConfig = ConfigFactory.parseResources("application.conf")
        val fileConfig = ConfigFactory.parseFile(File("application.conf"))

        defaultConfig = defaultConfig.withFallback(resourceConfig).withFallback(fileConfig)
      } else {
        return defaultConfig
      }
      return defaultConfig
    }

    /**
     * Account State的存储类组装。
     */
    fun getAccountStateStore(): ObjectStore<AccountState> {
      val dbName = "accounts"
      var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
      if (getDatabaseType().equals(DATABASE_TYPE.LEVELDB.name, true)) {
        ds = LevelDbDataSource(dbName)
      }
      ds.init()
      return ObjectStore(ds, AccountStateSerialize())
    }

    /**
     * Block的存储类组装。
     */
    fun getBlockStore(): ObjectStore<Block> {
      val dbName = "blocks"
      var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
      if (getDatabaseType().equals(DATABASE_TYPE.LEVELDB.name, true)) {
        ds = LevelDbDataSource(dbName)
      }
      ds.init()
      return ObjectStore(ds, BlockSerialize())
    }

    /**
     * Transaction的存储类组装。
     */
    fun getTransactionStore(): ObjectStore<Transaction> {
      val dbName = "transactions"
      var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
      if (getDatabaseType().equals(DATABASE_TYPE.LEVELDB.name, true)) {
        ds = LevelDbDataSource(dbName)
      }
      ds.init()
      return ObjectStore(ds, TransactionSerialize())
    }

    /**
     * 得到矿工的Coinbase地址。
     */
    fun getMinerCoinbase(): ByteArray {
      if (this.minerCoinBase.size > 0) {
        return minerCoinBase
      } else {
        if (getConfig().hasPathOrNull("miner.minerCoinBase")) {
          minerCoinBase = Hex.decode(getConfig().getString("miner.minerCoinBase"))
          return minerCoinBase;
        } else {
          throw RuntimeException("Miner Coinbase is empty")
        }
      }
    }

    /**
     * 设置矿工的Coinbase地址。
     */
    fun setMinerCoinbase(newCoinbase: ByteArray) {
      this.minerCoinBase = newCoinbase
    }

    /**
     * Key-Value存储的实现。
     */
    fun getDatabaseType(): String {
      if (getConfig().hasPathOrNull("database.type")) {
        return getConfig().getString("database.type")
      } else {
        return DATABASE_TYPE.MEMORY.name
      }
    }

    /**
     * 数据库的存储目录。
     */
    fun getDatabaseDir(): String {
      if (getConfig().hasPathOrNull("database.dir")) {
        return getConfig().getString("database.dir")
      } else {
        return "database"
      }
    }

  }

}
