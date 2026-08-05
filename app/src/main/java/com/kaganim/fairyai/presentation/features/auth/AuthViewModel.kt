package com.kaganim.fairyai.presentation.features.auth

import androidx.lifecycle.viewModelScope
import com.kaganim.fairyai.domain.usecase.AuthUseCases
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : BaseViewModel<AuthState>(AuthState()) {

    init {
        checkAuthStatus()
        observeCurrentUser()
    }

    private fun checkAuthStatus() {
        val isLoggedIn = authUseCases.isUserLoggedIn()
        updateState { copy(isLoggedIn = isLoggedIn) }
    }

    private fun observeCurrentUser() {
        authUseCases.getCurrentUser()
            .onEach { user ->
                updateState { copy(isLoggedIn = user != null) }
            }
            .launchIn(viewModelScope)
    }

    fun onEmailChange(email: String) {
        updateState { copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        updateState { copy(password = password, error = null) }
    }

    fun login() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            authUseCases.login(uiState.value.email, uiState.value.password)
                .onSuccess {
                    updateState { copy(isLoading = false, isSuccess = true, isLoggedIn = true) }
                }
                .onFailure { e ->
                    updateState { copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun register() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            authUseCases.register(uiState.value.email, uiState.value.password)
                .onSuccess {
                    updateState { copy(isLoading = false, isSuccess = true, isLoggedIn = true) }
                }
                .onFailure { e ->
                    updateState { copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authUseCases.logout()
            updateState { copy(isLoggedIn = false, isSuccess = false) }
        }
    }
}
