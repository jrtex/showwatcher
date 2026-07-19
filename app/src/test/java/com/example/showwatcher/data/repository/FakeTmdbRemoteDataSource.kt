package com.example.showwatcher.data.repository

import com.example.showwatcher.data.AppError
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.remote.TmdbRemoteDataSource
import com.example.showwatcher.data.remote.TmdbSearchResult
import com.example.showwatcher.data.remote.TmdbSeasonEpisodes
import com.example.showwatcher.data.remote.TmdbShowDetails

class FakeTmdbRemoteDataSource : TmdbRemoteDataSource {
    var searchResult: AppResult<List<TmdbSearchResult>> = AppResult.Success(emptyList())
    var showDetailsResult: AppResult<TmdbShowDetails> =
        AppResult.Error(AppError.Unknown(IllegalStateException("showDetailsResult not stubbed")))
    val seasonEpisodesResults = mutableMapOf<Int, AppResult<TmdbSeasonEpisodes>>()

    var getShowDetailsCallCount = 0
        private set
    var getSeasonEpisodesCallCount = 0
        private set

    override suspend fun searchTv(query: String): AppResult<List<TmdbSearchResult>> = searchResult

    override suspend fun getShowDetails(tmdbId: Long): AppResult<TmdbShowDetails> {
        getShowDetailsCallCount++
        return showDetailsResult
    }

    override suspend fun getSeasonEpisodes(tmdbId: Long, seasonNumber: Int): AppResult<TmdbSeasonEpisodes> {
        getSeasonEpisodesCallCount++
        return seasonEpisodesResults[seasonNumber]
            ?: AppResult.Error(AppError.Unknown(IllegalStateException("season $seasonNumber not stubbed")))
    }
}
