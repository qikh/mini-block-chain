package mbc.trie

import mbc.storage.DataSource
import mbc.util.CodecUtil
import mbc.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.spongycastle.asn1.*
import org.spongycastle.util.encoders.Hex
import java.util.*

/**
 * Convert string s to nibbles (half-bytes)
 *
 * >>> binToNibbbles("")
 * []
 * >>> binToNibbbles("h")
 * [6, 8]
 * >>> binToNibbbles("he")
 * [6, 8, 6, 5]
 * >>> binToNibbbles("hello")
 * [6, 8, 6, 5, 6, 12, 6, 12, 6, 15]
 **/
fun binToNibbbles(s: ByteArray): Array<Int> {

  val res = mutableListOf<Int>()
  for (c in s) {
    res.addAll(divMod(c, 16))
  }
  return res.toTypedArray()
}

fun divMod(c: Byte, i: Int): Array<Int> {
  val ui = if (c.toInt() > 0) c.toInt() else 256 + c.toInt()
  return arrayOf(ui / i, ui % i)
}


fun nibblesToBin(nibbles: Array<Int>): ByteArray {
  for (x in nibbles) {
    if (x > 15 || x < 0) {
      throw Exception("Nibbles can only be [0..15]")
    }

    if (nibbles.size % 2 != 0) {
      throw Exception("Nibbles must be of even numbers")
    }
  }

  val res = mutableListOf<Byte>()
  for (i in 0..nibbles.size - 1 step 2) {
    res += (16 * nibbles[i] + nibbles[i + 1]).toByte()
  }

  return res.toByteArray()
}

val NIBBLE_TERMINATOR = 16


fun withTerminator(nibbles: Array<Int>): Array<Int> {

  if (nibbles.isEmpty() || nibbles[nibbles.size - 1] != NIBBLE_TERMINATOR) {
    return nibbles + NIBBLE_TERMINATOR
  } else {
    return nibbles
  }
}

fun withoutTerminator(nibbles: Array<Int>): Array<Int> {
  if (nibbles.isNotEmpty() && nibbles[nibbles.size - 1] == NIBBLE_TERMINATOR) {
    return nibbles.copyOfRange(0, nibbles.size - 1)
  } else {
    return nibbles
  }
}

fun adaptTerminator(nibbles: Array<Int>, has_terminaltor: Boolean): Array<Int> {
  if (has_terminaltor) {
    return withTerminator(nibbles)
  } else {
    return withoutTerminator(nibbles)
  }
}

/**
 * Nibbles to binary.
 */
fun packNibbles(n: Array<Int>): ByteArray {
  var flags: Int
  var nibbles: Array<Int>

  if (n.last() == NIBBLE_TERMINATOR) {
    flags = 2
    nibbles = n.copyOfRange(0, n.size - 1)
  } else {
    flags = 0
    nibbles = n
  }

  val oddLen = nibbles.size % 2
  flags = flags or oddLen   // set lowest bit if odd number of nibbles
  if (oddLen == 1) {
    nibbles = arrayOf(flags) + nibbles
  } else {
    nibbles = arrayOf(flags, 0) + nibbles
  }

  val o = mutableListOf<Byte>()
  for (i in 0..nibbles.size - 1 step 2) {
    o += (16 * nibbles[i] + nibbles[i + 1]).toByte()
  }
  return o.toByteArray()
}

/**
 * Binary to nibbles.
 */
fun unpackToNibbles(bin: ByteArray): Array<Int> {

  var o = binToNibbbles(bin)

  // Terminator flag test.
  val flags = o[0]
  if (flags and 2 != 0) {
    o += NIBBLE_TERMINATOR
  }

  // Evenness test.
  if (flags and 1 == 1) { // Odd
    o = o.copyOfRange(1, o.size)
  } else { // Even
    o = o.copyOfRange(2, o.size)
  }

  return o
}

fun startWith(full: Array<Int>, part: Array<Int>): Boolean {
  if (full.size < part.size) {
    return false
  }

  return (0..part.size - 1).none { full[it] != part[it] }
}

enum class NodeType {
  NODE_TYPE_BLANK,
  NODE_TYPE_LEAF,
  NODE_TYPE_EXTENSION,
  NODE_TYPE_BRANCH
}

