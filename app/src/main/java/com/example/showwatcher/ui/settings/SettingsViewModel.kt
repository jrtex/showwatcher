package com.example.showwatcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showwatcher.data.SettingsRepository
import com.example.showwatcher.data.SortOrder
import com.example.showwatcher.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<UiState<SortOrder>> =
        settingsRepository.sortOrder
            .map<SortOrder, UiState<SortOrder>> { UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onSortOrderSelected(order: SortOrder) {
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }
}
