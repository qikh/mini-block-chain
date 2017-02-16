package mbc.storage

import mbc.serialization.Serializer

/**
 * 对象存储类，可以接入不同的DbSource(Memory, LevelDb)和Serializer(AccountState, Transaction, Block)实现。
 */
class ObjectStore<V>(val db: DataSource<ByteArray, ByteArray>, val serializer: Serializer<V, ByteArray>): DataSource<ByteArray, V> {
  override val name = db.name

  override fun delete(key: ByteArray) {
    db.delete(key)
  }

  override fun flush(): Boolean {
    return db.flush()
  }

  override fun init() {
    db.init()
  }

  override fun isAlive(): Boolean {
    return db.isAlive()
  }

  override fun close() {
    db.close()
  }

  override fun updateBatch(rows: Map<ByteArray, V>) {
    val transformed = rows.mapValues { serializer.serialize(it.value) }
    db.updateBatch(transformed)
  }

  override fun keys(): Set<ByteArray> {
    return db.keys()
  }

  override fun reset() {
    db.reset()
  }

  override fun put(k: ByteArray, v: V) {
    db.put(k, serializer.serialize(v))
  }


  override fun get(k: ByteArray): V? {
    val bytes = db.get(k)
    if (bytes == null) {
      return null
    } else {
      return serializer.deserialize(bytes)
    }
  }

}
