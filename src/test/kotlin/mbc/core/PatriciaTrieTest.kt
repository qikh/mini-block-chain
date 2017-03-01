package mbc.core

import mbc.storage.LevelDbDataSource
import mbc.trie.*
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PatriciaTrieTest {

  @Test fun testBinToNibbles() {
    val res0 = binToNibbbles("".toByteArray())
    assertArrayEquals(res0, arrayOf<Int>())

    val res1 = binToNibbbles("h".toByteArray())
    assertArrayEquals(res1, arrayOf(6, 8))

    val res2 = binToNibbbles("he".toByteArray())
    assertArrayEquals(res2, arrayOf(6, 8, 6, 5))

    val res3 = binToNibbbles("hello".toByteArray())
    assertArrayEquals(res2, arrayOf(6, 8, 6, 5, 6, 12, 6, 12, 6, 15))
  }

  @Test fun testNibblesToBin() {
    val res0 = nibblesToBin(arrayOf())
    assertArrayEquals(res0, "".toByteArray())

    val res1 = nibblesToBin(arrayOf(6, 8))
    assertArrayEquals(res1, "h".toByteArray())

    val res2 = nibblesToBin(arrayOf(6, 8, 6, 5))
    assertArrayEquals(res2, "he".toByteArray())

    val res3 = nibblesToBin(arrayOf(6, 8, 6, 5, 6, 12, 6, 12, 6, 15))
    assertArrayEquals(res3, "hello".toByteArray())
  }

  @Test fun testPackNibbles() {
    val key = arrayOf(0, 1, 0, 1, 0, 2)
    val packed = packNibbles(withTerminator(key))

    val expected = arrayOf(0x20.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte()).toByteArray()
    assertArrayEquals(packed, expected)
  }

  @Test fun testUnpackToNibbles() {
    val bin = arrayOf(0x20.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte()).toByteArray()
    val nibbles = unpackToNibbles(bin)

    assertArrayEquals(nibbles, arrayOf(0, 1, 0, 1, 0, 2, NIBBLE_TERMINATOR))
  }

  @Test fun testTrieNodeEncodeDecode() {
    val blankNode = BLANK_NODE
    val trie = PatriciaTrie(LevelDbDataSource("test", "test-database"))
    val blankNodeEncoded = trie.encodeNode(blankNode)
    assertNotNull(blankNodeEncoded)
    assertEquals(trie.decodeToNode(blankNodeEncoded), BLANK_NODE)

    val leafNode = TrieNode(arrayOf(0x20.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte()).toByteArray(),
                            "hello".toByteArray(), null)
    assertEquals(leafNode.type, NODE_TYPE.NODE_TYPE_LEAF)

    val leafNodeEncoded = trie.encodeNode(leafNode)
    assertNotNull(leafNodeEncoded)
    assertEquals(trie.decodeToNode(leafNodeEncoded), leafNode)

    val extensionNode = TrieNode(arrayOf(0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte()).toByteArray(),
                                 EMPTY_VALUE, null)
    assertEquals(extensionNode.type, NODE_TYPE.NODE_TYPE_EXTENSION)

    val extensionNodeEncoded = trie.encodeNode(extensionNode)
    assertNotNull(extensionNodeEncoded)
    assertEquals(trie.decodeToNode(extensionNodeEncoded), extensionNode)

    val branchNode = TrieNode(EMPTY_VALUE, "hello".toByteArray(),
                              arrayOf(EMPTY_VALUE, "234".toByteArray(), "3233".toByteArray(), EMPTY_VALUE, EMPTY_VALUE,
                                      EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE,
                                      EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE))
    assertEquals(branchNode.type, NODE_TYPE.NODE_TYPE_BRANCH)

    val branchNodeEncoded = trie.encodeNode(branchNode)
    assertNotNull(branchNodeEncoded)
    assertEquals(trie.decodeToNode(branchNodeEncoded), branchNode)
  }

  @Test fun testPutGet() {
    val db = LevelDbDataSource("test", "test-database")
    db.init()
    val trie = PatriciaTrie(db)
    trie.update(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray(), "11111".toByteArray())
    trie.update(arrayOf<Byte>(0x01, 0x01, 0x03).toByteArray(), "22222".toByteArray())

    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray()), "11111".toByteArray())
    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x03).toByteArray()), "22222".toByteArray())

    trie.delete(arrayOf<Byte>(0x01, 0x01, 0x03).toByteArray())

    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x03).toByteArray()), EMPTY_VALUE)
    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray()), "11111".toByteArray())
  }

  @Test fun testChangeRoot() {
    val db = LevelDbDataSource("test", "test-database")
    db.init()
    val trie = PatriciaTrie(db)
    trie.update(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray(), "11111".toByteArray())
    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray()), "11111".toByteArray())
    val rootHash1 = trie.rootHash


    trie.update(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray(), "22222".toByteArray())
    val rootHash2 = trie.rootHash
    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray()), "22222".toByteArray())

    trie.changeRoot(rootHash1)
    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray()), "11111".toByteArray())

    trie.changeRoot(rootHash2)
    assertArrayEquals(trie.get(arrayOf<Byte>(0x01, 0x01, 0x02).toByteArray()), "22222".toByteArray())
  }
}
