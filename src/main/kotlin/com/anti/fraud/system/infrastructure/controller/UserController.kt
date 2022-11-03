package com.anti.fraud.system.infrastructure.controller

import com.anti.fraud.system.domain.model.user.*
import com.anti.fraud.system.domain.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class UserController(@Autowired val userService: UserService) {
    @GetMapping("list")
    fun getUsers(): ResponseEntity<List<UserAuthResponse>> = try {
        val body = userService.getAllUsers()
        ResponseEntity(body, HttpStatus.OK)
    } catch (e: Exception) {
        e.printStackTrace()
        ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @PostMapping("user")
    fun authUser(@RequestBody user: UserAuthRequest): ResponseEntity<UserAuthResponse> =
        try {
            ResponseEntity(userService.createUser(user), HttpStatus.CREATED)
        } catch (e: AlreadyExistException) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.CONFLICT)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }

    @DeleteMapping("user/{username}")
    fun deleteUser(@PathVariable(name = "username") username: String): ResponseEntity<ActionResponse> {
        return try {
            userService.deleteUser(username)
            ResponseEntity(ActionResponse("Deleted successfully!", username), HttpStatus.OK)
        } catch (e: NotFoundException) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PutMapping("role")
    fun changeUserRole(@RequestBody user: UserRoleChangeRequest): ResponseEntity<UserAuthResponse> {
        return try {
            val changeUserRole = userService.changeUserRole(user)
            ResponseEntity(changeUserRole, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: NotFoundException) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: AlreadyExistException) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.CONFLICT)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PutMapping("access")
    fun accessUser(@RequestBody user: UserOperationRequest): ResponseEntity<ActionResponse> {
        return try {
            val isLock = userService.lockUser(user)
            val response = ActionResponse("User ${user.username} ${if (isLock) "locked" else "unlocked"}!")
            ResponseEntity(response, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: NotFoundException) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}