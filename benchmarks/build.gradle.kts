import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {

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
    val dbPath = layout.buildDirectory
        .dir("leveldb")
        .get()
        .asFile
        .absolutePath

    withType<Exec> {
        environment("DB_PATH", dbPath)
        environment(
            "JSON_OUTPUT_PATH",
            layout.buildDirectory
                .file("benchmark/data.json")
                .get()
                .asFile
                .toPath()
                .apply { parent.createDirectories() }
                .absolutePathString()
        )
        environment(
            "TABLE_OUTPUT_PATH",
            layout.buildDirectory
                .file("benchmark/table.txt")
                .get()
                .asFile
                .toPath()
                .apply { parent.createDirectories() }
                .absolutePathString()
        )
    }
    register("runBenchmark") {
        val mode = when {
            project.properties["leveldb.release"] == "true" -> "Release"
            else -> "Debug"
        }
        val os = OperatingSystem.current()
        val task = when {
            os.isWindows -> "run${mode}ExecutableMingwX64"
            os.isMacOsX -> "run${mode}ExecutableMacosArm64"
            os.isLinux -> "run${mode}ExecutableLinuxX64"
            else -> error("Unknown OS ${os.name}")
        }
        dependsOn(task)
    }
}