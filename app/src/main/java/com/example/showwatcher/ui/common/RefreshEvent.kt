package com.example.showwatcher.ui.common

/** Shared banner-only event shape for the three list ViewModels' bulk `refreshAll()`. */
sealed class RefreshEvent {
    data class Banner(val message: String) : RefreshEvent()
    data class Error(val message: String) : RefreshEvent()
}
