package mbc.network.message

/**
 * 消息接口。
 */
interface Message {

  /**
   * 消息的Code
   *
   * @see MessageCodes
   */
  fun code(): Byte

  fun encode(): ByteArray

}
