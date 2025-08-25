package com.trackeco.trackeco.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.entities.HotspotEntity
import java.time.LocalDateTime

@Dao
interface HotspotDao {
    @Query("SELECT * FROM hotspots WHERE isActive = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)")
    fun getActiveHotspotsFlow(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<HotspotEntity>>

    @Query("SELECT * FROM hotspots WHERE isActive = 1 AND (expiresAt IS NULL OR expiresAt > :currentTime)")
    suspend fun getActiveHotspots(currentTime: LocalDateTime = LocalDateTime.now()): List<HotspotEntity>

    @Query("SELECT * FROM hotspots WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng AND isActive = 1")
    suspend fun getHotspotsInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<HotspotEntity>

    @Query("SELECT * FROM hotspots WHERE id = :hotspotId")
    suspend fun getHotspotById(hotspotId: String): HotspotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspot(hotspot: HotspotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspots(hotspots: List<HotspotEntity>)

    @Update
    suspend fun updateHotspot(hotspot: HotspotEntity)

    @Query("UPDATE hotspots SET isActive = 0 WHERE expiresAt < :currentTime")
    suspend fun deactivateExpiredHotspots(currentTime: LocalDateTime = LocalDateTime.now())

    @Delete
    suspend fun deleteHotspot(hotspot: HotspotEntity)

    @Query("DELETE FROM hotspots WHERE isActive = 0 AND expiresAt < :cutoffTime")
    suspend fun deleteOldHotspots(cutoffTime: LocalDateTime)

    @Query("DELETE FROM hotspots")
    suspend fun deleteAllHotspots()
}