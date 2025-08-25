package com.trackeco.trackeco.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "waste_records")
data class WasteRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val subtype: String,
    val quantity: Int = 1,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val pointsEarned: Int = 10,
    val xpEarned: Int = 10,
    val aiValidation: String? = null,
    val confidenceScore: Float = 0f,
    val isValidated: Boolean = false,
    val isSynced: Boolean = false,
    val syncAttempts: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastSyncAttempt: LocalDateTime? = null
)