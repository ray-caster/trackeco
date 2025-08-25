package com.trackeco.trackeco.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.entities.DailyChallengeEntity
import java.time.LocalDateTime

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenges WHERE userId = :userId AND expiresAt > :currentTime ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentDailyChallenge(userId: String, currentTime: LocalDateTime = LocalDateTime.now()): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenges WHERE userId = :userId AND expiresAt > :currentTime ORDER BY createdAt DESC LIMIT 1")
    fun getCurrentDailyChallengeFlow(userId: String, currentTime: LocalDateTime = LocalDateTime.now()): Flow<DailyChallengeEntity?>

    @Query("SELECT * FROM daily_challenges WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllChallengesFlow(userId: String): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenges WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    suspend fun getCompletedChallenges(userId: String): List<DailyChallengeEntity>

    @Query("SELECT COUNT(*) FROM daily_challenges WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedChallengesCount(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: DailyChallengeEntity)

    @Update
    suspend fun updateChallenge(challenge: DailyChallengeEntity)

    @Query("UPDATE daily_challenges SET currentProgress = :progress, lastUpdated = :updateTime WHERE challengeId = :challengeId")
    suspend fun updateProgress(challengeId: String, progress: Int, updateTime: LocalDateTime = LocalDateTime.now())

    @Query("UPDATE daily_challenges SET isCompleted = 1, completedAt = :completionTime WHERE challengeId = :challengeId")
    suspend fun markAsCompleted(challengeId: String, completionTime: LocalDateTime = LocalDateTime.now())

    @Delete
    suspend fun deleteChallenge(challenge: DailyChallengeEntity)

    @Query("DELETE FROM daily_challenges WHERE expiresAt < :cutoffTime")
    suspend fun deleteExpiredChallenges(cutoffTime: LocalDateTime)

    @Query("DELETE FROM daily_challenges WHERE userId = :userId")
    suspend fun deleteChallengesForUser(userId: String)
}