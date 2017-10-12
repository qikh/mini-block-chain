package mbc.core

import org.joda.time.DateTime
import java.math.BigInteger

class BlockHeader(val version: Int, val height: Long, val parentHash: ByteArray,
    val coinBase: ByteArray, val time: DateTime, val difficulty: Int,
    val nonce: Int, val totalDifficulty: BigInteger, val stateRoot: ByteArray, val trxTrieRoot: ByteArray)
