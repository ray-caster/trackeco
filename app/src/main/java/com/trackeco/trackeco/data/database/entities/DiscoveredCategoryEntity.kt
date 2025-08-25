package com.trackeco.trackeco.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "discovered_categories")
data class DiscoveredCategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val subtype: String,
    val isDiscovered: Boolean = true,
    val discoveredAt: LocalDateTime = LocalDateTime.now(),
    val disposalCount: Int = 1,
    val lastDisposedAt: LocalDateTime = LocalDateTime.now(),
    val totalPoints: Int = 10,
    val totalXp: Int = 10
)