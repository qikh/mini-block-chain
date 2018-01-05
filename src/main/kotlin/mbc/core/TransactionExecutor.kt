package mbc.core

import mbc.storage.Repository
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

/**
 * 交易处理并更新账户状态。
 */
class TransactionExecutor(val repository: Repository) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * 增加账户余额(balance)，如果amount为负数则余额减少。
   */
  fun addBalance(address: ByteArray, amount: BigInteger) {
    repository.addBalance(address, amount)
  }

  /**
   * 转账功能，发送方减少金额，接收方增加金额。
   */
  fun transfer(fromAddress: ByteArray, toAddress: ByteArray, amount: BigInteger) {
    addBalance(fromAddress, amount.negate())
    addBalance(toAddress, amount)
  }

  /**
   * Coinbase Reward，coinbaseAddress增加金额。
   */
  fun coinbaseTransfer(coinbaseAddress: ByteArray, amount: BigInteger) {
    addBalance(coinbaseAddress, amount)
  }

  /**
   * 根据交易记录更新区块链的状态(state)，发送方的余额会减少，接收方的余额会增加。
   * 区块链的状态更新应该是原子操作，持久层是数据库可以使用事务功能。
   */
  fun execute(trx: Transaction) {
    if (trx.isValid) {

      // 发送方的Nonce+1
      repository.increaseNonce(trx.senderAddress)

      // 执行转账
      transfer(trx.senderAddress, trx.receiverAddress, trx.amount)
    } else {
      throw IllegalTransactionException()
    }
  }

  /**
   * 执行Coinbase Transaction。
   */
  fun executeCoinbaseTransaction(trx: Transaction) {
    if (trx.isCoinbaseTransaction()) {
      logger.debug("Reward ${trx.amount} coins to ${Hex.toHexString(trx.receiverAddress)}")

      // Execute reward
      coinbaseTransfer(trx.receiverAddress, trx.amount)
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
