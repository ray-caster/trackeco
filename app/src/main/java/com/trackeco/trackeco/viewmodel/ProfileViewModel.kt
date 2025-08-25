package com.trackeco.trackeco.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.trackeco.trackeco.repository.UserRepository
import com.trackeco.trackeco.repository.WasteRecordRepository
import com.trackeco.trackeco.repository.DailyChallengeRepository
import com.trackeco.trackeco.data.database.entities.UserEntity
import com.trackeco.trackeco.data.database.entities.WasteRecordEntity
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val wasteRecordRepository: WasteRecordRepository,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // User data flow
    val currentUser: StateFlow<UserEntity?> = userRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // User's waste records
    val userWasteRecords: StateFlow<List<WasteRecordEntity>> = currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            wasteRecordRepository.getWasteRecordsFlow(user.userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User statistics
    val userStats: StateFlow<UserStats> = combine(
        currentUser,
        userWasteRecords
    ) { user, records ->
        calculateUserStats(user, records)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserStats()
    )

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val currentUserId = userRepository.getCurrentUserId()
                if (currentUserId != null) {
                    // Refresh user data
                    userRepository.fetchAndSyncUserData(currentUserId)
                    
                    // Load completed challenges count
                    val completedChallenges = dailyChallengeRepository.getCompletedChallengesCount(currentUserId)
                    _uiState.value = _uiState.value.copy(
                        completedChallengesCount = completedChallenges
                    )
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load profile data: ${e.message}"
                )
            }
        }
    }

    fun refreshProfile() {
        loadProfileData()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.logout()
                _uiState.value = _uiState.value.copy(showLogoutConfirmation = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Logout failed: ${e.message}"
                )
            }
        }
    }

    fun showLogoutConfirmation() {
        _uiState.value = _uiState.value.copy(showLogoutConfirmation = true)
    }

    fun hideLogoutConfirmation() {
        _uiState.value = _uiState.value.copy(showLogoutConfirmation = false)
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = tabIndex)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Get waste records grouped by category
    fun getWasteRecordsByCategory(): StateFlow<Map<String, List<WasteRecordEntity>>> {
        return userWasteRecords.map { records ->
            records.groupBy { it.category }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    }

    // Get recent activity (last 10 records)
    fun getRecentActivity(): StateFlow<List<WasteRecordEntity>> {
        return userWasteRecords.map { records ->
            records.take(10)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private fun calculateUserStats(user: UserEntity?, records: List<WasteRecordEntity>): UserStats {
        if (user == null) return UserStats()

        val totalItems = records.sumOf { it.quantity }
        val totalPoints = records.sumOf { it.pointsEarned }
        val totalXp = records.sumOf { it.xpEarned }
        val categoriesDiscovered = records.map { it.category }.distinct().size
        val subtypesDiscovered = records.map { "${it.category}:${it.subtype}" }.distinct().size
        
        // Calculate this week's activity
        val now = java.time.LocalDateTime.now()
        val weekStart = now.minusDays(7)
        val thisWeekRecords = records.filter { it.createdAt.isAfter(weekStart) }
        val thisWeekItems = thisWeekRecords.sumOf { it.quantity }
        
        // Calculate level progress
        val currentLevelProgress = if (user.xpNeededForNext > 0) {
            ((user.xp.toFloat() / (user.xp + user.xpNeededForNext)) * 100).coerceIn(0f, 100f)
        } else {
            100f
        }

        return UserStats(
            totalItemsDisposed = totalItems,
            totalPointsEarned = totalPoints,
            totalXpEarned = totalXp,
            categoriesDiscovered = categoriesDiscovered,
            subtypesDiscovered = subtypesDiscovered,
            currentStreak = user.streak,
            thisWeekItems = thisWeekItems,
            currentLevel = user.currentLevel,
            currentLevelName = user.currentLevelName,
            currentLevelProgress = currentLevelProgress,
            nextLevelName = user.nextLevelName,
            xpNeededForNext = user.xpNeededForNext,
            memberSince = user.memberSince ?: "Recently",
            communityRank = user.communityRank
        )
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val selectedTabIndex: Int = 0,
    val showLogoutConfirmation: Boolean = false,
    val completedChallengesCount: Int = 0,
    val errorMessage: String? = null
)

data class UserStats(
    val totalItemsDisposed: Int = 0,
    val totalPointsEarned: Int = 0,
    val totalXpEarned: Int = 0,
    val categoriesDiscovered: Int = 0,
    val subtypesDiscovered: Int = 0,
    val currentStreak: Int = 0,
    val thisWeekItems: Int = 0,
    val currentLevel: Int = 1,
    val currentLevelName: String = "Eco Newcomer",
    val currentLevelProgress: Float = 0f,
    val nextLevelName: String = "Green Starter",
    val xpNeededForNext: Int = 50,
    val memberSince: String = "Recently",
    val communityRank: String = "#1 globally"
)