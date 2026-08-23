package com.example.showwatcher.ui.archivedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.EventBanner
import com.example.showwatcher.ui.components.PosterImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDetailScreen(
    onReactivated: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val seasonEpisodes by viewModel.seasonEpisodes.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var banner by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ArchiveDetailEvent.NavigateToShowDetail -> onReactivated(event.showId)
                is ArchiveDetailEvent.Banner -> banner = event.message to false
                is ArchiveDetailEvent.Error -> banner = event.message to true
            }
        }
    }

    LaunchedEffect(banner) {
        val current = banner
        if (current != null && !current.second) {
            delay(2500)
            banner = null
        }
    }

    when (val current = state) {
        is UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.FatalError -> EmptyState(current.message, modifier = modifier)
        is UiState.Content -> {
            val (show, seasonNumbers) = current.data
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            PosterImage(
                                title = show.title,
                                posterPath = show.posterPath,
                                modifier = Modifier.size(width = 100.dp, height = 150.dp),
                            )
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                Text(show.title, style = MaterialTheme.typography.titleLarge)
                                Text("${show.currentSeason} season(s) completed")
                                Row(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedButton(onClick = { viewModel.refresh() }, enabled = !isRefreshing) {
                                        Text(if (isRefreshing) "Refreshing…" else "Refresh")
                                    }
                                    Button(
                                        onClick = { viewModel.reactivate() },
                                        modifier = Modifier.padding(start = 8.dp),
                                    ) {
                                        Text("Reactivate")
                                    }
                                }
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(seasonNumbers, key = { it }) { seasonNumber ->
                                SeasonAccordionRow(
                                    seasonNumber = seasonNumber,
                                    episodes = seasonEpisodes[seasonNumber],
                                    onExpand = { viewModel.loadSeasonIfNeeded(seasonNumber) },
                                )
                            }
                        }
                    }

                    banner?.let { (message, isError) ->
                        EventBanner(
                            message = message,
                            isError = isError,
                            onDismiss = { banner = null },
                            modifier = Modifier.align(Alignment.TopCenter),
                            shadowElevation = 4.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonAccordionRow(
    seasonNumber: Int,
    episodes: List<com.example.showwatcher.data.local.EpisodeEntity>?,
    onExpand: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                if (expanded) onExpand()
            }
            .padding(16.dp),
    ) {
        val watched = episodes?.count { it.watched } ?: 0
        val total = episodes?.size
        Text(
            text = "Season $seasonNumber" + if (total != null) " — $watched/$total watched" else "",
            style = MaterialTheme.typography.titleMedium,
        )
        if (expanded) {
            if (episodes == null) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                episodes.forEach { episode ->
                    Text(
                        text = "${episode.episodeNumber}. ${episode.name}",
                        textDecoration = if (episode.watched) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}
