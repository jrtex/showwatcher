package com.example.showwatcher.ui.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.showwatcher.data.ViewMode
import com.example.showwatcher.ui.common.RefreshEvent
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.asListEntries
import com.example.showwatcher.ui.common.episodeLine
import com.example.showwatcher.ui.common.withMonthHeaders
import com.example.showwatcher.ui.components.BannerHost
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.ShowCard
import com.example.showwatcher.ui.components.ShowListRow
import com.example.showwatcher.ui.components.ShowsOverflowMenu
import com.example.showwatcher.ui.components.showListEntries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onShowClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val showMonthHeaders by viewModel.showMonthHeaders.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        // The outer ShowWatcherNavHost Scaffold already pads this screen below the status bar,
        // so zero the insets here to avoid reserving that space a second time.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                actions = {
                    ShowsOverflowMenu(
                        viewMode = viewMode,
                        onViewModeChange = viewModel::setViewMode,
                        onSettingsClick = onSettingsClick,
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                when (val current = state) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Content -> {
                        if (current.data.isEmpty()) {
                            EmptyState("No archived shows yet — finish a season to see it here.")
                        } else if (viewMode == ViewMode.LIST) {
                            val entries = if (showMonthHeaders) {
                                current.data.withMonthHeaders("shows", itemKey = { it.show.id }, date = { it.lastWatchedEpisodeAirDate })
                            } else {
                                current.data.asListEntries { it.show.id }
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                            ) {
                                showListEntries(entries) { item ->
                                    ShowListRow(
                                        title = item.show.title,
                                        subtitle = episodeLine(
                                            season = item.lastWatchedSeasonNumber,
                                            episode = item.lastWatchedEpisodeNumber,
                                            name = item.lastWatchedEpisodeName,
                                            airDate = item.lastWatchedEpisodeAirDate,
                                            fallback = "No episodes watched",
                                        ),
                                        posterPath = item.show.posterPath,
                                        onClick = { onShowClick(item.show.id) },
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                            ) {
                                items(current.data, key = { it.show.id }) { item ->
                                    ShowCard(
                                        title = item.show.title,
                                        posterPath = item.show.posterPath,
                                        onClick = { onShowClick(item.show.id) },
                                        modifier = Modifier.padding(6.dp),
                                        footer = { Text("${item.show.currentSeason} season(s) completed") },
                                    )
                                }
                            }
                        }
                    }
                    is UiState.FatalError -> EmptyState(current.message)
                }

                BannerHost(
                    events = viewModel.events,
                    toBanner = { event ->
                        when (event) {
                            is RefreshEvent.Banner -> event.message to false
                            is RefreshEvent.Error -> event.message to true
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}
