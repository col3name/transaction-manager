package com.anti.fraud.system

import com.anti.fraud.system.domain.model.antifraud.stolencard.isValidCreditCardNumber
import com.anti.fraud.system.domain.model.antifraud.transaction.DeclineReason
import com.anti.fraud.system.domain.model.antifraud.transaction.declineReasonToString
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AntiFaudSystemApplicationTests {

    @Test
    fun contextLoads() {
//        val blockedIpList: List<IpItem> = mutableListOf()
//        val first: IpItem? = blockedIpList.firstOrNull { it.ip == "123" }
//        if (first == null) {
//            println("null")
//        } else {
//            println(first)
//        }

//        System.out.println("Hello world")

        val message = isValidCreditCardNumber("4000008449433403")
        println("$message cardNumber")

//        println("Model.BASE ${Model.BASE}")

        println(declineReasonToString(DeclineReason.AMOUNT))
        println("reason ${DeclineReason.AMOUNT.name.lowercase()}")
    }
}