fun isKeyValueType(node_type: NodeType): Boolean {
  return node_type == NodeType.NODE_TYPE_LEAF || node_type == NodeType.NODE_TYPE_EXTENSION
}

val BLANK_ROOT = ByteArray(0)
val EMPTY_VALUE = ByteArray(0)
val BLANK_NODE = TrieNode(EMPTY_VALUE, EMPTY_VALUE, null)
val EMPTY_CHILDREN_LIST = arrayOf(EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE,
    EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE,
    EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE,
    EMPTY_VALUE)


class TrieNode(val key: ByteArray, val value: ByteArray, val children: Array<ByteArray>? = null) {
  init {
    if (children != null) {
      if (children.size != 16) {
        throw Exception("Children node for Trie Node must be array of 16 elements.")
      }
    }
  }

  val type: NodeType
    get() {
      if (key.isEmpty() && children == null) {
        return NodeType.NODE_TYPE_BLANK
      } else if (key.isNotEmpty() && children == null) {
        val nibbles = unpackToNibbles(key)
        val has_terminator = nibbles.last() == NIBBLE_TERMINATOR

        if (has_terminator) {
          return NodeType.NODE_TYPE_LEAF
        } else {
          return NodeType.NODE_TYPE_EXTENSION
        }
      } else if (key.isEmpty() && children != null) {
        return NodeType.NODE_TYPE_BRANCH
      } else {
        throw Exception("Unsupported Trie Node Type")
      }
    }

  override fun equals(other: Any?): Boolean {
    if (other is TrieNode) {
      if (!Arrays.equals(this.key, other.key)) {
        return false
      }
      if (!Arrays.equals(this.value, other.value)) {
        return false
      }

      if (this.children == null && other.children != null) {
        return false
      }

      if (this.children != null && other.children == null) {
        return false
      }

      if (this.children != null && other.children != null) {
        if (this.children.size != other.children.size) {
          return false
        }

        for (i in 0..children.size - 1) {
          if (!Arrays.equals(children[i], other.children[i])) {
            return false
          }
        }
      }

      return true
    } else {
      return false
    }
  }

  override fun toString(): String {
    if (type == NodeType.NODE_TYPE_BRANCH) {
      return "${this.children?.map { it.map(Byte::toString) }}"
    } else {
      return "${this.key.map(Byte::toString)} ${this.value.map(Byte::toString)} "
    }
  }
}

class PatriciaTrie {
  private val logger = LoggerFactory.getLogger(javaClass)

  var db: DataSource<ByteArray, ByteArray>
  var rootNode = BLANK_NODE

  constructor(db: DataSource<ByteArray, ByteArray>, rootHash: ByteArray = BLANK_ROOT) {
    this.db = db
    changeRoot(rootHash)
  }

  val rootHash: ByteArray
    get() {
      if (rootNode == BLANK_NODE) {
        return BLANK_ROOT
      } else {
        val hash = nodeHash(rootNode)
        return hash
      }
    }

  fun changeRoot(hash: ByteArray) {
    if (Arrays.equals(hash, BLANK_ROOT)) {
      this.rootNode = BLANK_NODE
    } else {
      this.rootNode = loadAndDecode(hash)
    }
  }

  fun loadAndDecode(hash: ByteArray): TrieNode {
    val nodeData = db.get(hash)
    if (nodeData != null) {
      return decodeToNode(nodeData)
    } else {
      return BLANK_NODE
    }
  }

  fun get(key: ByteArray): ByteArray {
    return get(rootNode, binToNibbbles(key))
  }

  private fun get(node: TrieNode, key: Array<Int>): ByteArray {
    when (node.type) {
      NodeType.NODE_TYPE_BLANK -> return EMPTY_VALUE
      NodeType.NODE_TYPE_BRANCH -> {
        if (key.isEmpty()) { // Key遍历结束，返回当前节点的Value
          return node.value
        } else if (node.children != null) { // 继续遍历Key
          val subNode = loadAndDecode(node.children[key[0]])
          return get(subNode, key.copyOfRange(1, key.size))
        }
      }
      NodeType.NODE_TYPE_LEAF -> {
        if (node.key.isNotEmpty()) {
          val currKey = withoutTerminator(unpackToNibbles(node.key))
          if (Arrays.equals(currKey, key)) {
            return node.value
          }
        }
      }
      NodeType.NODE_TYPE_EXTENSION -> {
        if (node.key.isNotEmpty()) {
          val currKey = withoutTerminator(unpackToNibbles(node.key))

          if (startWith(key, currKey) && node.value.isNotEmpty()) {
            val subNode = loadAndDecode(node.value)
            return get(subNode, key.copyOfRange(currKey.size, key.size))
          }
        }
      }
    }
    return EMPTY_VALUE
  }

