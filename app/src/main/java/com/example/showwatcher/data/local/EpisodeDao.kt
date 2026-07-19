package com.example.showwatcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    suspend fun getById(episodeId: Long): EpisodeEntity?

    @Query(
        "SELECT * FROM episodes WHERE showId = :showId AND seasonNumber = :seasonNumber " +
            "ORDER BY episodeNumber ASC",
    )
    suspend fun getForSeason(showId: Long, seasonNumber: Int): List<EpisodeEntity>

    @Query(
        "SELECT * FROM episodes WHERE showId = :showId AND seasonNumber = :seasonNumber " +
            "ORDER BY episodeNumber ASC",
    )
    fun observeForSeason(showId: Long, seasonNumber: Int): Flow<List<EpisodeEntity>>

    @Query(
        "SELECT DISTINCT seasonNumber FROM episodes WHERE showId = :showId " +
            "ORDER BY seasonNumber ASC",
    )
    suspend fun getDistinctSeasonNumbers(showId: Long): List<Int>

    @Query(
        "SELECT * FROM episodes WHERE showId = :showId AND seasonNumber = :seasonNumber " +
            "AND episodeNumber = :episodeNumber",
    )
    suspend fun findEpisode(showId: Long, seasonNumber: Int, episodeNumber: Int): EpisodeEntity?

    @Insert
    suspend fun insert(episode: EpisodeEntity): Long

    @Insert
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    /**
     * Updates only TMDB-sourced metadata columns. Deliberately excludes
     * `watched`/`watchedAt` — this is the safety rail behind the §4.4 refresh
     * invariant (never wipe watch history on a season-metadata refresh).
     */
    @Query(
        "UPDATE episodes SET name = :name, airDate = :airDate, tmdbEpisodeId = :tmdbEpisodeId " +
            "WHERE id = :episodeId",
    )
    suspend fun updateEpisodeMetadataOnly(episodeId: Long, name: String, airDate: String?, tmdbEpisodeId: Long?)

    /** Direct single-row toggle. No cascading to other episodes (§4.2). */
    @Query("UPDATE episodes SET watched = :watched, watchedAt = :watchedAt WHERE id = :episodeId")
    suspend fun setWatched(episodeId: Long, watched: Boolean, watchedAt: Long?)

    @Query(
        "UPDATE episodes SET watched = 1, watchedAt = :watchedAt WHERE showId = :showId " +
            "AND seasonNumber = :seasonNumber AND episodeNumber <= :upToEpisodeNumber",
    )
    suspend fun markWatchedUpToEpisodeNumber(showId: Long, seasonNumber: Int, upToEpisodeNumber: Int, watchedAt: Long)

    @Query(
        "SELECT COUNT(*) FROM episodes WHERE showId = :showId AND seasonNumber = :seasonNumber",
    )
    suspend fun countTotalForSeason(showId: Long, seasonNumber: Int): Int

    @Query(
        "SELECT COUNT(*) FROM episodes WHERE showId = :showId AND seasonNumber = :seasonNumber " +
            "AND watched = 1",
    )
    suspend fun countWatchedForSeason(showId: Long, seasonNumber: Int): Int
}
