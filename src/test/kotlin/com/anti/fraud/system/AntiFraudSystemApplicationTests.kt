package com.anti.fraud.system

import com.anti.fraud.system.domain.model.antifraud.stolencard.isValidCreditCardNumber
import com.anti.fraud.system.domain.model.antifraud.transaction.DeclineReason
import com.anti.fraud.system.domain.model.antifraud.transaction.declineReasonToString
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AntiFraudSystemApplicationTests {

    @Test
    fun contextLoads() {
        val message = isValidCreditCardNumber("4000008449433403")
        println("$message cardNumber")
        println(declineReasonToString(DeclineReason.AMOUNT))
        println("reason ${DeclineReason.AMOUNT.name.lowercase()}")
    }
}
