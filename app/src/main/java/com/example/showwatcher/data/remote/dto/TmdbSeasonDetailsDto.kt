package com.example.showwatcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSeasonDetailsDto(
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episodes") val episodes: List<TmdbEpisodeDto> = emptyList(),
)

@Serializable
data class TmdbEpisodeDto(
    @SerialName("id") val id: Long,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("name") val name: String,
    @SerialName("air_date") val airDate: String? = null,
)
