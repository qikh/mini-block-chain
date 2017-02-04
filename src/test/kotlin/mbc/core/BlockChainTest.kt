package mbc.core

import mbc.core.TransactionExecutor.addAmount
import mbc.core.TransactionExecutor.applyTrx
import mbc.util.CryptoUtil.Companion.generateKeyPair
import org.joda.time.DateTime
import org.junit.Test

class BlockChainTest {

  /**
   * 验证账户地址长度为40(20个byte)。
   */
  @Test fun validateAddress() {
    val keyPair = generateKeyPair() ?: return

    val account = Account(keyPair.public)
    assert(account.address.length == 40)
  }

  /**
   * 验证交易完成后账户余额(balance)是否正确。
   */
  @Test fun applyTransaction() {
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
    val trx = Transaction(alice.address, bob.address, 100, DateTime())

    // 根据交易记录更新区块链状态
    applyTrx(trx)

    // 查询余额是否正确
    assert(alice.balance == 100L)
    assert(bob.balance == 300L)
  }

}
