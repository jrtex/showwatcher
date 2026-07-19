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
import androidx.compose.material3.MaterialTheme
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
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.PosterImage

@Composable
fun ArchiveDetailScreen(
    onReactivated: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val seasonEpisodes by viewModel.seasonEpisodes.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ArchiveDetailEvent.NavigateToShowDetail -> onReactivated(event.showId)
            }
        }
    }

    when (val current = state) {
        is UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.FatalError -> EmptyState(current.message, modifier = modifier)
        is UiState.Content -> {
            val (show, seasonNumbers) = current.data
            Column(modifier = modifier.fillMaxSize()) {
                Row(modifier = Modifier.padding(16.dp)) {
                    PosterImage(
                        title = show.title,
                        posterPath = show.posterPath,
                        modifier = Modifier.size(width = 100.dp, height = 150.dp),
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(show.title, style = MaterialTheme.typography.titleLarge)
                        Text("${show.currentSeason} season(s) completed")
                        Button(onClick = { viewModel.reactivate() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Reactivate")
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
