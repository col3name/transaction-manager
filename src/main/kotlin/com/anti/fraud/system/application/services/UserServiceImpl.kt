package com.anti.fraud.system.application.services

import com.anti.fraud.system.domain.model.user.*
import com.anti.fraud.system.domain.services.UserService
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(var users: MutableList<User> = mutableListOf()) : UserService {
    override fun createUser(authUser: UserAuthRequest): UserAuthResponse {
        if (!users.none { it.username == authUser.username }) {
            throw AlreadyExistException("user with username ${authUser.username} already exist")
        }
        val role = getUserRole()
        val id = users.lastIndex + 1L
        users.add(User(id, authUser.name, authUser.username, authUser.password, role, role != UserRole.ADMINISTRATOR))
        return UserAuthResponse(id, authUser.name, authUser.username, role)
    }

    override fun getAllUsers(): List<UserAuthResponse> =
        users.map { UserAuthResponse(it.id, it.name, it.username, it.role) }
            .sortedBy { it.id }

    override fun deleteUser(username: String) {
        if (!users.removeIf { username == it.username }) {
            throw NotFoundException("user $username not exist")
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun changeUserRole(user: UserRoleChangeRequest): UserAuthResponse {
        val userRole = UserRole.valueOf(user.role)

        val userPair = findUserByUsername(user.username)
        if (userPair.first.role == userRole) {
            throw AlreadyExistException("user with username ${user.username} already exist")
        }
        if (userRole != UserRole.MERCHANT && userRole != UserRole.SUPPORT) {
            throw IllegalArgumentException("user role must be MERCHANT or SUPPORT")
        }
        val index = userPair.second
        users[index].role = userRole
        val userDb = users[index]

        return UserAuthResponse(userDb.id, userDb.name, userDb.username, userDb.role)
    }

    private fun findUserByUsername(username: String): Pair<User, Int> {
        val index = users.indexOfFirst { username == it.username }
        if (index == -1) {
            throw NotFoundException("user $username not exist")
        }
        return Pair(users[index], index)
    }

    override fun lockUser(userRequest: UserOperationRequest): Boolean {
        val userPair = findUserByUsername(userRequest.username)
        val user = userPair.first
        if (user.role == UserRole.ADMINISTRATOR) {
            throw IllegalArgumentException("ADMINISTRATOR cannot be blocked")
        }
        val index = userPair.second
        when (userRequest.operation) {
            UserOperation.LOCK -> users[index].locked = true
            UserOperation.UNLOCK -> users[index].locked = false
        }

        return users[index].locked
    }

    private fun getUserRole() = if (users.isEmpty()) UserRole.ADMINISTRATOR else UserRole.MERCHANT
}