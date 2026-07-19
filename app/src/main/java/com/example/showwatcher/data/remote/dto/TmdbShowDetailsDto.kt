package com.example.showwatcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbShowDetailsDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("seasons") val seasons: List<TmdbSeasonSummaryDto> = emptyList(),
) {
    /** Season 0 ("Specials") is excluded from tracking entirely (ANDROID_HANDOFF.md §3). */
    val trackedSeasons: List<TmdbSeasonSummaryDto>
        get() = seasons.filter { it.seasonNumber != 0 }.sortedBy { it.seasonNumber }
}

@Serializable
data class TmdbSeasonSummaryDto(
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
)
