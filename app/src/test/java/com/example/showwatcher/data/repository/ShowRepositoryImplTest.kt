package com.example.showwatcher.data.repository

import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.SeasonCacheMetaEntity
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.local.ShowStatus
import com.example.showwatcher.data.remote.TmdbEpisode
import com.example.showwatcher.data.remote.TmdbSeasonEpisodes
import com.example.showwatcher.data.remote.TmdbShowDetails
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers ANDROID_HANDOFF.md's two flagged highest-risk pieces: the §4.2 toggle/advance/archive
 * state machine, and the §4.4 "never wipe watched-state on refresh" invariant.
 */
class ShowRepositoryImplTest {

    private lateinit var showDao: FakeShowDao
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var seasonCacheDao: FakeSeasonCacheDao
    private lateinit var tmdb: FakeTmdbRemoteDataSource
    private lateinit var clock: FakeClock
    private lateinit var repository: ShowRepositoryImpl

    @Before
    fun setUp() {
        showDao = FakeShowDao()
        episodeDao = FakeEpisodeDao()
        seasonCacheDao = FakeSeasonCacheDao()
        tmdb = FakeTmdbRemoteDataSource()
        clock = FakeClock(time = 1_000_000L)
        repository = ShowRepositoryImpl(
            transactionRunner = FakeTransactionRunner(),
            showDao = showDao,
            episodeDao = episodeDao,
            seasonCacheDao = seasonCacheDao,
            tmdb = tmdb,
            clock = clock,
        )
    }

    private suspend fun seedShow(currentSeason: Int = 1, totalSeasons: Int = 1, tmdbId: Long = 42L): Long =
        showDao.insert(
            ShowEntity(
                tmdbId = tmdbId,
                title = "Test Show",
                posterPath = null,
                firstAirYear = 2020,
                status = ShowStatus.ACTIVE,
                currentSeason = currentSeason,
                totalSeasons = totalSeasons,
                createdAt = clock.nowMillis(),
                updatedAt = clock.nowMillis(),
                archivedAt = null,
            ),
        )

    private suspend fun seedEpisode(
        showId: Long,
        season: Int,
        episodeNumber: Int,
        watched: Boolean = false,
        name: String = "Episode $episodeNumber",
        airDate: String? = "2020-01-01",
    ): Long = episodeDao.insert(
        EpisodeEntity(
            showId = showId,
            seasonNumber = season,
            episodeNumber = episodeNumber,
            tmdbEpisodeId = episodeNumber.toLong(),
            name = name,
            airDate = airDate,
            watched = watched,
            watchedAt = if (watched) clock.nowMillis() else null,
        ),
    )

    // ---- §4.2 state machine ----

