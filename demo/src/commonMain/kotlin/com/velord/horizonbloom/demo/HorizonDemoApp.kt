package com.velord.horizonbloom.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velord.horizonbloom.demo.viewModel.HorizonDemoVM

@Composable
fun HorizonDemoApp(onBackClick: () -> Unit) {
    val viewModel = viewModel { HorizonDemoVM() }
    HorizonDemoScreen(
        viewModel = viewModel,
        onBackClick = onBackClick,
    )
}

@Preview
@Composable
private fun Preview() {
    HorizonDemoApp(onBackClick = {})
}
