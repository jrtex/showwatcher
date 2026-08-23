package com.example.showwatcher.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.Clock
import com.example.showwatcher.data.SettingsRepository
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.data.repository.ShowRepository
import com.example.showwatcher.data.today
import com.example.showwatcher.ui.common.RefreshEvent
import com.example.showwatcher.ui.common.ShowListKind
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.sortedByShowOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ShowRepository,
    settingsRepository: SettingsRepository,
    clock: Clock,
) : ViewModel() {
    val uiState: StateFlow<UiState<List<ShowWithProgress>>> =
        combine(repository.observeActiveShowsWithProgress(), settingsRepository.sortOrder) { shows, order ->
            shows.sortedByShowOrder(
                order,
                kind = ShowListKind.ACTIVE_OR_ARCHIVE,
                today = clock.today(),
                title = { it.show.title },
                sortDate = { it.nextEpisodeAirDate },
            )
        }
            .map<List<ShowWithProgress>, UiState<List<ShowWithProgress>>> { UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _events = MutableSharedFlow<RefreshEvent>()
    val events: SharedFlow<RefreshEvent> = _events

    /** Pull-to-refresh: re-fetches every show currently shown on this tab. */
    fun refreshAll() {
        if (_isRefreshing.value) return
        val ids = (uiState.value as? UiState.Content)?.data?.map { it.show.id } ?: return
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isRefreshing.value = true
            val semaphore = Semaphore(5)
            val results = coroutineScope {
                ids.map { id -> async { semaphore.withPermit { repository.refreshCurrentSeason(id, force = true) } } }.awaitAll()
            }
            val newSeasonCount = results.count { it is AppResult.Success && it.value.newSeasonAvailable != null }
            val errorCount = results.count { it is AppResult.Error }
            _events.emit(
                when {
                    errorCount > 0 -> RefreshEvent.Error("$errorCount show${if (errorCount == 1) "" else "s"} couldn't be refreshed")
                    newSeasonCount > 0 -> RefreshEvent.Banner(
                        "$newSeasonCount show${if (newSeasonCount == 1) "" else "s"} have new seasons available",
                    )
                    else -> RefreshEvent.Banner("Up to date")
                },
            )
            _isRefreshing.value = false
        }
    }
}
