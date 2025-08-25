package com.trackeco.trackeco.data.models

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

// Request/Response models for API communication

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("user_id") val userId: String,
    val username: String,
    val email: String?,
    val message: String,
    val success: Boolean = true,
    val token: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null
)

data class UserData(
    @SerializedName("user_id") val userId: String,
    val username: String? = null,
    val xp: Int = 0,
    val points: Int = 0,
    val streak: Int = 0,
    @SerializedName("eco_rank") val ecoRank: String = "Eco Novice",
    @SerializedName("has_completed_first_disposal") val hasCompletedFirstDisposal: Boolean = false,
    @SerializedName("member_since") val memberSince: String? = null,
    @SerializedName("community_rank") val communityRank: String = "#1 globally",
    @SerializedName("total_users") val totalUsers: Int = 1,
    @SerializedName("active_users") val activeUsers: Int = 1,
    @SerializedName("level_info") val levelInfo: LevelInfo? = null,
    @SerializedName("daily_challenge") val dailyChallenge: DailyChallenge? = null
)

data class LevelInfo(
    @SerializedName("current_level") val currentLevel: Int = 1,
    @SerializedName("current_level_name") val currentLevelName: String = "Eco Newcomer",
    @SerializedName("current_level_color") val currentLevelColor: String = "#8B5CF6",
    @SerializedName("next_level_name") val nextLevelName: String = "Green Starter",
    @SerializedName("xp_needed_for_next") val xpNeededForNext: Int = 50,
    @SerializedName("progress_percentage") val progressPercentage: Float = 0f,
    @SerializedName("current_xp") val currentXp: Int = 0,
    @SerializedName("next_level_total_xp") val nextLevelTotalXp: Int = 50,
    @SerializedName("xp_into_current_level") val xpIntoCurrentLevel: Int = 0,
    @SerializedName("xp_remaining") val xpRemaining: Int = 50,
    @SerializedName("current_level_base_xp") val currentLevelBaseXp: Int = 0,
    @SerializedName("current_level_reward") val currentLevelReward: String? = null,
    @SerializedName("is_max_level") val isMaxLevel: Boolean = false
)

data class DailyChallenge(
    val description: String,
    val goal: Int = 1,
    val progress: Int = 0,
    val reward: Int = 50,
    @SerializedName("challenge_id") val challengeId: String? = null,
    @SerializedName("challenge_type") val challengeType: String = "disposal",
    @SerializedName("target_category") val targetCategory: String? = null,
    @SerializedName("is_completed") val isCompleted: Boolean = false,
    @SerializedName("expires_at") val expiresAt: String? = null
)

data class WasteRecord(
    val id: String? = null,
    @SerializedName("user_id") val userId: String,
    val category: String,
    val subtype: String,
    val quantity: Int = 1,
    @SerializedName("image_url") val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("location_name") val locationName: String? = null,
    @SerializedName("points_earned") val pointsEarned: Int = 10,
    @SerializedName("xp_earned") val xpEarned: Int = 10,
    @SerializedName("ai_validation") val aiValidation: String? = null,
    @SerializedName("confidence_score") val confidenceScore: Float = 0f,
    @SerializedName("is_validated") val isValidated: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class WasteRecordResponse(
    val success: Boolean,
    val message: String,
    val record: WasteRecord? = null,
    @SerializedName("points_awarded") val pointsAwarded: Int = 0,
    @SerializedName("xp_awarded") val xpAwarded: Int = 0,
    @SerializedName("level_up") val levelUp: Boolean = false,
    @SerializedName("challenge_progress") val challengeProgress: DailyChallenge? = null
)

data class Hotspot(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val intensity: Float,
    val title: String,
    val description: String,
    @SerializedName("category_type") val categoryType: String = "mixed",
    @SerializedName("estimated_items") val estimatedItems: Int = 0,
    val radius: Float = 100f,
    @SerializedName("expires_at") val expiresAt: String? = null
)

data class DiscoveredCategory(
    val category: String,
    val subtype: String,
    @SerializedName("is_discovered") val isDiscovered: Boolean = true,
    @SerializedName("discovered_at") val discoveredAt: String? = null,
    @SerializedName("disposal_count") val disposalCount: Int = 1
)

data class AIValidationRequest(
    @SerializedName("image_data") val imageData: String,
    val category: String,
    val subtype: String
)

data class AIValidationResponse(
    val success: Boolean,
    @SerializedName("is_valid") val isValid: Boolean,
    val confidence: Float,
    val message: String,
    @SerializedName("detected_category") val detectedCategory: String? = null,
    @SerializedName("detected_subtype") val detectedSubtype: String? = null
)

data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String = "",
    val data: T? = null,
    val error: String? = null
)