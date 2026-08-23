package com.example.showwatcher.ui.archivedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.repository.ShowRepository
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.toUserMessage
import com.example.showwatcher.ui.nav.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ArchiveDetailData(val show: ShowEntity, val seasonNumbers: List<Int>)

sealed class ArchiveDetailEvent {
    data class NavigateToShowDetail(val showId: Long) : ArchiveDetailEvent()
    data class Banner(val message: String) : ArchiveDetailEvent()
    data class Error(val message: String) : ArchiveDetailEvent()
}

@HiltViewModel
class ArchiveDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShowRepository,
) : ViewModel() {

    private val showId: Long = savedStateHandle.toRoute<Destination.ArchiveDetail>().showId

    private val _uiState = MutableStateFlow<UiState<ArchiveDetailData>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArchiveDetailData>> = _uiState

    private val _seasonEpisodes = MutableStateFlow<Map<Int, List<EpisodeEntity>>>(emptyMap())
    val seasonEpisodes: StateFlow<Map<Int, List<EpisodeEntity>>> = _seasonEpisodes

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _events = MutableSharedFlow<ArchiveDetailEvent>()
    val events: SharedFlow<ArchiveDetailEvent> = _events

    init {
        viewModelScope.launch {
            val show = repository.getShow(showId)
            _uiState.value = if (show == null) {
                UiState.FatalError("Show not found")
            } else {
                UiState.Content(ArchiveDetailData(show, repository.getDistinctSeasonNumbers(showId)))
            }
        }
    }

    fun loadSeasonIfNeeded(seasonNumber: Int) {
        if (_seasonEpisodes.value.containsKey(seasonNumber)) return
        viewModelScope.launch {
            val episodes = repository.getEpisodes(showId, seasonNumber)
            _seasonEpisodes.value = _seasonEpisodes.value + (seasonNumber to episodes)
        }
    }

    fun reactivate() {
        viewModelScope.launch {
            repository.reactivateShow(showId)
            _events.emit(ArchiveDetailEvent.NavigateToShowDetail(showId))
        }
    }

    /**
     * Same place/shape as Show Detail's "Refresh" (ShowDetailViewModel.refresh): checks TMDB for
     * a season released since this show archived. If one exists the show is reactivated onto it
     * and we navigate into Show Detail, same as [reactivate] — from there it surfaces on the
     * Upcoming or Active tab automatically per its new season's air date, same as any other active
     * show. If TMDB has nothing new, the show stays archived and a banner says so.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val result = repository.refreshArchivedShow(showId)) {
                is AppResult.Success -> {
                    val newSeason = result.value.newSeasonNumber
                    if (newSeason != null) {
                        _events.emit(ArchiveDetailEvent.NavigateToShowDetail(showId))
                    } else {
                        _events.emit(ArchiveDetailEvent.Banner("No new seasons found"))
                    }
                }
                is AppResult.Error -> _events.emit(ArchiveDetailEvent.Error(result.error.toUserMessage()))
            }
            _isRefreshing.value = false
        }
    }
}
