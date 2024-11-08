import kotlin.io.path.absolutePathString
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

kotlin {

//    jvm()

    iosArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-ios-static-arm64.a")
    }

    iosX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-ios-static-simulator-x64.a")
    }

    iosSimulatorArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-ios-static-simulator-arm64.a")
    }

    tvosArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-tvos-static-simulator-x64.a")
    }

    tvosX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-tvos-static-x64.a")
    }

    tvosSimulatorArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-tvos-static-simulator-arm64.a")
    }

    watchosArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-watchos-static-arm64.a")
    }

    watchosX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-watchos-static-simulator-x64.a")
    }

    watchosSimulatorArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-watchos-static-simulator-arm64.a")
    }

    androidNativeX86 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-android-static-x86.a")
    }

    androidNativeX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-android-static-x86_64.a")
    }

    androidNativeArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-android-static-arm64.a")
    }

    linuxX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-linux-static-x64.a")
    }

    linuxArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-linux-static-arm64.a")
    }

    mingwX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-windows-static-x64.a")
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.cinterop.UnsafeNumber")
            }
        }
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
            }
        }
    }
}

publishing {
    repositories {
        maven(layout.buildDirectory.dir("repo")) {
            name = "test"
        }
    }
}

fun KotlinNativeTarget.registerLeveldbCinterop(
    libraryName: String,
    libraryFolder: String = rootProject.file("libs/static").absolutePath,
    packageName: String = "libleveldb",
    generateDefTaskName: String = "generate${libraryName.toCamelCase().capitalized()}DefFile",
    defFileName: String = "${libraryName.toCamelCase()}.def",
    action: CreateDefFileTask.() -> Unit = {},
) {

    val generateDefTask =
        tasks.register<CreateDefFileTask>(generateDefTaskName) {
            val headersDir = rootProject.file("libs/headers").toPath()
            headers.from(headersDir.resolve("leveldb/c.h").toFile())
            staticLibs.add(libraryName)
            defFile = layout.buildDirectory.file("generated/cinterop/$defFileName")
            compilerOpts.add("-I${headersDir.absolutePathString()}")
            libraryPaths.add(libraryFolder)
            action()
        }

    val compilation = compilations.getByName("main")

    compilation.compileTaskProvider {
        dependsOn(generateDefTask)
    }

    compilation.cinterops.register("libleveldb") {
        tasks.all {
            if (name == interopProcessingTaskName) {
                dependsOn(generateDefTask)
            }
        }
        this.packageName = packageName
        definitionFile.set(generateDefTask.flatMap { it.defFile })
    }
}

fun String.toCamelCase(): String {
    return this.split("[^A-Za-z0-9]+".toRegex())
        .joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
        .replaceFirstChar(Char::lowercase)
}

tasks {
    val directory = layout.buildDirectory
        .dir("testdb")
        .get()
        .asFile
        .toPath()
        .absolutePathString()

    withType<Exec> {
        environment("LEVELDB_LOCATION", directory)
    }
    withType<Test> {
        environment("LEVELDB_LOCATION", directory)
    }
    withType<KotlinNativeHostTest> {
        environment("LEVELDB_LOCATION", directory)
    }
}