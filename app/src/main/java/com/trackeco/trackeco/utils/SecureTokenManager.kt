package com.trackeco.trackeco.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "SecureTokenManager"
        private const val PREFS_NAME = "trackeco_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAuthTokens(
        authToken: String,
        refreshToken: String? = null,
        expiryTimeMillis: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours default
        userId: String? = null
    ) {
        try {
            encryptedPrefs.edit().apply {
                putString(KEY_AUTH_TOKEN, authToken)
                refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
                putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)
                userId?.let { putString(KEY_USER_ID, it) }
                apply()
            }
            Log.d(TAG, "Auth tokens saved securely")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving auth tokens", e)
        }
    }

    fun getAuthToken(): String? {
        return try {
            val token = encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
            val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
            
            if (token != null && System.currentTimeMillis() < expiry) {
                token
            } else {
                if (token != null) {
                    Log.w(TAG, "Auth token expired")
                    clearTokens()
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving auth token", e)
            null
        }
    }

    fun getRefreshToken(): String? {
        return try {
            encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving refresh token", e)
            null
        }
    }

    fun getUserId(): String? {
        return try {
            encryptedPrefs.getString(KEY_USER_ID, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving user ID", e)
            null
        }
    }

    fun isTokenValid(): Boolean {
        return try {
            val token = encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
            val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
            token != null && System.currentTimeMillis() < expiry
        } catch (e: Exception) {
            Log.e(TAG, "Error checking token validity", e)
            false
        }
    }

    fun clearTokens() {
        try {
            encryptedPrefs.edit().apply {
                remove(KEY_AUTH_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_TOKEN_EXPIRY)
                remove(KEY_USER_ID)
                apply()
            }
            Log.d(TAG, "Auth tokens cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing tokens", e)
        }
    }

    // JWT token parsing utility
    fun parseJwtPayload(token: String): JwtPayload? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            gson.fromJson(payload, JwtPayload::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JWT token", e)
            null
        }
    }

    data class JwtPayload(
        val sub: String?, // User ID
        val exp: Long?,   // Expiration time
        val iat: Long?,   // Issued at
        val username: String?,
        val email: String?
    )
}