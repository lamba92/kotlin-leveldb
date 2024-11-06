@file:Suppress("UnstableApiUsage")

rootProject.name = "kotlin-leveldb"

pluginManagement {
    plugins {
        val kotlinVersion = "2.0.20"
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}