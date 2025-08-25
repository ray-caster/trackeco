package com.trackeco.trackeco.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import android.net.Uri
import com.trackeco.trackeco.data.database.dao.WasteRecordDao
import com.trackeco.trackeco.data.database.entities.WasteRecordEntity
import com.trackeco.trackeco.data.models.*
import com.trackeco.trackeco.network.TrackEcoApiService
import com.trackeco.trackeco.utils.AIValidationManager
import com.trackeco.trackeco.utils.AntiCheatManager
import com.trackeco.trackeco.utils.GameificationManager
import com.trackeco.trackeco.utils.LocationManager
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WasteRecordRepository @Inject constructor(
    private val wasteRecordDao: WasteRecordDao,
    private val apiService: TrackEcoApiService,
    private val aiValidationManager: AIValidationManager,
    private val antiCheatManager: AntiCheatManager,
    private val gamificationManager: GameificationManager,
    private val locationManager: LocationManager
) {
    companion object {
        private const val TAG = "WasteRecordRepository"
    }

    // Get waste records for user
    fun getWasteRecordsFlow(userId: String): Flow<List<WasteRecordEntity>> {
        return wasteRecordDao.getWasteRecordsByUserFlow(userId)
    }

    suspend fun getWasteRecords(userId: String): List<WasteRecordEntity> {
        return wasteRecordDao.getWasteRecordsByUser(userId)
    }

    // Enhanced waste record submission with anti-cheat and AI validation
    suspend fun submitWasteRecord(
        userId: String,
        category: String,
        subtype: String,
        quantity: Int = 1,
        imageUri: Uri? = null,
        localImagePath: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ): Result<EnhancedWasteRecordResponse> {
        return try {
            val recordId = UUID.randomUUID().toString()
            val now = LocalDateTime.now()
            
            // 1. Get current location for anti-cheat validation
            val currentLocation = locationManager.getCurrentLocation()
            
            // 2. Anti-cheat validation
            val antiCheatResult = antiCheatManager.validateDisposal(category, subtype, currentLocation)
            if (!antiCheatResult.isValid) {
                return Result.failure(Exception(antiCheatResult.message))
            }
            
            // 3. AI validation if image is provided
            var aiValidationResult: AIValidationResponse? = null
            if (imageUri != null) {
                val validationResult = aiValidationManager.validateWasteDisposal(imageUri, category, subtype)
                if (validationResult.isSuccess) {
                    aiValidationResult = validationResult.getOrNull()
                    // If AI says it's invalid, still allow but reduce points
                    if (aiValidationResult?.isValid == false && aiValidationResult.confidence > 0.7f) {
                        Log.w(TAG, "AI validation failed with high confidence: ${aiValidationResult.message}")
                    }
                }
            }
            
            // 4. Calculate enhanced points and XP based on validation
            val trustMultiplier = if (aiValidationResult?.isValid == true) {
                1.0f + (aiValidationResult.confidence * 0.5f) // Bonus for validated disposal
            } else if (aiValidationResult?.isValid == false && aiValidationResult.confidence > 0.7f) {
                0.5f // Reduce points for likely invalid disposal
            } else {
                antiCheatResult.trustScore // Use anti-cheat trust score
            }
            
            val basePoints = calculatePoints(category, subtype, quantity)
            val finalPoints = (basePoints * trustMultiplier).toInt()
            val finalXp = (calculateXp(category, subtype, quantity) * trustMultiplier).toInt()
            
            // Create local record first
            val localRecord = WasteRecordEntity(
                id = recordId,
                userId = userId,
                category = category,
                subtype = subtype,
                quantity = quantity,
                localImagePath = localImagePath,
                latitude = latitude ?: currentLocation?.latitude,
                longitude = longitude ?: currentLocation?.longitude,
                locationName = locationName,
                pointsEarned = finalPoints,
                xpEarned = finalXp,
                aiValidation = aiValidationResult?.message,
                confidenceScore = aiValidationResult?.confidence ?: 0f,
                isValidated = aiValidationResult?.isValid ?: false,
                createdAt = now,
                isSynced = false
            )
            
            // Save locally first
            wasteRecordDao.insertRecord(localRecord)
            
            // Try to sync to server
            try {
                val wasteRecord = WasteRecord(
                    id = recordId,
                    userId = userId,
                    category = category,
                    subtype = subtype,
                    quantity = quantity,
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName
                )
                
                val response = apiService.submitWasteRecord(wasteRecord)
                if (response.isSuccessful && response.body() != null) {
                    val responseData = response.body()!!
                    
                    // Update local record with server response
                    val updatedRecord = localRecord.copy(
                        imageUrl = responseData.record?.imageUrl,
                        pointsEarned = responseData.pointsAwarded,
                        xpEarned = responseData.xpAwarded,
                        isValidated = responseData.record?.isValidated ?: false,
                        isSynced = true
                    )
                    wasteRecordDao.updateRecord(updatedRecord)
                    
                    // 5. Process gamification after successful server response
                    val wasteRecord = WasteRecord(
                        id = recordId,
                        userId = userId,
                        category = category,
                        subtype = subtype,
                        quantity = quantity,
                        latitude = localRecord.latitude,
                        longitude = localRecord.longitude,
                        pointsEarned = responseData.pointsAwarded,
                        xpEarned = responseData.xpAwarded
                    )
                    val gamificationResult = gamificationManager.processWasteDisposal(wasteRecord)
                    
                    val enhancedResponse = EnhancedWasteRecordResponse(
                        baseResponse = responseData,
                        antiCheatResult = antiCheatResult,
                        aiValidation = aiValidationResult,
                        gamificationResult = gamificationResult
                    )
                    
                    Result.success(enhancedResponse)
                } else {
                    // Server failed, but local record is saved
                    Log.w(TAG, "Server submission failed, saved locally: ${response.message()}")
                    
                    // Still process gamification for offline experience
                    val wasteRecord = WasteRecord(
                        id = recordId,
                        userId = userId,
                        category = category,
                        subtype = subtype,
                        quantity = quantity,
                        latitude = localRecord.latitude,
                        longitude = localRecord.longitude,
                        pointsEarned = localRecord.pointsEarned,
                        xpEarned = localRecord.xpEarned
                    )
                    val gamificationResult = gamificationManager.processWasteDisposal(wasteRecord)
                    
                    val fallbackResponse = WasteRecordResponse(
                        success = true,
                        message = "Saved locally, will sync when online",
                        record = null,
                        pointsAwarded = localRecord.pointsEarned,
                        xpAwarded = localRecord.xpEarned
                    )
                    
                    val enhancedResponse = EnhancedWasteRecordResponse(
                        baseResponse = fallbackResponse,
                        antiCheatResult = antiCheatResult,
                        aiValidation = aiValidationResult,
                        gamificationResult = gamificationResult
                    )
                    
                    Result.success(enhancedResponse)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error, record saved locally", e)
                
                // Still process gamification for offline experience
                val wasteRecord = WasteRecord(
                    id = recordId,
                    userId = userId,
                    category = category,
                    subtype = subtype,
                    quantity = quantity,
                    latitude = localRecord.latitude,
                    longitude = localRecord.longitude,
                    pointsEarned = localRecord.pointsEarned,
                    xpEarned = localRecord.xpEarned
                )
                val gamificationResult = gamificationManager.processWasteDisposal(wasteRecord)
                
                // Network error, but local record is saved
                val fallbackResponse = WasteRecordResponse(
                    success = true,
                    message = "Saved locally, will sync when online",
                    record = null,
                    pointsAwarded = localRecord.pointsEarned,
                    xpAwarded = localRecord.xpEarned
                )
                
                val enhancedResponse = EnhancedWasteRecordResponse(
                    baseResponse = fallbackResponse,
                    antiCheatResult = antiCheatResult,
                    aiValidation = aiValidationResult,
                    gamificationResult = gamificationResult
                )
                
                Result.success(enhancedResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting waste record", e)
            Result.failure(e)
        }
    }

    // Sync unsynced records to server
    suspend fun syncUnsyncedRecords(): Result<Int> {
        return try {
            val unsyncedRecords = wasteRecordDao.getUnsyncedRecords()
            var syncedCount = 0
            
            for (record in unsyncedRecords) {
                try {
                    val wasteRecord = WasteRecord(
                        id = record.id,
                        userId = record.userId,
                        category = record.category,
                        subtype = record.subtype,
                        quantity = record.quantity,
                        latitude = record.latitude,
                        longitude = record.longitude,
                        locationName = record.locationName,
                        createdAt = record.createdAt.toString()
                    )
                    
                    val response = apiService.submitWasteRecord(wasteRecord)
                    if (response.isSuccessful) {
                        wasteRecordDao.markAsSynced(record.id)
                        syncedCount++
                    } else {
                        wasteRecordDao.incrementSyncAttempts(record.id, LocalDateTime.now())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync record ${record.id}", e)
                    wasteRecordDao.incrementSyncAttempts(record.id, LocalDateTime.now())
                }
            }
            
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing records", e)
            Result.failure(e)
        }
    }

    // Fetch records from server and update local database
    suspend fun fetchServerRecords(userId: String): Result<List<WasteRecord>> {
        return try {
            val response = apiService.getUserWasteRecords(userId)
            if (response.isSuccessful && response.body() != null) {
                val serverRecords = response.body()!!
                
                // Convert server records to local entities and update database
                val localEntities = serverRecords.map { serverRecord ->
                    WasteRecordEntity(
                        id = serverRecord.id ?: UUID.randomUUID().toString(),
                        userId = serverRecord.userId,
                        category = serverRecord.category,
                        subtype = serverRecord.subtype,
                        quantity = serverRecord.quantity,
                        imageUrl = serverRecord.imageUrl,
                        latitude = serverRecord.latitude,
                        longitude = serverRecord.longitude,
                        locationName = serverRecord.locationName,
                        pointsEarned = serverRecord.pointsEarned,
                        xpEarned = serverRecord.xpEarned,
                        aiValidation = serverRecord.aiValidation,
                        confidenceScore = serverRecord.confidenceScore,
                        isValidated = serverRecord.isValidated,
                        isSynced = true,
                        createdAt = serverRecord.createdAt?.let { 
                            LocalDateTime.parse(it) 
                        } ?: LocalDateTime.now()
                    )
                }
                
                wasteRecordDao.insertRecords(localEntities)
                Result.success(serverRecords)
            } else {
                Result.failure(Exception("Failed to fetch server records: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching server records", e)
            Result.failure(e)
        }
    }

    // Get discovered categories
    suspend fun getDiscoveredCategories(userId: String): List<String> {
        return wasteRecordDao.getDiscoveredCategories(userId)
    }

    // Enhanced helper functions with rarity and impact considerations
    private fun calculatePoints(category: String, subtype: String, quantity: Int): Int {
        val basePoints = when (category.lowercase()) {
            "plastic" -> 10
            "metal" -> 15
            "glass" -> 12
            "paper" -> 8
            "organic" -> 5
            "electronic" -> 25 // High value for e-waste
            "hazardous" -> 30 // Very high value for proper hazardous disposal
            else -> 10
        }
        
        // Rarity bonus based on subtype
        val rarityMultiplier = when (subtype.lowercase()) {
            "battery", "phone", "laptop" -> 2.0f // Rare e-waste
            "chemical", "paint", "oil" -> 2.5f // Hazardous waste
            "styrofoam", "bubble_wrap" -> 1.5f // Problematic plastics
            else -> 1.0f
        }
        
        return (basePoints * quantity * rarityMultiplier).toInt()
    }

    private fun calculateXp(category: String, subtype: String, quantity: Int): Int {
        // XP matches points with potential learning bonus
        val baseXp = calculatePoints(category, subtype, quantity)
        
        // Learning bonus for educational categories
        val learningBonus = when (category.lowercase()) {
            "electronic", "hazardous" -> 0.2f // 20% bonus for complex disposal
            else -> 0f
        }
        
        return (baseXp * (1 + learningBonus)).toInt()
    }
    
    // Crisis metrics calculation
    suspend fun getCrisisStats(userId: String): GameificationManager.CrisisStats {
        return gamificationManager.getCrisisStats()
    }
    
    // Get user's skill levels
    suspend fun getSkillLevels(): Map<String, Int> {
        return gamificationManager.getSkillLevels()
    }

    // Delete record
    suspend fun deleteRecord(recordId: String): Result<Unit> {
        return try {
            val record = wasteRecordDao.getRecordById(recordId)
            if (record != null) {
                // Try to delete from server if synced
                if (record.isSynced) {
                    try {
                        apiService.deleteWasteRecord(recordId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete from server, deleting locally", e)
                    }
                }
                
                wasteRecordDao.deleteRecord(record)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Record not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting record", e)
            Result.failure(e)
        }
    }
    
    // Enhanced response wrapper
    data class EnhancedWasteRecordResponse(
        val baseResponse: WasteRecordResponse,
        val antiCheatResult: AntiCheatManager.ValidationResult,
        val aiValidation: AIValidationResponse?,
        val gamificationResult: GameificationManager.GameificationResult
    )
}