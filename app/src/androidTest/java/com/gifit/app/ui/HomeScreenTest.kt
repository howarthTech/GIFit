package com.gifit.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.gifit.app.model.GifSettings
import com.gifit.app.model.PhotoFrame
import com.gifit.app.ui.screens.home.HomeScreen
import com.gifit.app.ui.theme.GIFitTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun emptyState_showsCreateYourGif() {
        composeTestRule.setContent {
            GIFitTheme {
                HomeScreen(
                    onNavigateToPreview = { _: List<PhotoFrame>, _: GifSettings -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Create your GIF").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Photos").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsAddAtLeast2Photos() {
        composeTestRule.setContent {
            GIFitTheme {
                HomeScreen(
                    onNavigateToPreview = { _: List<PhotoFrame>, _: GifSettings -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Add at least 2 photos to get started").assertIsDisplayed()
    }
}
