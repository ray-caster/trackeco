package com.trackeco.trackeco.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.trackeco.trackeco.data.database.dao.*
import com.trackeco.trackeco.data.database.entities.*

@Database(
    entities = [
        UserEntity::class,
        WasteRecordEntity::class,
        HotspotEntity::class,
        DailyChallengeEntity::class,
        UserProgressEntity::class,
        DiscoveredCategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrackEcoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun wasteRecordDao(): WasteRecordDao
    abstract fun hotspotDao(): HotspotDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun discoveredCategoryDao(): DiscoveredCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: TrackEcoDatabase? = null

        fun getDatabase(context: Context): TrackEcoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackEcoDatabase::class.java,
                    "trackeco_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}