package mbc.core

import org.joda.time.DateTime

/**
 * 交易记录类：记录了发送方(sender)向接受方(receiver)的转账记录，包括金额(amount)和时间戳(time)。
 * 为简化模型，没有加入费用(fee)。
 */
data class Transaction(val senderAddress: String, val receiverAddress: String, val amount: Long,
                       val time: DateTime)
