plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.multiplatform.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "com.velord.horizonbloom.demo"
        compileSdk = 37
        minSdk = 26
    }
    jvm("desktop")
    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "HorizonBloomDemo"
            isStatic = true
        }
    }
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":horizon-bloom"))
            implementation(libs.bundles.compose.all)
            implementation(libs.bundles.androidx.lifecycle.all)
            implementation(libs.kotlinx.coroutines.core)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
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
