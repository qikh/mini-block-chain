package mbc.network.message

import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.DERSequence

/**
 * Get Peers，
 */
class GetNodesMessage : Message {

  override fun code(): Byte = MessageCodes.GET_NODES.code

  override fun encode(): ByteArray {

    val v = ASN1EncodableVector()

    return DERSequence(v).encoded
  }

  companion object {
    fun decode(bytes: ByteArray): GetNodesMessage? {
      val v = ASN1InputStream(bytes).readObject()

      if (v != null) {
        return GetNodesMessage()
      }

      return null
    }
  }
}
