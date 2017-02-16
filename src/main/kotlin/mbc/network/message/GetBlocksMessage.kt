package mbc.network.message

import org.spongycastle.asn1.*

class GetBlocksMessage(val fromHeight: Long, val numOfBlocks: Int) : Message {
  override fun code(): Byte = MessageCodes.GET_BLOCKS.code

  override fun encode(): ByteArray {

    val v = ASN1EncodableVector()

    v.add(ASN1Integer(fromHeight))
    v.add(ASN1Integer(numOfBlocks.toLong()))

    return DERSequence(v).encoded
  }

  companion object {
    fun decode(bytes: ByteArray): GetBlocksMessage? {
      val v = ASN1InputStream(bytes).readObject()

      if (v != null) {
        val seq = ASN1Sequence.getInstance(v)

        val fromheight = ASN1Integer.getInstance(seq.getObjectAt(0))?.value?.toLong()
        val numOfBlocks = ASN1Integer.getInstance(seq.getObjectAt(1))?.value?.toInt()


        if (fromheight != null && numOfBlocks != null) {
          return GetBlocksMessage(fromheight, numOfBlocks)
        }
      }

      return null
    }
  }
}
