package com.example.showwatcher.ui.archivedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.ShowEntity
import com.example.showwatcher.data.repository.ShowRepository
import com.example.showwatcher.ui.common.UiState
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
}
