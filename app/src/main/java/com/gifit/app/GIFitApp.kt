package com.gifit.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gifit.app.model.PhotoFrame
import com.gifit.app.navigation.Screen
import com.gifit.app.ui.screens.home.HomeScreen
import com.gifit.app.ui.screens.preview.PreviewScreen
import com.gifit.app.ui.theme.GIFitTheme

@Composable
fun GIFitApp() {
    GIFitTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        var photoFrames by remember { mutableStateOf<List<PhotoFrame>>(emptyList()) }
        var intervalMs by remember { mutableIntStateOf(500) }
        var overlayText by remember { mutableStateOf("") }

        when (currentScreen) {
            Screen.Home -> HomeScreen(
                onNavigateToPreview = { frames, interval, text ->
                    photoFrames = frames
                    intervalMs = interval
                    overlayText = text
                    currentScreen = Screen.Preview
                }
            )
            Screen.Preview -> PreviewScreen(
                photoFrames = photoFrames,
                intervalMs = intervalMs,
                overlayText = overlayText,
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
    }
}
