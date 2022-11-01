package com.anti.fraud.system.domain.services

import com.anti.fraud.system.domain.model.antifraud.stolencard.AddStolenCard
import com.anti.fraud.system.domain.model.antifraud.stolencard.Card
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.AddSuspiciousIpRequest
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.AddSuspiciousIpResponse
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.IpItem
import com.anti.fraud.system.domain.model.antifraud.transaction.*

class UnprocessableEntityException(message: String) : Exception(message)

interface AntiFraudService {
    fun getTransactionHistory(): List<Transaction>
    fun getTransactionHistoryByCardNumber(cardNumber: String): List<Transaction>
    fun findTransactionById(transactionId: Long): Transaction
    fun calculate(transaction: TransactionAddRequest): TransactionAddResponse
    fun addFeedbackToTransaction(feedback: FeedbackForTransactionRequest): FeedbackForTransactionResponse

    fun addSuspiciousIp(ipRequest: AddSuspiciousIpRequest): AddSuspiciousIpResponse
    fun getAllSuspiciousIp(): List<IpItem>
    fun removeSuspiciousIp(ip: String)

    fun addStolenCard(ipRequest: AddStolenCard): Card
    fun getAllStolenCard(): List<Card>
    fun removeStolenCard(cardNumber: String)
}

interface TransactionLimitCalculator {
    fun increaseLimit(currentLimit: Long, valueFromTransaction: Long): Long
    fun decreaseLimit(currentLimit: Long, valueFromTransaction: Long): Long
}