package com.example.showwatcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.showwatcher.ui.common.ShowListEntry

/**
 * Renders [entries] into a LazyColumn: headers become [SectionDivider]s, rows are drawn by
 * [row], with a thin rule between consecutive rows (but not directly under a header).
 */
fun <T> LazyListScope.showListEntries(
    entries: List<ShowListEntry<T>>,
    row: @Composable (T) -> Unit,
) {
    itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
        when (entry) {
            is ShowListEntry.Header -> SectionDivider(entry.label, Modifier.padding(horizontal = 10.dp))
            is ShowListEntry.Row -> Column {
                if (index > 0 && entries[index - 1] is ShowListEntry.Row) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                row(entry.item)
            }
        }
    }
}
