package com.trackeco.trackeco.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.trackeco.trackeco.repository.UserRepository
import com.trackeco.trackeco.data.models.AuthResponse
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            try {
                val isAuthenticated = userRepository.isUserAuthenticated()
                _authState.value = if (isAuthenticated) {
                    AuthState.Authenticated
                } else {
                    AuthState.NotAuthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.NotAuthenticated
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = userRepository.login(username, password)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Login successful!"
                    )
                    _authState.value = AuthState.Authenticated
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An error occurred"
                )
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = userRepository.register(username, email, password)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Registration successful!"
                    )
                    _authState.value = AuthState.Authenticated
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An error occurred"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.logout()
                _authState.value = AuthState.NotAuthenticated
                _uiState.value = AuthUiState() // Reset UI state
            } catch (e: Exception) {
                // Even if logout fails, clear local state
                _authState.value = AuthState.NotAuthenticated
                _uiState.value = AuthUiState()
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun setIsLoginMode(isLogin: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoginMode = isLogin,
            errorMessage = null,
            successMessage = null
        )
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoginMode: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object NotAuthenticated : AuthState()
}