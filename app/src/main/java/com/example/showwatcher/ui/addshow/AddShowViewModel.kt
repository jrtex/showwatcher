package com.example.showwatcher.ui.addshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.remote.TmdbSearchResult
import com.example.showwatcher.data.repository.ShowRepository
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AddShowEvent {
    data class NavigateToShowDetail(val showId: Long) : AddShowEvent()
    data class Error(val message: String) : AddShowEvent()
}

@HiltViewModel
class AddShowViewModel @Inject constructor(
    private val repository: ShowRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchState: StateFlow<UiState<List<TmdbSearchResult>>> = query
        .debounce(400)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(UiState.Content(emptyList()))
            } else {
                flow {
                    emit(UiState.Loading)
                    when (val result = repository.searchTmdb(q)) {
                        is AppResult.Success -> emit(UiState.Content(result.value))
                        is AppResult.Error -> emit(UiState.FatalError(result.error.toUserMessage()))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Content(emptyList()))

    private val _addingTmdbId = MutableStateFlow<Long?>(null)
    val addingTmdbId: StateFlow<Long?> = _addingTmdbId

    private val _events = MutableSharedFlow<AddShowEvent>()
    val events: SharedFlow<AddShowEvent> = _events

    fun addShow(tmdbId: Long, startingSeason: Int, startingEpisode: Int) {
        if (_addingTmdbId.value != null) return
        _addingTmdbId.value = tmdbId
        viewModelScope.launch {
            when (val result = repository.addShow(tmdbId, startingSeason, startingEpisode)) {
                is AppResult.Success -> _events.emit(AddShowEvent.NavigateToShowDetail(result.value))
                is AppResult.Error -> _events.emit(AddShowEvent.Error(result.error.toUserMessage()))
            }
            _addingTmdbId.value = null
        }
    }
}
