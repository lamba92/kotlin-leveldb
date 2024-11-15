@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.MergeSourceSetFolders
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    `maven-publish`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

val downloadLeveDBBinaries by tasks.registering(DownloadTask::class) {
    val levelDbVersion = project.properties["leveldb.version"] as String?
        ?: System.getenv("LEVELDB_VERSION")
        ?: computeLevelDBWeeklyVersionString()
    link = getLevelDBBuildLink(levelDbVersion)
}

val levelDbBinariesForKNDir = project.layout.buildDirectory.dir("binaries/leveldb/kotlinNative")
val levelDbBinariesForJvmDir = project.layout.buildDirectory.dir("binaries/leveldb/jvm")
val levelDbBinariesForAndroidDir = project.layout.buildDirectory.dir("binaries/leveldb/android")
val cppstdlibBinariesForAndroidDir = project.layout.buildDirectory.dir("binaries/stdcpp/android")

val extractLevelDbBinariesForKotlinNative by registerExtractLevelDbTask(
    downloadLeveDBBinaries = downloadLeveDBBinaries,
    strategies = kotlinNativeRenamings,
    destinationDir = levelDbBinariesForKNDir
)

val extractLevelDbBinariesForJvm by registerExtractLevelDbTask(
    downloadLeveDBBinaries = downloadLeveDBBinaries,
    strategies = jvmRenamings,
    destinationDir = levelDbBinariesForJvmDir
)

val extractLevelDbBinariesForAndroidJvm by registerExtractLevelDbTask(
    downloadLeveDBBinaries = downloadLeveDBBinaries,
    strategies = androidJvmRenamings,
    destinationDir = levelDbBinariesForAndroidDir
)

val headersDir = layout.buildDirectory.dir("downloads/headers")

val headersDownloadTasks = LEVEL_DB_HEADERS_LINKS
    .map { link -> registerDownloadTask(link, headersDir.map { it.dir("leveldb") }) }

