package com.example.showwatcher.data.repository

import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.ShowEntity

/** Fired only on marking-watched of the show's *current* season (ANDROID_HANDOFF.md §4.2/§4.3). */
sealed class ToggleEvent {
    data object None : ToggleEvent()
    data class Advanced(val newSeasonNumber: Int) : ToggleEvent()
    data class Archived(val seasonsCompleted: Int) : ToggleEvent()
}

data class ToggleOutcome(
    val show: ShowEntity,
    /** Always the show's *current* season episode list, regardless of which season was toggled. */
    val episodes: List<EpisodeEntity>,
    val event: ToggleEvent,
)

data class RefreshOutcome(
    val show: ShowEntity,
    val episodes: List<EpisodeEntity>,
)
