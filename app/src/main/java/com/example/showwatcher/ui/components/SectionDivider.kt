package com.example.showwatcher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A centered label between two horizontal rules, e.g. "──── Caught-up ────". */
@Composable
fun SectionDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}
