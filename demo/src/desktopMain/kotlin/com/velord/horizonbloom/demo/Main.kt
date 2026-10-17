package com.velord.horizonbloom.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velord.horizonbloom.demo.viewModel.HorizonDemoVM

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Horizon Bloom",
    ) {
        MaterialTheme {
            val viewModel = viewModel { HorizonDemoVM() }
            HorizonDemoScreen(
                viewModel = viewModel,
                onBackClick = ::exitApplication,
            )
        }
    }
}
