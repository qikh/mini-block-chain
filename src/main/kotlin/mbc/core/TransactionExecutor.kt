package mbc.core

/**
 * 交易处理并更新账户状态。
 */
object TransactionExecutor {

  /**
   * 增加账户余额(balance)，如果amount为负数则余额减少。
   */
  fun addAmount(address: String, amount: Long) {
    val newBalance = AccountState.getAccountBalance(address) + amount
    AccountState.setAccountBalance(address, newBalance)
  }

  /**
   * 根据交易记录更新区块链的状态(state)，发送方的余额会减少，接收方的余额会增加。
   * 区块链的状态更新应该是原子操作，持久层是数据库可以使用事务功能。
   */
  fun applyTrx(trx: Transaction) {
    if (trx.isValid) {
      addAmount(trx.senderAddress, -trx.amount)
      addAmount(trx.receiverAddress, +trx.amount)
    } else {
      throw IllegalTransactionException()
    }
  }

}

/**
 * 交易内容非法（没有签名或签名错误）。
 */
class IllegalTransactionException : Throwable() {

}
