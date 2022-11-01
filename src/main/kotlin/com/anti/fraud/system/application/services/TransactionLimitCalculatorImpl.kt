package com.anti.fraud.system.application.services

import com.anti.fraud.system.domain.services.TransactionLimitCalculator
import org.springframework.stereotype.Service
import kotlin.math.ceil

@Service
class TransactionLimitCalculatorImpl : TransactionLimitCalculator {

    override fun increaseLimit(currentLimit: Long, valueFromTransaction: Long): Long {
        return ceil(0.8 * currentLimit + 0.2 * valueFromTransaction).toLong()
    }

    override fun decreaseLimit(currentLimit: Long, valueFromTransaction: Long): Long {
        return ceil(0.8 * currentLimit - 0.2 * valueFromTransaction).toLong()
    }
}