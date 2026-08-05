package com.kaganim.fairyai.domain.usecase

import com.kaganim.fairyai.domain.model.User
import com.kaganim.fairyai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class AuthUseCases(
    val login: LoginUseCase,
    val register: RegisterUseCase,
    val logout: LogoutUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val isUserLoggedIn: IsUserLoggedInUseCase
)

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> = repository.login(email, password)
}

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> = repository.register(email, password)
}

class LogoutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Flow<User?> = repository.currentUser
}

class IsUserLoggedInUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Boolean = repository.isUserLoggedIn()
}
