@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {

    jvm {
        mainRun {
            mainClass = "com.github.lamba92.leveldb.benchmarks.MainKt"
        }
    }
    mingwX64()
    linuxX64()
    macosArm64()

    applyDefaultHierarchyTemplate()

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable {
                entryPoint = "com.github.lamba92.leveldb.benchmarks.main"
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
    val dbPath =
        layout.buildDirectory
            .dir("leveldb")
            .get()
            .asFile
            .absolutePath

    val jsonOutputPath =
        layout.buildDirectory
            .file("benchmark/data.json")
            .get()
            .asFile
            .toPath()
            .apply { parent.createDirectories() }
            .absolutePathString()

    val tableOutputPath =
        layout.buildDirectory
            .file("benchmark/table.txt")
            .get()
            .asFile
            .toPath()
            .apply { parent.createDirectories() }
            .absolutePathString()

    withType<Exec> {
        environment("DB_PATH", dbPath)
        environment("JSON_OUTPUT_PATH", jsonOutputPath)
        environment("TABLE_OUTPUT_PATH", tableOutputPath)
    }

    withType<JavaExec> {
        environment("DB_PATH", dbPath)
        environment("JSON_OUTPUT_PATH", jsonOutputPath)
        environment("TABLE_OUTPUT_PATH", tableOutputPath)
    }

    register("runNativeBenchmark") {
        val os = OperatingSystem.current()
        val task =
            when {
                os.isWindows -> "runReleaseExecutableMingwX64"
                os.isMacOsX -> "runReleaseExecutableMacosArm64"
                os.isLinux -> "runReleaseExecutableLinuxX64"
                else -> error("Unknown OS ${os.name}")
            }
        dependsOn(task)
    }
}
