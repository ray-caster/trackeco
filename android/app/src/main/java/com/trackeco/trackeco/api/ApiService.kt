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

// --- DATA CLASSES ---

// For sending login credentials to the server
data class LoginRequest(val email: String, val password: String)

// For user information received from the backend
data class UserData(
    val points: Int,
    val streak: Int,
    val eco_rank: String,
    val xp: Int,
    val user_id: String // This field is crucial and must match the JSON key
)

// For the result of a disposal action
data class DisposalResult(
    val success: Boolean,
    val points_earned: Int,
    val xp_earned: Int,
    val reason_string: String
)

// --- API INTERFACE ---

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): UserData

    @GET("/api/user/{userId}")
    suspend fun getUserData(@Path("userId") userId: String): UserData

    @POST("/api/verify_disposal_mock/{userId}")
    suspend fun verifyDisposalMock(@Path("userId") userId: String): DisposalResult
}

// --- RETROFIT CLIENT ---

object RetrofitClient {
    // IMPORTANT: Replace "YOUR_COMPUTER_IP_HERE" with your actual IPv4 address from ipconfig.
    private const val BASE_URL = "http://YOUR_COMPUTER_IP_HERE:5000/"

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