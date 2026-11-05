package com.example.showwatcher.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val sortOrder: Flow<SortOrder>

    val viewMode: Flow<ViewMode>

    suspend fun setSortOrder(order: SortOrder)

    suspend fun setViewMode(mode: ViewMode)
}
