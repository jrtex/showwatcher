package com.example.showwatcher.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "season_cache_meta",
    primaryKeys = ["showId", "seasonNumber"],
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["showId"])],
)
data class SeasonCacheMetaEntity(
    val showId: Long,
    val seasonNumber: Int,
    val fetchedAt: Long,
    val episodeCount: Int,
)
