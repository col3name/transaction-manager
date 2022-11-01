package com.anti.fraud.system.domain.services

import com.anti.fraud.system.domain.model.user.UserAuthRequest
import com.anti.fraud.system.domain.model.user.UserAuthResponse
import com.anti.fraud.system.domain.model.user.UserOperationRequest
import com.anti.fraud.system.domain.model.user.UserRoleChangeRequest

interface UserService {
    fun createUser(authUser: UserAuthRequest): UserAuthResponse
    fun getAllUsers(): List<UserAuthResponse>
    fun deleteUser(username: String)
    fun changeUserRole(user: UserRoleChangeRequest): UserAuthResponse
    fun lockUser(userRequest: UserOperationRequest): Boolean
}