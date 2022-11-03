package com.anti.fraud.system.domain.repository

import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.IpItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SuspiciousIpRepository : JpaRepository<IpItem, Int> {
    fun findByIp(ip: String): List<IpItem>
}