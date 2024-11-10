import kotlin.io.path.absolutePathString
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    `maven-publish`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

android {
    namespace = "com.github.lamba92.levelkt"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res")
            jniLibs.srcDirs("src/androidMain/jniLibs")
        }
    }
}

kotlin {

    jvmToolchain(17)

    jvm()
    androidTarget()

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
        binaries {
            executable {
                entryPoint = "com.github.lamba92.levelkt.main"
            }
        }
    }

    linuxArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-linux-static-arm64.a")
    }

    macosArm64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-macos-static-arm64.a")
    }

    macosX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-macos-static-x64.a")
    }

    mingwX64 {
        registerLeveldbCinterop("libleveldb-weekly-2024-11-09-windows-static-x64.a")
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.cinterop.UnsafeNumber")
            }
        }
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
        androidMain {
            dependencies {
                api("net.java.dev.jna:jna:5.15.0")
            }
        }
        androidUnitTest {
            dependencies {
                implementation("androidx.test:runner:1.6.2")
                implementation("androidx.test:core:1.6.2")
            }
        }

        val appleMobileMain by creating {
            dependsOn(commonMain.get())
        }
        val appleMobileTest by creating {
            dependsOn(commonTest.get())
        }
        watchosMain {
            dependsOn(appleMobileMain)
        }
        watchosTest {
            dependsOn(appleMobileTest)
        }
        tvosMain {
            dependsOn(appleMobileMain)
        }
        tvosTest {
            dependsOn(appleMobileTest)
        }
        iosMain {
            dependsOn(appleMobileMain)
        }
        iosTest {
            dependsOn(appleMobileTest)
        }

        val nativeDesktopTest by creating {
            dependsOn(commonTest.get())
        }
        linuxTest {
            dependsOn(nativeDesktopTest)
        }
        macosTest {
            dependsOn(nativeDesktopTest)
        }
        mingwTest {
            dependsOn(nativeDesktopTest)
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

    register("configurations") {
        doLast {
            val myParam = project.findProperty("filter") as String?
            val filteredConfigurations = when (myParam) {
                null -> configurations
                else -> configurations.filter { it.name.contains(myParam, ignoreCase = true) }
            }

            println("Configurations: \n - ${filteredConfigurations.joinToString("\n - ") { it.name }}")
        }
    }

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