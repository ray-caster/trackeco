package com.trackeco.trackeco.di

import android.content.Context
import com.trackeco.trackeco.data.database.TrackEcoDatabase
import com.trackeco.trackeco.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTrackEcoDatabase(@ApplicationContext context: Context): TrackEcoDatabase {
        return TrackEcoDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(database: TrackEcoDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideWasteRecordDao(database: TrackEcoDatabase): WasteRecordDao {
        return database.wasteRecordDao()
    }

    @Provides
    fun provideHotspotDao(database: TrackEcoDatabase): HotspotDao {
        return database.hotspotDao()
    }

    @Provides
    fun provideDailyChallengeDao(database: TrackEcoDatabase): DailyChallengeDao {
        return database.dailyChallengeDao()
    }

    @Provides
    fun provideUserProgressDao(database: TrackEcoDatabase): UserProgressDao {
        return database.userProgressDao()
    }

    @Provides
    fun provideDiscoveredCategoryDao(database: TrackEcoDatabase): DiscoveredCategoryDao {
        return database.discoveredCategoryDao()
    }
}