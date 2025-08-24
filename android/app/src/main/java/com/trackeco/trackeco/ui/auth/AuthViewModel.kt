package com.trackeco.trackeco.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trackeco.trackeco.api.ApiService
import com.trackeco.trackeco.api.AuthRequest
import com.trackeco.trackeco.api.RetrofitClient
import com.trackeco.trackeco.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationSuccess: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferencesRepository(application)
    private val apiService: ApiService = RetrofitClient.instance
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiService.login(AuthRequest(email, password))
                userPreferences.saveUserId(response.user_id)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid credentials. Please try again.") }
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiService.register(AuthRequest(email, password))
                if (response.success) {
                    _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, errorMessage = "Registration failed. Email may already be in use.") }
            }
        }
    }
    
    fun resetRegistrationStatus() {
        _uiState.update { it.copy(registrationSuccess = false) }
    }
}