package com.trackeco.trackeco

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trackeco.trackeco.api.*
import com.trackeco.trackeco.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// This is the single source of truth for the entire logged-in UI
data class AppUiState(
    val isFetchingInitialData: Boolean = true,
    val isProcessingDisposal: Boolean = false,
    val userData: UserData? = null,
    val lastDisposalResult: DisposalResult? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val apiService: ApiService = RetrofitClient.instance

    fun fetchUserData(userId: String) {
        viewModelScope.launch {
            if (_uiState.value.userData == null) {
                _uiState.update { it.copy(isFetchingInitialData = true) }
            }
            try {
                val data = apiService.getUserData(userId)
                _uiState.update { it.copy(isFetchingInitialData = false, userData = data) }
            } catch (e: Exception) {
                println("Error fetching user data: ${e.message}")
                _uiState.update { it.copy(isFetchingInitialData = false) }
            }
        }
    }

    // This is the new, fully functional disposal method
    fun processDisposal(userId: String, videoUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingDisposal = true) }
            try {
                // Step 1: Convert video file to Base64 string
                val videoBytes = getApplication<Application>().contentResolver.openInputStream(videoUri)?.readBytes()
                if (videoBytes == null) {
                    throw IllegalStateException("Could not read video file")
                }
                val videoBase64 = Base64.encodeToString(videoBytes, Base64.DEFAULT)

                // Step 2: Create the request object
                val request = DisposalRequest(
                    user_id = userId,
                    latitude = -6.2088, // TODO: Replace with real GPS data
                    longitude = 106.8456,
                    video = videoBase64
                )

                // Step 3: Call the real API endpoint
                val result = apiService.verifyDisposal(request)
                
                // Step 4: Update the UI with the rich result
                _uiState.update { it.copy(lastDisposalResult = result) }
                
                // Step 5: Refresh user data to show updated points/streak
                fetchUserData(userId)

            } catch (e: Exception) {
                println("Error processing disposal: ${e.message}")
                val errorResult = DisposalResult(false, 0, 0, null, null, null, null, "CLIENT_ERROR", "An error occurred. Please try again.", null, null, null, null)
                _uiState.update { it.copy(lastDisposalResult = errorResult) }
            } finally {
                _uiState.update { it.copy(isProcessingDisposal = false) }
            }
        }
    }

    fun clearLastDisposalResult() {
        _uiState.update { it.copy(lastDisposalResult = null) }
    }
}