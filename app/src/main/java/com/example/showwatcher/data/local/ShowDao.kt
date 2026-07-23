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
    val nextEpisodeAirDate: String?,
)

/** Active-status shows whose current season's first episode hasn't aired yet (or has no known air date). */
data class ShowUpcoming(
    @Embedded val show: ShowEntity,
    val firstEpisodeAirDate: String?,
    val nextEpisodeAirDate: String?,
)

/** A show plus the air date of its earliest unwatched episode across all seasons, for sort purposes. */
data class ShowWithNextEpisode(
    @Embedded val show: ShowEntity,
    val nextEpisodeAirDate: String?,
)

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows WHERE id = :showId")
    suspend fun getById(showId: Long): ShowEntity?

    @Query("SELECT * FROM shows WHERE id = :showId")
    fun observeById(showId: Long): Flow<ShowEntity?>

    @Query("SELECT * FROM shows WHERE tmdbId = :tmdbId")
    suspend fun getByTmdbId(tmdbId: Long): ShowEntity?

    // The nextEpisodeAirDate subquery deliberately ignores currentSeason (unlike firstEpisodeAirDate
    // below) since it's used for "next episode to watch" sorting, which can span past a season
    // boundary if the user hasn't caught up.
    @Query(
        """
        SELECT s.*,
        (SELECT airDate FROM episodes e WHERE e.showId = s.id AND e.watched = 0
            ORDER BY e.seasonNumber ASC, e.episodeNumber ASC LIMIT 1) AS nextEpisodeAirDate
        FROM shows s WHERE s.status = :status
        ORDER BY s.updatedAt DESC
        """,
    )
    fun observeByStatusWithNextEpisode(status: String): Flow<List<ShowWithNextEpisode>>

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
                ORDER BY e.episodeNumber ASC LIMIT 1) AS firstEpisodeAirDate,
            (SELECT airDate FROM episodes e WHERE e.showId = s.id AND e.watched = 0
                ORDER BY e.seasonNumber ASC, e.episodeNumber ASC LIMIT 1) AS nextEpisodeAirDate
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
                ORDER BY e.episodeNumber ASC LIMIT 1) AS firstEpisodeAirDate,
            (SELECT airDate FROM episodes e WHERE e.showId = s.id AND e.watched = 0
                ORDER BY e.seasonNumber ASC, e.episodeNumber ASC LIMIT 1) AS nextEpisodeAirDate
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
