package com.trackeco.trackeco.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "hotspots")
data class HotspotEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val intensity: Float,
    val title: String,
    val description: String,
    val categoryType: String,
    val estimatedItems: Int = 0,
    val radius: Float = 100f,
    val isActive: Boolean = true,
    val expiresAt: LocalDateTime? = null,
    val lastUpdated: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)