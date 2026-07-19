package com.example.showwatcher.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WatchProgressBar(watched: Int, total: Int, modifier: Modifier = Modifier) {
    val progress = if (total > 0) watched.toFloat() / total.toFloat() else 0f
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
    )
}
