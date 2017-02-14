package mbc.storage

import mbc.serialization.Serializer

/**
 * 对象存储类，可以接入不同的DbSource(Memory, LevelDb)和Serializer(AccountState, Transaction, Block)实现。
 */
class ObjectStore<V>(val db: DataSource<ByteArray, ByteArray>, val serializer: Serializer<V, ByteArray>) {
  fun put(k: ByteArray, v: V) {
    db.put(k, serializer.serialize(v))
  }


  fun get(k: ByteArray): V? {
    val bytes = db.get(k)
    if (bytes == null) {
      return null
    } else {
      return serializer.deserialize(bytes)
    }
  }

}
