package mbc.miner

import mbc.core.Block

/**
 * 挖矿的结果数据。https://en.bitcoin.it/wiki/Block_hashing_algorithm
 */
data class MineResult(val success: Boolean, val difficulty: Int, val nonce: Int, val block: Block)
