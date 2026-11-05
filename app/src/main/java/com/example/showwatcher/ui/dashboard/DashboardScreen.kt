package com.example.showwatcher.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.ui.common.RefreshEvent
import com.example.showwatcher.ui.common.ShowListEntry
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.asListEntries
import com.example.showwatcher.ui.common.episodeLine
import com.example.showwatcher.ui.common.withMonthHeaders
import com.example.showwatcher.ui.components.BannerHost
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.SectionDivider
import com.example.showwatcher.ui.components.ShowListRow
import com.example.showwatcher.ui.components.ShowsOverflowMenu
import com.example.showwatcher.ui.components.showListEntries
import com.example.showwatcher.ui.components.ShowCard
import com.example.showwatcher.ui.components.WatchProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onShowClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
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
                title = { Text("Active") },
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
                        val data = current.data
                        if (data.all.isEmpty()) {
                            EmptyState("No active shows yet — tap + to add one")
                        } else if (viewMode == ViewMode.LIST) {
                            DashboardList(data, showMonthHeaders, onShowClick)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                            ) {
                                items(data.toWatch, key = { it.show.id }) { item ->
                                    DashboardShowCard(item, dimmed = false, onShowClick = onShowClick)
                                }
                                if (data.caughtUp.isNotEmpty()) {
                                    item(key = "caught-up-header", span = { GridItemSpan(maxLineSpan) }) {
                                        SectionDivider("Caught-up")
                                    }
                                    items(data.caughtUp, key = { it.show.id }) { item ->
                                        DashboardShowCard(item, dimmed = true, onShowClick = onShowClick)
                                    }
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

@Composable
private fun DashboardShowCard(item: ShowWithProgress, dimmed: Boolean, onShowClick: (Long) -> Unit) {
    ShowCard(
        title = item.show.title,
        posterPath = item.show.posterPath,
        onClick = { onShowClick(item.show.id) },
        modifier = Modifier.padding(6.dp),
        dimmed = dimmed,
        footer = {
            Text("Season ${item.show.currentSeason} — ${item.watchedCount}/${item.totalCount}")
            WatchProgressBar(watched = item.watchedCount, total = item.totalCount)
        },
    )
}

@Composable
private fun DashboardList(data: DashboardShows, showMonthHeaders: Boolean, onShowClick: (Long) -> Unit) {
    fun entries(shows: List<ShowWithProgress>, prefix: String): List<ShowListEntry<ShowWithProgress>> =
        if (showMonthHeaders) shows.withMonthHeaders(prefix, itemKey = { it.show.id }, date = { it.nextEpisodeAirDate })
        else shows.asListEntries { it.show.id }

    val toWatch = entries(data.toWatch, "towatch")
    val caughtUp = entries(data.caughtUp, "caughtup")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        showListEntries(toWatch) { item -> DashboardListRow(item, dimmed = false, onShowClick = onShowClick) }
        if (caughtUp.isNotEmpty()) {
            item(key = "caught-up-header") { SectionDivider("Caught-up") }
            showListEntries(caughtUp) { item -> DashboardListRow(item, dimmed = true, onShowClick = onShowClick) }
        }
    }
}

@Composable
private fun DashboardListRow(item: ShowWithProgress, dimmed: Boolean, onShowClick: (Long) -> Unit) {
    ShowListRow(
        title = item.show.title,
        subtitle = episodeLine(
            season = item.nextEpisodeSeasonNumber,
            episode = item.nextEpisodeNumber,
            name = item.nextEpisodeName,
            airDate = item.nextEpisodeAirDate,
            fallback = "All caught up",
        ),
        posterPath = item.show.posterPath,
        onClick = { onShowClick(item.show.id) },
        dimmed = dimmed,
    )
}
