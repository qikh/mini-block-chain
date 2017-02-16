package mbc.storage

import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.util.*

/**
 * 内存数据源实现。
 */
class LevelDbDataSource(override val name: String, val databaseDir: String) : DataSource<ByteArray, ByteArray> {

  lateinit var db: DB
  var alive: Boolean = false

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

    val databaseDir = File(databaseDir)
    if (!databaseDir.exists() || !databaseDir.isDirectory) {
      databaseDir.mkdirs()
    }

    val factory = Iq80DBFactory.factory
    db = factory.open(File(databaseDir.absolutePath + File.separator + name), options)

    alive = true
  }

  override fun isAlive(): Boolean {
    return alive
  }

  override fun close() {
    db.close()
  }

  override fun updateBatch(rows: Map<ByteArray, ByteArray>) {
    db.createWriteBatch().use { batch ->
      for ((key, value) in rows) {
        if (value == null) {
          batch.delete(key)
        } else {
          batch.put(key, value)
        }
      }
      db.write(batch)
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

  override fun reset() {
    close()
    val databaseDir = File(databaseDir)
    databaseDir.deleteRecursively()
    init()
  }

}
