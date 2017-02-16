package mbc.network.message

import mbc.core.Transaction
import mbc.util.CodecUtil
import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.ASN1Sequence
import org.spongycastle.asn1.DERSequence

class NewTransactionsMessage(val transactions: List<Transaction>) : Message {
  override fun code(): Byte {
    return MessageCodes.NEW_TRANSACTIONS.code
  }

  override fun encode(): ByteArray {
    val v = ASN1EncodableVector()

    transactions.map { v.add(ASN1InputStream(CodecUtil.encodeTransaction(it)).readObject()) } // transactions

    return DERSequence(v).encoded
  }

  companion object {
    fun decode(bytes: ByteArray): NewTransactionsMessage? {
      val v = ASN1InputStream(bytes).readObject()

      if (v != null) {
        val trxList = mutableListOf<Transaction>()

        val seq = ASN1Sequence.getInstance(v)

        for (element in seq.objects) {
          val trxSeq = DERSequence.getInstance(element) ?: return null
          val trx = CodecUtil.decodeTransactionFromSeq(trxSeq) ?: return null
          trxList.add(trx)
        }

        return NewTransactionsMessage(trxList)
      }

      return null
    }
  }

}
