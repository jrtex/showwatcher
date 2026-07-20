package com.example.showwatcher.data.repository

import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.data.remote.TmdbSearchResult
import kotlinx.coroutines.flow.Flow

interface ShowRepository {
    fun observeShowsByStatus(status: String): Flow<List<ShowEntity>>
    fun observeShowsWithProgressByStatus(status: String): Flow<List<ShowWithProgress>>
    fun observeShow(showId: Long): Flow<ShowEntity?>
    suspend fun getShow(showId: Long): ShowEntity?
    fun observeEpisodes(showId: Long, seasonNumber: Int): Flow<List<EpisodeEntity>>
    suspend fun getEpisodes(showId: Long, seasonNumber: Int): List<EpisodeEntity>
    suspend fun getDistinctSeasonNumbers(showId: Long): List<Int>

    suspend fun searchTmdb(query: String): AppResult<List<TmdbSearchResult>>

    /** Season numbers available for a show (season 0 excluded), for the Add Show season picker. */
    suspend fun getShowSeasons(tmdbId: Long): AppResult<List<Int>>

    /** ANDROID_HANDOFF.md §4.1. */
    suspend fun addShow(tmdbId: Long, startingSeason: Int = 1, startingEpisode: Int = 0): AppResult<Long>

    /** The §4.2 state machine: direct toggle, guarded season-completion evaluation, advance/archive. */
    suspend fun toggleEpisodeWatched(episodeId: Long): AppResult<ToggleOutcome>

    /** §4.4: staleness-gated (or forced) re-fetch that never clears watched/watchedAt. */
    suspend fun refreshCurrentSeason(showId: Long, force: Boolean = false): AppResult<RefreshOutcome>

    /** §4.5: no TMDB check, no currentSeason change. */
    suspend fun reactivateShow(showId: Long)

    /** §4.6: cascades to episodes and season_cache_meta via Room FKs. */
    suspend fun deleteShow(showId: Long)
}
