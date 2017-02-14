package mbc.storage

import mbc.config.BlockChainConfig
import mbc.core.AccountState
import java.math.BigInteger

/**
 * 管理区块链状态的管理类(Account State, Blocks和Transactions)
 */
object Repository {

  /**
   * Account State Db.
   */
  val accountStateStore = BlockChainConfig.getAccountStateStore()

  /**
   * Blocks Db.
   */
  val blockStore = BlockChainConfig.getBlockStore()

  /**
   * Transactions Db.
   */
  val transactionStore = BlockChainConfig.getTransactionStore()

  /**
   * 读取账户余额。
   */
  fun getBalance(address: ByteArray): BigInteger {
    return accountStateStore.get(address)?.balance ?: BigInteger.ZERO
  }

  /**
   * 账户的Nonce+1
   */
  fun increaseNonce(address: ByteArray) {
    val accountState = getOrCreateAccountState(address)
    return accountStateStore.put(address, accountState.increaseNonce())
  }

  /**
   * 增加账户余额。
   */
  fun addBalance(address: ByteArray, amount: BigInteger) {
    val accountState = getOrCreateAccountState(address)
    return accountStateStore.put(address, accountState.increaseBalance(amount))
  }

  /**
   * 新建账户。
   */
  private fun createAccountState(address: ByteArray): AccountState {
    val state = AccountState(BigInteger.ZERO, BigInteger.ZERO)
    accountStateStore.put(address, state)
    return state
  }

  /**
   * 判断账户状态Account State是不是存在，如果不存在就新建账户。
   */
  private fun getOrCreateAccountState(address: ByteArray): AccountState {
    var ret = accountStateStore.get(address)
    if (ret == null) {
      ret = createAccountState(address)
    }
    return ret
  }

}
