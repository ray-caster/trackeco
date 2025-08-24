package com.trackeco.trackeco.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trackeco.trackeco.api.ApiService
import com.trackeco.trackeco.api.LoginRequest
import com.trackeco.trackeco.api.RetrofitClient
import com.trackeco.trackeco.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferencesRepository(application)
    private val apiService: ApiService = RetrofitClient.instance
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        // Basic validation
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // This is the REAL API call to your Python backend
                val loginRequest = LoginRequest(email, password)
                val userData = apiService.login(loginRequest)
                
                // If the login is successful, the server returns user data.
                // We save the user's ID to persist the session.
                // This now correctly references the 'user_id' field from the UserData class.
                userPreferences.saveUserId(userData.user_id)
                
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                // If the backend returns an error (like 401 Unauthorized), it will be caught here.
                _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid credentials. Please try again.") }
                println("Login failed: ${e.message}")
            }
        }
    }

    fun signUp(email: String, password: String) {
       // TODO: Implement sign up API call, similar to login
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserId()
        }
    }
}