package mbc.miner

/**
 * 挖矿的结果数据。https://en.bitcoin.it/wiki/Block_hashing_algorithm
 */
data class MineResult(val difficulty: Int, val nonce: Int)
