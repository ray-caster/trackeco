package com.trackeco.trackeco.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isAuthenticated = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isAuthenticated = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET points = :points, xp = :xp, streak = :streak WHERE userId = :userId")
    suspend fun updateUserStats(userId: String, points: Int, xp: Int, streak: Int)

    @Query("UPDATE users SET isAuthenticated = 0, authToken = null")
    suspend fun logoutAllUsers()

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}