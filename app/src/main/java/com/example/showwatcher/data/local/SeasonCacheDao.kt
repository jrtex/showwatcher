package com.example.showwatcher.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SeasonCacheDao {
    @Query("SELECT * FROM season_cache_meta WHERE showId = :showId AND seasonNumber = :seasonNumber")
    suspend fun getMeta(showId: Long, seasonNumber: Int): SeasonCacheMetaEntity?

    @Upsert
    suspend fun upsertMeta(meta: SeasonCacheMetaEntity)
}
