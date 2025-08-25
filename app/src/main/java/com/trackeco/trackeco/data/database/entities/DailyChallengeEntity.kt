package com.trackeco.trackeco.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey val challengeId: String,
    val userId: String,
    val description: String,
    val goal: Int,
    val currentProgress: Int = 0,
    val reward: Int = 50,
    val challengeType: String = "disposal",
    val targetCategory: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)