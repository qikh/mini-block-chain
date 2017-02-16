package mbc.network.message

import mbc.core.Node
import org.spongycastle.asn1.*

/**
 * Get Peers，
 */
class NodesMessage(val nodes: List<Node>) : Message {

  override fun code(): Byte = MessageCodes.GET_NODES.code

  override fun encode(): ByteArray {

    val v = ASN1EncodableVector()

    nodes.forEach {
      val t = ASN1EncodableVector()
      t.add(DERIA5String(it.nodeId))
      t.add(DERIA5String(it.ip))
      t.add(ASN1Integer(it.port.toLong()))

      v.add(DERSequence(t))
    }
    return DERSequence(v).encoded
  }

  companion object {
    fun decode(bytes: ByteArray): NodesMessage? {
      val v = ASN1InputStream(bytes).readObject()

      if (v != null) {

        val seq = ASN1Sequence.getInstance(v)

        val elements = seq.objects
        val peers = mutableListOf<Node>()
        for (element in elements) {
          val der = DERSequence.getInstance(element)

          val nodeId = DERIA5String.getInstance(der.getObjectAt(0)).string
          val ip = DERIA5String.getInstance(der.getObjectAt(1)).string
          val port = ASN1Integer.getInstance(der.getObjectAt(2)).value.toInt()

          if (nodeId != null && ip != null && port != null) {
            peers.add(Node(nodeId, ip, port))
          }
        }

        return NodesMessage(peers)
      }

      return null
    }
  }
}
