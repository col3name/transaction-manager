package com.anti.fraud.system.application.services

import com.anti.fraud.system.domain.model.antifraud.stolencard.AddStolenCard
import com.anti.fraud.system.domain.model.antifraud.stolencard.Card
import com.anti.fraud.system.domain.model.antifraud.stolencard.isValidCreditCardNumber
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.AddSuspiciousIpRequest
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.AddSuspiciousIpResponse
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.IpItem
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.isValidInet4Address
import com.anti.fraud.system.domain.model.antifraud.transaction.*
import com.anti.fraud.system.domain.model.user.AlreadyExistException
import com.anti.fraud.system.domain.model.user.NotFoundException
import com.anti.fraud.system.domain.services.AntiFraudService
import com.anti.fraud.system.domain.services.RegionValidator
import com.anti.fraud.system.domain.services.TransactionLimitCalculator
import com.anti.fraud.system.domain.services.UnprocessableEntityException
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*


@Service
class AntiFraudServiceImpl(
    var blockedIpList: MutableList<IpItem> = mutableListOf(),
    var stolenCardList: MutableList<Card> = mutableListOf(),
    var transactions: MutableList<Transaction> = mutableListOf(),
    val regionValidator: RegionValidator,
    val transactionLimitCalculator: TransactionLimitCalculator
) : AntiFraudService {
    private var limitAllowed: Long = 200L
    private var limitManualProcessing: Long = 1500L

    override fun getTransactionHistory(): List<Transaction> = transactions

    override fun getTransactionHistoryByCardNumber(cardNumber: String): List<Transaction> {
        if (!isValidCreditCardNumber(cardNumber)) {
            throw IllegalArgumentException("invalid credit card number")
        }
        val history = transactions.filter { it.number == cardNumber }
        if (history.isEmpty()) {
            throw NotFoundException("not found $cardNumber transaction history")
        }
        return history
    }

    override fun calculate(transaction: TransactionAddRequest): TransactionAddResponse {
        val dateTime = validateTime(transaction.date)
        if (dateTime.isEmpty) {
            throw IllegalArgumentException("invalid date time")
        }
        if (!isValidInet4Address(transaction.ip)) {
            throw IllegalArgumentException("invalid ip address")
        }
        if (!isValidCreditCardNumber(transaction.number)) {
            throw IllegalArgumentException("invalid credit card number")
        }
        if (transaction.amount < 0) {
            throw IllegalArgumentException("not enough money")
        }
        if (!regionValidator.validate(transaction.region)) {
            throw IllegalArgumentException("invalid region")
        }

        var status: TransactionStatus = TransactionStatus.PROHIBITED

        val declineReasonList = mutableSetOf<DeclineReason>()

        if (transaction.amount < limitAllowed) {
            status = TransactionStatus.ALLOWED
        } else if (transaction.amount < limitManualProcessing) {
            status = TransactionStatus.MANUAL_PROCESSING
            declineReasonList.add(DeclineReason.AMOUNT)
        } else {
            declineReasonList.add(DeclineReason.AMOUNT)
        }

        if (findBlockedCardNumber(transaction.number) != null) {
            status = TransactionStatus.PROHIBITED
            declineReasonList.add(DeclineReason.CARD_NUMBER)
        }
        if (findBlockedIp(transaction.ip) != null) {
            status = TransactionStatus.PROHIBITED
            declineReasonList.add(DeclineReason.IP)
        }

        val time = dateTime.get()
        if (!determineStatus(
                transaction,
                declineReasonList,
                time,
                { regions -> regions.size == 2 },
                { ips -> ips.size == 2 })
        ) {
            status = TransactionStatus.MANUAL_PROCESSING
        }
        if (!determineStatus(
                transaction,
                declineReasonList,
                time,
                { regions -> regions.size > 2 },
                { ips -> ips.size > 2 })
        ) {
            status = TransactionStatus.PROHIBITED
        }

        transactions.add(
            Transaction(
                1L + transactions.lastIndex,
                transaction.amount,
                transaction.number,
                transaction.region,
                time,
                status,
                transaction.ip,
            )
        )
        if (declineReasonList.isEmpty()) {
            declineReasonList.add(DeclineReason.NONE)
        }
        return TransactionAddResponse(status.toString(), declineReasonList)
    }

    override fun addFeedbackToTransaction(feedback: FeedbackForTransactionRequest): FeedbackForTransactionResponse {
        val transaction: Transaction
        try {
            transaction = findTransactionById(feedback.transactionId)
            if (isAllowAddFeedback(transaction)) {
                throw AlreadyExistException("already have feedback")
            }
        } catch (e: NoSuchElementException) {
            throw NotFoundException("transaction ${feedback.transactionId} not exist")
        }

        val feedbackValue = feedback.feedback
        val amount = transaction.amount
        val status = transaction.status

        updateLimits(amount, status, feedbackValue)
        updateTransactionFeedbackInRepository(transaction, feedbackValue)

        return FeedbackForTransactionResponse(
            transaction.transactionId,
            amount,
            transaction.ip,
            transaction.number,
            transaction.region,
            formatter.format(transaction.date),
            status,
            transaction.feedback
        )
    }

    private fun isAllowAddFeedback(transaction: Transaction) =
        transaction.feedback != TransactionStatus.NONE

    private fun updateTransactionFeedbackInRepository(
        transaction: Transaction,
        feedbackValue: String
    ) {
        transaction.updateFeedback(feedbackValue)

        val index = transactions.indexOfFirst { it.transactionId == transaction.transactionId }
        if (index == -1) {
            throw IllegalArgumentException()
        }
        transactions[index] = transaction
    }

    private fun updateLimits(
        amount: Long,
        status: TransactionStatus,
        feedbackValue: String
    ) {
        val allowed = TransactionStatus.ALLOWED.toString()
        val manualProcessing = TransactionStatus.MANUAL_PROCESSING.toString()
        val prohibited = TransactionStatus.PROHIBITED.toString()
        when (status) {
            TransactionStatus.ALLOWED -> {
                when (feedbackValue) {
                    manualProcessing -> decreaseAllowedLimit(amount)

                    prohibited -> {
                        decreaseAllowedLimit(amount)
                        decreaseManualLimit(amount)
                    }

                    else -> {
                        throw UnprocessableEntityException("$status - $feedbackValue unprocessable")
                    }
                }
            }

            TransactionStatus.MANUAL_PROCESSING -> {
                when (feedbackValue) {
                    allowed -> increaseAllowedLimit(amount)
                    prohibited -> decreaseManualLimit(amount)
                    else -> {
                        throw UnprocessableEntityException("$status - $feedbackValue unprocessable")
                    }
                }
            }

            TransactionStatus.PROHIBITED -> {
                when (feedbackValue) {
                    allowed -> {
                        increaseAllowedLimit(amount)
                        increaseManualLimit(amount)
                    }

                    manualProcessing -> increaseManualLimit(amount)
                    else -> {
                        throw UnprocessableEntityException("$status - $feedbackValue unprocessable")
                    }
                }
            }

            else -> {
                throw IllegalArgumentException("unsupported status")
            }
        }
    }

    private fun increaseAllowedLimit(amount: Long) {
        limitAllowed = transactionLimitCalculator.increaseLimit(limitAllowed, amount)
    }

    private fun decreaseAllowedLimit(amount: Long) {
        limitAllowed = transactionLimitCalculator.decreaseLimit(limitAllowed, amount)
    }

    private fun increaseManualLimit(amount: Long) {
        limitAllowed = transactionLimitCalculator.increaseLimit(limitAllowed, amount)
    }

    private fun decreaseManualLimit(amount: Long) {
        limitAllowed = transactionLimitCalculator.decreaseLimit(limitAllowed, amount)
    }

    override fun findTransactionById(transactionId: Long): Transaction {
        return transactions.first { it.transactionId == transactionId }
    }

    private fun determineStatus(
        transaction: TransactionAddRequest,
        declineReasonList: MutableSet<DeclineReason>,
        dateTime: LocalDateTime,
        regionCorrelationFn: (regions: List<String>) -> Boolean,
        ipCorrelationFn: (regions: List<String>) -> Boolean
    ): Boolean {
        val distinctRegions = getDistinctRegionsOfTransaction(transaction, dateTime)
        val distinctIps = getDistinctIps(transaction, dateTime)
        var ok = true
        if (regionCorrelationFn(distinctRegions)) {
            declineReasonList.add(DeclineReason.REGION_CORRELATION)
            ok = false
        }
        if (ipCorrelationFn(distinctIps)) {
            declineReasonList.add(DeclineReason.IP_CORRELATION)
            ok = false
        }
        return ok
    }

    private fun getDistinctIps(
        transaction: TransactionAddRequest,
        dateTime: LocalDateTime
    ) = transactions.filter { it.ip != transaction.ip && it.date > dateTime.minusHours(2) }
        .map { it.ip }
        .distinct()

    private fun getDistinctRegionsOfTransaction(
        transaction: TransactionAddRequest,
        dateTime: LocalDateTime
    ) = transactions.filter { it.region != transaction.region && it.date > dateTime.minusHours(2) }
        .map { it.region }
        .distinct()

    private fun validateTime(date: String): Optional<LocalDateTime> {
        return try {
            Optional.of(LocalDateTime.parse(date, formatter))
        } catch (e: DateTimeParseException) {
            Optional.empty()
        }
    }

    override fun getAllSuspiciousIp(): List<IpItem> = blockedIpList.sortedBy { it.id }

    override fun addSuspiciousIp(ipRequest: AddSuspiciousIpRequest): AddSuspiciousIpResponse {
        val ip = ipRequest.ip

        if (findBlockedIp(ip) != null) {
            throw AlreadyExistException("IP already banned")
        }

        val id = blockedIpList.lastIndex.toLong() + 1L
        blockedIpList.add(IpItem(id, ip))
        return AddSuspiciousIpResponse(id, ip)
    }

    private fun findBlockedIp(ip: String): IpItem? {
        return blockedIpList.firstOrNull { ip == it.ip }
    }

    override fun removeSuspiciousIp(ip: String) {
        if (!isValidInet4Address(ip)) {
            throw IllegalArgumentException("invalid $ip")
        }

        if (!blockedIpList.removeIf { ip == it.ip }) {
            throw NotFoundException("IP $ip not exist")
        }
    }

    override fun getAllStolenCard(): List<Card> = stolenCardList.sortedBy { it.id }

    override fun addStolenCard(ipRequest: AddStolenCard): Card {
        val cardNumber = ipRequest.number
        if (!isValidCreditCardNumber(cardNumber)) {
            throw IllegalArgumentException("invalid $cardNumber")
        }

        val ipItem = findBlockedCardNumber(cardNumber)
        if (ipItem != null) {
            throw AlreadyExistException("card ${ipRequest.number} already banned")
        }
        val id = stolenCardList.lastIndex.toLong() + 1L
        val card = Card(id, cardNumber)
        stolenCardList.add(card)
        return card
    }

    private fun findBlockedCardNumber(cardNumber: String) = stolenCardList.firstOrNull { cardNumber == it.number }

    override fun removeStolenCard(cardNumber: String) {
        if (!isValidCreditCardNumber(cardNumber)) {
            throw IllegalArgumentException("invalid $cardNumber")
        }

        if (!stolenCardList.removeIf { cardNumber == it.number }) {
            throw NotFoundException("IP $cardNumber not exist")
        }
    }
}