package mbc

import mbc.config.BlockChainConfig
import mbc.core.BlockChain
import mbc.core.BlockChainManager
import mbc.network.client.PeerClient
import mbc.network.server.PeerServer
import org.apache.commons.cli.*
import java.io.File


fun main(args: Array<String>) {
  println("MiniBlockChain starting ......")
  val options = Options()

  val configOption = Option("c", "config", true, "config file path")
  configOption.isRequired = false
  options.addOption(configOption)

  val parser = DefaultParser()
  val formatter = HelpFormatter()
  val cmd: CommandLine

  try {
    cmd = parser.parse(options, args)
  } catch (e: ParseException) {
    println(e.message)
    formatter.printHelp("MiniBlockChain", options)

    System.exit(1)
    return
  }

  var mbc: MiniBlockChain
  val configFilePath = cmd.getOptionValue("config") ?: "conf/application.conf"
  val configFile = File(configFilePath)
  if (!configFile.exists()) {
    mbc = MiniBlockChain()
  } else {
    mbc = MiniBlockChain(BlockChainConfig(configFile))
  }
  mbc.init()
  mbc.start()
  println("MiniBlockChain started.")
}

class MiniBlockChain(val config: BlockChainConfig = BlockChainConfig.default()) {

  lateinit var server: PeerServer

  val blockChain = BlockChain(config)

  fun init() {
  }

  fun start() {
    val manager = BlockChainManager(blockChain)

    server = PeerServer(manager)
    server.start()

    manager.startPeerDiscovery()
//        manager.startMining()
  }
}
