package com.example.showwatcher.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showwatcher.data.SettingsRepository
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.data.repository.ShowRepository
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.sortedByShowOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: ShowRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<UiState<List<ShowWithProgress>>> =
        combine(repository.observeActiveShowsWithProgress(), settingsRepository.sortOrder) { shows, order ->
            shows.sortedByShowOrder(order, title = { it.show.title }, nextEpisodeAirDate = { it.nextEpisodeAirDate })
        }
            .map<List<ShowWithProgress>, UiState<List<ShowWithProgress>>> { UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
}
