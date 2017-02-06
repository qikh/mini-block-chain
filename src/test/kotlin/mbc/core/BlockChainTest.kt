package mbc.core

import mbc.core.TransactionExecutor.addAmount
import mbc.core.TransactionExecutor.applyTrx
import mbc.util.CryptoUtil.Companion.generateKeyPair
import mbc.util.CryptoUtil.Companion.signTransaction
import mbc.util.CryptoUtil.Companion.verifyTransactionSignature
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.spec.ECGenParameterSpec
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature;


class BlockChainTest {
  @Before fun setup() {
    Security.insertProviderAt(BouncyCastleProvider(), 1)
  }

  /**
   * 验证账户地址长度为40(20个byte)。
   */
  @Test fun validateAddressTest() {
    val keyPair = generateKeyPair() ?: return

    val account = Account(keyPair.public)
    assert(account.address.length == 40)
  }

  /**
   * 验证交易完成后账户余额(balance)是否正确。
   */
  @Test fun applyTransactionTest() {
    // 初始化Alice账户
    val kp1 = generateKeyPair() ?: return
    val alice = Account(kp1.public)

    // 初始化Bob账户
    val kp2 = generateKeyPair() ?: return
    val bob = Account(kp2.public)

    // 初始金额为200
    addAmount(alice.address, 200)
    addAmount(bob.address, 200)

    // Alice向Bob转账100
    val trx = Transaction(alice.address, bob.address, 100, DateTime(), kp1.public)
    // Alice用私钥签名
    trx.sign(kp1.private)

    // 根据交易记录更新区块链状态
    applyTrx(trx)

    // 查询余额是否正确
    assert(alice.balance == 100L)
    assert(bob.balance == 300L)
  }

  /**
   * 验证ECDSAE签名算法。
   */
  @Test fun verifyECDSASignatureTest() {
    // Get the instance of the Key Generator with "EC" algorithm

    val gen = KeyPairGenerator.getInstance("EC", "SC")
    gen.initialize(ECGenParameterSpec("secp256r1"))

    val pair = gen.generateKeyPair()
    // Instance of signature class with SHA256withECDSA algorithm
    val signer = Signature.getInstance("SHA256withECDSA")
    signer.initSign(pair.private)

    println("Private Keys is::" + pair.private)
    println("Public Keys is::" + pair.public)

    val msg = "text ecdsa with sha256"//getSHA256(msg)
    signer.update(msg.toByteArray())

    val signature = signer.sign()
    println("Signature is::" + BigInteger(1, signature).toString(16))

    // Validation
    signer.initVerify(pair.public)
    signer.update(msg.toByteArray())
    assert(signer.verify(signature))

  }

  /**
   * 验证交易签名。
   */
  @Test fun verifyTransactionSignatureTest() {
    // 初始化Alice账户
    val kp1 = generateKeyPair() ?: return
    val alice = Account(kp1.public)

    // 初始化Bob账户
    val kp2 = generateKeyPair() ?: return
    val bob = Account(kp2.public)

    // Alice向Bob转账100
    val trx = Transaction(alice.address, bob.address, 100, DateTime(), kp1.public)

    // Alice用私钥签名
    val signature = trx.sign(kp1.private)

    // 用Alice的公钥验证交易签名
    assert(verifyTransactionSignature(trx, signature))

    // 验证交易的合法性(签名验证)
    assert(trx.isValid)
  }

}
