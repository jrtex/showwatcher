package com.example.showwatcher.di

import android.content.Context
import androidx.room.Room
import com.example.showwatcher.data.local.EpisodeDao
import com.example.showwatcher.data.local.SeasonCacheDao
import com.example.showwatcher.data.local.ShowDao
import com.example.showwatcher.data.local.ShowWatcherDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): ShowWatcherDatabase =
        Room.databaseBuilder(context, ShowWatcherDatabase::class.java, "showwatcher.db").build()

    @Provides
    fun provideShowDao(database: ShowWatcherDatabase): ShowDao = database.showDao()

    @Provides
    fun provideEpisodeDao(database: ShowWatcherDatabase): EpisodeDao = database.episodeDao()

    @Provides
    fun provideSeasonCacheDao(database: ShowWatcherDatabase): SeasonCacheDao = database.seasonCacheDao()
}
