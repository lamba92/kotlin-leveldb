@file:Suppress("UnstableApiUsage")

rootProject.name = "kotlin-leveldb"

pluginManagement {
    plugins {
        val kotlinVersion = "2.0.20"
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.android.library") version "8.7.2"
    }
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("com.gradle.develocity") version "3.17.6"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing {
            onlyIf { System.getenv("CI") == "true" }
        }
    }
}
