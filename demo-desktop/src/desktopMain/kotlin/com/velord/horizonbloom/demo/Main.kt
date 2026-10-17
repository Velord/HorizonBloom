package com.velord.horizonbloom.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Horizon Bloom",
    ) {
        MaterialTheme {
            HorizonDemoApp(onBackClick = ::exitApplication)
        }
    }
}
