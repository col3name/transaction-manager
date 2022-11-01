package com.anti.fraud.system.infrastructure.controller

import com.anti.fraud.system.domain.model.*
import com.anti.fraud.system.domain.model.antifraud.stolencard.AddStolenCard
import com.anti.fraud.system.domain.model.antifraud.stolencard.Card
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.AddSuspiciousIpRequest
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.AddSuspiciousIpResponse
import com.anti.fraud.system.domain.model.antifraud.suspiciousIp.IpItem
import com.anti.fraud.system.domain.model.antifraud.transaction.*
import com.anti.fraud.system.domain.model.user.ActionResult
import com.anti.fraud.system.domain.model.user.AlreadyExistException
import com.anti.fraud.system.domain.model.user.NotFoundException
import com.anti.fraud.system.domain.services.AntiFraudService
import com.anti.fraud.system.domain.services.UnprocessableEntityException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/antifraud")
class AntiFraudController(@Autowired val antiFraudService: AntiFraudService) {
    @GetMapping("history")
    fun getTransactionHistory(): ResponseEntity<List<Transaction>> {
        return try {
            ResponseEntity(antiFraudService.getTransactionHistory(), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("history/{cardNumber}")
    fun getTransactionHistoryByCardNumber(@PathVariable(name = "cardNumber") cardNumber: String): ResponseEntity<List<Transaction>> {
        return try {
            ResponseEntity(antiFraudService.getTransactionHistoryByCardNumber(cardNumber), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: NotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("transaction")
    fun addTransaction(@RequestBody transactionAdd: TransactionAddRequest): ResponseEntity<TransactionAddResponse> {
        return try {
            ResponseEntity(antiFraudService.calculate(transactionAdd), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            println(e)
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            println(e)
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PutMapping("transaction")
    fun addFeedbackToTransaction(@RequestBody transaction: FeedbackForTransactionRequest):
            ResponseEntity<FeedbackForTransactionResponse> {
        return try {
            ResponseEntity(antiFraudService.addFeedbackToTransaction(transaction), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            println(e)
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: NotFoundException) {
            println(e)
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: AlreadyExistException) {
            println(e)
            ResponseEntity(HttpStatus.CONFLICT)
        } catch (e: UnprocessableEntityException) {
            println(e)
            ResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY)
        } catch (e: Exception) {
            println(e)
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("suspicious-ip")
    fun getAllSuspiciousIp(): ResponseEntity<List<IpItem>> {
        return try {
            ResponseEntity(antiFraudService.getAllSuspiciousIp(), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("suspicious-ip")
    fun addSuspiciousIp(@RequestBody ipRequest: AddSuspiciousIpRequest): ResponseEntity<AddSuspiciousIpResponse> {
        return try {
            ResponseEntity(antiFraudService.addSuspiciousIp(ipRequest), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: AlreadyExistException) {
            ResponseEntity(HttpStatus.CONFLICT)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @DeleteMapping("suspicious-ip/{ip}")
    fun deleteSuspiciousIp(@PathVariable(name = "ip") ip: String): ResponseEntity<ActionResult> {
        return try {
            antiFraudService.removeSuspiciousIp(ip)
            ResponseEntity(ActionResult("IP $ip removed!"), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: NotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("stolencard")
    fun getAllStolenCard(): ResponseEntity<List<Card>> {
        return try {
            ResponseEntity(antiFraudService.getAllStolenCard(), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("stolencard")
    fun addStolenCard(@RequestBody ipRequest: AddStolenCard): ResponseEntity<Card> {
        return try {
            ResponseEntity(antiFraudService.addStolenCard(ipRequest), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: AlreadyExistException) {
            ResponseEntity(HttpStatus.CONFLICT)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @DeleteMapping("stolencard/{number}")
    fun deleteStolenCard(@PathVariable(name = "number") cardNumber: String): ResponseEntity<ActionResult> {
        return try {
            antiFraudService.removeStolenCard(cardNumber)
            ResponseEntity(ActionResult("Card $cardNumber successfully removed!"), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: NotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}