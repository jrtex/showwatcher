package com.example.showwatcher.data.remote

import com.example.showwatcher.data.remote.dto.TmdbSearchResponseDto
import com.example.showwatcher.data.remote.dto.TmdbSeasonDetailsDto
import com.example.showwatcher.data.remote.dto.TmdbShowDetailsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB v3 API surface (ANDROID_HANDOFF.md §5). The `api_key` query param is
 * injected on every call by an OkHttp interceptor (see di/NetworkModule),
 * not declared per-method here.
 */
interface TmdbApi {
    @GET("search/tv")
    suspend fun searchTv(@Query("query") query: String): Response<TmdbSearchResponseDto>

    @GET("tv/{id}")
    suspend fun getShowDetails(@Path("id") tmdbId: Long): Response<TmdbShowDetailsDto>

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeasonEpisodes(
        @Path("id") tmdbId: Long,
        @Path("season_number") seasonNumber: Int,
    ): Response<TmdbSeasonDetailsDto>
}
