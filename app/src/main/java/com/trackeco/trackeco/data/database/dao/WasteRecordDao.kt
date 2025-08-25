package com.trackeco.trackeco.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.entities.WasteRecordEntity
import java.time.LocalDateTime

@Dao
interface WasteRecordDao {
    @Query("SELECT * FROM waste_records WHERE userId = :userId ORDER BY createdAt DESC")
    fun getWasteRecordsByUserFlow(userId: String): Flow<List<WasteRecordEntity>>

    @Query("SELECT * FROM waste_records WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getWasteRecordsByUser(userId: String): List<WasteRecordEntity>

    @Query("SELECT * FROM waste_records WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedRecords(): List<WasteRecordEntity>

    @Query("SELECT * FROM waste_records WHERE id = :recordId")
    suspend fun getRecordById(recordId: String): WasteRecordEntity?

    @Query("SELECT COUNT(*) FROM waste_records WHERE userId = :userId AND DATE(createdAt) = DATE(:date)")
    suspend fun getRecordCountForDate(userId: String, date: LocalDateTime): Int

    @Query("SELECT DISTINCT category FROM waste_records WHERE userId = :userId")
    suspend fun getDiscoveredCategories(userId: String): List<String>

    @Query("SELECT * FROM waste_records WHERE userId = :userId AND category = :category ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestRecordForCategory(userId: String, category: String): WasteRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WasteRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<WasteRecordEntity>)

    @Update
    suspend fun updateRecord(record: WasteRecordEntity)

    @Query("UPDATE waste_records SET isSynced = 1 WHERE id = :recordId")
    suspend fun markAsSynced(recordId: String)

    @Query("UPDATE waste_records SET syncAttempts = syncAttempts + 1, lastSyncAttempt = :attemptTime WHERE id = :recordId")
    suspend fun incrementSyncAttempts(recordId: String, attemptTime: LocalDateTime)

    @Delete
    suspend fun deleteRecord(record: WasteRecordEntity)

    @Query("DELETE FROM waste_records WHERE userId = :userId")
    suspend fun deleteRecordsForUser(userId: String)
}