package com.trackeco.trackeco.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- V2 DATA CLASSES ---

// For sending login/registration data
data class AuthRequest(val email: String, val password: String)
data class RegisterResponse(val success: Boolean, val user_id: String)

// For the main user data object, now includes the daily challenge
data class DailyChallenge(val description: String, val progress: Int, val goal: Int, val reward: Int)
data class UserData(
    val user_id: String,
    val xp: Int,
    val points: Int,
    val streak: Int,
    val eco_rank: String,
    val has_completed_first_disposal: Boolean,
    val daily_challenge: DailyChallenge
)

// For sending the video and location data
data class DisposalRequest(
    val user_id: String,
    val latitude: Double,
    val longitude: Double,
    val video: String // Base64 encoded video string
)

// For the rich response after a disposal
data class DisposalResult(
    val success: Boolean,
    val points_earned: Int,
    val xp_earned: Int,
    val waste_category: String?,
    val waste_sub_type: String?,
    val bonuses_awarded: List<String>?,
    val challenges_completed: List<String>?,
    val reason_code: String,
    val message: String,
    val new_total_points: Int?,
    val new_total_xp: Int?,
    val new_streak: Int?,
    val eco_rank: String?
)

// --- V2 API INTERFACE ---

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: AuthRequest): UserData

    @POST("/api/register")
    suspend fun register(@Body request: AuthRequest): RegisterResponse

    @GET("/api/user/{userId}")
    suspend fun getUserData(@Path("userId") userId: String): UserData

    @POST("/api/verify_disposal")
    suspend fun verifyDisposal(@Body request: DisposalRequest): DisposalResult
}

// --- RETROFIT CLIENT (No changes needed, but included for completeness) ---

object RetrofitClient {
    // IMPORTANT: Replace with your actual computer IP or public server URL
    private const val BASE_URL = "http://157.66.55.198:5000/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}