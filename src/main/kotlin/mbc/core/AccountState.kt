package mbc.core

import java.util.*

/**
 * 账户状态管理。
 */
object AccountState {
  /**
   * 账户余额存储，为了演示方便使用HashMap。
   */
  val accountBalances: HashMap<String, Long> = HashMap()

  /**
   * 根据账户地址(address)读取账户余额(balance)。
   */
  fun  getAccountBalance(address: String): Long {
    return accountBalances.getOrDefault(address, 0)
  }

  /**
   * 根据账户地址(address)更新账户余额(balance)。
   */
  fun  setAccountBalance(address: String, amount: Long): Long? {
    return accountBalances.put(address, amount)
  }

}
