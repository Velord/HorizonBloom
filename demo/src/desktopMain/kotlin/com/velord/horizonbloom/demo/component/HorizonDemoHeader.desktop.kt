package com.velord.horizonbloom.demo.component

import androidx.compose.runtime.Composable

@Composable
internal actual fun HorizonDemoHeader(
    title: String,
    onBackClick: () -> Unit,
) {
    HorizonDemoBackHeader(title = title, onBackClick = onBackClick)
}
