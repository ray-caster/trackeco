package com.trackeco.trackeco.utils

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AntiCheatManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager
) {
    companion object {
        private const val TAG = "AntiCheatManager"
        private const val GPS_COOLDOWN_MINUTES = 5
        private const val MIN_DISTANCE_METERS = 10.0
        private const val MAX_DAILY_SUBTYPES = 3
        private const val SUSPICIOUS_SPEED_THRESHOLD_KMH = 100.0
        
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anti_cheat_prefs")
        
        private val LAST_DISPOSAL_TIME = longPreferencesKey("last_disposal_time")
        private val LAST_DISPOSAL_LAT = doublePreferencesKey("last_disposal_lat")
        private val LAST_DISPOSAL_LNG = doublePreferencesKey("last_disposal_lng")
        private val DAILY_SUBTYPES_KEY = stringPreferencesKey("daily_subtypes")
        private val DAILY_DISPOSAL_COUNT = intPreferencesKey("daily_disposal_count")
        private val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
    }

    private val dataStore = context.dataStore

    /**
     * Validates if a waste disposal attempt is legitimate
     */
    suspend fun validateDisposal(
        category: String,
        subtype: String,
        currentLocation: Location?
    ): ValidationResult {
        try {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            
            // Check if we need to reset daily counters
            resetDailyCountersIfNeeded(today)
            
            // 1. GPS Cooldown Check
            val cooldownResult = checkGpsCooldown(now, currentLocation)
            if (!cooldownResult.isValid) {
                return cooldownResult
            }
            
            // 2. Daily Sub-type Limit Check
            val subtypeResult = checkDailySubtypeLimit(subtype, today)
            if (!subtypeResult.isValid) {
                return subtypeResult
            }
            
            // 3. Movement Pattern Analysis
            val movementResult = checkMovementPattern(currentLocation, now)
            if (!movementResult.isValid) {
                return movementResult
            }
            
            // 4. Daily Disposal Frequency Check
            val frequencyResult = checkDailyFrequency()
            if (!frequencyResult.isValid) {
                return frequencyResult
            }
            
            // All checks passed - update tracking data
            updateTrackingData(subtype, currentLocation, now, today)
            
            return ValidationResult(
                isValid = true,
                message = "Disposal validated successfully",
                trustScore = calculateTrustScore(category, subtype)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating disposal", e)
            return ValidationResult(
                isValid = false,
                message = "Validation error occurred",
                reason = ValidationFailureReason.SYSTEM_ERROR
            )
        }
    }

    private suspend fun checkGpsCooldown(
        currentTime: Long,
        currentLocation: Location?
    ): ValidationResult {
        val prefs = dataStore.data.first()
        val lastDisposalTime = prefs[LAST_DISPOSAL_TIME] ?: 0L
        
        val timeSinceLastDisposal = currentTime - lastDisposalTime
        val cooldownMs = GPS_COOLDOWN_MINUTES * 60 * 1000L
        
        if (timeSinceLastDisposal < cooldownMs) {
            val remainingMinutes = (cooldownMs - timeSinceLastDisposal) / 60000
            return ValidationResult(
                isValid = false,
                message = "Please wait $remainingMinutes more minute(s) before next disposal",
                reason = ValidationFailureReason.GPS_COOLDOWN,
                cooldownRemaining = remainingMinutes
            )
        }
        
        // Check minimum distance if location is available
        if (currentLocation != null) {
            val lastLat = prefs[LAST_DISPOSAL_LAT]
            val lastLng = prefs[LAST_DISPOSAL_LNG]
            
            if (lastLat != null && lastLng != null) {
                val distance = locationManager.calculateDistance(
                    lastLat, lastLng,
                    currentLocation.latitude, currentLocation.longitude
                )
                
                if (distance < MIN_DISTANCE_METERS) {
                    return ValidationResult(
                        isValid = false,
                        message = "Please move to a different location for your next disposal",
                        reason = ValidationFailureReason.LOCATION_TOO_CLOSE
                    )
                }
            }
        }
        
        return ValidationResult(isValid = true, message = "GPS cooldown check passed")
    }

    private suspend fun checkDailySubtypeLimit(
        subtype: String,
        today: LocalDate
    ): ValidationResult {
        val prefs = dataStore.data.first()
        val dailySubtypesJson = prefs[DAILY_SUBTYPES_KEY] ?: ""
        
        val dailySubtypes = parseDailySubtypes(dailySubtypesJson)
        val todaySubtypes = dailySubtypes[today.toString()] ?: emptySet()
        
        if (todaySubtypes.contains(subtype)) {
            return ValidationResult(
                isValid = false,
                message = "You've already disposed of this waste type today. Try a different type!",
                reason = ValidationFailureReason.SUBTYPE_ALREADY_USED
            )
        }
        
        if (todaySubtypes.size >= MAX_DAILY_SUBTYPES) {
            return ValidationResult(
                isValid = false,
                message = "Daily waste type limit reached. Come back tomorrow!",
                reason = ValidationFailureReason.DAILY_LIMIT_REACHED
            )
        }
        
        return ValidationResult(isValid = true, message = "Daily subtype check passed")
    }

    private suspend fun checkMovementPattern(
        currentLocation: Location?,
        currentTime: Long
    ): ValidationResult {
        if (currentLocation == null) {
            return ValidationResult(isValid = true, message = "No location data for movement analysis")
        }
        
        val prefs = dataStore.data.first()
        val lastTime = prefs[LAST_DISPOSAL_TIME] ?: 0L
        val lastLat = prefs[LAST_DISPOSAL_LAT]
        val lastLng = prefs[LAST_DISPOSAL_LNG]
        
        if (lastTime > 0 && lastLat != null && lastLng != null) {
            val timeDiffHours = (currentTime - lastTime) / (1000.0 * 60 * 60)
            val distance = locationManager.calculateDistance(
                lastLat, lastLng,
                currentLocation.latitude, currentLocation.longitude
            )
            
            if (timeDiffHours > 0) {
                val speedKmh = (distance / 1000) / timeDiffHours
                
                if (speedKmh > SUSPICIOUS_SPEED_THRESHOLD_KMH) {
                    return ValidationResult(
                        isValid = false,
                        message = "Suspicious movement pattern detected. Please try again later.",
                        reason = ValidationFailureReason.SUSPICIOUS_MOVEMENT
                    )
                }
            }
        }
        
        return ValidationResult(isValid = true, message = "Movement pattern check passed")
    }

    private suspend fun checkDailyFrequency(): ValidationResult {
        val prefs = dataStore.data.first()
        val dailyCount = prefs[DAILY_DISPOSAL_COUNT] ?: 0
        
        // Allow up to 10 disposals per day (reasonable limit)
        if (dailyCount >= 10) {
            return ValidationResult(
                isValid = false,
                message = "Daily disposal limit reached. Take a break and come back tomorrow!",
                reason = ValidationFailureReason.DAILY_LIMIT_REACHED
            )
        }
        
        return ValidationResult(isValid = true, message = "Daily frequency check passed")
    }

    private suspend fun resetDailyCountersIfNeeded(today: LocalDate) {
        val prefs = dataStore.data.first()
        val lastResetDate = prefs[LAST_RESET_DATE]
        
        if (lastResetDate != today.toString()) {
            dataStore.edit { preferences ->
                preferences[DAILY_SUBTYPES_KEY] = ""
                preferences[DAILY_DISPOSAL_COUNT] = 0
                preferences[LAST_RESET_DATE] = today.toString()
            }
        }
    }

    private suspend fun updateTrackingData(
        subtype: String,
        location: Location?,
        currentTime: Long,
        today: LocalDate
    ) {
        dataStore.edit { preferences ->
            preferences[LAST_DISPOSAL_TIME] = currentTime
            
            location?.let {
                preferences[LAST_DISPOSAL_LAT] = it.latitude
                preferences[LAST_DISPOSAL_LNG] = it.longitude
            }
            
            // Update daily subtypes
            val currentSubtypes = parseDailySubtypes(preferences[DAILY_SUBTYPES_KEY] ?: "")
            val todaySubtypes = currentSubtypes[today.toString()]?.toMutableSet() ?: mutableSetOf()
            todaySubtypes.add(subtype)
            
            val updatedSubtypes = currentSubtypes.toMutableMap()
            updatedSubtypes[today.toString()] = todaySubtypes
            
            preferences[DAILY_SUBTYPES_KEY] = serializeDailySubtypes(updatedSubtypes)
            preferences[DAILY_DISPOSAL_COUNT] = (preferences[DAILY_DISPOSAL_COUNT] ?: 0) + 1
        }
    }

    private fun calculateTrustScore(category: String, subtype: String): Float {
        // Basic trust score calculation
        var score = 1.0f
        
        // Bonus for diverse waste types
        when (category.lowercase()) {
            "plastic" -> score += 0.1f
            "paper" -> score += 0.05f
            "glass" -> score += 0.15f
            "metal" -> score += 0.2f
            "organic" -> score += 0.05f
        }
        
        return score.coerceIn(0f, 1f)
    }

    private fun parseDailySubtypes(json: String): Map<String, Set<String>> {
        if (json.isEmpty()) return emptyMap()
        
        return try {
            val result = mutableMapOf<String, Set<String>>()
            val lines = json.split("\n")
            for (line in lines) {
                val parts = line.split(":")
                if (parts.size >= 2) {
                    val date = parts[0]
                    val subtypes = parts[1].split(",").filter { it.isNotEmpty() }.toSet()
                    result[date] = subtypes
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing daily subtypes", e)
            emptyMap()
        }
    }

    private fun serializeDailySubtypes(subtypes: Map<String, Set<String>>): String {
        return subtypes.entries.joinToString("\n") { (date, types) ->
            "$date:${types.joinToString(",")}"
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String,
        val reason: ValidationFailureReason? = null,
        val trustScore: Float = 1.0f,
        val cooldownRemaining: Long = 0L
    )

    enum class ValidationFailureReason {
        GPS_COOLDOWN,
        LOCATION_TOO_CLOSE,
        SUBTYPE_ALREADY_USED,
        DAILY_LIMIT_REACHED,
        SUSPICIOUS_MOVEMENT,
        SYSTEM_ERROR
    }
}