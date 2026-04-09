package com.gifit.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun IntervalSlider(
    intervalMs: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = intervalMs / 1000f

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Frame delay: ${"%.1f".format(seconds)}s",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = seconds,
            onValueChange = { onIntervalChange((it * 1000).roundToInt()) },
            valueRange = 0.1f..3.0f,
            steps = 28, // 0.1s increments: (3.0 - 0.1) / 0.1 - 1 = 28
            modifier = Modifier.fillMaxWidth()
        )
    }
}
