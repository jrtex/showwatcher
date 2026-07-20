package com.example.showwatcher.ui.addshow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.showwatcher.data.AppResult
import com.example.showwatcher.data.remote.TmdbSearchResult
import com.example.showwatcher.ui.common.UiState
import com.example.showwatcher.ui.common.toUserMessage
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
                                fetchSeasons = { viewModel.fetchSeasons(result.tmdbId) },
                                onConfirmAdd = { season ->
                                    viewModel.addShow(result.tmdbId, season)
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
    fetchSeasons: suspend () -> AppResult<List<Int>>,
    onConfirmAdd: (season: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var seasonsState by remember { mutableStateOf<UiState<List<Int>>?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(expanded, retryTrigger) {
        if (expanded && seasonsState !is UiState.Content) {
            seasonsState = UiState.Loading
            seasonsState = when (val outcome = fetchSeasons()) {
                is AppResult.Success -> {
                    selectedSeason = outcome.value.firstOrNull()
                    UiState.Content(outcome.value)
                }
                is AppResult.Error -> UiState.FatalError(outcome.error.toUserMessage())
            }
        }
    }

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
            Text(text = "Starting season", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            when (val current = seasonsState) {
                null, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                is UiState.FatalError -> EventBanner(
                    message = current.message,
                    onDismiss = { retryTrigger++ },
                    isError = true,
                )
                is UiState.Content -> {
                    if (current.data.isEmpty()) {
                        EmptyState("No seasons found for this show")
                    } else {
                        FlowRow(modifier = Modifier.fillMaxWidth()) {
                            for (season in current.data) {
                                SeasonButton(
                                    number = season,
                                    selected = season == selectedSeason,
                                    onClick = { selectedSeason = season },
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { selectedSeason?.let(onConfirmAdd) },
                enabled = selectedSeason != null && !isAdding && !addDisabled,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (isAdding) "Adding…" else "Start tracking")
            }
        }
    }
}

@Composable
private fun SeasonButton(number: Int, selected: Boolean, onClick: () -> Unit) {
    val modifier = Modifier.size(48.dp).padding(4.dp)
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = number.toString())
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = number.toString())
        }
    }
}
