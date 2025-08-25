package com.trackeco.trackeco.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.dao.HotspotDao
import com.trackeco.trackeco.data.database.entities.HotspotEntity
import com.trackeco.trackeco.data.models.Hotspot
import com.trackeco.trackeco.network.TrackEcoApiService
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotspotRepository @Inject constructor(
    private val hotspotDao: HotspotDao,
    private val apiService: TrackEcoApiService
) {
    companion object {
        private const val TAG = "HotspotRepository"
    }

    fun getActiveHotspotsFlow(): Flow<List<HotspotEntity>> {
        return hotspotDao.getActiveHotspotsFlow()
    }

    suspend fun getActiveHotspots(): List<HotspotEntity> {
        return hotspotDao.getActiveHotspots()
    }

    suspend fun fetchAndSyncHotspots(): Result<List<Hotspot>> {
        return try {
            val response = apiService.getHotspots()
            if (response.isSuccessful && response.body() != null) {
                val serverHotspots = response.body()!!
                
                // Convert and save to local database
                val localEntities = serverHotspots.map { serverHotspot ->
                    HotspotEntity(
                        id = serverHotspot.id,
                        latitude = serverHotspot.latitude,
                        longitude = serverHotspot.longitude,
                        intensity = serverHotspot.intensity,
                        title = serverHotspot.title,
                        description = serverHotspot.description,
                        categoryType = serverHotspot.categoryType,
                        estimatedItems = serverHotspot.estimatedItems,
                        radius = serverHotspot.radius,
                        expiresAt = serverHotspot.expiresAt?.let { 
                            LocalDateTime.parse(it) 
                        },
                        lastUpdated = LocalDateTime.now()
                    )
                }
                
                hotspotDao.insertHotspots(localEntities)
                Result.success(serverHotspots)
            } else {
                Result.failure(Exception("Failed to fetch hotspots: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hotspots", e)
            Result.failure(e)
        }
    }

    suspend fun getNearbyHotspots(latitude: Double, longitude: Double, radius: Double = 5000.0): Result<List<Hotspot>> {
        return try {
            val response = apiService.getNearbyHotspots(latitude, longitude, radius)
            if (response.isSuccessful && response.body() != null) {
                val nearbyHotspots = response.body()!!
                
                // Also save to local database
                val localEntities = nearbyHotspots.map { hotspot ->
                    HotspotEntity(
                        id = hotspot.id,
                        latitude = hotspot.latitude,
                        longitude = hotspot.longitude,
                        intensity = hotspot.intensity,
                        title = hotspot.title,
                        description = hotspot.description,
                        categoryType = hotspot.categoryType,
                        estimatedItems = hotspot.estimatedItems,
                        radius = hotspot.radius,
                        expiresAt = hotspot.expiresAt?.let { 
                            LocalDateTime.parse(it) 
                        },
                        lastUpdated = LocalDateTime.now()
                    )
                }
                
                hotspotDao.insertHotspots(localEntities)
                Result.success(nearbyHotspots)
            } else {
                // Fallback to local data
                val localBounds = calculateBounds(latitude, longitude, radius)
                val localHotspots = hotspotDao.getHotspotsInBounds(
                    localBounds.minLat, localBounds.maxLat,
                    localBounds.minLng, localBounds.maxLng
                )
                
                val hotspots = localHotspots.map { entity ->
                    Hotspot(
                        id = entity.id,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        intensity = entity.intensity,
                        title = entity.title,
                        description = entity.description,
                        categoryType = entity.categoryType,
                        estimatedItems = entity.estimatedItems,
                        radius = entity.radius,
                        expiresAt = entity.expiresAt?.toString()
                    )
                }
                Result.success(hotspots)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching nearby hotspots", e)
            // Fallback to local data
            try {
                val localBounds = calculateBounds(latitude, longitude, radius)
                val localHotspots = hotspotDao.getHotspotsInBounds(
                    localBounds.minLat, localBounds.maxLat,
                    localBounds.minLng, localBounds.maxLng
                )
                
                val hotspots = localHotspots.map { entity ->
                    Hotspot(
                        id = entity.id,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        intensity = entity.intensity,
                        title = entity.title,
                        description = entity.description,
                        categoryType = entity.categoryType,
                        estimatedItems = entity.estimatedItems,
                        radius = entity.radius,
                        expiresAt = entity.expiresAt?.toString()
                    )
                }
                Result.success(hotspots)
            } catch (localError: Exception) {
                Result.failure(localError)
            }
        }
    }

    suspend fun cleanupExpiredHotspots() {
        try {
            hotspotDao.deactivateExpiredHotspots()
            hotspotDao.deleteOldHotspots(LocalDateTime.now().minusDays(7))
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired hotspots", e)
        }
    }

    private fun calculateBounds(lat: Double, lng: Double, radiusInMeters: Double): LatLngBounds {
        val radiusInDegrees = radiusInMeters / 111320.0 // Approximate conversion
        return LatLngBounds(
            minLat = lat - radiusInDegrees,
            maxLat = lat + radiusInDegrees,
            minLng = lng - radiusInDegrees,
            maxLng = lng + radiusInDegrees
        )
    }

    private data class LatLngBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    )
}