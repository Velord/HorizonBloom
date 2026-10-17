package com.velord.horizonbloom.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velord.horizonbloom.demo.viewModel.HorizonDemoVM
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .safeDrawingPadding(),
        ) {
            val viewModel = viewModel { HorizonDemoVM() }
            HorizonDemoScreen(
                viewModel = viewModel,
                onBackClick = {},
            )
        }
    }
}
