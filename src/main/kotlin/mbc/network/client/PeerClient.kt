package mbc.network.client

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import mbc.core.BlockChainManager
import mbc.core.Node
import mbc.network.Peer
import mbc.network.message.HelloMessage
import mbc.network.message.MessageCodes
import mbc.network.message.handler.MessageDecodeHandler
import mbc.util.NetworkUtil
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * PeerClient客户端类，向已知的Peer发起连接请求。每个BlockChain应该只有一个PeerClient。
 */
class PeerClient(val manager: BlockChainManager) {

  private val logger = LoggerFactory.getLogger(javaClass)

  val group = NioEventLoopGroup()

  fun connectAsync(node: Node) {

    val b = Bootstrap()
    b.group(group)
        .channel(NioSocketChannel::class.java)
        .remoteAddress(InetSocketAddress(node.ip, node.port))

    b.handler(object : ChannelInitializer<NioSocketChannel>() {
      override fun initChannel(ch: NioSocketChannel) {
        ch.pipeline()
            .addLast(LengthFieldBasedFrameDecoder(Int.MAX_VALUE, 0, 4, 0, 4)) // 4个Byte的Length Header
            .addLast(PeerClientHelloMessageHandler(node, manager)) // 对握手数据进行解码和处理
      }
    })

    b.connect()
  }

  fun closeAsync() {
    logger.info("PeerClient close")
    group.shutdownGracefully()
  }

}

/**
 * PeerClient的握手消息处理。
 */
class PeerClientHelloMessageHandler(val node: Node, val manager: BlockChainManager) : ByteToMessageDecoder() {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * 建立连接后应该首先握手(发送并接收HELLO消息)。
   */
  override fun channelActive(ctx: ChannelHandlerContext?) {
    super.channelActive(ctx)

    val channel = ctx?.channel()
    if (channel != null) {
      sendHelloMessage(channel)
    }
  }

  /**
   * 服务器端返回HELLO消息后握手完成。
   */
  override fun decode(ctx: ChannelHandlerContext, data: ByteBuf, out: MutableList<Any>) {

    val code = data.readByte()

    val buffer = ByteArray(data.readableBytes())
    data.readBytes(buffer)

    if (code == MessageCodes.HELLO.code) {
      val msg = HelloMessage.decode(buffer)

      if (msg == null) { // 对方返回的数据无法正确解码，关闭连接。
        ctx.close()
      } else { // 握手完成！
        val peer = Peer(node, manager, ctx.channel())

        logger.info(
            "PeerClient(${peer.channel.localAddress()}) finished handshake with ${peer.channel.remoteAddress()}")

        peer.handshakeComplete = true

        peer.sendStatusMessage()

        // 将当前Peer加入到BlockChainManager
        manager.addPeer(peer)

        // 移除HELLO消息处理类
        ctx.pipeline().remove(this)

        // 增加区块链消息处理类
        ctx.pipeline().addLast(MessageDecodeHandler(peer))
      }
    }

  }

  fun sendHelloMessage(channel: Channel) {
    val config = manager.blockChain.config

    val msg = HelloMessage(config.getPeerVersion(), config.getClientId(), config.getPeerListenPort(),
        config.getNodeId())

    logger.debug("Client ${channel.localAddress()} say HELLO to ${channel.remoteAddress()}")

    NetworkUtil.sendMessage(channel, msg)
  }

}
