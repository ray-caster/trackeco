package com.trackeco.trackeco.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.dao.DailyChallengeDao
import com.trackeco.trackeco.data.database.entities.DailyChallengeEntity
import com.trackeco.trackeco.data.models.DailyChallenge
import com.trackeco.trackeco.network.TrackEcoApiService
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyChallengeRepository @Inject constructor(
    private val dailyChallengeDao: DailyChallengeDao,
    private val apiService: TrackEcoApiService
) {
    companion object {
        private const val TAG = "DailyChallengeRepository"
    }

    fun getCurrentDailyChallengeFlow(userId: String): Flow<DailyChallengeEntity?> {
        return dailyChallengeDao.getCurrentDailyChallengeFlow(userId)
    }

    suspend fun getCurrentDailyChallenge(userId: String): DailyChallengeEntity? {
        return dailyChallengeDao.getCurrentDailyChallenge(userId)
    }

    suspend fun fetchAndSyncDailyChallenge(userId: String): Result<DailyChallenge> {
        return try {
            val response = apiService.getDailyChallenge(userId)
            if (response.isSuccessful && response.body() != null) {
                val serverChallenge = response.body()!!
                
                // Convert to local entity
                val localEntity = DailyChallengeEntity(
                    challengeId = serverChallenge.challengeId ?: UUID.randomUUID().toString(),
                    userId = userId,
                    description = serverChallenge.description,
                    goal = serverChallenge.goal,
                    currentProgress = serverChallenge.progress,
                    reward = serverChallenge.reward,
                    challengeType = serverChallenge.challengeType,
                    targetCategory = serverChallenge.targetCategory,
                    isCompleted = serverChallenge.isCompleted,
                    expiresAt = serverChallenge.expiresAt?.let { 
                        LocalDateTime.parse(it) 
                    } ?: LocalDateTime.now().plusDays(1),
                    lastUpdated = LocalDateTime.now()
                )
                
                dailyChallengeDao.insertChallenge(localEntity)
                Result.success(serverChallenge)
            } else {
                // Return cached challenge if server fails
                val cachedChallenge = dailyChallengeDao.getCurrentDailyChallenge(userId)
                if (cachedChallenge != null) {
                    val challenge = DailyChallenge(
                        description = cachedChallenge.description,
                        goal = cachedChallenge.goal,
                        progress = cachedChallenge.currentProgress,
                        reward = cachedChallenge.reward,
                        challengeId = cachedChallenge.challengeId,
                        challengeType = cachedChallenge.challengeType,
                        targetCategory = cachedChallenge.targetCategory,
                        isCompleted = cachedChallenge.isCompleted,
                        expiresAt = cachedChallenge.expiresAt.toString()
                    )
                    Result.success(challenge)
                } else {
                    // Create a default challenge
                    val defaultChallenge = createDefaultChallenge(userId)
                    dailyChallengeDao.insertChallenge(defaultChallenge)
                    
                    val challenge = DailyChallenge(
                        description = defaultChallenge.description,
                        goal = defaultChallenge.goal,
                        progress = defaultChallenge.currentProgress,
                        reward = defaultChallenge.reward,
                        challengeId = defaultChallenge.challengeId,
                        challengeType = defaultChallenge.challengeType,
                        targetCategory = defaultChallenge.targetCategory,
                        isCompleted = defaultChallenge.isCompleted,
                        expiresAt = defaultChallenge.expiresAt.toString()
                    )
                    Result.success(challenge)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching daily challenge", e)
            
            // Fallback to cached data
            val cachedChallenge = dailyChallengeDao.getCurrentDailyChallenge(userId)
            if (cachedChallenge != null) {
                val challenge = DailyChallenge(
                    description = cachedChallenge.description,
                    goal = cachedChallenge.goal,
                    progress = cachedChallenge.currentProgress,
                    reward = cachedChallenge.reward,
                    challengeId = cachedChallenge.challengeId,
                    challengeType = cachedChallenge.challengeType,
                    targetCategory = cachedChallenge.targetCategory,
                    isCompleted = cachedChallenge.isCompleted,
                    expiresAt = cachedChallenge.expiresAt.toString()
                )
                Result.success(challenge)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateChallengeProgress(userId: String, progressIncrement: Int = 1): Result<DailyChallenge> {
        return try {
            val currentChallenge = dailyChallengeDao.getCurrentDailyChallenge(userId)
            if (currentChallenge != null) {
                val newProgress = (currentChallenge.currentProgress + progressIncrement).coerceAtMost(currentChallenge.goal)
                val isCompleted = newProgress >= currentChallenge.goal
                
                // Update local database
                if (isCompleted && !currentChallenge.isCompleted) {
                    dailyChallengeDao.markAsCompleted(currentChallenge.challengeId)
                } else {
                    dailyChallengeDao.updateProgress(currentChallenge.challengeId, newProgress)
                }
                
                // Try to sync with server
                try {
                    val response = apiService.updateChallengeProgress(
                        userId, 
                        mapOf("progress" to newProgress)
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val serverChallenge = response.body()!!
                        
                        // Update local entity with server response
                        val updatedEntity = currentChallenge.copy(
                            currentProgress = serverChallenge.progress,
                            isCompleted = serverChallenge.isCompleted,
                            completedAt = if (serverChallenge.isCompleted) LocalDateTime.now() else null,
                            lastUpdated = LocalDateTime.now()
                        )
                        dailyChallengeDao.updateChallenge(updatedEntity)
                        
                        Result.success(serverChallenge)
                    } else {
                        // Server update failed, but local update succeeded
                        val updatedEntity = currentChallenge.copy(
                            currentProgress = newProgress,
                            isCompleted = isCompleted,
                            completedAt = if (isCompleted) LocalDateTime.now() else null,
                            lastUpdated = LocalDateTime.now()
                        )
                        dailyChallengeDao.updateChallenge(updatedEntity)
                        
                        val challenge = DailyChallenge(
                            description = updatedEntity.description,
                            goal = updatedEntity.goal,
                            progress = updatedEntity.currentProgress,
                            reward = updatedEntity.reward,
                            challengeId = updatedEntity.challengeId,
                            challengeType = updatedEntity.challengeType,
                            targetCategory = updatedEntity.targetCategory,
                            isCompleted = updatedEntity.isCompleted,
                            expiresAt = updatedEntity.expiresAt.toString()
                        )
                        Result.success(challenge)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Network error updating challenge progress, saved locally", e)
                    
                    // Network error, but local update succeeded
                    val updatedEntity = currentChallenge.copy(
                        currentProgress = newProgress,
                        isCompleted = isCompleted,
                        completedAt = if (isCompleted) LocalDateTime.now() else null,
                        lastUpdated = LocalDateTime.now()
                    )
                    dailyChallengeDao.updateChallenge(updatedEntity)
                    
                    val challenge = DailyChallenge(
                        description = updatedEntity.description,
                        goal = updatedEntity.goal,
                        progress = updatedEntity.currentProgress,
                        reward = updatedEntity.reward,
                        challengeId = updatedEntity.challengeId,
                        challengeType = updatedEntity.challengeType,
                        targetCategory = updatedEntity.targetCategory,
                        isCompleted = updatedEntity.isCompleted,
                        expiresAt = updatedEntity.expiresAt.toString()
                    )
                    Result.success(challenge)
                }
            } else {
                Result.failure(Exception("No current challenge found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating challenge progress", e)
            Result.failure(e)
        }
    }

    suspend fun getCompletedChallengesCount(userId: String): Int {
        return dailyChallengeDao.getCompletedChallengesCount(userId)
    }

    suspend fun cleanupExpiredChallenges() {
        try {
            dailyChallengeDao.deleteExpiredChallenges(LocalDateTime.now().minusDays(7))
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired challenges", e)
        }
    }

    private fun createDefaultChallenge(userId: String): DailyChallengeEntity {
        val challenges = listOf(
            "Dispose of 3 items today" to "mixed",
            "Find 2 different waste types" to null,
            "Clean up 5 plastic items" to "Plastic",
            "Discover a new waste category" to null,
            "Dispose of 4 different sub-types" to null
        )
        
        val (description, targetCategory) = challenges.random()
        
        return DailyChallengeEntity(
            challengeId = UUID.randomUUID().toString(),
            userId = userId,
            description = description,
            goal = when {
                description.contains("3") -> 3
                description.contains("2") -> 2
                description.contains("5") -> 5
                description.contains("4") -> 4
                else -> 1
            },
            reward = 50,
            challengeType = "disposal",
            targetCategory = targetCategory,
            expiresAt = LocalDateTime.now().plusDays(1)
        )
    }
}