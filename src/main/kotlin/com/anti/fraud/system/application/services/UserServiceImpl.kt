package com.anti.fraud.system.application.services

import com.anti.fraud.system.domain.model.user.*
import com.anti.fraud.system.domain.repository.UserRepository
import com.anti.fraud.system.domain.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(@Autowired val userRepository: UserRepository) : UserService {
    override fun createUser(authUser: UserAuthRequest): UserAuthResponse {
        val users = userRepository.findByUsername(authUser.username)
        if (users.isNotEmpty()) {
            throw AlreadyExistException("user with username ${authUser.username} already exist")
        }
        val admins = userRepository.findByRole(UserRole.ADMINISTRATOR)
        val role = getUserRole(admins)

        val save = userRepository.save(
            User(
                authUser.name,
                authUser.username,
                authUser.password,
                role,
                role != UserRole.ADMINISTRATOR
            )
        )
        return UserAuthResponse(save.id, authUser.name, authUser.username, role)
    }

    override fun getAllUsers(): List<UserAuthResponse> =
        userRepository.findAll(sortByIdAsc()).map {
            UserAuthResponse(it.id, it.name, it.username, it.role)
        }

    fun sortByIdAsc(): Sort {
        return Sort.by(Sort.Direction.ASC, "id")
    }

    override fun deleteUser(username: String) {
        userRepository.findByUsername(username)
            .map(userRepository::delete)
    }

    @Throws(IllegalArgumentException::class)
    override fun changeUserRole(user: UserRoleChangeRequest): UserAuthResponse {
        val userRole = UserRole.valueOf(user.role)

        val userDB = userRepository.findByUsername(user.username).first()
        if (userDB.role == userRole) {
            throw AlreadyExistException("user with username ${user.username} already exist")
        }
        if (userRole != UserRole.MERCHANT && userRole != UserRole.SUPPORT) {
            throw IllegalArgumentException("user role must be MERCHANT or SUPPORT")
        }

        userDB.role = userRole
        userRepository.save(userDB)

        return UserAuthResponse(userDB.id, userDB.name, userDB.username, userDB.role)
    }

    override fun lockUser(userRequest: UserOperationRequest): Boolean {
        val user = userRepository.findByUsername(userRequest.username).first()

        if (user.role == UserRole.ADMINISTRATOR) {
            throw IllegalArgumentException("ADMINISTRATOR cannot be blocked")
        }
        when (userRequest.operation) {
            UserOperation.LOCK -> user.locked = true
            UserOperation.UNLOCK -> user.locked = false
        }

        userRepository.save(user)

        return user.locked
    }

    private fun getUserRole(users: List<User>) =
        if (users.isEmpty()) UserRole.ADMINISTRATOR else UserRole.MERCHANT
}