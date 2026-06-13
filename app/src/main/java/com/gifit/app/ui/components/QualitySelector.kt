package com.gifit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gifit.app.model.QuantizerType

@Composable
fun QualitySelector(
    selected: QuantizerType,
    onSelect: (QuantizerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Color quality",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selected == QuantizerType.MEDIAN_CUT,
                onClick = { onSelect(QuantizerType.MEDIAN_CUT) },
                label = { Text("Fast") }
            )
            FilterChip(
                selected = selected == QuantizerType.NEUQUANT,
                onClick = { onSelect(QuantizerType.NEUQUANT) },
                label = { Text("Best") }
            )
        }
    }
}
