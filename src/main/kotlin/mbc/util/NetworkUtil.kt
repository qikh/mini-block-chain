package mbc.util

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import mbc.network.message.Message

object NetworkUtil {

  fun sendMessage(channel: Channel, msg: Message) {
    val pipeline = channel.pipeline()

    val msgData = msg.encode()

    val len = 1 + msgData.size
    val buffer = Unpooled.buffer(len)

    buffer.writeInt(len)
    buffer.writeByte(msg.code().toInt())
    buffer.writeBytes(msgData)

    if (pipeline != null) {
      pipeline.write(Unpooled.wrappedBuffer(buffer))
      pipeline.flush()
    }
  }

}
