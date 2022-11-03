package com.anti.fraud.system.domain.repository

import com.anti.fraud.system.domain.model.antifraud.stolencard.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlockedCardRepository : JpaRepository<Card, Int> {
    fun findByNumber(ip: String): List<Card>
}