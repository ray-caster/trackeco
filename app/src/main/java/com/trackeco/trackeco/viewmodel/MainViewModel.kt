package com.trackeco.trackeco.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.trackeco.trackeco.repository.UserRepository
import com.trackeco.trackeco.repository.WasteRecordRepository
import com.trackeco.trackeco.repository.HotspotRepository
import com.trackeco.trackeco.repository.DailyChallengeRepository
import com.trackeco.trackeco.data.database.entities.UserEntity
import com.trackeco.trackeco.data.database.entities.WasteRecordEntity
import com.trackeco.trackeco.data.database.entities.HotspotEntity
import com.trackeco.trackeco.data.database.entities.DailyChallengeEntity
import com.trackeco.trackeco.utils.OfflineSyncManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val wasteRecordRepository: WasteRecordRepository,
    private val hotspotRepository: HotspotRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val offlineSyncManager: OfflineSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // User data flow
    val currentUser: StateFlow<UserEntity?> = userRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Waste records flow
    val wasteRecords: StateFlow<List<WasteRecordEntity>> = currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            wasteRecordRepository.getWasteRecordsFlow(user.userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Hotspots flow
    val hotspots: StateFlow<List<HotspotEntity>> = hotspotRepository.getActiveHotspotsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Daily challenge flow
    val dailyChallenge: StateFlow<DailyChallengeEntity?> = currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            dailyChallengeRepository.getCurrentDailyChallengeFlow(user.userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Network status
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        refreshData()
        checkNetworkStatus()
        schedulePeriodicSync()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            try {
                val currentUserId = userRepository.getCurrentUserId()
                if (currentUserId != null) {
                    // Try to sync all data
                    launch { userRepository.fetchAndSyncUserData(currentUserId) }
                    launch { hotspotRepository.fetchAndSyncHotspots() }
                    launch { dailyChallengeRepository.fetchAndSyncDailyChallenge(currentUserId) }
                    launch { wasteRecordRepository.syncUnsyncedRecords() }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Refresh failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun performSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true)
                offlineSyncManager.performImmediateSync()
                
                // Wait a bit for sync to complete
                kotlinx.coroutines.delay(2000)
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Sync failed: ${e.message}"
                )
            }
        }
    }

    private fun checkNetworkStatus() {
        viewModelScope.launch {
            _isOnline.value = offlineSyncManager.isNetworkAvailable()
        }
    }

    private fun schedulePeriodicSync() {
        offlineSyncManager.schedulePeriodicSync()
    }

    fun submitWasteRecord(
        category: String,
        subtype: String,
        quantity: Int = 1,
        imagePath: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ) {
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserId()
            if (currentUserId != null) {
                try {
                    val result = wasteRecordRepository.submitWasteRecord(
                        userId = currentUserId,
                        category = category,
                        subtype = subtype,
                        quantity = quantity,
                        localImagePath = imagePath,
                        latitude = latitude,
                        longitude = longitude,
                        locationName = locationName
                    )
                    
                    if (result.isSuccess) {
                        val response = result.getOrNull()
                        if (response?.levelUp == true) {
                            _uiState.value = _uiState.value.copy(
                                showLevelUpDialog = true,
                                levelUpMessage = "Congratulations! You've reached a new level!"
                            )
                        }
                        
                        // Refresh user data to get updated points/XP
                        userRepository.fetchAndSyncUserData(currentUserId)
                        
                        // Update challenge progress if applicable
                        dailyChallengeRepository.updateChallengeProgress(currentUserId, 1)
                        
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Waste record submitted successfully! Earned ${response?.pointsAwarded ?: 10} points."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit record"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error submitting record: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun dismissLevelUpDialog() {
        _uiState.value = _uiState.value.copy(showLevelUpDialog = false, levelUpMessage = null)
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = tabIndex)
    }

    override fun onCleared() {
        super.onCleared()
        offlineSyncManager.cancelPeriodicSync()
    }
}

data class MainUiState(
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showLevelUpDialog: Boolean = false,
    val levelUpMessage: String? = null,
    val selectedTabIndex: Int = 0,
    val lastSyncTime: Long? = null
)