    @Test
    fun `unwatching in current season never triggers advance or archive`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 2)
        val e1 = seedEpisode(showId, season = 1, episodeNumber = 1, watched = true)
        seedEpisode(showId, season = 1, episodeNumber = 2, watched = true)

        val result = repository.toggleEpisodeWatched(e1) as AppResult.Success
        assertEquals(ToggleEvent.None, result.value.event)
        assertEquals(0, tmdb.getShowDetailsCallCount)
        assertEquals(1, showDao.getById(showId)!!.currentSeason)
    }

    @Test
    fun `toggling watched true in an already-advanced season never triggers evaluation`() = runTest {
        val showId = seedShow(currentSeason = 2, totalSeasons = 2)
        val oldSeasonEpisode = seedEpisode(showId, season = 1, episodeNumber = 1, watched = false)
        seedEpisode(showId, season = 2, episodeNumber = 1, watched = false)

        val result = repository.toggleEpisodeWatched(oldSeasonEpisode) as AppResult.Success
        assertEquals(ToggleEvent.None, result.value.event)
        assertEquals(0, tmdb.getShowDetailsCallCount)
        assertEquals(2, showDao.getById(showId)!!.currentSeason)
        assertTrue(episodeDao.getById(oldSeasonEpisode)!!.watched)
    }

    @Test
    fun `completing current season advances to next season when one exists`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        seedEpisode(showId, season = 1, episodeNumber = 1, watched = true)
        val lastEpisode = seedEpisode(showId, season = 1, episodeNumber = 2, watched = false)

        tmdb.showDetailsResult = AppResult.Success(
            TmdbShowDetails(
                tmdbId = 42L,
                title = "Test Show",
                posterPath = null,
                firstAirYear = 2020,
                trackedSeasonNumbers = listOf(1, 2),
            ),
        )
        tmdb.seasonEpisodesResults[2] = AppResult.Success(
            TmdbSeasonEpisodes(
                seasonNumber = 2,
                episodes = listOf(TmdbEpisode(episodeNumber = 1, name = "S2E1", airDate = "2021-01-01", tmdbEpisodeId = 201L)),
            ),
        )

        val result = repository.toggleEpisodeWatched(lastEpisode) as AppResult.Success
        assertEquals(ToggleEvent.Advanced(2), result.value.event)
        val updatedShow = showDao.getById(showId)!!
        assertEquals(2, updatedShow.currentSeason)
        assertEquals(2, updatedShow.totalSeasons)
        val newSeasonEpisodes = episodeDao.getForSeason(showId, 2)
        assertEquals(1, newSeasonEpisodes.size)
        assertFalse(newSeasonEpisodes.first().watched)
    }

    @Test
    fun `completing last season archives show and freezes currentSeason`() = runTest {
        val showId = seedShow(currentSeason = 3, totalSeasons = 3)
        val lastEpisode = seedEpisode(showId, season = 3, episodeNumber = 1, watched = false)

        tmdb.showDetailsResult = AppResult.Success(
            TmdbShowDetails(
                tmdbId = 42L,
                title = "Test Show",
                posterPath = null,
                firstAirYear = 2020,
                trackedSeasonNumbers = listOf(1, 2, 3),
            ),
        )

        val result = repository.toggleEpisodeWatched(lastEpisode) as AppResult.Success
        assertEquals(ToggleEvent.Archived(3), result.value.event)
        val updatedShow = showDao.getById(showId)!!
        assertEquals(ShowStatus.ARCHIVED, updatedShow.status)
        assertEquals(3, updatedShow.currentSeason)
        assertEquals(clock.nowMillis(), updatedShow.archivedAt)
        assertEquals(0, tmdb.getSeasonEpisodesCallCount)
    }

    @Test
    fun `watching a single out-of-order episode does not falsely complete the season`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        seedEpisode(showId, season = 1, episodeNumber = 1, watched = false)
        seedEpisode(showId, season = 1, episodeNumber = 2, watched = false)
        val e3 = seedEpisode(showId, season = 1, episodeNumber = 3, watched = false)

        val result = repository.toggleEpisodeWatched(e3) as AppResult.Success
        assertEquals(ToggleEvent.None, result.value.event)
        assertEquals(0, tmdb.getShowDetailsCallCount)
    }

    @Test
    fun `reactivate only flips status and clears archivedAt`() = runTest {
        val showId = seedShow(currentSeason = 2, totalSeasons = 3)
        showDao.archive(showId, archivedAt = 555L, updatedAt = 555L)
        seedEpisode(showId, season = 2, episodeNumber = 1, watched = true)

        clock.time = 2_000_000L
        repository.reactivateShow(showId)

        val show = showDao.getById(showId)!!
        assertEquals(ShowStatus.ACTIVE, show.status)
        assertNull(show.archivedAt)
        assertEquals(2, show.currentSeason)
        assertTrue(episodeDao.getForSeason(showId, 2).first().watched)
    }

    @Test
    fun `addShow marks episodes 1 through N watched by episode number`() = runTest {
        tmdb.showDetailsResult = AppResult.Success(
            TmdbShowDetails(tmdbId = 42L, title = "Test Show", posterPath = null, firstAirYear = 2020, trackedSeasonNumbers = listOf(1)),
        )
        tmdb.seasonEpisodesResults[1] = AppResult.Success(
            TmdbSeasonEpisodes(
                seasonNumber = 1,
                episodes = listOf(
                    TmdbEpisode(episodeNumber = 1, name = "E1", airDate = null, tmdbEpisodeId = 1L),
                    TmdbEpisode(episodeNumber = 2, name = "E2", airDate = null, tmdbEpisodeId = 2L),
                    TmdbEpisode(episodeNumber = 3, name = "E3", airDate = null, tmdbEpisodeId = 3L),
                ),
            ),
        )

        val result = repository.addShow(tmdbId = 42L, startingSeason = 1, startingEpisode = 2) as AppResult.Success
        val episodes = episodeDao.getForSeason(result.value, 1)
        assertTrue(episodes.first { it.episodeNumber == 1 }.watched)
        assertTrue(episodes.first { it.episodeNumber == 2 }.watched)
        assertFalse(episodes.first { it.episodeNumber == 3 }.watched)
    }

    // ---- §4.4 refresh invariant ----

    @Test
    fun `refresh preserves watched state while updating metadata`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        val episodeId = seedEpisode(showId, season = 1, episodeNumber = 1, watched = true, name = "Old name", airDate = "2020-01-01")
        seasonCacheDao.upsertMeta(SeasonCacheMetaEntity(showId, 1, fetchedAt = clock.nowMillis(), episodeCount = 1))

        tmdb.seasonEpisodesResults[1] = AppResult.Success(
            TmdbSeasonEpisodes(
                seasonNumber = 1,
                episodes = listOf(TmdbEpisode(episodeNumber = 1, name = "New name", airDate = "2020-02-02", tmdbEpisodeId = 99L)),
            ),
        )

        repository.refreshCurrentSeason(showId, force = true)

        val episode = episodeDao.getById(episodeId)!!
        assertTrue(episode.watched)
        assertEquals("New name", episode.name)
        assertEquals("2020-02-02", episode.airDate)
        assertEquals(99L, episode.tmdbEpisodeId)
    }

    @Test
    fun `non-forced refresh within 3 days makes zero network calls`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        seedEpisode(showId, season = 1, episodeNumber = 1)
        seasonCacheDao.upsertMeta(SeasonCacheMetaEntity(showId, 1, fetchedAt = clock.nowMillis(), episodeCount = 1))

        clock.time += 2 * 24 * 60 * 60 * 1000L

        repository.refreshCurrentSeason(showId, force = false)
        assertEquals(0, tmdb.getSeasonEpisodesCallCount)
    }

    @Test
    fun `non-forced refresh past 3 days calls network and bumps fetchedAt`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        seedEpisode(showId, season = 1, episodeNumber = 1)
        seasonCacheDao.upsertMeta(SeasonCacheMetaEntity(showId, 1, fetchedAt = clock.nowMillis(), episodeCount = 1))
        tmdb.seasonEpisodesResults[1] = AppResult.Success(
            TmdbSeasonEpisodes(
                seasonNumber = 1,
                episodes = listOf(TmdbEpisode(episodeNumber = 1, name = "E1", airDate = null, tmdbEpisodeId = 1L)),
            ),
        )

        clock.time += 4 * 24 * 60 * 60 * 1000L

        repository.refreshCurrentSeason(showId, force = false)
        assertEquals(1, tmdb.getSeasonEpisodesCallCount)
        assertEquals(clock.nowMillis(), seasonCacheDao.getMeta(showId, 1)!!.fetchedAt)
    }

    @Test
    fun `refresh with no cache meta is treated as stale`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        seedEpisode(showId, season = 1, episodeNumber = 1)
        tmdb.seasonEpisodesResults[1] = AppResult.Success(
            TmdbSeasonEpisodes(
                seasonNumber = 1,
                episodes = listOf(TmdbEpisode(episodeNumber = 1, name = "E1", airDate = null, tmdbEpisodeId = 1L)),
            ),
        )

        repository.refreshCurrentSeason(showId, force = false)
        assertEquals(1, tmdb.getSeasonEpisodesCallCount)
    }

    @Test
    fun `forced refresh always calls network regardless of freshness`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        seedEpisode(showId, season = 1, episodeNumber = 1)
        seasonCacheDao.upsertMeta(SeasonCacheMetaEntity(showId, 1, fetchedAt = clock.nowMillis(), episodeCount = 1))
        tmdb.seasonEpisodesResults[1] = AppResult.Success(
            TmdbSeasonEpisodes(
                seasonNumber = 1,
                episodes = listOf(TmdbEpisode(episodeNumber = 1, name = "E1", airDate = null, tmdbEpisodeId = 1L)),
            ),
        )

        repository.refreshCurrentSeason(showId, force = true)
        assertEquals(1, tmdb.getSeasonEpisodesCallCount)
    }

    @Test
    fun `refresh inserts a newly added TMDB episode without conflict`() = runTest {
        val showId = seedShow(currentSeason = 1, totalSeasons = 1)
        seedEpisode(showId, season = 1, episodeNumber = 1, watched = true)
        seasonCacheDao.upsertMeta(SeasonCacheMetaEntity(showId, 1, fetchedAt = clock.nowMillis(), episodeCount = 1))
        tmdb.seasonEpisodesResults[1] = AppResult.Success(
            TmdbSeasonEpisodes(
                seasonNumber = 1,
                episodes = listOf(
                    TmdbEpisode(episodeNumber = 1, name = "E1", airDate = null, tmdbEpisodeId = 1L),
                    TmdbEpisode(episodeNumber = 2, name = "E2 (new)", airDate = null, tmdbEpisodeId = 2L),
                ),
            ),
        )

        repository.refreshCurrentSeason(showId, force = true)

        val episodes = episodeDao.getForSeason(showId, 1)
        assertEquals(2, episodes.size)
        assertTrue(episodes.first { it.episodeNumber == 1 }.watched)
        assertFalse(episodes.first { it.episodeNumber == 2 }.watched)
    }
}
