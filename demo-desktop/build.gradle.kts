plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.multiplatform.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        getByName("desktopMain") {
            dependencies {
                // Module UI
                implementation(project(":demo"))
                // Compose
                implementation(libs.bundles.compose.all)
                implementation(compose.desktop.currentOs)
                // Kotlin
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.velord.horizonbloom.demo.MainKt"
    }
}
