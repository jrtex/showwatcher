package com.example.showwatcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSearchResponseDto(
    @SerialName("results") val results: List<TmdbSearchResultDto> = emptyList(),
)

@Serializable
data class TmdbSearchResultDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("overview") val overview: String? = null,
)
