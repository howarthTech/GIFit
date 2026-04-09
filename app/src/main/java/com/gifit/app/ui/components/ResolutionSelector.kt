package com.gifit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gifit.app.model.ResolutionPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResolutionSelector(
    selected: ResolutionPreset,
    onSelect: (ResolutionPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Resolution",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ResolutionPreset.entries.forEach { preset ->
                FilterChip(
                    selected = selected == preset,
                    onClick = { onSelect(preset) },
                    label = { Text(preset.label) }
                )
            }
        }
    }
}
