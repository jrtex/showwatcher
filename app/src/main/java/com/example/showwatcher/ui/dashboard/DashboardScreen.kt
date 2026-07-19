package com.example.showwatcher.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.showwatcher.data.local.ShowWithProgress
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.ShowCard
import com.example.showwatcher.ui.components.WatchProgressBar

@Composable
fun DashboardScreen(
    onShowClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        is UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Content -> {
            if (current.data.isEmpty()) {
                EmptyState("No active shows yet — tap + to add one", modifier = modifier)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                ) {
                    items(current.data, key = { it.show.id }) { item: ShowWithProgress ->
                        ShowCard(
                            title = item.show.title,
                            posterPath = item.show.posterPath,
                            onClick = { onShowClick(item.show.id) },
                            modifier = Modifier.padding(6.dp),
                            footer = {
                                Text("Season ${item.show.currentSeason} — ${item.watchedCount}/${item.totalCount}")
                                WatchProgressBar(watched = item.watchedCount, total = item.totalCount)
                            },
                        )
                    }
                }
            }
        }
        is UiState.FatalError -> EmptyState(current.message, modifier = modifier)
    }
}
