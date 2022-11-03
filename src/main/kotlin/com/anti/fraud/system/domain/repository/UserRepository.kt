package com.anti.fraud.system.domain.repository

import com.anti.fraud.system.domain.model.user.User
import com.anti.fraud.system.domain.model.user.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): List<User>
    fun findByRole(username: UserRole): List<User>
}