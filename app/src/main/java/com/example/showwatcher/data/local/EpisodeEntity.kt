package com.example.showwatcher.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["showId", "seasonNumber", "episodeNumber"], unique = true),
        Index(value = ["showId"]),
    ],
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val showId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val tmdbEpisodeId: Long?,
    val name: String,
    val airDate: String?,
    val watched: Boolean = false,
    val watchedAt: Long? = null,
)
