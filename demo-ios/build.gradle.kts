plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.multiplatform.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "HorizonBloomDemo"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Module UI
            implementation(project(":demo"))
            // Compose
            implementation(libs.bundles.compose.all)
        }
    }
}
