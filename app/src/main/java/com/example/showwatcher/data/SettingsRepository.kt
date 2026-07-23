package com.example.showwatcher.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val sortOrder: Flow<SortOrder>

    suspend fun setSortOrder(order: SortOrder)
}
