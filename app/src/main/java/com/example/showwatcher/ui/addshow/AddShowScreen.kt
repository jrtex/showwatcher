package com.example.showwatcher.ui.addshow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.showwatcher.data.remote.TmdbSearchResult
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.components.EmptyState
import com.example.showwatcher.ui.components.EventBanner
import com.example.showwatcher.ui.components.PosterImage

@Composable
fun AddShowScreen(
    onShowAdded: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddShowViewModel = hiltViewModel(),
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val addingTmdbId by viewModel.addingTmdbId.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddShowEvent.NavigateToShowDetail -> onShowAdded(event.showId)
                is AddShowEvent.Error -> errorMessage = event.message
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onQueryChange(it)
            },
            label = { Text("Search TMDB") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        errorMessage?.let { message ->
            EventBanner(message = message, onDismiss = { errorMessage = null }, isError = true)
        }

        when (val current = searchState) {
            is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            is UiState.Content -> {
                if (query.isNotBlank() && current.data.isEmpty()) {
                    EmptyState("No results for \"$query\"")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(current.data, key = { it.tmdbId }) { result ->
                            SearchResultRow(
                                result = result,
                                isAdding = addingTmdbId == result.tmdbId,
                                addDisabled = addingTmdbId != null,
                                onConfirmAdd = { season, episode ->
                                    viewModel.addShow(result.tmdbId, season, episode)
                                },
                            )
                        }
                    }
                }
            }
            is UiState.FatalError -> EmptyState(current.message)
        }
    }
}

@Composable
private fun SearchResultRow(
    result: TmdbSearchResult,
    isAdding: Boolean,
    addDisabled: Boolean,
    onConfirmAdd: (season: Int, episode: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var season by remember { mutableIntStateOf(1) }
    var watchedUpTo by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row {
            PosterImage(
                title = result.title,
                posterPath = result.posterPath,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = result.year?.let { "${result.title} ($it)" } ?: result.title)
                if (!expanded) {
                    Button(onClick = { expanded = true }, enabled = !addDisabled) {
                        Text(if (isAdding) "Adding…" else "Add")
                    }
                }
            }
        }
        if (expanded) {
            OutlinedTextField(
                value = season.toString(),
                onValueChange = { season = it.toIntOrNull() ?: season },
                label = { Text("Season") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = watchedUpTo.toString(),
                onValueChange = { watchedUpTo = it.toIntOrNull() ?: watchedUpTo },
                label = { Text("Watched up to episode") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onConfirmAdd(season, watchedUpTo) },
                enabled = !isAdding && !addDisabled,
            ) {
                Text(if (isAdding) "Adding…" else "Start tracking")
            }
        }
    }
}
