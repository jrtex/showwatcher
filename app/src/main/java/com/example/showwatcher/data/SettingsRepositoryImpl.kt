package com.example.showwatcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override val sortOrder: Flow<SortOrder> =
        context.settingsDataStore.data.map { prefs -> SortOrder.fromStorageKey(prefs[SORT_ORDER_KEY]) }

    override val viewMode: Flow<ViewMode> =
        context.settingsDataStore.data.map { prefs -> ViewMode.fromStorageKey(prefs[VIEW_MODE_KEY]) }

    override suspend fun setSortOrder(order: SortOrder) {
        context.settingsDataStore.edit { it[SORT_ORDER_KEY] = order.storageKey }
    }

    override suspend fun setViewMode(mode: ViewMode) {
        context.settingsDataStore.edit { it[VIEW_MODE_KEY] = mode.storageKey }
    }
}
