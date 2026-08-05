package com.kaganim.fairyai.data.repository

import android.content.SharedPreferences
import com.kaganim.fairyai.domain.model.User
import com.kaganim.fairyai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    private val PREF_IS_LOGGED_IN = "is_logged_in"
    private val PREF_USER_EMAIL = "user_email"

    init {
        if (isUserLoggedIn()) {
            val email = sharedPreferences.getString(PREF_USER_EMAIL, "") ?: ""
            _currentUser.value = User("local_id", email, "Master")
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return if (email == "phaethon" && password == "ghani") {
            val user = User("local_id", email, "Master")
            sharedPreferences.edit()
                .putBoolean(PREF_IS_LOGGED_IN, true)
                .putString(PREF_USER_EMAIL, email)
                .apply()
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Kredensial salah, akses ditolak!"))
        }
    }

    override suspend fun register(email: String, password: String): Result<User> {
        return Result.failure(Exception("Registrasi tidak diizinkan untuk asisten pribadi."))
    }

    override suspend fun logout() {
        sharedPreferences.edit().clear().apply()
        _currentUser.value = null
    }

    override fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }
}
