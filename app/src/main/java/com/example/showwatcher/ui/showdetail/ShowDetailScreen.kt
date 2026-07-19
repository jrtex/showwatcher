package com.example.showwatcher.ui.showdetail

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.example.showwatcher.data.local.EpisodeEntity
import com.example.showwatcher.data.local.ShowStatus
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.components.ConfirmDialog
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.EventBanner
import com.example.showwatcher.ui.components.PosterImage
import com.example.showwatcher.ui.components.WatchProgressBar

@Composable
fun ShowDetailScreen(
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShowDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var banner by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ShowDetailEvent.Banner -> banner = event.message to false
                is ShowDetailEvent.Error -> banner = event.message to true
                ShowDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Remove show",
            message = "This deletes the show and all its watch history. This can't be undone.",
            confirmLabel = "Remove",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteShow()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    when (val current = state) {
        is UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.FatalError -> EmptyState(current.message, modifier = modifier)
        is UiState.Content -> {
            val (show, episodes) = current.data
            val isArchived = show.status == ShowStatus.ARCHIVED
            val watchedCount = episodes.count { it.watched }

            Column(modifier = modifier.fillMaxSize()) {
                banner?.let { (message, isError) ->
                    EventBanner(message = message, isError = isError, onDismiss = { banner = null })
                }

                Row(modifier = Modifier.padding(16.dp)) {
                    PosterImage(
                        title = show.title,
                        posterPath = show.posterPath,
                        modifier = Modifier.size(width = 100.dp, height = 150.dp),
                    )
                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(show.title, style = MaterialTheme.typography.titleLarge)
                        Text("Season ${show.currentSeason} — $watchedCount/${episodes.size}")
                        WatchProgressBar(watched = watchedCount, total = episodes.size)
                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            if (!isArchived) {
                                OutlinedButton(onClick = { viewModel.refresh() }, enabled = !isRefreshing) {
                                    Text(if (isRefreshing) "Refreshing…" else "Refresh")
                                }
                            }
                            Button(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text("Remove show")
                            }
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(episodes, key = { it.id }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            enabled = !isArchived,
                            onToggle = { viewModel.toggle(episode.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: EpisodeEntity, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = episode.watched, onCheckedChange = { if (enabled) onToggle() }, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.name}",
                textDecoration = if (episode.watched) TextDecoration.LineThrough else TextDecoration.None,
            )
            episode.airDate?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
