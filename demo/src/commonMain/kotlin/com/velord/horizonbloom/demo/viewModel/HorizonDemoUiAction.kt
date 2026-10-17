package com.velord.horizonbloom.demo.viewModel

import androidx.compose.ui.graphics.Color

internal sealed interface HorizonDemoUiAction {
    data object Reset : HorizonDemoUiAction
    data object SelectDawn : HorizonDemoUiAction
    data object SelectTwilight : HorizonDemoUiAction
    data object SelectAurora : HorizonDemoUiAction
    data class ChangeHorizon(val value: Float) : HorizonDemoUiAction
    data class ChangeCurvature(val value: Float) : HorizonDemoUiAction
    data class ChangeTilt(val value: Float) : HorizonDemoUiAction
    data class ChangeSunPosition(val value: Float) : HorizonDemoUiAction
    data class ChangeSunHeight(val value: Float) : HorizonDemoUiAction
    data class ChangeSunSize(val value: Float) : HorizonDemoUiAction
    data class ChangeSunrise(val value: Float) : HorizonDemoUiAction
    data class ChangeSpread(val value: Float) : HorizonDemoUiAction
    data class ChangeAtmosphere(val value: Float) : HorizonDemoUiAction
    data class ChangeThickness(val value: Float) : HorizonDemoUiAction
    data class ChangeRim(val value: Float) : HorizonDemoUiAction
    data class ChangeFlare(val value: Float) : HorizonDemoUiAction
    data class ChangeSunburst(val value: Float) : HorizonDemoUiAction
    data class SetSunburstCount(val value: Int) : HorizonDemoUiAction
    data class ChangeAirglow(val value: Float) : HorizonDemoUiAction
    data class ChangeClouds(val value: Float) : HorizonDemoUiAction
    data class ChangeStars(val value: Float) : HorizonDemoUiAction
    data class ChangeAurora(val value: Float) : HorizonDemoUiAction
    data class ChangeAuroraSpeed(val value: Float) : HorizonDemoUiAction
    data class ChangeSpeed(val value: Float) : HorizonDemoUiAction
    data class ChangeDrift(val value: Float) : HorizonDemoUiAction
    data class ChangeParallax(val value: Float) : HorizonDemoUiAction
    data class ChangeBloom(val value: Float) : HorizonDemoUiAction
    data class ChangeGrain(val value: Float) : HorizonDemoUiAction
    data class ChangeWarmColor(val value: Color) : HorizonDemoUiAction
    data class ChangeCoolColor(val value: Color) : HorizonDemoUiAction
    data class ChangeBackground(val value: Color) : HorizonDemoUiAction
    data class SetAutoAurora(val value: Boolean) : HorizonDemoUiAction
    data class SetInteractive(val value: Boolean) : HorizonDemoUiAction
    data class SetIntro(val value: Boolean) : HorizonDemoUiAction
    data class SetPaused(val value: Boolean) : HorizonDemoUiAction
}
