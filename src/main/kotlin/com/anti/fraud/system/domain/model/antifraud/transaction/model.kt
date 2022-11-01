package com.anti.fraud.system.domain.model.antifraud.transaction

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class TransactionStatus {
    ALLOWED, MANUAL_PROCESSING, PROHIBITED, NONE
}

enum class DeclineReason {
    AMOUNT,
    CARD_NUMBER,
    IP,
    IP_CORRELATION,
    REGION_CORRELATION,
    NONE,
}

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun declineReasonToString(reason: DeclineReason): String {
    return reason.name.lowercase()
}

@Serializable
data class TransactionAddResponse(val result: String, val info: String = "") {
    constructor(result: String, declineReasons: MutableSet<DeclineReason>) : this(
        result,
        declineReasons.toList().sorted().joinToString(", ").lowercase()
    )
}

@Serializable
data class TransactionAddRequest(
    val ip: String, val amount: Long, val number: String, val region: String, val date: String
)

@Serializable
data class FeedbackForTransactionRequest(val transactionId: Long, val feedback: String)

@Serializable
data class FeedbackForTransactionResponse(
    val transactionId: Long,
    val amount: Long,
    val ip: String,
    val number: String,
    val region: String,
    val date: String,
    val result: TransactionStatus,
    val feedback: TransactionStatus,
)

data class Transaction(
    val transactionId: Long,
    val amount: Long,
    val number: String,
    val region: String,
    val date: LocalDateTime,
    val status: TransactionStatus,
    val ip: String,
    var feedback: TransactionStatus = TransactionStatus.NONE,
) {
    fun updateFeedback(value: String) {
        this.feedback = TransactionStatus.valueOf(value)
    }
}