  fun update(key: ByteArray, value: ByteArray) {
    if (value.isEmpty()) {
      delete(key)
    }

    rootNode = updateAndSaveNode(rootNode, binToNibbbles(key), value)

    //logger.debug("root hash before write to db: ${Hex.toHexString(rootHash)}")
  }

  private fun updateAndSaveNode(node: TrieNode, key: Array<Int>, value: ByteArray): TrieNode {
    val oldNode = node
    val newNode = update(node, key, value)

    if (oldNode != newNode) {
      //deleteNodeStorage(oldNode) // 不要删除旧的节点，否则无法实现root切换的功能。应该定期Prune Storage来清理无效节点。
      saveNodeToStorage(newNode)
    }
    return newNode
  }

  private fun update(node: TrieNode, key: Array<Int>, value: ByteArray): TrieNode {
    when (node.type) {
      NodeType.NODE_TYPE_BLANK -> return TrieNode(packNibbles(withTerminator(key)), value, null)
      NodeType.NODE_TYPE_BRANCH -> {
        if (key.isEmpty()) { // End of branch.
          return TrieNode(node.key, value, node.children)
        } else if (node.children != null) {
          val subNodeData = node.children[key[0]]
          val subNode = loadAndDecode(subNodeData)
          val newNode = updateAndSaveNode(subNode, key.copyOfRange(1, key.size), value)
          saveNodeToStorage(newNode)

          val children = node.children.copyOf()
          children[key[0]] = nodeHash(newNode)
          return TrieNode(node.key, value, children)
        }
      }
      NodeType.NODE_TYPE_LEAF -> {
        return updateKvNode(node, key, value)
      }
      NodeType.NODE_TYPE_EXTENSION -> {
        return updateKvNode(node, key, value)
      }
    }
    return BLANK_NODE
  }

  private fun updateKvNode(node: TrieNode, key: Array<Int>, value: ByteArray): TrieNode {

    val currKey = withoutTerminator(unpackToNibbles(node.key))

    val isLeaf = node.type == NodeType.NODE_TYPE_LEAF
    val isExtension = node.type == NodeType.NODE_TYPE_EXTENSION

    //logger.debug("this node is an extension node? $isExtension")
    //logger.debug("cur key , next key ${currKey.map(Int::toString)} ${key.map(Int::toString)}")

    var prefixLen = 0
    var commonSize = 0
    if (currKey.size < key.size) {
      commonSize = currKey.size
    } else {
      commonSize = key.size
    }
    for (i in 0..commonSize - 1) {
      if (key[i] != currKey[i]) {
        break
      }
      prefixLen++
    }

    var newNode: TrieNode
    val remainKey = key.copyOfRange(prefixLen, key.size)
    val remainCurrKey = currKey.copyOfRange(prefixLen, currKey.size)

    //logger.debug("remain keys..")
    //logger.debug("$prefixLen, ${remainKey.map(Int::toString)}, ${remainCurrKey.map(Int::toString)}")

    if (remainKey.isEmpty() && remainCurrKey.isEmpty()) { // Keys are same.
      //logger.debug("keys were same ${node.key.map(Byte::toString)} ${key.map(Int::toString)}")
      if (isLeaf) {
        //logger.debug("not an extension node")
        return TrieNode(node.key, value)
      } else {
        //logger.debug("yes an extension node!")
        newNode = updateAndSaveNode(loadAndDecode(node.value), remainKey, value)
      }
    } else if (remainCurrKey.isEmpty()) { // Old key exhausted.
      //logger.debug("old key exhausted")
      if (isExtension) {
        //logger.debug("\t is extension")
        newNode = updateAndSaveNode(loadAndDecode(node.value), remainKey, value)
      } else {
        //logger.debug("\t new branch")
        val children = EMPTY_CHILDREN_LIST
        val subNode = TrieNode(
            packNibbles(withTerminator(remainKey.copyOfRange(1, remainKey.size))), value)
        saveNodeToStorage(subNode)
        children[remainKey[0]] = nodeHash(subNode)

        newNode = TrieNode(EMPTY_VALUE, node.value, children)
      }
    } else { // Making branch
      //logger.debug("making a branch")
      val children = EMPTY_CHILDREN_LIST
      if (remainCurrKey.size == 1 && node.type == NodeType.NODE_TYPE_EXTENSION) {
        //logger.debug("key done and is inner")
        children[remainCurrKey[0]] = node.value
      } else {
        //logger.debug("key not done or not inner $node ${key.map(Int::toString)} ${value.map(Byte::toString)}")
        val subNode = TrieNode(packNibbles(
            adaptTerminator(remainKey.copyOfRange(1, remainKey.size),
                !isExtension)), node.value)
        saveNodeToStorage(subNode)
        children[remainCurrKey[0]] = nodeHash(subNode)
      }
      newNode = TrieNode(EMPTY_VALUE, EMPTY_VALUE, children)

      if (remainKey.isEmpty()) {
        newNode = TrieNode(newNode.key, value, newNode.children)
      } else {
        val children = EMPTY_CHILDREN_LIST
        val subNode = TrieNode(
            packNibbles(withTerminator(remainKey.copyOfRange(1, remainKey.size))), value)
        saveNodeToStorage(subNode)
        children[remainKey[0]] = nodeHash(subNode)
      }
    }

    saveNodeToStorage(newNode)

    if (prefixLen > 0) {
      //logger.debug("prefix length $prefixLen")
      newNode = TrieNode(packNibbles(currKey.copyOfRange(0, prefixLen)), nodeHash(newNode))
      //logger.debug("new node type ${newNode.type}")
    }

    return newNode
  }

