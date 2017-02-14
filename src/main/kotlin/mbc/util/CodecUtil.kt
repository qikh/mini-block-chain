package mbc.util

import mbc.core.AccountState
import mbc.core.Block
import mbc.core.Transaction
import org.joda.time.DateTime
import org.spongycastle.asn1.*
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec


object CodecUtil {
  /**
   * 序列化账户状态(Account State)。(使用ASN.1规范)
   */
  fun encodeAccountState(accountState: AccountState): ByteArray {
    val v = ASN1EncodableVector()

    v.add(ASN1Integer(accountState.nonce))
    v.add(ASN1Integer(accountState.balance))

    return DERSequence(v).encoded
  }

  /**
   * 反序列化账户状态(Account State)。(使用ASN.1规范)
   */
  fun decodeAccountState(bytes: ByteArray): AccountState? {
    val v = ASN1InputStream(bytes)?.readObject()

    if (v != null) {
      val seq = ASN1Sequence.getInstance(v)
      val nonce = ASN1Integer.getInstance(seq.getObjectAt(0))?.value
      val balance = ASN1Integer.getInstance(seq.getObjectAt(1))?.value

      if (nonce != null && balance != null) {
        return AccountState(nonce, balance)
      }
    }

    return null
  }

  /**
   * 序列化交易(Transaction)。(使用ASN.1规范)
   */
  fun encodeTransaction(trx: Transaction): ByteArray {
    val v = ASN1EncodableVector()

    v.add(DERBitString(trx.senderAddress))
    v.add(DERBitString(trx.receiverAddress))
    v.add(ASN1Integer(trx.amount))
    v.add(ASN1Integer(trx.time.millis))
    v.add(DERBitString(trx.publicKey.encoded))

    return DERSequence(v).encoded
  }

  /**
   * 反序列化交易(Transaction)。(使用ASN.1规范)
   */
  fun decodeTransaction(bytes: ByteArray): Transaction? {
    val v = ASN1InputStream(bytes)?.readObject()

    if (v != null) {
      val seq = ASN1Sequence.getInstance(v)
      val senderAddress = DERBitString.getInstance(seq.getObjectAt(0))?.bytes
      val receiverAddress = DERBitString.getInstance(seq.getObjectAt(1))?.bytes
      val amount = ASN1Integer.getInstance(seq.getObjectAt(2))?.value
      val millis = ASN1Integer.getInstance(seq.getObjectAt(3))?.value
      val publicKeyBytes = DERBitString.getInstance(seq.getObjectAt(4))?.bytes

      val kf = KeyFactory.getInstance("EC", "SC")
      val publicKey = kf.generatePublic(X509EncodedKeySpec(publicKeyBytes))

      if (senderAddress != null && receiverAddress != null && amount != null && millis != null) {
        return Transaction(senderAddress, receiverAddress, amount, DateTime(millis.toLong()), publicKey)
      }
    }

    return null
  }

  /**
   * 序列化区块(Block)。(使用ASN.1规范)
   */
  fun encodeBlock(block: Block): ByteArray {

    val v = ASN1EncodableVector()

    v.add(ASN1Integer(block.version.toLong()))
    v.add(ASN1Integer(block.height))
    v.add(DERBitString(block.parentHash))
    v.add(DERBitString(block.coinBase))
    v.add(DERBitString(block.merkleRoot))
    v.add(ASN1Integer(block.difficulty.toLong()))
    v.add(ASN1Integer(block.nonce.toLong()))
    v.add(ASN1Integer(block.time.millis))

    v.add(ASN1Integer(block.transactions.size.toLong()))

    val t = ASN1EncodableVector()
    block.transactions.map { t.add(ASN1InputStream(encodeTransaction(it)).readObject()) } // transactions
    v.add(DERSequence(t))

    return DERSequence(v).encoded
  }

  /**
   * 反序列化区块(Block)。(使用ASN.1规范)
   */
  fun decodeBlock(bytes: ByteArray): Block? {
    val v = ASN1InputStream(bytes)?.readObject()

    if (v != null) {
      val seq = ASN1Sequence.getInstance(v)
      val version = ASN1Integer.getInstance(seq.getObjectAt(0)).value
      val height = ASN1Integer.getInstance(seq.getObjectAt(1)).value
      val parentHash = DERBitString.getInstance(seq.getObjectAt(2))?.bytes
      val minerAddress = DERBitString.getInstance(seq.getObjectAt(3))?.bytes
      val merkleRoot = DERBitString.getInstance(seq.getObjectAt(4))?.bytes
      val difficulty = ASN1Integer.getInstance(seq.getObjectAt(5))?.value
      val nonce = ASN1Integer.getInstance(seq.getObjectAt(6))?.value
      val millis = ASN1Integer.getInstance(seq.getObjectAt(7))?.value

      val trxSize = ASN1Integer.getInstance(seq.getObjectAt(8))?.value

      val trxValues = ASN1Sequence.getInstance(seq.getObjectAt(9))

      val trxList = mutableListOf<Transaction>()

      for (trxValue in trxValues.objects) {
        val trxObj = DERSequence.getInstance(trxValue) ?: return null
        val trx = decodeTransaction(trxObj.encoded) ?: return null
        trxList.add(trx)
      }

      if (version == null || height == null || parentHash == null || minerAddress == null ||
          merkleRoot == null || difficulty == null || nonce == null || millis == null) {
        return null
      }

      return Block(version.toInt(), height.toLong(), parentHash, CryptoUtil.merkleRoot(trxList), minerAddress, trxList,
                   DateTime(millis.toLong()), difficulty.toInt(), nonce.toInt())
    }

    return null
  }

}
