package com.example.showwatcher.data.repository

import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.local.ShowUpcoming
import com.example.showwatcher.data.local.ShowWithLastWatchedEpisode
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.data.remote.TmdbSearchResult
import kotlinx.coroutines.flow.Flow

interface ShowRepository {
    fun observeShowsByLastWatchedEpisode(status: String): Flow<List<ShowWithLastWatchedEpisode>>

    /** Active shows whose current season's first episode has already aired. */
    fun observeActiveShowsWithProgress(): Flow<List<ShowWithProgress>>

    /** Active shows whose current season's first episode hasn't aired yet (or has no known air date). */
    fun observeUpcomingShows(): Flow<List<ShowUpcoming>>
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

    /**
     * Bulk-sets every episode in the given season. Marking all watched only runs the TMDB
     * advance/archive evaluation from [toggleEpisodeWatched] when [seasonNumber] is the show's
     * current season; unmarking, or acting on any other (browsed) season, never does.
     */
    suspend fun setAllEpisodesWatched(showId: Long, seasonNumber: Int, watched: Boolean): AppResult<ToggleOutcome>

    /**
     * Guarantees a season's episodes exist locally, fetching and caching them from TMDB the
     * first time a season is browsed to. No-ops (no network call) if already cached.
     */
    suspend fun ensureSeasonLoaded(showId: Long, seasonNumber: Int): AppResult<Unit>

    /**
     * §4.4: staleness-gated (or forced) re-fetch that never clears watched/watchedAt. Also
     * refreshes show-level metadata (title/posterPath/totalSeasons) and reports the highest
     * newly-discovered season number via [RefreshOutcome.newSeasonAvailable], if any.
     */
    suspend fun refreshCurrentSeason(showId: Long, force: Boolean = false): AppResult<RefreshOutcome>

    /** §4.5: no TMDB check, no currentSeason change. */
    suspend fun reactivateShow(showId: Long)

    /**
     * Post-implementation feature (the "v2 nicety" flagged in ANDROID_HANDOFF.md §4.5): checks
     * TMDB for a season past the archived show's frozen `currentSeason` — i.e. a renewal. If one
     * exists, reactivates the show onto that season (status -> active, `archivedAt` cleared) and
     * caches its episodes, same as a normal season advance. If TMDB has nothing new, only
     * show-level metadata (title/poster/totalSeasons) is refreshed and the show stays archived.
     */
    suspend fun refreshArchivedShow(showId: Long): AppResult<ArchiveRefreshOutcome>

    /** §4.6: cascades to episodes and season_cache_meta via Room FKs. */
    suspend fun deleteShow(showId: Long)
}
