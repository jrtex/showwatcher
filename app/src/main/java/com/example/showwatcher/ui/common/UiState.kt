package com.example.showwatcher.ui.common

/** Shared screen-state shape (ANDROID_HANDOFF.md's optimistic-then-authoritative pattern in §7.3). */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Content<T>(
        val data: T,
        val isRefreshing: Boolean = false,
        val inlineError: String? = null,
    ) : UiState<T>()
    data class FatalError(val message: String) : UiState<Nothing>()
}
