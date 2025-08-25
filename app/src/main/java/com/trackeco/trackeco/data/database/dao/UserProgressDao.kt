package com.trackeco.trackeco.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.entities.UserProgressEntity

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getUserProgressFlow(userId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    suspend fun getUserProgress(userId: String): List<UserProgressEntity>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND category = :category")
    suspend fun getProgressForCategory(userId: String, category: String): UserProgressEntity?

    @Query("SELECT SUM(totalDisposed) FROM user_progress WHERE userId = :userId")
    suspend fun getTotalDisposedCount(userId: String): Int

    @Query("SELECT SUM(totalPoints) FROM user_progress WHERE userId = :userId")
    suspend fun getTotalPointsFromProgress(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<UserProgressEntity>)

    @Update
    suspend fun updateProgress(progress: UserProgressEntity)

    @Query("UPDATE user_progress SET totalDisposed = totalDisposed + 1, lastDisposedAt = :disposalTime, updatedAt = :updateTime WHERE userId = :userId AND category = :category")
    suspend fun incrementDisposal(userId: String, category: String, disposalTime: java.time.LocalDateTime, updateTime: java.time.LocalDateTime = java.time.LocalDateTime.now())

    @Delete
    suspend fun deleteProgress(progress: UserProgressEntity)

    @Query("DELETE FROM user_progress WHERE userId = :userId")
    suspend fun deleteProgressForUser(userId: String)
}