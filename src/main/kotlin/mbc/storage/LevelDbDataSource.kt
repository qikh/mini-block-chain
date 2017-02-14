package mbc.storage

import mbc.config.BlockChainConfig
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.util.*

/**
 * 内存数据源实现。
 */
class LevelDbDataSource(dbName: String) : DataSource<ByteArray, ByteArray> {

  lateinit var db: DB
  var alive: Boolean = false

  override val name = dbName

  override fun put(key: ByteArray, value: ByteArray) {
    db.put(key, value)
  }

  override fun get(key: ByteArray): ByteArray? {
    return db[key]
  }

  override fun delete(key: ByteArray) {
    db.delete(key)
  }

  override fun flush(): Boolean {
    return true
  }

  override fun init() {
    if (isAlive()) return

    val options = Options()
    options.createIfMissing(true)
    options.compressionType(CompressionType.NONE)
    options.blockSize(10 * 1024 * 1024)
    options.writeBufferSize(10 * 1024 * 1024)
    options.cacheSize(0)
    options.paranoidChecks(true)
    options.verifyChecksums(true)
    options.maxOpenFiles(32)

    val databaseDir = File(BlockChainConfig.getDatabaseDir())
    if (!databaseDir.exists() || !databaseDir.isDirectory) {
      databaseDir.mkdirs()

      val factory = Iq80DBFactory.factory
      db = factory.open(File(databaseDir.absolutePath + File.separator + name), options)

      alive = true
    }
  }

  override fun isAlive(): Boolean {
    return alive
  }

  override fun close() {
    db.close()
  }

  override fun updateBatch(rows: Map<ByteArray, ByteArray>) {
    for ((key, value) in rows) {
      put(key, value)
    }
  }

  override fun keys(): Set<ByteArray> {
    db.iterator().use { iterator ->
      val result = HashSet<ByteArray>()
      iterator.seekToFirst()
      while (iterator.hasNext()) {
        result.add(iterator.peekNext().key)
        iterator.next()
      }
      return result
    }
  }

}
