package com.example.showwatcher.ui.upcoming

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.ShowCard
import java.text.SimpleDateFormat
import java.util.Locale

private val isoParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val displayFormatter = SimpleDateFormat("MMM d, yyyy", Locale.US)

private fun formatAirDate(airDate: String?): String {
    if (airDate == null) return "Release date TBA"
    val parsed = runCatching { isoParser.parse(airDate) }.getOrNull() ?: return "Premieres $airDate"
    return "Premieres ${displayFormatter.format(parsed)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    onShowClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        // The outer ShowWatcherNavHost Scaffold already pads this screen below the status bar,
        // so zero the insets here to avoid reserving that space a second time.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Upcoming") },
                actions = {
                    IconButton(onClick = onSettingsClick) { Text("⋮") }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        when (val current = state) {
            is UiState.Loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Content -> {
                if (current.data.isEmpty()) {
                    EmptyState(
                        "No upcoming shows — shows you add before their season premieres will appear here.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        items(current.data, key = { it.show.id }) { item ->
                            ShowCard(
                                title = item.show.title,
                                posterPath = item.show.posterPath,
                                onClick = { onShowClick(item.show.id) },
                                modifier = Modifier.padding(6.dp),
                                footer = {
                                    Text("Season ${item.show.currentSeason}")
                                    Text(formatAirDate(item.firstEpisodeAirDate))
                                },
                            )
                        }
                    }
                }
            }
            is UiState.FatalError -> EmptyState(current.message, modifier = Modifier.padding(padding))
        }
    }
}
