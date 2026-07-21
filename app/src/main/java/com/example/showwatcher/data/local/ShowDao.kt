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

/** Active-status shows whose current season's first episode hasn't aired yet (or has no known air date). */
data class ShowUpcoming(
    @Embedded val show: ShowEntity,
    val firstEpisodeAirDate: String?,
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

    // Air dates are stored as ISO "YYYY-MM-DD" text, so lexicographic comparison against
    // `:today` (also "YYYY-MM-DD") is a correct chronological comparison without needing
    // date parsing. A null/missing first-episode air date counts as not-yet-aired.
    @Query(
        """
        SELECT * FROM (
            SELECT s.*,
            (SELECT COUNT(*) FROM episodes e WHERE e.showId = s.id AND e.seasonNumber = s.currentSeason) AS totalCount,
            (SELECT COUNT(*) FROM episodes e WHERE e.showId = s.id AND e.seasonNumber = s.currentSeason AND e.watched = 1) AS watchedCount,
            (SELECT airDate FROM episodes e WHERE e.showId = s.id AND e.seasonNumber = s.currentSeason
                ORDER BY e.episodeNumber ASC LIMIT 1) AS firstEpisodeAirDate
            FROM shows s WHERE s.status = :status
        )
        WHERE firstEpisodeAirDate IS NOT NULL AND firstEpisodeAirDate <= :today
        ORDER BY updatedAt DESC
        """,
    )
    fun observeActiveWithProgress(today: String, status: String = ShowStatus.ACTIVE): Flow<List<ShowWithProgress>>

    @Query(
        """
        SELECT * FROM (
            SELECT s.*,
            (SELECT airDate FROM episodes e WHERE e.showId = s.id AND e.seasonNumber = s.currentSeason
                ORDER BY e.episodeNumber ASC LIMIT 1) AS firstEpisodeAirDate
            FROM shows s WHERE s.status = :status
        )
        WHERE firstEpisodeAirDate IS NULL OR firstEpisodeAirDate > :today
        ORDER BY updatedAt DESC
        """,
    )
    fun observeUpcoming(today: String, status: String = ShowStatus.ACTIVE): Flow<List<ShowUpcoming>>

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
