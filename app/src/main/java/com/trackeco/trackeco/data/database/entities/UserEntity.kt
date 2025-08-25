package com.trackeco.trackeco.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val email: String?,
    val points: Int = 0,
    val xp: Int = 0,
    val streak: Int = 0,
    val ecoRank: String = "Eco Novice",
    val hasCompletedFirstDisposal: Boolean = false,
    val memberSince: String? = null,
    val communityRank: String = "#1 globally",
    val totalUsers: Int = 1,
    val activeUsers: Int = 1,
    val currentLevel: Int = 1,
    val currentLevelName: String = "Eco Newcomer",
    val currentLevelColor: String = "#8B5CF6",
    val nextLevelName: String = "Green Starter",
    val xpNeededForNext: Int = 50,
    val progressPercentage: Float = 0f,
    val isAuthenticated: Boolean = false,
    val authToken: String? = null,
    val lastSyncTime: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)