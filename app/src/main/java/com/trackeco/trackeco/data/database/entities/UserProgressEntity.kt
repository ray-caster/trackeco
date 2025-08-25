package com.trackeco.trackeco.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val totalDisposed: Int = 0,
    val firstDiscoveredAt: LocalDateTime? = null,
    val lastDisposedAt: LocalDateTime? = null,
    val streakCount: Int = 0,
    val bestStreak: Int = 0,
    val totalPoints: Int = 0,
    val totalXp: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)