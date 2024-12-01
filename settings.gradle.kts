@file:Suppress("UnstableApiUsage")

rootProject.name = "kotlin-leveldb"

pluginManagement {
    plugins {
        val kotlinVersion = "2.1.0"
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.android.library") version "8.7.2"
        id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
        id("org.jetbrains.dokka") version "2.0.0-Beta"
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

include(":testKmpApp", ":benchmarks")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")