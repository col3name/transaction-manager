package com.anti.fraud.system.domain.repository

import com.anti.fraud.system.domain.model.antifraud.transaction.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByNumber(cardNumber: String): List<Transaction>

    @Query(
        nativeQuery = true,
        value = "SELECT DISTINCT region FROM transactions t WHERE t.region != ?1 AND ?2 < t.date AND status != 0;"
    )
    fun findDistinctByRegionAndDateLessThanAndStatusNotEqual(
        region: String,
        @Param("date") dateTime: LocalDateTime
    ): List<String>

    @Query(
        nativeQuery = true,
        value = "SELECT DISTINCT ip FROM transactions t WHERE ip != ?1 AND ?2 < t.date AND status != 0;"
    )
    fun findDistinctByIpAndDateLessThanAndStatusNotEqual(
        ip: String,
        @Param(value = "date") dateTime: LocalDateTime
    ): List<String>
}