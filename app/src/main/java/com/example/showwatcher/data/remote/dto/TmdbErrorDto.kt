package com.example.showwatcher.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbErrorDto(
    @SerialName("status_message") val statusMessage: String? = null,
    @SerialName("status_code") val statusCode: Int? = null,
)
