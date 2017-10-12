package mbc.util

import mbc.core.Block
import mbc.core.Transaction
import mbc.trie.Trie
import org.spongycastle.crypto.params.ECDomainParameters
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.jce.spec.ECPublicKeySpec
import java.security.*
import java.security.Security.insertProviderAt
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


/**
 * 密码学工具类。
 */
class CryptoUtil {

  companion object {
    init {
      insertProviderAt(BouncyCastleProvider(), 1)
    }

    /**
     * 根据公钥(public key)推算出账户地址，使用以太坊的算法，先KECCAK-256计算哈希值(32位)，取后20位作为账户地址。
     * 比特币地址算法：http://www.infoq.com/cn/articles/bitcoin-and-block-chain-part03
     * 以太坊地址算法：http://ethereum.stackexchange.com/questions/3542/how-are-ethereum-addresses-generated
     */
    fun generateAddress(publicKey: PublicKey): ByteArray {
      val digest = MessageDigest.getInstance("KECCAK-256", "SC")
      digest.update(publicKey.encoded)
      val hash = digest.digest()

      return hash.drop(12).toByteArray()
    }

    /**
     * 生成公私钥对，使用以太坊的ECDSA算法(secp256k1)。
     */
    fun generateKeyPair(): KeyPair? {
      val gen = KeyPairGenerator.getInstance("EC", "SC")
      gen.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
      val keyPair = gen.generateKeyPair()
      return keyPair
    }

    /**
     * 发送方用私钥对交易Transaction进行签名。
     */
    fun signTransaction(trx: Transaction, privateKey: PrivateKey): ByteArray {
      val signer = Signature.getInstance("SHA256withECDSA")
      signer.initSign(privateKey)
      val msgToSign = CodecUtil.encodeTransactionWithoutSignatureToAsn1(trx).encoded
      signer.update(msgToSign)
      return signer.sign()
    }

    /**
     * 验证交易Transaction签名的有效性。
     */
    fun verifyTransactionSignature(trx: Transaction, signature: ByteArray): Boolean {
      val signer = Signature.getInstance("SHA256withECDSA")
      signer.initVerify(trx.publicKey)

      signer.update(CodecUtil.encodeTransactionWithoutSignatureToAsn1(trx).encoded)
      return signer.verify(signature)
    }

    /**
     * 运算区块的哈希值。
     */
    fun hashBlock(block: Block): ByteArray {
      val digest = MessageDigest.getInstance("KECCAK-256", "SC")
      digest.update(block.encode())
      return digest.digest()
    }


    /**
     * 计算Merkle Root Hash
     */
    fun merkleRoot(transactions: List<Transaction>): ByteArray {
      val trxTrie = Trie<Transaction>()
      for (i in 0 until transactions.size) {
        trxTrie.put(i.toString(), transactions[i])
      }
      return trxTrie.root?.hash() ?: ByteArray(0)
    }

    /**
     * SHA-256
     */
    fun sha256(msg: ByteArray): ByteArray {
      val digest = MessageDigest.getInstance("SHA-256", "SC")
      digest.update(msg)
      val hash = digest.digest()

      return hash
    }

    /**
     * SHA3
     */
    fun sha3(msg: ByteArray): ByteArray {
      return sha256((msg))
    }

    fun deserializePrivateKey(bytes: ByteArray): PrivateKey {
      val kf = KeyFactory.getInstance("EC", "SC")
      return kf.generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    fun deserializePublicKey(bytes: ByteArray): PublicKey {
      val kf = KeyFactory.getInstance("EC", "SC")
      return kf.generatePublic(X509EncodedKeySpec(bytes))
    }

    /**
     * 从PrivateKey计算出PublicKey，参考了以太坊的代码和http://stackoverflow.com/questions/26159149/how-can-i-default-a-publickey-object-from-ec-public-key-bytes
     */
    fun generatePublicKey(privateKey: PrivateKey): PublicKey? {
      val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
      val kf = KeyFactory.getInstance("EC", "SC")

      val curve = ECDomainParameters(spec.curve, spec.g, spec.n, spec.h)

      if (privateKey is BCECPrivateKey) {
        val d = privateKey.d
        val point = curve.g.multiply(d)
        val pubKeySpec = ECPublicKeySpec(point, spec)
        val publicKey = kf.generatePublic(pubKeySpec) as ECPublicKey
        return publicKey
      } else {
        return null
      }
    }
  }

}
