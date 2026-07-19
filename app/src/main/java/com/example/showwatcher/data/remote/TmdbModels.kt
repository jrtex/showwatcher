package com.example.showwatcher.data.remote

/** Thin domain-ish shapes the repository consumes, decoupled from TMDB's raw JSON DTOs. */
data class TmdbSearchResult(
    val tmdbId: Long,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    val overview: String?,
)

data class TmdbShowDetails(
    val tmdbId: Long,
    val title: String,
    val posterPath: String?,
    val firstAirYear: Int?,
    /** Season numbers with season 0 ("Specials") excluded, ascending (ANDROID_HANDOFF.md §3). */
    val trackedSeasonNumbers: List<Int>,
)

data class TmdbSeasonEpisodes(
    val seasonNumber: Int,
    val episodes: List<TmdbEpisode>,
)

data class TmdbEpisode(
    val episodeNumber: Int,
    val name: String,
    val airDate: String?,
    val tmdbEpisodeId: Long,
)
