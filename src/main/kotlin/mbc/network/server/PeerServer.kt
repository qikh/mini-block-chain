package mbc.network.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import mbc.core.BlockChainManager
import mbc.core.Node
import mbc.network.Peer
import mbc.network.message.HelloMessage
import mbc.network.message.MessageCodes
import mbc.network.message.handler.MessageDecodeHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/**
 * PeerServer服务器类，监听端口并处理Peer的连接请求。
 */
class PeerServer(val manager: BlockChainManager) {

  private val logger = LoggerFactory.getLogger(javaClass)

  val group = NioEventLoopGroup()

  val listenPort = manager.blockChain.config.getPeerListenPort()

  var isClosed = false

  fun start(): Boolean {
    val group = NioEventLoopGroup()
    try {
      val b = ServerBootstrap()
      b.group(group)
          .channel(NioServerSocketChannel::class.java)
          .localAddress(InetSocketAddress(listenPort))

      b.childHandler(object : ChannelInitializer<NioSocketChannel>() {
        override fun initChannel(ch: NioSocketChannel) {
          ch.pipeline()
              .addLast(LengthFieldBasedFrameDecoder(Int.MAX_VALUE, 0, 4, 0, 4))
              .addLast(PeerServerHelloMessageHandler(manager)) // 对消息数据进行解码和处理
        }
      })

      val f = b.bind()

      if (f.awaitUninterruptibly(10, TimeUnit.SECONDS)) {
        if (f.isSuccess) {
          logger.info("PeerServer started and listen on " + listenPort)
          return true
        } else {
          logger.error("PeerServer failed to listen on " + listenPort)
          return false
        }
      }

    } catch (e: Exception) {
      isClosed = true
    } finally {
      if (isClosed) {
        group.shutdownGracefully().sync()
      }
    }
    return false
  }

  fun closeAsync() {
    logger.info("PeerServer close")
    group.shutdownGracefully()
  }

}

class PeerServerHelloMessageHandler(val manager: BlockChainManager) : ByteToMessageDecoder() {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * 服务器端返回HELLO消息后握手完成。
   */
  override fun decode(ctx: ChannelHandlerContext, data: ByteBuf, out: MutableList<Any>) {

    val code = data.readByte()

    val buffer = ByteArray(data.readableBytes())
    data.readBytes(buffer)

    if (code == MessageCodes.HELLO.code) {
      val msg = HelloMessage.decode(buffer)

      if (msg == null) { // 对方返回的数据无法正确解码, 断开连接。
        ctx.close()
      } else { // 握手完成！
        val node = Node(msg.nodeId, ctx.channel().remoteAddress().toString(), msg.listenPort)
        val peer = Peer(node, manager, ctx.channel())

        if (manager.peerConnected(peer)) {
          ctx.pipeline().remove(this)
          peer.close()
          return
        }

        peer.sendHelloMessage()

        logger.info(
            "PeerServer(${peer.channel.localAddress()}) finished handshake with ${peer.channel.remoteAddress()}")

        peer.handshakeComplete = true

        peer.sendStatusMessage()

        manager.addPeer(peer)

        // 移除HELLO消息处理类
        ctx.pipeline().remove(this)

        // 增加区块链消息处理类
        ctx.pipeline().addLast(MessageDecodeHandler(peer))
      }
    }
  }

}
