package com.anti.fraud.system.domain.model.antifraud.suspiciousIp

import kotlinx.serialization.Serializable
import java.util.regex.Matcher
import java.util.regex.Pattern


@Serializable
data class AddSuspiciousIpRequest(var ip: String) {
    init {
        if (!isValidInet4Address(ip)) {
            throw IllegalArgumentException("invalid ip $ip address")
        }
    }
}

const val IPV4_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"

val IPv4_PATTERN: Pattern = Pattern.compile(IPV4_REGEX)

fun isValidInet4Address(ip: String): Boolean {
    val matcher: Matcher = IPv4_PATTERN.matcher(ip)
    return matcher.matches()
}


@Serializable
data class AddSuspiciousIpResponse(val id: Long, val ip: String)

data class IpItem(val id: Long, val ip: String)