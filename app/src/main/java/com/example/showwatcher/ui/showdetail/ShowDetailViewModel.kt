package com.example.showwatcher.ui.showdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.repository.ShowRepository
import com.example.showwatcher.data.repository.ToggleEvent
import com.example.showwatcher.data.repository.ToggleOutcome
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.toUserMessage
import com.example.showwatcher.ui.nav.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShowDetailData(val show: ShowEntity, val viewedSeason: Int, val episodes: List<EpisodeEntity>)

sealed class ShowDetailEvent {
    data class Banner(val message: String) : ShowDetailEvent()
    data class Error(val message: String) : ShowDetailEvent()
    data object Deleted : ShowDetailEvent()
}

@HiltViewModel
class ShowDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShowRepository,
) : ViewModel() {

    private val showId: Long = savedStateHandle.toRoute<Destination.ShowDetail>().showId

    /** `null` means "follow the show's current season" (default); set once the user browses away. */
    private val _selectedSeason = MutableStateFlow<Int?>(null)

    /**
     * Room's Flow is already the "authoritative replace" half of the §7.3 optimistic pattern:
     * the direct episode-watched flip commits before any TMDB advance/archive network call, so
     * this Flow reflects the toggle almost immediately, then re-emits again if the season
     * advances/archives — no manual optimistic-state bookkeeping needed in the ViewModel.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<ShowDetailData>> = repository.observeShow(showId)
        .flatMapLatest { show ->
            if (show == null) {
                flowOf(UiState.FatalError("Show not found"))
            } else {
                _selectedSeason.flatMapLatest { selected ->
                    val season = selected ?: show.currentSeason
                    repository.observeEpisodes(showId, season).map { episodes ->
                        UiState.Content(ShowDetailData(show, season, episodes)) as UiState<ShowDetailData>
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isSeasonLoading = MutableStateFlow(false)
    val isSeasonLoading: StateFlow<Boolean> = _isSeasonLoading

    private val _events = MutableSharedFlow<ShowDetailEvent>()
    val events: SharedFlow<ShowDetailEvent> = _events

    init {
        viewModelScope.launch { refreshInternal(force = false) }
    }

    fun toggle(episodeId: Long) {
        viewModelScope.launch { emitToggleEvents(repository.toggleEpisodeWatched(episodeId)) }
    }

    fun setAllWatched(watched: Boolean) {
        val season = (uiState.value as? UiState.Content)?.data?.viewedSeason ?: return
        viewModelScope.launch { emitToggleEvents(repository.setAllEpisodesWatched(showId, season, watched)) }
    }

    fun previousSeason() = navigateSeason(-1)

    fun nextSeason() = navigateSeason(1)

    private fun navigateSeason(delta: Int) {
        val data = (uiState.value as? UiState.Content)?.data ?: return
        val target = data.viewedSeason + delta
        if (target < 1 || target > data.show.totalSeasons) return
        viewModelScope.launch {
            _isSeasonLoading.value = true
            when (val result = repository.ensureSeasonLoaded(showId, target)) {
                is AppResult.Success -> _selectedSeason.value = target
                is AppResult.Error -> _events.emit(ShowDetailEvent.Error(result.error.toUserMessage()))
            }
            _isSeasonLoading.value = false
        }
    }

    private suspend fun emitToggleEvents(result: AppResult<ToggleOutcome>) {
        when (result) {
            is AppResult.Success -> {
                when (val event = result.value.event) {
                    is ToggleEvent.Advanced ->
                        _events.emit(ShowDetailEvent.Banner("Advanced to Season ${event.newSeasonNumber}"))
                    is ToggleEvent.Archived ->
                        _events.emit(ShowDetailEvent.Banner("Series complete — moved to Archive"))
                    ToggleEvent.None -> Unit
                }
            }
            is AppResult.Error -> _events.emit(ShowDetailEvent.Error(result.error.toUserMessage()))
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshInternal(force = true) }
    }

    private suspend fun refreshInternal(force: Boolean) {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        when (val result = repository.refreshCurrentSeason(showId, force)) {
            is AppResult.Success -> result.value.newSeasonAvailable?.let { newSeason ->
                _events.emit(ShowDetailEvent.Banner("New season available: Season $newSeason"))
            }
            is AppResult.Error -> _events.emit(ShowDetailEvent.Error(result.error.toUserMessage()))
        }
        _isRefreshing.value = false
    }

    fun deleteShow() {
        viewModelScope.launch {
            repository.deleteShow(showId)
            _events.emit(ShowDetailEvent.Deleted)
        }
    }
}
