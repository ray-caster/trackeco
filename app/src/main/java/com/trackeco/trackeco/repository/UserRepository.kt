package com.trackeco.trackeco.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.trackeco.trackeco.data.database.dao.UserDao
import com.trackeco.trackeco.data.database.entities.UserEntity
import com.trackeco.trackeco.data.models.*
import com.trackeco.trackeco.network.TrackEcoApiService
import com.trackeco.trackeco.utils.SecureTokenManager
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: TrackEcoApiService,
    private val secureTokenManager: SecureTokenManager
) {
    companion object {
        private const val TAG = "UserRepository"
    }

    // Authentication
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Store JWT tokens securely
                authResponse.token?.let { token ->
                    val payload = secureTokenManager.parseJwtPayload(token)
                    val expiryTime = payload?.exp?.let { it * 1000 } ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                    
                    secureTokenManager.saveAuthTokens(
                        authToken = token,
                        refreshToken = authResponse.refreshToken,
                        expiryTimeMillis = expiryTime,
                        userId = authResponse.userId
                    )
                }
                
                // Store user locally
                val userEntity = UserEntity(
                    userId = authResponse.userId,
                    username = authResponse.username,
                    email = authResponse.email,
                    isAuthenticated = true,
                    authToken = "secured", // Indicate tokens are stored securely
                    lastSyncTime = LocalDateTime.now()
                )
                userDao.insertUser(userEntity)
                
                // Fetch complete user data
                fetchAndSyncUserData(authResponse.userId)
                
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Store JWT tokens securely
                authResponse.token?.let { token ->
                    val payload = secureTokenManager.parseJwtPayload(token)
                    val expiryTime = payload?.exp?.let { it * 1000 } ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                    
                    secureTokenManager.saveAuthTokens(
                        authToken = token,
                        refreshToken = authResponse.refreshToken,
                        expiryTimeMillis = expiryTime,
                        userId = authResponse.userId
                    )
                }
                
                // Store user locally
                val userEntity = UserEntity(
                    userId = authResponse.userId,
                    username = authResponse.username,
                    email = authResponse.email,
                    isAuthenticated = true,
                    authToken = "secured", // Indicate tokens are stored securely
                    lastSyncTime = LocalDateTime.now()
                )
                userDao.insertUser(userEntity)
                
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            // Try to logout from server
            try {
                apiService.logout()
            } catch (e: Exception) {
                Log.w(TAG, "Server logout failed, proceeding with local logout", e)
            }
            
            // Clear local authentication and secure tokens
            secureTokenManager.clearTokens()
            userDao.logoutAllUsers()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
            Result.failure(e)
        }
    }

    // User Data Management
    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getCurrentUser()
    }

    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow()
    }

    suspend fun fetchAndSyncUserData(userId: String): Result<UserData> {
        return try {
            val response = apiService.getUserById(userId)
            if (response.isSuccessful && response.body() != null) {
                val userData = response.body()!!
                
                // Update local user entity with fresh data
                val existingUser = userDao.getUserById(userId)
                val updatedUser = existingUser?.copy(
                    points = userData.points,
                    xp = userData.xp,
                    streak = userData.streak,
                    ecoRank = userData.ecoRank,
                    hasCompletedFirstDisposal = userData.hasCompletedFirstDisposal,
                    memberSince = userData.memberSince,
                    communityRank = userData.communityRank,
                    totalUsers = userData.totalUsers,
                    activeUsers = userData.activeUsers,
                    currentLevel = userData.levelInfo?.currentLevel ?: 1,
                    currentLevelName = userData.levelInfo?.currentLevelName ?: "Eco Newcomer",
                    currentLevelColor = userData.levelInfo?.currentLevelColor ?: "#8B5CF6",
                    nextLevelName = userData.levelInfo?.nextLevelName ?: "Green Starter",
                    xpNeededForNext = userData.levelInfo?.xpNeededForNext ?: 50,
                    progressPercentage = userData.levelInfo?.progressPercentage ?: 0f,
                    lastSyncTime = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ) ?: UserEntity(
                    userId = userData.userId,
                    username = userData.username ?: "Unknown",
                    email = null,
                    points = userData.points,
                    xp = userData.xp,
                    streak = userData.streak,
                    ecoRank = userData.ecoRank,
                    hasCompletedFirstDisposal = userData.hasCompletedFirstDisposal,
                    memberSince = userData.memberSince,
                    communityRank = userData.communityRank,
                    totalUsers = userData.totalUsers,
                    activeUsers = userData.activeUsers,
                    currentLevel = userData.levelInfo?.currentLevel ?: 1,
                    currentLevelName = userData.levelInfo?.currentLevelName ?: "Eco Newcomer",
                    currentLevelColor = userData.levelInfo?.currentLevelColor ?: "#8B5CF6",
                    nextLevelName = userData.levelInfo?.nextLevelName ?: "Green Starter",
                    xpNeededForNext = userData.levelInfo?.xpNeededForNext ?: 50,
                    progressPercentage = userData.levelInfo?.progressPercentage ?: 0f,
                    isAuthenticated = true,
                    authToken = "authenticated",
                    lastSyncTime = LocalDateTime.now()
                )
                
                userDao.insertUser(updatedUser)
                Result.success(userData)
            } else {
                Result.failure(Exception("Failed to fetch user data: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user data", e)
            // Return cached data if network fails
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val userData = UserData(
                    userId = cachedUser.userId,
                    username = cachedUser.username,
                    xp = cachedUser.xp,
                    points = cachedUser.points,
                    streak = cachedUser.streak,
                    ecoRank = cachedUser.ecoRank,
                    hasCompletedFirstDisposal = cachedUser.hasCompletedFirstDisposal,
                    memberSince = cachedUser.memberSince,
                    communityRank = cachedUser.communityRank,
                    totalUsers = cachedUser.totalUsers,
                    activeUsers = cachedUser.activeUsers,
                    levelInfo = LevelInfo(
                        currentLevel = cachedUser.currentLevel,
                        currentLevelName = cachedUser.currentLevelName,
                        currentLevelColor = cachedUser.currentLevelColor,
                        nextLevelName = cachedUser.nextLevelName,
                        xpNeededForNext = cachedUser.xpNeededForNext,
                        progressPercentage = cachedUser.progressPercentage
                    )
                )
                Result.success(userData)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateUserStats(userId: String, points: Int, xp: Int, streak: Int) {
        userDao.updateUserStats(userId, points, xp, streak)
    }

    // Check authentication state
    suspend fun isUserAuthenticated(): Boolean {
        val localUser = getCurrentUser()
        val hasValidToken = secureTokenManager.isTokenValid()
        return localUser?.isAuthenticated == true && hasValidToken
    }

    // Get user ID for authenticated user
    suspend fun getCurrentUserId(): String? {
        return getCurrentUser()?.userId
    }
}