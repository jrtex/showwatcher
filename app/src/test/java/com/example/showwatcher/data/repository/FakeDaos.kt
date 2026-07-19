package com.example.showwatcher.data.repository

import com.example.showwatcher.data.local.EpisodeDao
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.SeasonCacheDao
import com.example.showwatcher.data.local.SeasonCacheMetaEntity
import com.example.showwatcher.data.local.ShowDao
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.data.local.TransactionRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** In-memory ShowDao double. No real transactionality needed since [FakeTransactionRunner] just runs inline. */
class FakeShowDao : ShowDao {
    private val shows = mutableMapOf<Long, ShowEntity>()
    private var nextId = 1L

    override suspend fun getById(showId: Long): ShowEntity? = shows[showId]

    override fun observeById(showId: Long): Flow<ShowEntity?> = flowOf(shows[showId])

    override suspend fun getByTmdbId(tmdbId: Long): ShowEntity? = shows.values.find { it.tmdbId == tmdbId }

    override fun observeByStatus(status: String): Flow<List<ShowEntity>> =
        flowOf(shows.values.filter { it.status == status })

    override fun observeByStatusWithProgress(status: String): Flow<List<ShowWithProgress>> =
        flowOf(emptyList())

    override suspend fun insert(show: ShowEntity): Long {
        val id = nextId++
        shows[id] = show.copy(id = id)
        return id
    }

    override suspend fun advanceSeason(showId: Long, currentSeason: Int, totalSeasons: Int, updatedAt: Long) {
        val existing = shows.getValue(showId)
        shows[showId] = existing.copy(currentSeason = currentSeason, totalSeasons = totalSeasons, updatedAt = updatedAt)
    }

    override suspend fun archive(showId: Long, status: String, archivedAt: Long, updatedAt: Long) {
        val existing = shows.getValue(showId)
        shows[showId] = existing.copy(status = status, archivedAt = archivedAt, updatedAt = updatedAt)
    }

    override suspend fun reactivate(showId: Long, status: String, updatedAt: Long) {
        val existing = shows.getValue(showId)
        shows[showId] = existing.copy(status = status, archivedAt = null, updatedAt = updatedAt)
    }

    override suspend fun deleteById(showId: Long) {
        shows.remove(showId)
    }
}

class FakeEpisodeDao : EpisodeDao {
    private val episodes = mutableMapOf<Long, EpisodeEntity>()
    private var nextId = 1L

    override suspend fun getById(episodeId: Long): EpisodeEntity? = episodes[episodeId]

    override suspend fun getForSeason(showId: Long, seasonNumber: Int): List<EpisodeEntity> =
        episodes.values.filter { it.showId == showId && it.seasonNumber == seasonNumber }
            .sortedBy { it.episodeNumber }

    override fun observeForSeason(showId: Long, seasonNumber: Int): Flow<List<EpisodeEntity>> =
        flowOf(episodes.values.filter { it.showId == showId && it.seasonNumber == seasonNumber }.sortedBy { it.episodeNumber })

    override suspend fun getDistinctSeasonNumbers(showId: Long): List<Int> =
        episodes.values.filter { it.showId == showId }.map { it.seasonNumber }.distinct().sorted()

    override suspend fun findEpisode(showId: Long, seasonNumber: Int, episodeNumber: Int): EpisodeEntity? =
        episodes.values.find {
            it.showId == showId && it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber
        }

    override suspend fun insert(episode: EpisodeEntity): Long {
        val id = nextId++
        episodes[id] = episode.copy(id = id)
        return id
    }

    override suspend fun insertAll(episodes: List<EpisodeEntity>) {
        episodes.forEach { insert(it) }
    }

    override suspend fun updateEpisodeMetadataOnly(episodeId: Long, name: String, airDate: String?, tmdbEpisodeId: Long?) {
        val existing = episodes.getValue(episodeId)
        episodes[episodeId] = existing.copy(name = name, airDate = airDate, tmdbEpisodeId = tmdbEpisodeId)
    }

    override suspend fun setWatched(episodeId: Long, watched: Boolean, watchedAt: Long?) {
        val existing = episodes.getValue(episodeId)
        episodes[episodeId] = existing.copy(watched = watched, watchedAt = watchedAt)
    }

    override suspend fun markWatchedUpToEpisodeNumber(
        showId: Long,
        seasonNumber: Int,
        upToEpisodeNumber: Int,
        watchedAt: Long,
    ) {
        episodes.values
            .filter { it.showId == showId && it.seasonNumber == seasonNumber && it.episodeNumber <= upToEpisodeNumber }
            .forEach { episodes[it.id] = it.copy(watched = true, watchedAt = watchedAt) }
    }

    override suspend fun countTotalForSeason(showId: Long, seasonNumber: Int): Int =
        episodes.values.count { it.showId == showId && it.seasonNumber == seasonNumber }

    override suspend fun countWatchedForSeason(showId: Long, seasonNumber: Int): Int =
        episodes.values.count { it.showId == showId && it.seasonNumber == seasonNumber && it.watched }
}

class FakeSeasonCacheDao : SeasonCacheDao {
    private val meta = mutableMapOf<Pair<Long, Int>, SeasonCacheMetaEntity>()

    override suspend fun getMeta(showId: Long, seasonNumber: Int): SeasonCacheMetaEntity? = meta[showId to seasonNumber]

    override suspend fun upsertMeta(meta: SeasonCacheMetaEntity) {
        this.meta[meta.showId to meta.seasonNumber] = meta
    }
}

class FakeTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
