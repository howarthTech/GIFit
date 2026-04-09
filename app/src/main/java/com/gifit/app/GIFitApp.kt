package com.gifit.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gifit.app.model.GifSettings
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
        var gifSettings by remember { mutableStateOf(GifSettings()) }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == Screen.Preview) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    onNavigateToPreview = { frames, settings ->
                        photoFrames = frames
                        gifSettings = settings
                        currentScreen = Screen.Preview
                    }
                )
                Screen.Preview -> PreviewScreen(
                    photoFrames = photoFrames,
                    gifSettings = gifSettings,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
