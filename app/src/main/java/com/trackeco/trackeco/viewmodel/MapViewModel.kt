package com.trackeco.trackeco.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.trackeco.trackeco.repository.HotspotRepository
import com.trackeco.trackeco.data.database.entities.HotspotEntity
import com.trackeco.trackeco.utils.LocationManager
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val hotspotRepository: HotspotRepository,
    private val locationManager: LocationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val hotspots: StateFlow<List<HotspotEntity>> = hotspotRepository.getActiveHotspotsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentLocation = MutableStateFlow<LocationManager.LocationData?>(null)
    val currentLocation: StateFlow<LocationManager.LocationData?> = _currentLocation.asStateFlow()

    init {
        getCurrentLocation()
        refreshHotspots()
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLocationLoading = true)
                
                if (!locationManager.hasLocationPermission()) {
                    _uiState.value = _uiState.value.copy(
                        isLocationLoading = false,
                        showLocationPermissionRequest = true
                    )
                    return@launch
                }

                val location = locationManager.getLocationWithFallback()
                _currentLocation.value = location
                _uiState.value = _uiState.value.copy(
                    isLocationLoading = false,
                    hasLocationPermission = true
                )

                // Fetch nearby hotspots based on current location
                location?.let {
                    fetchNearbyHotspots(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLocationLoading = false,
                    errorMessage = "Failed to get location: ${e.message}"
                )
            }
        }
    }

    fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = true,
            showLocationPermissionRequest = false
        )
        getCurrentLocation()
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = false,
            showLocationPermissionRequest = false
        )
        // Use default location
        _currentLocation.value = LocationManager.LocationData(
            latitude = -6.2088, // Jakarta default
            longitude = 106.8456,
            accuracy = 10000f,
            timestamp = System.currentTimeMillis(),
            isFallback = true
        )
    }

    private fun fetchNearbyHotspots(latitude: Double, longitude: Double, radius: Double = 5000.0) {
        viewModelScope.launch {
            try {
                hotspotRepository.getNearbyHotspots(latitude, longitude, radius)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to fetch nearby hotspots: ${e.message}"
                )
            }
        }
    }

    fun refreshHotspots() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
                hotspotRepository.fetchAndSyncHotspots()
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    errorMessage = "Failed to refresh hotspots: ${e.message}"
                )
            }
        }
    }

    fun selectHotspot(hotspot: HotspotEntity) {
        _uiState.value = _uiState.value.copy(selectedHotspot = hotspot)
    }

    fun clearSelectedHotspot() {
        _uiState.value = _uiState.value.copy(selectedHotspot = null)
    }

    fun navigateToHotspot(hotspot: HotspotEntity) {
        // This would typically open navigation app
        _uiState.value = _uiState.value.copy(
            navigationTarget = hotspot,
            showNavigationDialog = true
        )
    }

    fun dismissNavigationDialog() {
        _uiState.value = _uiState.value.copy(
            showNavigationDialog = false,
            navigationTarget = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Filter hotspots by category
    fun filterHotspotsByCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = category)
    }

    // Get filtered hotspots
    fun getFilteredHotspots(): StateFlow<List<HotspotEntity>> {
        return combine(hotspots, uiState) { allHotspots, state ->
            if (state.selectedCategoryFilter.isNullOrEmpty()) {
                allHotspots
            } else {
                allHotspots.filter { it.categoryType == state.selectedCategoryFilter }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getHotspotCategories(): List<String> {
        return listOf("All", "Plastic", "Metal", "Glass", "Paper", "Organic", "Mixed")
    }
}

data class MapUiState(
    val isLocationLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val showLocationPermissionRequest: Boolean = false,
    val selectedHotspot: HotspotEntity? = null,
    val navigationTarget: HotspotEntity? = null,
    val showNavigationDialog: Boolean = false,
    val selectedCategoryFilter: String? = null,
    val errorMessage: String? = null
)