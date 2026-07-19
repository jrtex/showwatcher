package com.example.showwatcher.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object ShowStatus {
    const val ACTIVE = "active"
    const val ARCHIVED = "archived"
}

@Entity(
    tableName = "shows",
    indices = [Index(value = ["tmdbId"], unique = true)],
)
data class ShowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tmdbId: Long,
    val title: String,
    val posterPath: String?,
    val firstAirYear: Int?,
    val status: String,
    val currentSeason: Int,
    val totalSeasons: Int,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(name = "archivedAt")
    val archivedAt: Long?,
)