android {
    namespace = "com.github.lamba92.levelkt"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets {
        named("main") {
            jniLibs.srcDirs(levelDbBinariesForAndroidDir, cppstdlibBinariesForAndroidDir)
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {

    jvmToolchain(8)

    jvm()

    androidTarget {

        // KT-46452 Allow to run common tests as Android Instrumentation tests
        // https://youtrack.jetbrains.com/issue/KT-46452
        instrumentedTestVariant {
            sourceSetTree = KotlinSourceSetTree.test
        }
    }

    mingwX64 {
        registerLeveldbCinterop("windows-x64")
    }

    linuxX64 {
        registerLeveldbCinterop("linux-x64")
    }
    linuxArm64 {
        registerLeveldbCinterop("linux-arm64")
    }

    macosX64 {
        registerLeveldbCinterop("macos-x64")
    }
    macosArm64 {
        registerLeveldbCinterop("macos-arm64")
    }

    androidNativeArm64 {
        registerLeveldbCinterop("android-arm64")
    }
    androidNativeX64 {
        registerLeveldbCinterop("android-x64")
    }
    androidNativeX86 {
        registerLeveldbCinterop("android-x86")
    }
    androidNativeArm32 {
        registerLeveldbCinterop("android-arm32")
    }

    iosArm64 {
        registerLeveldbCinterop("ios-arm64")
    }
    iosX64 {
        registerLeveldbCinterop("ios-simulator-x64")
    }
    iosSimulatorArm64 {
        registerLeveldbCinterop("ios-simulator-arm64")
    }

    tvosArm64 {
        registerLeveldbCinterop("tvos-arm64")
    }
    tvosSimulatorArm64 {
        registerLeveldbCinterop("tvos-simulator-arm64")
    }
    tvosX64 {
        registerLeveldbCinterop("tvos-simulator-x64")
    }

    watchosArm64 {
        registerLeveldbCinterop("watchos-arm64")
    }
    watchosSimulatorArm64 {
        registerLeveldbCinterop("watchos-simulator-arm64")
    }
    watchosX64 {
        registerLeveldbCinterop("watchos-simulator-x64")
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

        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                compileOnly("net.java.dev.jna:jna:5.15.0")
            }
        }

        val jvmCommonTest by creating {
            dependsOn(commonTest.get())
        }

        androidMain {
            dependsOn(jvmCommonMain)
            dependencies {
                api("net.java.dev.jna:jna:5.15.0@aar")
            }
        }

        jvmMain {
            dependsOn(jvmCommonMain)
            resources.srcDirs(levelDbBinariesForJvmDir)
            dependencies {
                api("net.java.dev.jna:jna:5.15.0")
            }
        }

        jvmTest {
            dependsOn(jvmCommonTest)
            dependencies {
                api(kotlin("test-junit5"))
            }
        }

        androidUnitTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        androidInstrumentedTest {
            dependsOn(jvmCommonTest)
            dependencies {
                implementation("androidx.test:runner:1.6.2")
                implementation("androidx.test:core:1.6.1")
                implementation("androidx.test.ext:junit:1.2.1")
                implementation(kotlin("test-junit"))
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
    platformName: String,
    packageName: String = "libleveldb",
    generateDefTaskName: String = "generate${platformName.toCamelCase().capitalized()}DefFile",
    defFileName: String = "${platformName.toCamelCase()}.def",
    action: CreateDefFileTask.() -> Unit = {},
) {
    val generateDefTask =
        tasks.register<CreateDefFileTask>(generateDefTaskName) {
            dependsOn(extractLevelDbBinariesForKotlinNative, headersDownloadTasks)
            headers.from(headersDir.map { it.asFileTree })
            staticLibs.add("libleveldb.a")
            defFile = layout.buildDirectory.file("generated/cinterop/$defFileName")
            compilerOpts.add(headersDir.map { "-I${it.asFile.absolutePath}" })
            libraryPaths.add(levelDbBinariesForKNDir.map { it.dir(platformName).asFile.absolutePath })
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

tasks {
    val testCacheDir = layout.buildDirectory
        .dir("testdb")
        .get()
        .asFile
        .toPath()
        .absolutePathString()

    withType<Exec> {
        environment("LEVELDB_LOCATION", testCacheDir)
    }
    withType<Test> {
        environment("LEVELDB_LOCATION", testCacheDir)
        testLogging.showStandardStreams = true
        useJUnitPlatform()
    }
    withType<KotlinNativeHostTest> {
        environment("LEVELDB_LOCATION", testCacheDir)
    }

    named<ProcessResources>("jvmProcessResources") {
        dependsOn(extractLevelDbBinariesForJvm)
    }

    val androidNdkPath = project.findProperty("ndk.dir") as String?
        ?: project.localProperties["ndk.dir"]
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: System.getenv("ANDROID_NDK_ROOT")
        ?: System.getenv("ANDROID_NDK")

    val copyCppStdlibFromAndroidNdk by registering(Sync::class) {
        val ndkPath = Path(androidNdkPath)
        doFirst {
            if (!ndkPath.exists() || !ndkPath.isDirectory()) {
                error("NDK not found in $ndkPath, please install it.")
            }
        }

        from(ndkPath) {
            include("toolchains/llvm/prebuilt/*/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so")
            eachFile { path = "arm64-v8a/libc++_shared.so" }
        }
        from(ndkPath) {
            include("toolchains/llvm/prebuilt/*/sysroot/usr/lib/arm-linux-androideabi/libc++_shared.so")
            eachFile { path = "armeabi-v7a/libc++_shared.so" }
        }
        from(ndkPath) {
            include("toolchains/llvm/prebuilt/*/sysroot/usr/lib/i686-linux-android/libc++_shared.so")
            eachFile { path = "x86/libc++_shared.so" }
        }
        from(ndkPath) {
            include("toolchains/llvm/prebuilt/*/sysroot/usr/lib/x86_64-linux-android/libc++_shared.so")
            eachFile { path = "x86_64/libc++_shared.so" }
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        into(cppstdlibBinariesForAndroidDir)
    }
    withType<MergeSourceSetFolders> {
        dependsOn(extractLevelDbBinariesForAndroidJvm, copyCppStdlibFromAndroidNdk)
    }

    // disable android unit tests, only instrumentation tests are supported
    withType<AndroidUnitTest> {
        onlyIf { false }
    }
}
