@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        mainRun {
            mainClass = "com.github.lamba92.levelkt.app.MainKt"
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
                entryPoint = "com.github.lamba92.levelkt.app.main"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinLeveldb)
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
    }
}

tasks {
    val dbPath = layout.buildDirectory
        .dir("leveldb")
        .get()
        .asFile
        .absolutePath

    withType<Exec> {
        environment("DB_PATH", dbPath)
    }
    withType<JavaExec> {
        environment("DB_PATH", dbPath)
    }
}