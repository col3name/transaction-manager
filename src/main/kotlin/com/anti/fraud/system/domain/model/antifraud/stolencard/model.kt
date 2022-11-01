package com.anti.fraud.system.domain.model.antifraud.stolencard

import kotlinx.serialization.Serializable
import java.util.*

fun isValidCreditCardNumber(cardNumber: String): Boolean {
    if (cardNumber.length != 16) {
        return false
    }
    val cardIntArray = IntArray(cardNumber.length)
    for (i in cardNumber.indices) {
        val c = cardNumber[i]
        cardIntArray[i] = ("" + c).toInt()
    }
    var i = cardIntArray.size - 2
    while (i >= 0) {
        var num = cardIntArray[i]
        num *= 2
        if (num > 9) {
            num = num % 10 + num / 10
        }
        cardIntArray[i] = num
        i -= 2
    }
    return sumDigits(cardIntArray) % 10 == 0
}

fun sumDigits(arr: IntArray?): Int {
    return Arrays.stream(arr).sum()
}

@Serializable
data class AddStolenCard(val number: String)

@Serializable
data class Card(val id: Long, val number: String)
