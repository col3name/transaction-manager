package com.anti.fraud.system.domain.services

interface RegionValidator {
    fun validate(region: String): Boolean
}