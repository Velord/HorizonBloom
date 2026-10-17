plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.multiplatform.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

group = "io.github.velord"
version = providers.gradleProperty("VERSION_NAME").orElse("0.1.0-SNAPSHOT").get()

kotlin {
    android {
        namespace = "com.velord.horizonbloom"
        compileSdk = 37
        minSdk = 26
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui.tooling.preview)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name = "Horizon Bloom"
        description = "A configurable Compose Multiplatform horizon light scene."
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
