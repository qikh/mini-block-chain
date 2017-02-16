package mbc.network.message

import org.spongycastle.asn1.*

/**
 * HELLO消息，
 */
class HelloMessage(val version: Int, val clientId: String, val listenPort: Int, val nodeId: String) : Message {

  override fun code(): Byte = MessageCodes.HELLO.code

  override fun encode(): ByteArray {

    val v = ASN1EncodableVector()

    v.add(ASN1Integer(version.toLong()))
    v.add(DERUTF8String(clientId))
    v.add(ASN1Integer(listenPort.toLong()))
    v.add(DERUTF8String(nodeId))

    return DERSequence(v).encoded
  }

  companion object {
    fun decode(bytes: ByteArray): HelloMessage? {
      val v = ASN1InputStream(bytes).readObject()

      if (v != null) {
        val seq = ASN1Sequence.getInstance(v)

        val version = ASN1Integer.getInstance(seq.getObjectAt(0))?.value?.toInt()
        val clientId = DERUTF8String.getInstance(seq.getObjectAt(1))?.string
        val listenPort = ASN1Integer.getInstance(seq.getObjectAt(2))?.value?.toInt()
        val nodeId = DERUTF8String.getInstance(seq.getObjectAt(3))?.string

        if (version != null && clientId != null && listenPort != null && nodeId != null) {
          return HelloMessage(version, clientId, listenPort, nodeId)
        }
      }

      return null
    }
  }
}
