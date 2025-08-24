package com.trackeco.trackeco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackeco.trackeco.api.ApiService
import com.trackeco.trackeco.api.DisposalResult
import com.trackeco.trackeco.api.RetrofitClient
import com.trackeco.trackeco.api.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val isFetchingInitialData: Boolean = true,
    val isSimulatingDisposal: Boolean = false,
    val userData: UserData? = null,
    val lastDisposalResult: DisposalResult? = null
)

// The ViewModel now takes the userId as a parameter in its functions
class MainViewModel : ViewModel() {

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

    fun simulateDisposal(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSimulatingDisposal = true) }
            try {
                val result = apiService.verifyDisposalMock(userId)
                _uiState.update { it.copy(lastDisposalResult = result) }
                fetchUserData(userId) // Refresh data after result
            } catch (e: Exception) {
                println("Error simulating disposal: ${e.message}")
                val errorResult = DisposalResult(false, 0, 0, "Could not connect to the server.")
                _uiState.update { it.copy(lastDisposalResult = errorResult) }
            } finally {
                _uiState.update { it.copy(isSimulatingDisposal = false) }
            }
        }
    }

    fun clearLastDisposalResult() {
        _uiState.update { it.copy(lastDisposalResult = null) }
    }
}