@file:Suppress("UnstableApiUsage")

import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import kotlin.io.path.absolutePathString


plugins {
    `publishing-convention`
    `kotlin-multiplatform-with-android-convention`
    `fix-jna-publication`
    versions
    `leveldb-binaries`
    id("io.github.gradle-nexus.publish-plugin")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {

    explicitApi()

    jvmToolchain(8)

    jvm()
    androidTarget()

    mingwX64 {
        registerLeveldbCinterop("mingw-x64")
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
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                api(libs.kotlinx.coroutines.test)
            }
        }

        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                compileOnly(libs.jna)
            }
        }

        val jvmCommonTest by creating {
            dependsOn(commonTest.get())
        }

        androidMain {
            dependsOn(jvmCommonMain)
            dependencies {
                api(libs.jna)
            }
        }

        jvmMain {
            dependsOn(jvmCommonMain)
            resources.srcDirs(tasks.copyCppStdlibFromAndroidNdk.map { it.destinationDir })
            dependencies {
                api(libs.jna)
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
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.core)
                implementation(libs.android.test.junit)
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

fun KotlinNativeTarget.registerLeveldbCinterop(
    platformName: String,
    packageName: String = "libleveldb",
    generateDefTaskName: String = "generate${
        platformName.toCamelCase().capitalized()
    }LeveldbDefFile",
    defFileName: String = "${platformName.toCamelCase()}.def",
) {
    val generateDefTask =
        tasks.register<CreateDefFileTask>(generateDefTaskName) {
            dependsOn(tasks.extractLevelDbBinariesForKotlinNative, tasks.extractHeaders)
            headers = listOf("leveldb/c.h")
            staticLibs.add("libleveldb.a")
            defFile = layout.buildDirectory.file("generated/cinterop/$defFileName")
            compilerOpts.add(
                tasks
                    .extractHeaders
                    .map { "-I${it.destinationDir.absolutePath}" },
            )
            libraryPaths.add(
                tasks
                    .extractLevelDbBinariesForKotlinNative
                    .map { it.destinationDir.resolve(platformName).absolutePath },
            )
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
        definitionFile = generateDefTask.flatMap { it.defFile }
    }
}

tasks {
    val testCacheDir =
        layout.buildDirectory
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
        val logsDir =
            layout.buildDirectory.dir("crash-logs")
                .get()
                .asFile
                .toPath()
                .absolutePathString()
        jvmArgs("-XX:ErrorFile=$logsDir/hs_err_pid%p.log")
        systemProperty("jna.debug_load", "true")
        systemProperty("jna.debug_load.jna", "true")
    }

    withType<KotlinNativeHostTest> {
        environment("LEVELDB_LOCATION", testCacheDir)
    }

    withType<AbstractTestTask> {
        testLogging {
            showExceptions = true
            showCauses = true
            showStandardStreams = true
            showStackTraces = true
        }
    }
}

nexusPublishing {
    // repositoryDescription is used by the nexus publish plugin as identifier
    // for the repository to publish to.
    val repoDesc =
        System.getenv("SONATYPE_REPOSITORY_DESCRIPTION")
            ?: project.properties["central.sonatype.repositoryDescription"] as? String
    repoDesc?.let { repositoryDescription = it }

    repositories {
        sonatype {
            username = System.getenv("SONATYPE_USERNAME")
                ?: project.properties["central.sonatype.username"] as? String
            password = System.getenv("SONATYPE_PASSWORD")
                ?: project.properties["central.sonatype.password"] as? String
        }
    }
}
