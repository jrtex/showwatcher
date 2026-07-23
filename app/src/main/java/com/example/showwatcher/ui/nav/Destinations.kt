package com.example.showwatcher.ui.nav

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Upcoming : Destination

    @Serializable
    data object Dashboard : Destination

    @Serializable
    data object AddShow : Destination

    @Serializable
    data class ShowDetail(val showId: Long) : Destination

    @Serializable
    data object Archive : Destination

    @Serializable
    data class ArchiveDetail(val showId: Long) : Destination

    @Serializable
    data object Settings : Destination
}
