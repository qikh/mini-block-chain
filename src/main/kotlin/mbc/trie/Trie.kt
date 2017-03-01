package mbc.trie

import mbc.util.CodecUtil
import mbc.util.CryptoUtil
import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.DERSequence
import org.spongycastle.util.encoders.Hex

class Trie<Value> {

  /**
   * Radix of the Trie.
   */
  val radix = 256

  /**
   * Root TrieNode.
   */
  var root: TrieNode<Value>? = null

  /**
   * Trie TrieNode.
   */
  class TrieNode<Value> {

    val radix = 256
    val EMPTY_VALUE = ByteArray(0)

    var value: Value? = null
    val next = kotlin.arrayOfNulls<TrieNode<Value>>(radix)

    fun hash(): ByteArray {
      val bin = encode()

      val hash = CryptoUtil.sha256(bin)
      return hash
    }

    private fun encode(): ByteArray {
      val vec = ASN1EncodableVector()

      if (value != null) {
        vec.add(CodecUtil.asn1Encode(value!!))
      } else {
        vec.add(CodecUtil.asn1Encode(EMPTY_VALUE))
      }

      val nextVec = ASN1EncodableVector()
      next.forEach {
        // 如果是Null就序列化为Byte(0)
        if (it == null) {
          nextVec.add(CodecUtil.asn1Encode(EMPTY_VALUE))
        } else if (it is TrieNode) {
          nextVec.add(CodecUtil.asn1Encode(it.hash()))
        }
      }
      vec.add(DERSequence(nextVec))

      val bin = DERSequence(vec).encoded
      return bin
    }
  }

  fun get(key: String): Value? {
    val x = getSubNode(root, key, 0) ?: return null
    return x.value
  }

  private fun getSubNode(x: TrieNode<Value>?, key: String, d: Int): TrieNode<Value>? {
    if (x == null) return null
    if (d == key.length) return x
    val c = key[d]
    return getSubNode(x.next[c.toInt()], key, d + 1)
  }

  fun put(key: String, v: Value?) {
    if (v == null)
      delete(key)
    else
      root = putSubNode(root, key, v, 0)
  }

  private fun putSubNode(x: TrieNode<Value>?, key: String, v: Value, d: Int): TrieNode<Value> {
    var node: TrieNode<Value>?

    if (x == null) {
      node = TrieNode<Value>()
    } else {
      node = x
    }

    // 如果到达Radix的最后一位，完成节点构造并返回。
    if (d == key.length) {
      node.value = v
      return node
    }

    // 继续构造节点。
    val c = key[d]
    node.next[c.toInt()] = putSubNode(node.next[c.toInt()], key, v, d + 1)
    return node
  }

  fun delete(key: String) {
    root = deleteNode(root, key, 0)
  }

  private fun deleteNode(x: TrieNode<Value>?, key: String, d: Int): TrieNode<Value>? {
    /**
     * 1. 检查节点是否为Null。
     */
    if (x == null) return null

    /**
     * 2. 如果到达Radix的最后一位，删除节点数据。否则继续执行删除节点操作。
     */
    if (d == key.length) {
      x.value = null
    } else {
      val c = key[d]
      x.next[c.toInt()] = deleteNode(x.next[c.toInt()], key, d + 1)
    }

    /**
     * 3. 遍历删除后当前节点的Value如果不为空，说明Root没有变化。否则Root节点替换为子节点(Value不为空)。
     */
    if (x.value != null) {
      return x
    } else {
      for (c in 0..radix - 1) {
        if (x.next[c] != null) {
          return x
        }
      }
    }
    return null
  }

}

fun main(args: Array<String>) {
  val trie1 = Trie<Int>()

  trie1.put("hello", 342)
  println(Hex.toHexString(trie1.root?.hash()))

  trie1.put("message", 432)
  println(Hex.toHexString(trie1.root?.hash()))

  trie1.put("message2", 456)
  println(Hex.toHexString(trie1.root?.hash()))

  trie1.put("message3", 555)
  println(Hex.toHexString(trie1.root?.hash()))

  trie1.delete("message2")
  println(Hex.toHexString(trie1.root?.hash()))

  println(trie1.get("hello"))
}
