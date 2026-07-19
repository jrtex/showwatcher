package com.example.showwatcher.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.showwatcher.data.AppError
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.Clock
import com.example.showwatcher.data.local.EpisodeDao
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.SeasonCacheDao
import com.example.showwatcher.data.local.SeasonCacheMetaEntity
import com.example.showwatcher.data.local.ShowDao
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.local.ShowStatus
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.data.local.TransactionRunner
import com.example.showwatcher.data.remote.TmdbEpisode
import com.example.showwatcher.data.remote.TmdbRemoteDataSource
import com.example.showwatcher.data.remote.TmdbSearchResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

private const val STALE_AFTER_MILLIS = 3L * 24 * 60 * 60 * 1000

class ShowRepositoryImpl @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val showDao: ShowDao,
    private val episodeDao: EpisodeDao,
    private val seasonCacheDao: SeasonCacheDao,
    private val tmdb: TmdbRemoteDataSource,
    private val clock: Clock,
) : ShowRepository {

    override fun observeShowsByStatus(status: String): Flow<List<ShowEntity>> =
        showDao.observeByStatus(status)

    override fun observeShowsWithProgressByStatus(status: String): Flow<List<ShowWithProgress>> =
        showDao.observeByStatusWithProgress(status)

    override fun observeShow(showId: Long): Flow<ShowEntity?> = showDao.observeById(showId)

    override suspend fun getShow(showId: Long): ShowEntity? = showDao.getById(showId)

    override fun observeEpisodes(showId: Long, seasonNumber: Int): Flow<List<EpisodeEntity>> =
        episodeDao.observeForSeason(showId, seasonNumber)

    override suspend fun getEpisodes(showId: Long, seasonNumber: Int): List<EpisodeEntity> =
        episodeDao.getForSeason(showId, seasonNumber)

    override suspend fun getDistinctSeasonNumbers(showId: Long): List<Int> =
        episodeDao.getDistinctSeasonNumbers(showId)

    override suspend fun searchTmdb(query: String): AppResult<List<TmdbSearchResult>> =
        tmdb.searchTv(query)

    override suspend fun addShow(tmdbId: Long, startingSeason: Int, startingEpisode: Int): AppResult<Long> {
        if (showDao.getByTmdbId(tmdbId) != null) {
            return AppResult.Error(AppError.AlreadyAdded)
        }

        val detailsResult = tmdb.getShowDetails(tmdbId)
        val details = when (detailsResult) {
            is AppResult.Error -> return detailsResult
            is AppResult.Success -> detailsResult.value
        }

        val seasonResult = tmdb.getSeasonEpisodes(tmdbId, startingSeason)
        val season = when (seasonResult) {
            is AppResult.Error -> return seasonResult
            is AppResult.Success -> seasonResult.value
        }

        val now = clock.nowMillis()
        val show = ShowEntity(
            tmdbId = tmdbId,
            title = details.title,
            posterPath = details.posterPath,
            firstAirYear = details.firstAirYear,
            status = ShowStatus.ACTIVE,
            currentSeason = startingSeason,
            totalSeasons = details.trackedSeasonNumbers.size,
            createdAt = now,
            updatedAt = now,
            archivedAt = null,
        )

        return try {
            val showId = transactionRunner.run {
                val id = showDao.insert(show)
                upsertSeasonEpisodes(id, startingSeason, season.episodes, now)
                if (startingEpisode > 0) {
                    episodeDao.markWatchedUpToEpisodeNumber(id, startingSeason, startingEpisode, now)
                }
                id
            }
            AppResult.Success(showId)
        } catch (e: SQLiteConstraintException) {
            AppResult.Error(AppError.AlreadyAdded)
        }
    }

    override suspend fun toggleEpisodeWatched(episodeId: Long): AppResult<ToggleOutcome> {
        val episode = episodeDao.getById(episodeId)
            ?: return AppResult.Error(AppError.Unknown(IllegalStateException("Episode $episodeId not found")))

        val newWatched = !episode.watched
        val now = clock.nowMillis()
        episodeDao.setWatched(episodeId, newWatched, if (newWatched) now else null)

        val show = showDao.getById(episode.showId)
            ?: return AppResult.Error(AppError.Unknown(IllegalStateException("Show ${episode.showId} not found")))

        val evaluatesCompletion = newWatched && episode.seasonNumber == show.currentSeason
        if (!evaluatesCompletion) {
            return AppResult.Success(
                ToggleOutcome(show, episodeDao.getForSeason(show.id, show.currentSeason), ToggleEvent.None),
            )
        }

        val watchedCount = episodeDao.countWatchedForSeason(show.id, show.currentSeason)
        val totalCount = episodeDao.countTotalForSeason(show.id, show.currentSeason)
        if (watchedCount < totalCount) {
            return AppResult.Success(
                ToggleOutcome(show, episodeDao.getForSeason(show.id, show.currentSeason), ToggleEvent.None),
            )
        }

        // Season complete: consult TMDB for the authoritative season list before writing anything.
        val detailsResult = tmdb.getShowDetails(show.tmdbId)
        val details = when (detailsResult) {
            is AppResult.Error -> return detailsResult
            is AppResult.Success -> detailsResult.value
        }
        val nextSeasonNumber = details.trackedSeasonNumbers.filter { it > show.currentSeason }.minOrNull()

        if (nextSeasonNumber == null) {
            transactionRunner.run {
                showDao.archive(showId = show.id, archivedAt = now, updatedAt = now)
            }
            val archivedShow = showDao.getById(show.id)!!
            return AppResult.Success(
                ToggleOutcome(
                    archivedShow,
                    episodeDao.getForSeason(show.id, show.currentSeason),
                    ToggleEvent.Archived(seasonsCompleted = show.currentSeason),
                ),
            )
        }

        val nextSeasonResult = tmdb.getSeasonEpisodes(show.tmdbId, nextSeasonNumber)
        val nextSeason = when (nextSeasonResult) {
            is AppResult.Error -> return nextSeasonResult
            is AppResult.Success -> nextSeasonResult.value
        }

        transactionRunner.run {
            showDao.advanceSeason(
                showId = show.id,
                currentSeason = nextSeasonNumber,
                totalSeasons = details.trackedSeasonNumbers.size,
                updatedAt = now,
            )
            upsertSeasonEpisodes(show.id, nextSeasonNumber, nextSeason.episodes, now)
        }

        val advancedShow = showDao.getById(show.id)!!
        return AppResult.Success(
            ToggleOutcome(
                advancedShow,
                episodeDao.getForSeason(show.id, nextSeasonNumber),
                ToggleEvent.Advanced(nextSeasonNumber),
            ),
        )
    }

    override suspend fun refreshCurrentSeason(showId: Long, force: Boolean): AppResult<RefreshOutcome> {
        val show = showDao.getById(showId)
            ?: return AppResult.Error(AppError.Unknown(IllegalStateException("Show $showId not found")))
        val season = show.currentSeason
        val meta = seasonCacheDao.getMeta(showId, season)
        val now = clock.nowMillis()
        val isStale = meta == null || (now - meta.fetchedAt) > STALE_AFTER_MILLIS

        if (!force && !isStale) {
            return AppResult.Success(RefreshOutcome(show, episodeDao.getForSeason(showId, season)))
        }

        val seasonResult = tmdb.getSeasonEpisodes(show.tmdbId, season)
        val seasonEpisodes = when (seasonResult) {
            is AppResult.Error -> return seasonResult
            is AppResult.Success -> seasonResult.value
        }

        transactionRunner.run {
            upsertSeasonEpisodes(showId, season, seasonEpisodes.episodes, now)
        }

        return AppResult.Success(RefreshOutcome(showDao.getById(showId)!!, episodeDao.getForSeason(showId, season)))
    }

    override suspend fun reactivateShow(showId: Long) {
        showDao.reactivate(showId = showId, updatedAt = clock.nowMillis())
    }

    override suspend fun deleteShow(showId: Long) {
        showDao.deleteById(showId)
    }

    /**
     * Upsert-by-episode_number, keyed on (showId, seasonNumber, episodeNumber): existing rows only
     * get name/airDate/tmdbEpisodeId updated (watched/watchedAt are never touched here), new rows
     * are inserted unwatched. Never delete-and-reinsert (ANDROID_HANDOFF.md §4.4). Shared by both
     * the refresh path and the advance-to-a-new-season path so there is one upsert implementation,
     * not two. Must be called from inside a `TransactionRunner.run` block.
     */
    private suspend fun upsertSeasonEpisodes(
        showId: Long,
        seasonNumber: Int,
        episodes: List<TmdbEpisode>,
        fetchedAt: Long,
    ) {
        for (ep in episodes) {
            val existing = episodeDao.findEpisode(showId, seasonNumber, ep.episodeNumber)
            if (existing != null) {
                episodeDao.updateEpisodeMetadataOnly(existing.id, ep.name, ep.airDate, ep.tmdbEpisodeId)
            } else {
                episodeDao.insert(
                    EpisodeEntity(
                        showId = showId,
                        seasonNumber = seasonNumber,
                        episodeNumber = ep.episodeNumber,
                        tmdbEpisodeId = ep.tmdbEpisodeId,
                        name = ep.name,
                        airDate = ep.airDate,
                        watched = false,
                        watchedAt = null,
                    ),
                )
            }
        }
        seasonCacheDao.upsertMeta(SeasonCacheMetaEntity(showId, seasonNumber, fetchedAt, episodes.size))
    }
}
