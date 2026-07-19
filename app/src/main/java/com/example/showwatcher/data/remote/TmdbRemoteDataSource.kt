package com.example.showwatcher.data.remote

import com.example.showwatcher.data.AppError
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.remote.dto.TmdbErrorDto
import com.example.showwatcher.data.remote.dto.TmdbSeasonDetailsDto
import com.example.showwatcher.data.remote.dto.TmdbShowDetailsDto
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.json.Json
import retrofit2.Response

interface TmdbRemoteDataSource {
    suspend fun searchTv(query: String): AppResult<List<TmdbSearchResult>>
    suspend fun getShowDetails(tmdbId: Long): AppResult<TmdbShowDetails>
    suspend fun getSeasonEpisodes(tmdbId: Long, seasonNumber: Int): AppResult<TmdbSeasonEpisodes>
}

class TmdbRemoteDataSourceImpl @Inject constructor(
    private val api: TmdbApi,
    private val json: Json,
) : TmdbRemoteDataSource {

    override suspend fun searchTv(query: String): AppResult<List<TmdbSearchResult>> =
        callApi { api.searchTv(query) }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(
                    result.value.results.map { dto ->
                        TmdbSearchResult(
                            tmdbId = dto.id,
                            title = dto.name,
                            year = parseYear(dto.firstAirDate),
                            posterPath = dto.posterPath,
                            overview = dto.overview,
                        )
                    },
                )
                is AppResult.Error -> result
            }
        }

    override suspend fun getShowDetails(tmdbId: Long): AppResult<TmdbShowDetails> =
        callApi { api.getShowDetails(tmdbId) }.let { result ->
            when (result) {
                is AppResult.Success -> {
                    val dto: TmdbShowDetailsDto = result.value
                    AppResult.Success(
                        TmdbShowDetails(
                            tmdbId = dto.id,
                            title = dto.name,
                            posterPath = dto.posterPath,
                            firstAirYear = parseYear(dto.firstAirDate),
                            trackedSeasonNumbers = dto.trackedSeasons.map { it.seasonNumber },
                        ),
                    )
                }
                is AppResult.Error -> result
            }
        }

    override suspend fun getSeasonEpisodes(tmdbId: Long, seasonNumber: Int): AppResult<TmdbSeasonEpisodes> =
        callApi { api.getSeasonEpisodes(tmdbId, seasonNumber) }.let { result ->
            when (result) {
                is AppResult.Success -> {
                    val dto: TmdbSeasonDetailsDto = result.value
                    AppResult.Success(
                        TmdbSeasonEpisodes(
                            seasonNumber = dto.seasonNumber,
                            episodes = dto.episodes.map {
                                TmdbEpisode(
                                    episodeNumber = it.episodeNumber,
                                    name = it.name,
                                    airDate = it.airDate,
                                    tmdbEpisodeId = it.id,
                                )
                            },
                        ),
                    )
                }
                is AppResult.Error -> result
            }
        }

    private suspend fun <T> callApi(call: suspend () -> Response<T>): AppResult<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AppResult.Success(body)
            } else {
                val errorMessage = parseErrorBody(response)
                AppResult.Error(AppError.Tmdb(errorMessage, response.code()))
            }
        } catch (e: IOException) {
            AppResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    private fun parseErrorBody(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        val parsed = raw?.let { runCatching { json.decodeFromString<TmdbErrorDto>(it) }.getOrNull() }
        return parsed?.statusMessage ?: "TMDB request failed (${response.code()})"
    }

    private fun parseYear(firstAirDate: String?): Int? =
        firstAirDate?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()
}