  fun delete(key: ByteArray) {
    rootNode = deleteAndDeleteStorage(rootNode, binToNibbbles(key))
  }

  private fun deleteAndDeleteStorage(node: TrieNode, key: Array<Int>): TrieNode {
    val oldNode = node
    val newNode = delete(node, key)
    if (oldNode != newNode) {
      deleteNodeStorage(oldNode)
      saveNodeToStorage(newNode)
    }

    return newNode
  }

  private fun delete(node: TrieNode, key: Array<Int>): TrieNode {
    when (node.type) {
      NodeType.NODE_TYPE_BLANK -> return BLANK_NODE
      NodeType.NODE_TYPE_BRANCH -> return deleteBranchNode(node, key)
      NodeType.NODE_TYPE_LEAF -> return deleteKvNode(node, key)
      NodeType.NODE_TYPE_EXTENSION -> return deleteKvNode(node, key)
    }
  }

  private fun normalizeBranchNode(node: TrieNode): TrieNode {
    if (node.children != null) {
      val notBlankItemsCount = node.children.count { it.isNotEmpty() }

      if (notBlankItemsCount > 1) {
        return node
      }

      if (notBlankItemsCount == 0 && node.value.isNotEmpty()) {
        return TrieNode(packNibbles(withTerminator(arrayOf(0))), node.value)
      }

      val notBlankIndex = node.children.indexOfFirst { it.isNotEmpty() }

      val subNode = loadAndDecode(node.children[notBlankIndex])

      if (isKeyValueType(subNode.type)) {
        val newKey = arrayOf(notBlankIndex) + unpackToNibbles(subNode.key)
        return TrieNode(packNibbles(newKey), subNode.value)
      } else if (subNode.type == NodeType.NODE_TYPE_BRANCH) {
        return TrieNode(packNibbles(arrayOf(notBlankIndex)), nodeHash(subNode))
      }
    }

    return node
  }

  private fun deleteBranchNode(node: TrieNode, key: Array<Int>): TrieNode {
    if (key.isEmpty()) {
      return normalizeBranchNode(TrieNode(node.key, EMPTY_VALUE, node.children))
    }

    if (node.children != null) {
      val subNodeHash = nodeHash(
          deleteAndDeleteStorage(loadAndDecode(node.children[key[0]]),
              key.copyOfRange(1, key.size)))

      if (Arrays.equals(subNodeHash, node.children[key[0]])) {
        return node
      }

      val newChildren = node.children.copyOf()
      newChildren[key[0]] = subNodeHash
      val newNode = TrieNode(node.key, node.value, newChildren)
      if (subNodeHash.isEmpty()) {
        return normalizeBranchNode(newNode)
      }

      return newNode
    }

    return node
  }

