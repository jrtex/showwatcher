package com.example.showwatcher.data.remote

/** Builds display URLs from TMDB's relative poster_path fragments (ANDROID_HANDOFF.md §5). */
object TmdbImageUrlBuilder {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"

    fun posterUrl(posterPath: String?, size: String = "w300"): String? {
        if (posterPath.isNullOrBlank()) return null
        return "$BASE_URL$size$posterPath"
    }
}
