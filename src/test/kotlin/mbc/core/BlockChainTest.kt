package mbc.core

import mbc.core.TransactionExecutor.addAmount
import mbc.core.TransactionExecutor.applyTrx
import mbc.miner.BlockMiner
import mbc.util.CryptoUtil
import mbc.util.CryptoUtil.Companion.generateKeyPair
import mbc.util.CryptoUtil.Companion.sha256
import mbc.util.CryptoUtil.Companion.verifyTransactionSignature
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


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

  /**
   * 构造新的区块
   */
  @Test fun createBlockTest() {
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

    // 初始化矿工Charlie账户
    val kp3 = generateKeyPair() ?: return
    val charlie = Account(kp3.public)

    // 构造原始区块(高度为0)
    val genesisBlock = Block(0, ByteArray(0), "1234567890123456789012345678901234567890", emptyList(),
                             DateTime(2017, 2, 1, 0, 0))

    // 构造新的区块
    val blockChain = BlockChain(charlie.address)
    blockChain.createNewBlock(genesisBlock, listOf(trx))

    // 查询余额是否正确
    assert(alice.balance == 100L)
    assert(bob.balance == 300L)
  }

  /**
   * 挖矿算法测试。
   */
  @Test fun mineAlgorithmTest() {
    val ver: Int = 1
    val parentHash = "000000000000000117c80378b8da0e33559b5997f2ad55e2f7d18ec1975b9717"
    val merkleRoot = "871714dcbae6c8193a2bb9b2a69fe1c0440399f38d94b3a0f1b447275a29978a"
    val time = 0x53058b35 // 2014-02-20 04:57:25
    val difficulty = 0x1f00ffff // difficulty，比特币的最小(初始)难度为0x1d00ffff，为测试方便我们降低难度为0x1f00ffff

    // 挖矿难度的算法：https://en.bitcoin.it/wiki/Difficulty
    val exp = difficulty shr 24
    val mant = difficulty and 0xffffff
    val target = BigInteger.valueOf(mant.toLong()).multiply(BigInteger.valueOf(2).pow(8 * (exp - 3)))
    val targetStr = "%064x".format(target)
    println("Target:$targetStr")

    var nonce = 0
    while (nonce < 0x100000000) {

      val headerBuffer = ByteBuffer.allocate(4+32+32+4+4+4)
      headerBuffer.put(ByteBuffer.allocate(4).putInt(ver).array()) // version
      headerBuffer.put(Hex.decode(parentHash)) // parentHash
      headerBuffer.put(Hex.decode(merkleRoot)) // merkleRoot
      headerBuffer.put(ByteBuffer.allocate(4).putInt(time).array()) // time
      headerBuffer.put(ByteBuffer.allocate(4).putInt(difficulty).array()) // difficulty(current difficulty)
      headerBuffer.put(ByteBuffer.allocate(4).putInt(nonce).array()) // nonce

      val header = headerBuffer.array()
      val hit = Hex.toHexString(sha256(sha256(header)))
      println("$nonce : $hit")

      if (hit < targetStr) {
        println("Got Nonce : $nonce")
        println("Got Hit : $hit")
        break
      }
      nonce += 1
    }
  }

  /**
   * 挖矿难度(Difficulty)运算测试。
   */
  @Test fun difficultyTest() {
    val difficulty = BigInteger.valueOf(0x0404cbL).multiply(BigInteger.valueOf(2).pow(8 * (0x1b - 3)))
    assertEquals(difficulty.toString(16), "404cb000000000000000000000000000000000000000000000000")
  }

  /**
   * Merkle Root Hash测试。
   */
  @Test fun merkleTest() {
    // 初始化Alice账户
    val kp1 = generateKeyPair() ?: return
    val alice = Account(kp1.public)

    // 初始化Bob账户
    val kp2 = generateKeyPair() ?: return
    val bob = Account(kp2.public)

    // Alice向Bob转账100
    val trx1 = Transaction(alice.address, bob.address, 100, DateTime(), kp1.public)

    // Alice用私钥签名
    val signature = trx1.sign(kp1.private)

    // Alice向Bob转账50
    val trx2 = Transaction(alice.address, bob.address, 50, DateTime(), kp1.public)

    // Alice用私钥签名
    val signature2 = trx2.sign(kp1.private)

    val merkleRoot = CryptoUtil.merkleRoot(listOf(trx1, trx2))
    println(Hex.toHexString(merkleRoot))
  }

  /**
   * 挖矿
   */
  @Test fun mineBlockTest() {
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

    // 初始化矿工Charlie账户
    val kp3 = generateKeyPair() ?: return
    val charlie = Account(kp3.public)

    // 构造原始区块(高度为0)
    val genesisBlock = Block(0, ByteArray(0), "1234567890123456789012345678901234567890", emptyList(),
                             DateTime(2017, 2, 1, 0, 0))

    // 构造新的区块
    val blockChain = BlockChain(charlie.address)
    val block = blockChain.createNewBlock(genesisBlock, listOf(trx))

    // 查询余额是否正确
    assert(alice.balance == 100L)
    assert(bob.balance == 300L)

    val mineResult = BlockMiner.mine(block)
    block.difficulty = mineResult.target
    block.nonce = mineResult.nonce

    println("Block nonce: ${block.nonce}")
    assertNotEquals(block.difficulty, 0)
    assertNotEquals(block.nonce, 0)
  }

}
