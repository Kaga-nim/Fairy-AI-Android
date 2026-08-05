package com.kaganim.fairyai.domain.repository

import com.kaganim.fairyai.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String): Result<User>
    suspend fun logout()
    fun isUserLoggedIn(): Boolean
}
