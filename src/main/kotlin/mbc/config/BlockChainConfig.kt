package mbc.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mbc.core.Block
import mbc.util.CryptoUtil
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.math.BigInteger
import java.security.PrivateKey
import java.util.*

class BlockChainConfig {

  private val DEFAULT_PEER_LISTEN_PORT = 9333

  private val DEFAULT_PEER_CONNECTION_TIMEOUT = 10

  private val DEFAULT_PEER_VERSION = 0x01

  private val MAINNET_NETWORK_ID = 1

  enum class DATABASE_TYPE {
    MEMORY, LEVELDB
  }

  private var minerCoinBase: ByteArray = ByteArray(0)

  private var defaultConfig: Config

  private val logger = LoggerFactory.getLogger(this.javaClass)

  constructor() {
    this.defaultConfig = ConfigFactory.load()
  }

  constructor(config: Config) : this() {
    this.defaultConfig = config
  }

  constructor(configFile: File) : this(ConfigFactory.parseFile(configFile))

  constructor(resource: String) : this(ConfigFactory.parseResources(resource))

  companion object {
    fun default(): BlockChainConfig {
      return BlockChainConfig()
    }
  }

  fun getConfig(): Config {
    if (defaultConfig.isEmpty) {
      val fileConfig = ConfigFactory.parseFile(File("conf" + File.separator + "application.conf"))

      defaultConfig = defaultConfig.withFallback(fileConfig)
    } else {
      return defaultConfig
    }
    return defaultConfig
  }

  /**
   * 得到矿工的Coinbase地址。
   */
  fun getMinerCoinbase(): ByteArray {
    if (this.minerCoinBase.size > 0) {
      return minerCoinBase
    } else {
      if (getConfig().hasPathOrNull("miner.coinbase")) {
        minerCoinBase = Hex.decode(getConfig().getString("miner.coinbase"))
        return minerCoinBase;
      } else {
        throw RuntimeException("Miner Coinbase is empty")
      }
    }
  }

  /**
   * 设置矿工的Coinbase地址。
   */
  fun setMinerCoinbase(newCoinbase: ByteArray) {
    this.minerCoinBase = newCoinbase
  }

  /**
   * Key-Value存储的实现。
   */
  fun getDatabaseType(): String {
    if (getConfig().hasPathOrNull("database.type")) {
      return getConfig().getString("database.type")
    } else {
      return DATABASE_TYPE.MEMORY.name
    }
  }

  /**
   * 数据库的存储目录。
   */
  fun getDatabaseDir(): String {
    if (getConfig().hasPathOrNull("database.dir")) {
      return getConfig().getString("database.dir")
    } else {
      return "database"
    }
  }

  /**
   * 节点的Private Key，如果没有配置私钥则自动生成公私钥对。
   */
  fun getNodeKey(): PrivateKey {
    val config = getConfig()
    if (config.hasPath("peer.privateKey")) {
      val key = config.getString("peer.privateKey")
      if (key.length != 64) {
        throw RuntimeException("The peer.privateKey needs to be Hex encoded and 32 byte length")
      }
      return CryptoUtil.deserializePrivateKey(Hex.decode(key))
    } else {
      val file = File(getDatabaseDir(), "nodeId.properties")
      val props = Properties()
      if (file.canRead()) {
        FileReader(file).use({ r -> props.load(r) })
        val key = props.getProperty("nodeIdPrivateKey")
        return CryptoUtil.deserializePrivateKey(Hex.decode(key))
      } else {
        val key = CryptoUtil.generateKeyPair()
        if (key != null) {
          props.setProperty("nodeIdPrivateKey", Hex.toHexString(key.private.encoded))
          props.setProperty("nodeId", Hex.toHexString(key.public.encoded))
          file.parentFile.mkdirs()
          FileWriter(file).use({ w ->
                                 props.store(w, "自动生成的NodeID，可以设置'peer.privateKey'来替换成自定义的NodeId")
                               })
          logger.info("自动生成NodeID: " + props.getProperty("nodeId"))
          logger.info("NodeID与私钥保存在 " + file)

          return key.private
        } else {
          throw Exception("NodeId or PrivateKey can not be empty.")
        }
      }
    }
  }

  /**
   * 节点的NodeId(PublicKey的序列化)。
   */
  fun getNodeId(): String {
    val privateKey = getNodeKey()
    val publicKey = CryptoUtil.generatePublicKey(privateKey)
    return Hex.toHexString(publicKey?.encoded)
  }


  /**
   * 节点的监听端口。
   */
  fun getPeerListenPort(): Int {
    val config = getConfig()
    if (config.hasPath("peer.listen.port")) {
      return config.getInt("peer.listen.port")
    }

    return DEFAULT_PEER_LISTEN_PORT
  }

  /**
   * 节点的连接超时(秒)。
   */
  fun getPeerConnectionTimeout(): Int {
    val config = getConfig()
    if (config.hasPath("peer.connection.timeout")) {
      return config.getInt("peer.connection.timeout")
    }

    return DEFAULT_PEER_CONNECTION_TIMEOUT * 1000
  }

  /**
   * 协议版本。
   */
  fun getPeerVersion(): Int {
    val config = getConfig()
    if (config.hasPath("peer.version")) {
      return config.getInt("peer.version")
    }

    return DEFAULT_PEER_VERSION
  }

  fun getClientId(): String {
    return "MiniBlockChain Kotlin Client"
  }

  fun getGenesisBlock(): Block {
    val genesisBlock = Block(1, 0, ByteArray(0), CryptoUtil.merkleRoot(emptyList()),
                             Hex.decode("1234567890123456789012345678901234567890"),
                             DateTime(2017, 2, 1, 0, 0), 0, 0, BigInteger.ZERO, emptyList())
    return genesisBlock
  }

  fun getNetworkId(): Int {
    val config = getConfig()
    if (config.hasPath("peer.networkId")) {
      return config.getInt("peer.networkId")
    }

    return MAINNET_NETWORK_ID
  }

}
