package mbc.serialization

import mbc.core.AccountState
import mbc.core.Block
import mbc.core.Transaction
import mbc.util.CodecUtil

/**
 * 序列化/反序列化接口。
 */
interface Serializer<T, S> {
  /**
   * Converts T ==> S
   * Should correctly handle null parameter
   */
  fun serialize(obj: T): S

  /**
   * Converts S ==> T
   * Should correctly handle null parameter
   */
  fun deserialize(s: S): T?
}

class AccountStateSerialize : Serializer<AccountState, ByteArray> {
  override fun deserialize(s: ByteArray): AccountState? {
    return CodecUtil.decodeAccountState(s)
  }

  override fun serialize(obj: AccountState): ByteArray {
    return CodecUtil.encodeAccountState(obj)
  }

}

class BlockSerialize : Serializer<Block, ByteArray> {
  override fun deserialize(s: ByteArray): Block? {
    return CodecUtil.decodeBlock(s)
  }

  override fun serialize(obj: Block): ByteArray {
    return CodecUtil.encodeBlock(obj)
  }

}

class TransactionSerialize : Serializer<Transaction, ByteArray> {
  override fun deserialize(s: ByteArray): Transaction? {
    return CodecUtil.decodeTransaction(s)
  }

  override fun serialize(obj: Transaction): ByteArray {
    return CodecUtil.encodeTransaction(obj)
  }

}
