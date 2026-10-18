plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.multiplatform.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

group = "io.github.velord.horizonbloom"
version = providers.gradleProperty("RELEASE_VERSION").orElse("0.1.1-SNAPSHOT").get()

kotlin {
    android {
        namespace = "com.velord.horizonbloom.demo"
        compileSdk = 37
        minSdk = 26
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            // Module Core
            implementation(project(":horizon-bloom"))
            // Compose
            implementation(libs.bundles.compose.all)
            // Lifecycle
            implementation(libs.bundles.androidx.lifecycle.all)
            // Kotlin
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "demo", version.toString())
    publishToMavenCentral()
    signAllPublications()

    pom {
        name = "Horizon Bloom Demo"
        description = "An interactive Compose Multiplatform editor for Horizon Bloom."
        inceptionYear = "2026"
        url = "https://github.com/Velord/HorizonBloom"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "Velord"
                name = "Velord"
                providers.gradleProperty("POM_DEVELOPER_EMAIL").orNull?.let { publicEmail ->
                    email = publicEmail
                }
                organization = "Velord"
                organizationUrl = "https://github.com/Velord"
                url = "https://github.com/Velord"
            }
        }
        scm {
            url = "https://github.com/Velord/HorizonBloom"
            connection = "scm:git:https://github.com/Velord/HorizonBloom.git"
            developerConnection = "scm:git:ssh://git@github.com/Velord/HorizonBloom.git"
        }
    }
}
