package com.trackeco.trackeco.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "LocationManager"
    }

    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services not enabled")
            return null
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000L // 5 seconds
                ).apply {
                    setMaxUpdates(1)
                    setIntervalMillis(5000L)
                    setMinUpdateIntervalMillis(2000L)
                }.build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { location ->
                            Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                            continuation.resume(location)
                        } ?: run {
                            Log.w(TAG, "Location result was null")
                            continuation.resume(null)
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

                try {
                    // First try to get last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null && isLocationRecent(location)) {
                            Log.d(TAG, "Using cached location: ${location.latitude}, ${location.longitude}")
                            continuation.resume(location)
                        } else {
                            // Request fresh location
                            Log.d(TAG, "Requesting fresh location")
                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                android.os.Looper.getMainLooper()
                            )
                        }
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to get last location", exception)
                        // Still try to request fresh location
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            android.os.Looper.getMainLooper()
                        )
                    }

                    // Set up timeout
                    continuation.invokeOnCancellation {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                    }

                } catch (securityException: SecurityException) {
                    Log.e(TAG, "Security exception getting location", securityException)
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            null
        }
    }

    suspend fun getLocationWithFallback(): LocationData? {
        val location = getCurrentLocation()
        return location?.let {
            LocationData(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                timestamp = it.time
            )
        } ?: run {
            // Fallback to approximate location based on network
            Log.w(TAG, "Using fallback location")
            LocationData(
                latitude = -6.2088, // Jakarta default (can be changed)
                longitude = 106.8456,
                accuracy = 10000f, // Very low accuracy indicator
                timestamp = System.currentTimeMillis(),
                isFallback = true
            )
        }
    }

    private fun isLocationRecent(location: Location, maxAgeMs: Long = 60000): Boolean {
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < maxAgeMs
    }

    fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadiusKm * c * 1000 // Return in meters
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val timestamp: Long,
        val isFallback: Boolean = false
    )
}