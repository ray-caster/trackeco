package com.trackeco.trackeco.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.trackeco.trackeco.BuildConfig
import com.trackeco.trackeco.data.models.AIValidationRequest
import com.trackeco.trackeco.data.models.AIValidationResponse
import com.trackeco.trackeco.network.TrackEcoApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIValidationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: TrackEcoApiService
) {
    companion object {
        private const val TAG = "AIValidationManager"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
    }

    private val geminiModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY ?: ""
        )
    }

    /**
     * Validates waste disposal using AI
     */
    suspend fun validateWasteDisposal(
        imageUri: Uri,
        declaredCategory: String,
        declaredSubtype: String
    ): Result<AIValidationResponse> {
        return try {
            // First try server-side validation
            val serverResult = tryServerValidation(imageUri, declaredCategory, declaredSubtype)
            if (serverResult.isSuccess) {
                return serverResult
            }

            // Fallback to local Gemini validation
            Log.w(TAG, "Server validation failed, using local Gemini fallback")
            localGeminiValidation(imageUri, declaredCategory, declaredSubtype)
        } catch (e: Exception) {
            Log.e(TAG, "AI validation failed", e)
            Result.failure(e)
        }
    }

    private suspend fun tryServerValidation(
        imageUri: Uri,
        category: String,
        subtype: String
    ): Result<AIValidationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val imageData = uriToBase64(imageUri)
                val request = AIValidationRequest(
                    imageData = imageData,
                    category = category,
                    subtype = subtype
                )
                
                val response = apiService.validateImage(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Server validation failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server validation error", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun localGeminiValidation(
        imageUri: Uri,
        declaredCategory: String,
        declaredSubtype: String
    ): Result<AIValidationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmapFromUri(imageUri)
                
                val prompt = """
                    Analyze this image and determine if it shows waste disposal of the declared type.
                    
                    Declared Category: $declaredCategory
                    Declared Subtype: $declaredSubtype
                    
                    Please assess:
                    1. Is this actually waste/recyclable material?
                    2. Does it match the declared category and subtype?
                    3. Is this a legitimate disposal photo (not a stock photo or unrelated image)?
                    
                    Respond with:
                    - VALID if the image matches the declared waste type
                    - INVALID if it doesn't match or isn't waste
                    - UNCERTAIN if you can't determine clearly
                    
                    Also provide a confidence score (0.0 to 1.0) and brief explanation.
                    
                    Format your response as: STATUS|confidence_score|explanation|detected_category|detected_subtype
                """.trimIndent()

                val response = geminiModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val result = response.text?.let { parseGeminiResponse(it, declaredCategory, declaredSubtype) }
                    ?: AIValidationResponse(
                        success = false,
                        isValid = false,
                        confidence = 0f,
                        message = "Unable to process image"
                    )

                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Local Gemini validation error", e)
                Result.failure(e)
            }
        }
    }

    private fun parseGeminiResponse(
        responseText: String,
        declaredCategory: String,
        declaredSubtype: String
    ): AIValidationResponse {
        return try {
            val parts = responseText.split("|")
            if (parts.size >= 3) {
                val status = parts[0].trim().uppercase()
                val confidence = parts[1].trim().toFloatOrNull() ?: 0f
                val explanation = parts[2].trim()
                val detectedCategory = if (parts.size > 3) parts[3].trim() else null
                val detectedSubtype = if (parts.size > 4) parts[4].trim() else null

                AIValidationResponse(
                    success = true,
                    isValid = status == "VALID",
                    confidence = confidence,
                    message = explanation,
                    detectedCategory = detectedCategory,
                    detectedSubtype = detectedSubtype
                )
            } else {
                // Fallback parsing for simple responses
                val isValid = responseText.contains("VALID", ignoreCase = true) && 
                             !responseText.contains("INVALID", ignoreCase = true)
                AIValidationResponse(
                    success = true,
                    isValid = isValid,
                    confidence = if (isValid) 0.7f else 0.3f,
                    message = responseText.take(100)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response", e)
            AIValidationResponse(
                success = false,
                isValid = false,
                confidence = 0f,
                message = "Error parsing AI response"
            )
        }
    }

    private fun uriToBase64(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = loadBitmapFromStream(inputStream)
            bitmapToBase64(bitmap)
        } ?: throw IllegalArgumentException("Unable to read image from URI")
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            loadBitmapFromStream(inputStream)
        } ?: throw IllegalArgumentException("Unable to load bitmap from URI")
    }

    private fun loadBitmapFromStream(inputStream: InputStream): Bitmap {
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        
        // Resize if too large
        return if (bitmap.byteCount > MAX_IMAGE_SIZE) {
            val scaleFactor = kotlin.math.sqrt(MAX_IMAGE_SIZE.toDouble() / bitmap.byteCount)
            val newWidth = (bitmap.width * scaleFactor).toInt()
            val newHeight = (bitmap.height * scaleFactor).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}