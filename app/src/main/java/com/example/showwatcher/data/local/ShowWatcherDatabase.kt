package com.example.showwatcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ShowEntity::class, EpisodeEntity::class, SeasonCacheMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ShowWatcherDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun seasonCacheDao(): SeasonCacheDao
}
