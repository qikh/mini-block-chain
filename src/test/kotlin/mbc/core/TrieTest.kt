package mbc.core

import mbc.storage.MemoryDataSource
import mbc.trie.Trie
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.io.File
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrieTest {

  @Test fun testTrie() {
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

    assertEquals(trie1.get("hello"), 342)
    assertNull(trie1.get("helo"))
  }

  @Test fun readDb() {
    val db: DB
    val options = Options()
    options.createIfMissing(false)

    val factory = Iq80DBFactory.factory
    db = factory.open(File("/Users/qikh/github/understanding_ethereum_trie/triedb"), options)

    db.iterator().use { iterator ->
      val result = HashSet<ByteArray>()
      iterator.seekToFirst()
      while (iterator.hasNext()) {
        result.add(iterator.peekNext().key)
        iterator.next()
      }

      result.forEach { println(it) }
    }
  }
}
