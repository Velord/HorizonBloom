plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.velord.horizonbloom.demo.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.velord.horizonbloom.demo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Module UI
    implementation(project(":demo"))
    // Compose
    implementation(libs.bundles.compose.all)
    // AndroidX
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
