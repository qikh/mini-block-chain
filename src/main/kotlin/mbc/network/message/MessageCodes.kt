package mbc.network.message

/**
 * Message code list.
 */
enum class MessageCodes(val code: Byte) {
  /**
   * P2P Messages
   */
  HELLO(0x00),
  DISCONNECT(0x01),
  GET_NODES(0x03),
  NODES(0x04),

  /**
   * BlockChain Messages
   */
  STATUS(0x10),
  NEW_TRANSACTIONS(0x11),
  NEW_BLOCK(0x12),
  GET_BLOCKS(0x13),
  BLOCKS(0x14),

}
