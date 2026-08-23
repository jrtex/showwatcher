package com.example.showwatcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Collects a banner-only event stream and renders it via [EventBanner], auto-dismissing
 * non-error banners after 2.5s. `toBanner` maps an event to (message, isError), or `null` to
 * ignore it. Screens whose events also carry non-banner side effects (navigation, deletion)
 * keep handling those separately — this only owns the banner lifecycle.
 */
@Composable
fun <T> BannerHost(events: Flow<T>, toBanner: (T) -> Pair<String, Boolean>?, modifier: Modifier = Modifier) {
    var banner by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    LaunchedEffect(Unit) {
        events.collect { event -> toBanner(event)?.let { banner = it } }
    }

    LaunchedEffect(banner) {
        val current = banner
        if (current != null && !current.second) {
            delay(2500)
            banner = null
        }
    }

    banner?.let { (message, isError) ->
        EventBanner(
            message = message,
            isError = isError,
            onDismiss = { banner = null },
            modifier = modifier,
            shadowElevation = 4.dp,
        )
    }
}
