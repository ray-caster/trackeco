package com.trackeco.trackeco.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.trackeco.trackeco.utils.CameraManager
import com.trackeco.trackeco.utils.LocationManager
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val locationManager: LocationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun setupCamera(onCameraSetup: (Boolean) -> Unit) {
        // This will be called from the composable with lifecycle owner
        _uiState.value = _uiState.value.copy(isCameraReady = false)
    }

    fun onCameraSetupComplete(success: Boolean) {
        _uiState.value = _uiState.value.copy(
            isCameraReady = success,
            errorMessage = if (!success) "Failed to initialize camera" else null
        )
    }

    fun captureImage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCapturing = true)
            
            cameraManager.captureImage { uri, error ->
                if (uri != null) {
                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        capturedImageUri = uri,
                        errorMessage = null
                    )
                    
                    // Get location for the captured image
                    getCurrentLocation()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        errorMessage = error ?: "Failed to capture image"
                    )
                }
            }
        }
    }

    private fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                val locationData = locationManager.getLocationWithFallback()
                _uiState.value = _uiState.value.copy(
                    currentLocation = locationData,
                    locationName = if (locationData?.isFallback == true) 
                        "Approximate location" 
                    else 
                        "Current location"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to get location: ${e.message}"
                )
            }
        }
    }

    fun retakePhoto() {
        _uiState.value = _uiState.value.copy(
            capturedImageUri = null,
            currentLocation = null,
            locationName = null,
            selectedCategory = null,
            selectedSubtype = null,
            errorMessage = null
        )
    }

    fun setSelectedCategory(category: String) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            selectedSubtype = null // Reset subtype when category changes
        )
    }

    fun setSelectedSubtype(subtype: String) {
        _uiState.value = _uiState.value.copy(selectedSubtype = subtype)
    }

    fun confirmSubmission() {
        val state = _uiState.value
        if (state.capturedImageUri != null && 
            state.selectedCategory != null && 
            state.selectedSubtype != null) {
            
            _uiState.value = state.copy(
                isReadyToSubmit = true,
                submissionData = SubmissionData(
                    imageUri = state.capturedImageUri,
                    category = state.selectedCategory,
                    subtype = state.selectedSubtype,
                    location = state.currentLocation,
                    locationName = state.locationName
                )
            )
        }
    }

    fun resetSubmission() {
        _uiState.value = CameraUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Get available waste categories and subtypes
    fun getWasteCategories(): Map<String, List<String>> {
        return mapOf(
            "Plastic" to listOf("Bottles", "Bags", "Containers", "Packaging", "Other"),
            "Metal" to listOf("Cans", "Foil", "Containers", "Other"),
            "Glass" to listOf("Bottles", "Jars", "Containers", "Other"),
            "Paper" to listOf("Newspaper", "Cardboard", "Magazines", "Packaging", "Other"),
            "Organic" to listOf("Food waste", "Garden waste", "Other"),
            "Electronic" to listOf("Batteries", "Small devices", "Cables", "Other"),
            "Textile" to listOf("Clothing", "Fabric", "Other"),
            "Mixed" to listOf("General waste", "Other")
        )
    }
}

data class CameraUiState(
    val isCameraReady: Boolean = false,
    val isCapturing: Boolean = false,
    val capturedImageUri: Uri? = null,
    val currentLocation: LocationManager.LocationData? = null,
    val locationName: String? = null,
    val selectedCategory: String? = null,
    val selectedSubtype: String? = null,
    val isReadyToSubmit: Boolean = false,
    val submissionData: SubmissionData? = null,
    val errorMessage: String? = null
)

data class SubmissionData(
    val imageUri: Uri,
    val category: String,
    val subtype: String,
    val location: LocationManager.LocationData?,
    val locationName: String?
)