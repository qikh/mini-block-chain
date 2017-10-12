package mbc.network.message

import org.spongycastle.asn1.*
import java.math.BigInteger

/**
 * HELLO消息，
 */
class StatusMessage(val protocolVersion: Int, val networkId: Int, val totalDifficulty: BigInteger,
    val bestHash: ByteArray, val genesisHash: ByteArray) : Message {

  override fun code(): Byte = MessageCodes.STATUS.code

  override fun encode(): ByteArray {

    val v = ASN1EncodableVector()

    v.add(ASN1Integer(protocolVersion.toLong()))
    v.add(ASN1Integer(networkId.toLong()))
    v.add(ASN1Integer(totalDifficulty.toLong()))
    v.add(DERBitString(bestHash))
    v.add(DERBitString(genesisHash))

    return DERSequence(v).encoded
  }

  companion object {
    fun decode(bytes: ByteArray): StatusMessage? {
      val v = ASN1InputStream(bytes).readObject()

      if (v != null) {
        val seq = ASN1Sequence.getInstance(v)

        val protocolVersion = ASN1Integer.getInstance(seq.getObjectAt(0))?.value?.toInt()
        val networkId = ASN1Integer.getInstance(seq.getObjectAt(1))?.value?.toInt()
        val totalDifficulty = ASN1Integer.getInstance(seq.getObjectAt(2))?.value
        val bestHash = DERBitString.getInstance(seq.getObjectAt(3))?.bytes
        val genesisHash = DERBitString.getInstance(seq.getObjectAt(4))?.bytes

        if (protocolVersion != null && networkId != null && totalDifficulty != null && bestHash != null && genesisHash != null) {
          return StatusMessage(protocolVersion, networkId, totalDifficulty, bestHash, genesisHash)
        }
      }

      return null
    }
  }
}
