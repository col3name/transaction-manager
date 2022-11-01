package com.anti.fraud.system.application.services

import com.anti.fraud.system.domain.services.RegionValidator
import org.springframework.stereotype.Service

data class Region(val code: String, val description: String)

@Service
class RegionValidatorImpl : RegionValidator {
    private val supportedRegions = mutableMapOf(
        "EAP" to "East Asia and Pacific",
        "ECA" to "Europe and Central Asia",
        "HIC" to "High-Income countries",
        "LAC" to "Latin America and the Caribbean",
        "MENA" to "The Middle East and North Africa",
        "SA" to "South Asia",
        "SSA" to "Sub-Saharan Africa"
    )

    override fun validate(region: String): Boolean {
        return supportedRegions.containsKey(region)
    }
}