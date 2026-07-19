package com.example.showwatcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ShowCard(
    title: String,
    posterPath: String?,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        PosterImage(
            title = title,
            posterPath = posterPath,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
        )
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            footer()
        }
    }
}
