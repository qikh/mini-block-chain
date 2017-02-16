package mbc.storage

import java.math.BigInteger

class BlockInfo(val hash: ByteArray, val isMain: Boolean, val totalDifficulty: BigInteger)
