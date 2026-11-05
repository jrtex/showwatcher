package com.example.showwatcher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.showwatcher.data.ViewMode

/** Top-bar "⋮" menu shared by the show-list tabs: view-mode toggle plus a link to Settings. */
@Composable
fun ShowsOverflowMenu(
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onSettingsClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Text("⋮") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ViewMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    trailingIcon = { if (mode == viewMode) Text("✓") },
                    onClick = {
                        expanded = false
                        onViewModeChange(mode)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    expanded = false
                    onSettingsClick()
                },
            )
        }
    }
}
