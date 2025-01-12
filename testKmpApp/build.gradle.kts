@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvm {
        mainRun {
            mainClass = "com.github.lamba92.leveldb.app.MainKt"
        }
    }

    mingwX64()
    linuxX64()
    macosArm64()
    macosX64()

    applyDefaultHierarchyTemplate()

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable {
                entryPoint = "com.github.lamba92.leveldb.app.main"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinLeveldb)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}

tasks {
    val dbPath =
        layout.buildDirectory
            .dir("leveldb")
            .get()
            .asFile
            .absolutePath

    withType<Exec> {
        environment("DB_PATH", dbPath)
    }
    withType<JavaExec> {
        jvmArgs = listOf("-Djna.debug_load=true", "-Djna.debug_load.jna=true")
        environment("DB_PATH", dbPath)
    }
}
