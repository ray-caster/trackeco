package com.trackeco.trackeco.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.trackeco.trackeco.data.database.entities.DiscoveredCategoryEntity

@Dao
interface DiscoveredCategoryDao {
    @Query("SELECT * FROM discovered_categories WHERE userId = :userId ORDER BY discoveredAt DESC")
    fun getDiscoveredCategoriesFlow(userId: String): Flow<List<DiscoveredCategoryEntity>>

    @Query("SELECT * FROM discovered_categories WHERE userId = :userId ORDER BY discoveredAt DESC")
    suspend fun getDiscoveredCategories(userId: String): List<DiscoveredCategoryEntity>

    @Query("SELECT * FROM discovered_categories WHERE userId = :userId AND category = :category AND subtype = :subtype")
    suspend fun getDiscoveredCategory(userId: String, category: String, subtype: String): DiscoveredCategoryEntity?

    @Query("SELECT COUNT(*) FROM discovered_categories WHERE userId = :userId")
    suspend fun getDiscoveredCategoriesCount(userId: String): Int

    @Query("SELECT DISTINCT category FROM discovered_categories WHERE userId = :userId")
    suspend fun getDiscoveredCategoriesList(userId: String): List<String>

    @Query("SELECT * FROM discovered_categories WHERE userId = :userId AND category = :category ORDER BY discoveredAt DESC")
    suspend fun getDiscoveredSubtypesForCategory(userId: String, category: String): List<DiscoveredCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoveredCategory(category: DiscoveredCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoveredCategories(categories: List<DiscoveredCategoryEntity>)

    @Update
    suspend fun updateDiscoveredCategory(category: DiscoveredCategoryEntity)

    @Query("UPDATE discovered_categories SET disposalCount = disposalCount + 1, lastDisposedAt = :disposalTime, totalPoints = totalPoints + :points, totalXp = totalXp + :xp WHERE userId = :userId AND category = :category AND subtype = :subtype")
    suspend fun incrementDisposal(userId: String, category: String, subtype: String, disposalTime: java.time.LocalDateTime, points: Int, xp: Int)

    @Delete
    suspend fun deleteDiscoveredCategory(category: DiscoveredCategoryEntity)

    @Query("DELETE FROM discovered_categories WHERE userId = :userId")
    suspend fun deleteDiscoveredCategoriesForUser(userId: String)
}