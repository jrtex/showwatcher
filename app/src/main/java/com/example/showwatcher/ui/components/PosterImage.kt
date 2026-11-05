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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.showwatcher.data.remote.TmdbImageUrlBuilder

/** Saturation kept for dimmed posters (0 = grayscale, 1 = full colour). */
private const val DIMMED_SATURATION = 0.2f

/** Opacity of dimmed posters. */
private const val DIMMED_ALPHA = 0.85f

private val dimmedColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(DIMMED_SATURATION) })

@Composable
fun PosterImage(
    title: String,
    posterPath: String?,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val url = TmdbImageUrlBuilder.posterUrl(posterPath)
    val alphaModifier = if (dimmed) Modifier.alpha(DIMMED_ALPHA) else Modifier
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            colorFilter = if (dimmed) dimmedColorFilter else null,
            modifier = modifier.then(alphaModifier).clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .then(alphaModifier)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (dimmed) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer,
                )
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.firstOrNull()?.uppercase() ?: "?",
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 28.sp,
            )
        }
    }
}
