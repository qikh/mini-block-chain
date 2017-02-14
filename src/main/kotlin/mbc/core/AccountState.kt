package mbc.core

import mbc.util.CodecUtil
import java.math.BigInteger

/**
 * 账户状态。
 */
class AccountState(val nonce: BigInteger, val balance: BigInteger) {

  fun encode(): ByteArray {
    return CodecUtil.encodeAccountState(this)
  }

  fun increaseNonce(): AccountState {
    return AccountState(nonce + BigInteger.ONE, balance)
  }


  fun increaseBalance(amount: BigInteger): AccountState {
    return AccountState(nonce, balance + amount)
  }
}
