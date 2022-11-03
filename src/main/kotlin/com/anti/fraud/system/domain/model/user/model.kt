package com.anti.fraud.system.domain.model.user

import kotlinx.serialization.Serializable
import javax.persistence.*

@Serializable
data class UserAuthRequest(val name: String, val username: String, val password: String)

@Serializable
data class UserAuthResponse(val id: Long, val name: String, val username: String, val role: UserRole)

@Serializable
data class ActionResponse(val status: String, val username: String = "")

@Serializable
data class ActionResult(val status: String)

@Serializable
data class UserRoleChangeRequest(val username: String, val role: String)

@Serializable
data class UserOperationRequest(val username: String, val operation: UserOperation)

@Serializable
enum class UserOperation {
    LOCK,
    UNLOCK
}

@Serializable
enum class UserRole {
    MERCHANT, ADMINISTRATOR, SUPPORT
}

@Entity
@Table(name = "users")
data class User(
    val name: String,
    @Column(unique = true)
    val username: String,
    val password: String,
    var role: UserRole = UserRole.MERCHANT,
    var locked: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
)

class AlreadyExistException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
