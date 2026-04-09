package com.gifit.app.navigation

sealed class Screen {
    data object Home : Screen()
    data object Preview : Screen()
}
