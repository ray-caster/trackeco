package com.trackeco.trackeco.network

import com.trackeco.trackeco.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface TrackEcoApiService {
    
    // Authentication
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("api/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
    
    // User Data
    @GET("api/user")
    suspend fun getCurrentUser(): Response<UserData>
    
    @GET("api/user/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserData>
    
    @PUT("api/user/{userId}")
    suspend fun updateUser(@Path("userId") userId: String, @Body userData: UserData): Response<UserData>
    
    // Waste Records
    @POST("api/waste-record")
    suspend fun submitWasteRecord(@Body record: WasteRecord): Response<WasteRecordResponse>
    
    @GET("api/user/{userId}/records")
    suspend fun getUserWasteRecords(@Path("userId") userId: String): Response<List<WasteRecord>>
    
    @DELETE("api/waste-record/{recordId}")
    suspend fun deleteWasteRecord(@Path("recordId") recordId: String): Response<ApiResponse<Unit>>
    
    // Hotspots
    @GET("api/hotspots")
    suspend fun getHotspots(): Response<List<Hotspot>>
    
    @GET("api/hotspots/nearby")
    suspend fun getNearbyHotspots(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radius: Double = 5000.0
    ): Response<List<Hotspot>>
    
    // Daily Challenges
    @GET("api/user/{userId}/daily-challenge")
    suspend fun getDailyChallenge(@Path("userId") userId: String): Response<DailyChallenge>
    
    @PUT("api/user/{userId}/daily-challenge/progress")
    suspend fun updateChallengeProgress(
        @Path("userId") userId: String,
        @Body progress: Map<String, Int>
    ): Response<DailyChallenge>
    
    // Discovered Categories
    @GET("api/user/{userId}/discovered_categories")
    suspend fun getDiscoveredCategories(@Path("userId") userId: String): Response<List<DiscoveredCategory>>
    
    @POST("api/user/{userId}/discovered_categories")
    suspend fun addDiscoveredCategory(
        @Path("userId") userId: String,
        @Body category: DiscoveredCategory
    ): Response<DiscoveredCategory>
    
    // Progress and Stats
    @GET("api/user/{userId}/progress")
    suspend fun getUserProgress(@Path("userId") userId: String): Response<Map<String, Any>>
    
    @GET("api/user/{userId}/stats")
    suspend fun getUserStats(@Path("userId") userId: String): Response<Map<String, Any>>
    
    // AI Validation
    @POST("api/ai/validate")
    suspend fun validateImage(@Body request: AIValidationRequest): Response<AIValidationResponse>
    
    // Image Upload
    @Multipart
    @POST("api/upload-image")
    suspend fun uploadImage(
        @Part("image") image: okhttp3.MultipartBody.Part,
        @Part("user_id") userId: okhttp3.RequestBody
    ): Response<ApiResponse<String>>
    
    // Sync Operations
    @POST("api/sync/upload")
    suspend fun syncUpload(@Body records: List<WasteRecord>): Response<ApiResponse<List<WasteRecordResponse>>>
    
    @GET("api/sync/download/{userId}")
    suspend fun syncDownload(@Path("userId") userId: String): Response<ApiResponse<Map<String, Any>>>
}