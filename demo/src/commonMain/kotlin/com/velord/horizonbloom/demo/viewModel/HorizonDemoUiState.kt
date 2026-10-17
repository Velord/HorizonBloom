package com.velord.horizonbloom.demo.viewModel

import com.velord.horizonbloom.HorizonBloomConfig

internal data class HorizonDemoUiState(val config: HorizonBloomConfig) {

    companion object {
        val INIT = HorizonDemoUiState(config = HorizonBloomConfig.DAWN)
    }
}