  private fun deleteKvNode(node: TrieNode, key: Array<Int>): TrieNode {
    val currKey = withoutTerminator(unpackToNibbles(node.key))

    if (!startWith(key, currKey)) {
      return node
    }

    if (node.type == NodeType.NODE_TYPE_LEAF) {
      if (Arrays.equals(key, currKey)) {
        return BLANK_NODE
      } else {
        return node
      }
    } else {
      val newSubNode = deleteAndDeleteStorage(loadAndDecode(node.value),
          key.copyOfRange(currKey.size, key.size))

      if (Arrays.equals(nodeHash(newSubNode), node.value)) {
        return node
      }

      if (newSubNode == BLANK_NODE) {
        return BLANK_NODE
      }

      if (isKeyValueType(newSubNode.type)) {
        val newKey = currKey + unpackToNibbles(newSubNode.key)
        return TrieNode(packNibbles(newKey), newSubNode.value)
      } else {
        return TrieNode(packNibbles(currKey), nodeHash(newSubNode))
      }
    }
  }

  private fun deleteNodeStorage(node: TrieNode) {
    if (node == BLANK_NODE) return

    val hash = nodeHash(node)

    logger.debug("DELETE NODE: ${Hex.toHexString(hash)}")
    db.delete(hash)
  }

  fun nodeHash(node: TrieNode): ByteArray {
    return CryptoUtil.sha3(encodeNode(node))
  }

  fun encodeNode(node: TrieNode): ByteArray {
    val vec = ASN1EncodableVector()

    when (node.type) {
      NodeType.NODE_TYPE_BLANK -> {
        return EMPTY_VALUE
      }
      NodeType.NODE_TYPE_LEAF -> {
        vec.add(CodecUtil.asn1Encode(node.key))
        vec.add(CodecUtil.asn1Encode(node.value))
      }
      NodeType.NODE_TYPE_EXTENSION -> {
        vec.add(CodecUtil.asn1Encode(node.key))
        vec.add(CodecUtil.asn1Encode(node.value))
      }
      NodeType.NODE_TYPE_BRANCH -> {
        // 16 slots
        node.children?.forEach {
          vec.add(CodecUtil.asn1Encode(it))
        }

        // Value object if end of key index.
        vec.add(CodecUtil.asn1Encode(node.value))
      }
    }

    val bin = DERSequence(vec).encoded

    return bin
  }

  fun decodeToNode(bytes: ByteArray): TrieNode {
    if (Arrays.equals(bytes, EMPTY_VALUE)) return BLANK_NODE

    val asn1: ASN1Primitive?
    try {
      asn1 = ASN1InputStream(bytes).readObject()
    } catch (e: Exception) {
      return BLANK_NODE
    }

    /**
     * Decode为数组类型，根据数组的size(0,2,17)来分别Decode为blank, normal/extension, branch.
     *
     * Decode为Binary类型，以Decode值为Key读取Db的Value，然后继续decode。
     */
    if (asn1 is ASN1Sequence) {
      val v = DERSequence.getInstance(asn1)

      if (v != null) {
        when (v.size()) {
          0 -> return BLANK_NODE
          2 -> {
            val key = DERBitString.getInstance(v.getObjectAt(0)).bytes
            val value = DERBitString.getInstance(v.getObjectAt(1)).bytes
            return TrieNode(key, value, null)
          }
          17 -> {
            val children = mutableListOf<ByteArray>()
            for (i in 0..15) {
              children.add(DERBitString.getInstance(v.getObjectAt(i)).bytes)
            }

            val value = DERBitString.getInstance(v.getObjectAt(16)).bytes

            return TrieNode(EMPTY_VALUE, value, children.toTypedArray())
          }
        }
      }

    }

    return BLANK_NODE
  }

  private fun saveNodeToStorage(node: TrieNode) {
    val bin = encodeNode(node)
    val hash = CryptoUtil.sha3(bin)
    db.put(hash, bin)
    //logger.debug("SAVE NODE: ${Hex.toHexString(hash)}")
  }

}
