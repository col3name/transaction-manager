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
import com.anti.fraud.system.domain.repository.BlockedCardRepository
import com.anti.fraud.system.domain.repository.SuspiciousIpRepository
import com.anti.fraud.system.domain.repository.TransactionRepository
import com.anti.fraud.system.domain.services.AntiFraudService
import com.anti.fraud.system.domain.services.RegionValidator
import com.anti.fraud.system.domain.services.TransactionLimitCalculator
import com.anti.fraud.system.domain.services.UnprocessableEntityException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*


@Service
class AntiFraudServiceImpl(
    @Autowired
    val blockedIpRepository: SuspiciousIpRepository,
    @Autowired
    val blockedCardRepository: BlockedCardRepository,
    @Autowired
    val transactionRepository: TransactionRepository,

    val regionValidator: RegionValidator,
    val transactionLimitCalculator: TransactionLimitCalculator
) : AntiFraudService {
    private var limitAllowed: Long = 200L
    private var limitManualProcessing: Long = 1500L

    override fun getTransactionHistory(): List<Transaction> = transactionRepository.findAll()

    override fun getTransactionHistoryByCardNumber(cardNumber: String): List<Transaction> {
        if (!isValidCreditCardNumber(cardNumber)) {
            throw IllegalArgumentException("invalid credit card number")
        }

        val history = transactionRepository.findByNumber(cardNumber)
        if (history.isEmpty()) {
            throw NotFoundException("not found $cardNumber transaction history")
        }
        return history
    }

    override fun addTransaction(transaction: TransactionAddRequest): TransactionAddResponse {
        val transactionTime = validateTime(transaction.date)
        validateTransaction(transactionTime, transaction)
        val time = transactionTime.get()

        val (status, declineReasonList) =
            determineStatusAndDeclineReasons(transaction, time)

        transactionRepository.save(
            Transaction(
                transaction.amount,
                transaction.number,
                transaction.region,
                time,
                status,
                transaction.ip,
            )
        )
        return TransactionAddResponse(status.toString(), declineReasonList)
    }

    private fun determineStatusAndDeclineReasons(
        transaction: TransactionAddRequest,
        time: LocalDateTime
    ): Pair<TransactionStatus, MutableSet<DeclineReason>> {
        val dateTime = time.minusHours(2)

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

        if (blockedCardRepository.findByNumber(transaction.number).isNotEmpty()) {
            status = TransactionStatus.PROHIBITED
            declineReasonList.add(DeclineReason.CARD_NUMBER)
        }

        if (blockedIpRepository.findByIp(transaction.ip).isNotEmpty()) {
            status = TransactionStatus.PROHIBITED
            declineReasonList.add(DeclineReason.IP)
        }

        val distinctCardNumberByRegions =
            transactionRepository.findDistinctByRegionAndDateLessThanAndStatusNotEqual(transaction.region, dateTime)
        val distinctCardNumberByIp =
            transactionRepository.findDistinctByIpAndDateLessThanAndStatusNotEqual(transaction.ip, dateTime)

        if (!determineStatus(
                declineReasonList,
                distinctCardNumberByRegions,
                distinctCardNumberByIp,
                { regions -> regions.size == 2 }
            ) { ips -> ips.size == 2 }
        ) {
            status = TransactionStatus.MANUAL_PROCESSING
        }
        if (!determineStatus(
                declineReasonList,
                distinctCardNumberByRegions,
                distinctCardNumberByIp,
                { regions -> regions.size > 2 }
            ) { ips -> ips.size > 2 }
        ) {
            status = TransactionStatus.PROHIBITED
        }
        if (declineReasonList.isEmpty()) {
            declineReasonList.add(DeclineReason.NONE)
        }
        return Pair(status, declineReasonList)
    }

    private fun validateTransaction(
        transactionTime: Optional<LocalDateTime>,
        transaction: TransactionAddRequest
    ) {
        if (transactionTime.isEmpty) {
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
    }

    override fun addFeedbackToTransaction(feedback: FeedbackForTransactionRequest): FeedbackForTransactionResponse {
        val transaction: Transaction
        try {
            val transactionOptional = transactionRepository.findById(feedback.transactionId)
            if (transactionOptional.isEmpty) {
                throw NotFoundException("transaction not exist")
            }
            transaction = transactionOptional.get()
            if (transaction.feedback != TransactionStatus.NONE) {
                throw AlreadyExistException("already have feedback")
            }
        } catch (e: NoSuchElementException) {
            throw NotFoundException("transaction ${feedback.transactionId} not exist")
        }

        val feedbackValue = feedback.feedback
        val amount = transaction.amount
        val status = transaction.status

        updateLimits(amount, status, feedbackValue)
        transaction.updateFeedback(feedbackValue)

        transactionRepository.save(transaction)

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

    private fun determineStatus(
        declineReasonList: MutableSet<DeclineReason>,
        distinctCardNumberByRegions: List<String>,
        distinctCardNumberByIp: List<String>,
        regionCorrelationFn: (regions: List<String>) -> Boolean,
        ipCorrelationFn: (regions: List<String>) -> Boolean
    ): Boolean {
        println(distinctCardNumberByRegions)
        println(distinctCardNumberByIp)
        var ok = true
        if (regionCorrelationFn(distinctCardNumberByRegions)) {
            declineReasonList.add(DeclineReason.REGION_CORRELATION)
            ok = false
        }
        if (ipCorrelationFn(distinctCardNumberByIp)) {
            declineReasonList.add(DeclineReason.IP_CORRELATION)
            ok = false
        }
        return ok
    }

    private fun validateTime(date: String): Optional<LocalDateTime> {
        return try {
            Optional.of(LocalDateTime.parse(date, formatter))
        } catch (e: DateTimeParseException) {
            Optional.empty()
        }
    }

    override fun getAllSuspiciousIp(): List<IpItem> =
        blockedIpRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))

    override fun addSuspiciousIp(ipRequest: AddSuspiciousIpRequest): AddSuspiciousIpResponse {
        val ip = ipRequest.ip
        val blockedIp = blockedIpRepository.findByIp(ip)
        if (blockedIp.isNotEmpty()) {
            throw AlreadyExistException("IP already banned")
        }
        val userDB = blockedIpRepository.save(IpItem(ip))
        return AddSuspiciousIpResponse(userDB.id, ip)
    }

    override fun removeSuspiciousIp(ip: String) {
        if (!isValidInet4Address(ip)) {
            throw IllegalArgumentException("invalid $ip")
        }

        blockedIpRepository.findByIp(ip)
            .map(blockedIpRepository::delete)
    }

    override fun getAllStolenCard(): List<Card> {
        return blockedCardRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
    }

    override fun addStolenCard(ipRequest: AddStolenCard): Card {
        val cardNumber = ipRequest.number
        if (!isValidCreditCardNumber(cardNumber)) {
            throw IllegalArgumentException("invalid $cardNumber")
        }

        val blockedCard = blockedCardRepository.findByNumber(cardNumber)
        if (blockedCard.isNotEmpty()) {
            throw AlreadyExistException("card ${ipRequest.number} already banned")
        }

        return blockedCardRepository.save(Card(number = cardNumber))
    }

    override fun removeStolenCard(cardNumber: String) {
        if (!isValidCreditCardNumber(cardNumber)) {
            throw IllegalArgumentException("invalid $cardNumber")
        }

        blockedCardRepository.findByNumber(cardNumber)
            .map(blockedCardRepository::delete)
    }
}