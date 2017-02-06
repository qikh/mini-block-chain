package mbc.core

import mbc.util.CryptoUtil
import org.joda.time.DateTime
import java.security.PrivateKey
import java.security.PublicKey

/**
 * 交易记录类：记录了发送方(sender)向接受方(receiver)的转账记录，包括金额(amount)和时间戳(time)。
 * 为简化模型，没有加入费用(fee)。
 */
class Transaction(val senderAddress: String, val receiverAddress: String, val amount: Long,
                       val time: DateTime, val publicKey: PublicKey) {

  /**
   * 签名数据，初始值为byte[0]
   */
  var signature: ByteArray = ByteArray(0)

  /**
   * 交易合法性验证。目前只验证签名长度和签名合法性。
   */
  val isValid: Boolean
    get() = (signature.size > 0 && CryptoUtil.verifyTransactionSignature(this, signature))

  /**
   * 用发送方的私钥进行签名。
   */
  fun sign(privateKey: PrivateKey): ByteArray {
    signature = CryptoUtil.signTransaction(this, privateKey)
    return signature
  }

}
