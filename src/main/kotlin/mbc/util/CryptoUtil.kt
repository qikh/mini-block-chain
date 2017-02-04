package mbc.util

import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.encoders.Hex
import java.security.*
import java.security.Security.insertProviderAt
import java.security.spec.ECGenParameterSpec

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
    fun generateAddress(publicKey: PublicKey): String {
      val digest = MessageDigest.getInstance("KECCAK-256", "SC")
      digest.update(publicKey.encoded)
      val hash = digest.digest()

      return Hex.toHexString(hash.drop(12).toByteArray())
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
  }

}
