package mbc.storage

import org.spongycastle.util.encoders.Hex

/**
 * 内存数据源实现。
 */
class MemoryDataSource(override val name: String) : DataSource<ByteArray, ByteArray> {

  val db = mutableMapOf<String, ByteArray>()

  override fun put(key: ByteArray, value: ByteArray) {
    db.put(Hex.toHexString(key), value)
  }

  override fun get(key: ByteArray): ByteArray? {
    return db[Hex.toHexString(key)]
  }

  override fun delete(key: ByteArray) {
    db.remove(Hex.toHexString(key))
  }

  override fun flush(): Boolean {
    return true
  }

  override fun init() {}

  override fun isAlive(): Boolean {
    return true
  }

  override fun close() {}

  override fun updateBatch(rows: Map<ByteArray, ByteArray>) {
    for ((key, value) in rows) {
      put(key, value)
    }
  }

  override fun keys(): Set<ByteArray> {
    val stringKeys = db.keys
    val result = mutableSetOf<ByteArray>()
    stringKeys.map { result.add(Hex.decode(it)) }
    return result
  }

  override fun reset() {

  }

}
