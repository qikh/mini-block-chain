package mbc.core

import mbc.trie.Trie
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrieTest {

  @Test
  fun testTrie() {
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

}
