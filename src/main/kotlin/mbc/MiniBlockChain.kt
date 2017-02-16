package mbc

import mbc.config.BlockChainConfig
import mbc.network.client.PeerClient
import mbc.network.server.PeerServer

fun main(args: Array<String>) {
  val mbc = MiniBlockChain()
  mbc.init()
  mbc.start()
}

class MiniBlockChain {

  var config: BlockChainConfig

  constructor() : this(BlockChainConfig.default())

  constructor(config: BlockChainConfig) {
    this.config = config


  }

  lateinit var peerClient: PeerClient

  lateinit var peerServer: PeerServer

  fun init() {

    // 检查NodeId，如果不存在就自动生成NodeId。
    val nodeId = config.getNodeId()

  }

  fun start() {
  }
}
