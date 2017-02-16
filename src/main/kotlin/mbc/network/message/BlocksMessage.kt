package mbc.network.message

import mbc.core.Block
import mbc.util.CodecUtil
import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.ASN1Sequence
import org.spongycastle.asn1.DERSequence

class BlocksMessage(val blocks: List<Block>) : Message {
  override fun code(): Byte = MessageCodes.BLOCKS.code

  override fun encode(): ByteArray {

    val v = ASN1EncodableVector()

    blocks.forEach { v.add(CodecUtil.encodeBlockToAsn1(it)) }

    return DERSequence(v).encoded
  }

  companion object {
    fun decode(bytes: ByteArray): BlocksMessage? {
      val v = ASN1InputStream(bytes).readObject()

      if (v != null) {
        val seq = ASN1Sequence.getInstance(v)

        val blocks = mutableListOf<Block>()
        for (element in seq.objects) {
          val der = DERSequence.getInstance(element)
          val block = CodecUtil.decodeBlock(der.encoded)
          if (block != null) {
            blocks.add(block)
          }
        }

        return BlocksMessage(blocks)
      }

      return null
    }
  }
}
