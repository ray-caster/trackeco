package com.trackeco.trackeco.utils

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val secureTokenManager: SecureTokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip authentication for login/register endpoints
        val url = originalRequest.url.toString()
        if (url.contains("/api/login") || url.contains("/api/register")) {
            return chain.proceed(originalRequest)
        }
        
        // Add JWT token to other requests
        val token = secureTokenManager.getAuthToken()
        return if (token != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}