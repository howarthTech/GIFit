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
import androidx.hilt.navigation.compose.hiltViewModel
import com.gifit.app.model.GifSettings
import com.gifit.app.model.PhotoFrame
import com.gifit.app.navigation.Screen
import com.gifit.app.ui.screens.home.HomeScreen
import com.gifit.app.ui.screens.home.HomeViewModel
import com.gifit.app.ui.screens.preview.PreviewScreen
import com.gifit.app.ui.theme.GIFitTheme

@Composable
fun GIFitApp() {
    GIFitTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        var photoFrames by remember { mutableStateOf<List<PhotoFrame>>(emptyList()) }
        var gifSettings by remember { mutableStateOf(GifSettings()) }

        // Hoisted so "New GIF" on the Preview screen can clear the home project too. Both
        // screens are backed by the same activity-scoped instance either way; taking it here
        // makes that sharing explicit rather than incidental.
        val homeViewModel: HomeViewModel = hiltViewModel()

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
                    viewModel = homeViewModel,
                    onNavigateToPreview = { frames, settings ->
                        photoFrames = frames
                        gifSettings = settings
                        currentScreen = Screen.Preview
                    }
                )
                Screen.Preview -> PreviewScreen(
                    photoFrames = photoFrames,
                    gifSettings = gifSettings,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onStartNewProject = {
                        homeViewModel.startNewProject()
                        photoFrames = emptyList()
                        gifSettings = gifSettings.copy(globalOverlayText = "")
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}
