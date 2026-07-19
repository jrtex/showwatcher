package com.example.showwatcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.showwatcher.data.remote.TmdbImageUrlBuilder

@Composable
fun PosterImage(title: String, posterPath: String?, modifier: Modifier = Modifier) {
    val url = TmdbImageUrlBuilder.posterUrl(posterPath)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 28.sp,
            )
        }
    }
}
