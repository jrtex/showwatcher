package com.example.showwatcher.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ShowWithProgress(
    @Embedded val show: ShowEntity,
    val totalCount: Int,
    val watchedCount: Int,
)

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows WHERE id = :showId")
    suspend fun getById(showId: Long): ShowEntity?

    @Query("SELECT * FROM shows WHERE id = :showId")
    fun observeById(showId: Long): Flow<ShowEntity?>

    @Query("SELECT * FROM shows WHERE tmdbId = :tmdbId")
    suspend fun getByTmdbId(tmdbId: Long): ShowEntity?

    @Query("SELECT * FROM shows WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<ShowEntity>>

    @Query(
        """
        SELECT s.*,
        (SELECT COUNT(*) FROM episodes e WHERE e.showId = s.id AND e.seasonNumber = s.currentSeason) AS totalCount,
        (SELECT COUNT(*) FROM episodes e WHERE e.showId = s.id AND e.seasonNumber = s.currentSeason AND e.watched = 1) AS watchedCount
        FROM shows s WHERE s.status = :status ORDER BY s.updatedAt DESC
        """,
    )
    fun observeByStatusWithProgress(status: String): Flow<List<ShowWithProgress>>

    @Insert
    suspend fun insert(show: ShowEntity): Long

    @Query(
        "UPDATE shows SET currentSeason = :currentSeason, totalSeasons = :totalSeasons, " +
            "updatedAt = :updatedAt WHERE id = :showId",
    )
    suspend fun advanceSeason(showId: Long, currentSeason: Int, totalSeasons: Int, updatedAt: Long)

    @Query(
        "UPDATE shows SET status = :status, archivedAt = :archivedAt, updatedAt = :updatedAt " +
            "WHERE id = :showId",
    )
    suspend fun archive(showId: Long, status: String = ShowStatus.ARCHIVED, archivedAt: Long, updatedAt: Long)

    @Query(
        "UPDATE shows SET status = :status, archivedAt = NULL, updatedAt = :updatedAt " +
            "WHERE id = :showId",
    )
    suspend fun reactivate(showId: Long, status: String = ShowStatus.ACTIVE, updatedAt: Long)

    @Query("DELETE FROM shows WHERE id = :showId")
    suspend fun deleteById(showId: Long)
}
