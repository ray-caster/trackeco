package com.trackeco.trackeco.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.*
import com.trackeco.trackeco.repository.WasteRecordRepository
import com.trackeco.trackeco.repository.UserRepository
import com.trackeco.trackeco.repository.HotspotRepository
import com.trackeco.trackeco.repository.DailyChallengeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSyncManager @Inject constructor(
    private val context: Context,
    private val wasteRecordRepository: WasteRecordRepository,
    private val userRepository: UserRepository,
    private val hotspotRepository: HotspotRepository,
    private val dailyChallengeRepository: DailyChallengeRepository
) {
    companion object {
        private const val TAG = "OfflineSyncManager"
        private const val SYNC_WORK_NAME = "trackeco_sync_work"
    }

    private val workManager = WorkManager.getInstance(context)
    private val syncScope = CoroutineScope(Dispatchers.IO)

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Sync every 15 minutes when online
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )

        Log.d(TAG, "Periodic sync scheduled")
    }

    fun performImmediateSync() {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "Network not available, skipping immediate sync")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
        Log.d(TAG, "Immediate sync requested")
    }

    suspend fun syncAllData(): Result<SyncResult> {
        if (!isNetworkAvailable()) {
            return Result.failure(Exception("Network not available"))
        }

        return try {
            val currentUserId = userRepository.getCurrentUserId()
            if (currentUserId == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            var syncedRecords = 0
            var errors = 0

            // 1. Sync unsynced waste records
            try {
                val recordsResult = wasteRecordRepository.syncUnsyncedRecords()
                if (recordsResult.isSuccess) {
                    syncedRecords += recordsResult.getOrDefault(0)
                } else {
                    errors++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing waste records", e)
                errors++
            }

            // 2. Fetch fresh user data
            try {
                val userResult = userRepository.fetchAndSyncUserData(currentUserId)
                if (userResult.isFailure) {
                    errors++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user data", e)
                errors++
            }

            // 3. Fetch fresh hotspots
            try {
                val hotspotsResult = hotspotRepository.fetchAndSyncHotspots()
                if (hotspotsResult.isFailure) {
                    errors++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing hotspots", e)
                errors++
            }

            // 4. Fetch fresh daily challenge
            try {
                val challengeResult = dailyChallengeRepository.fetchAndSyncDailyChallenge(currentUserId)
                if (challengeResult.isFailure) {
                    errors++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing daily challenge", e)
                errors++
            }

            // 5. Cleanup old data
            try {
                hotspotRepository.cleanupExpiredHotspots()
                dailyChallengeRepository.cleanupExpiredChallenges()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
                errors++
            }

            val result = SyncResult(
                syncedRecords = syncedRecords,
                errors = errors,
                timestamp = System.currentTimeMillis()
            )

            Log.d(TAG, "Sync completed: $result")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        Log.d(TAG, "Periodic sync cancelled")
    }

    data class SyncResult(
        val syncedRecords: Int,
        val errors: Int,
        val timestamp: Long
    )
}

// Worker class for background sync
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineSyncManager: OfflineSyncManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background sync")
        
        return try {
            val syncResult = offlineSyncManager.syncAllData()
            if (syncResult.isSuccess) {
                Log.d(TAG, "Background sync completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "Background sync failed: ${syncResult.exceptionOrNull()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background sync error", e)
            Result.retry()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(context: Context, workerParams: WorkerParameters): SyncWorker
    }
}

// Factory for Hilt integration with WorkManager
@AssistedInject
class ChildWorkerFactory @Assisted private val syncWorkerFactory: SyncWorker.Factory : ChildWorkerFactory {
    override fun create(appContext: Context, workerParameters: WorkerParameters): ListenableWorker {
        return syncWorkerFactory.create(appContext, workerParameters)
    }
}