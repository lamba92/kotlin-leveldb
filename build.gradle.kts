@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.MergeSourceSetFolders
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("io.github.gradle-nexus.publish-plugin")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
}

group = "com.github.lamba92"

val githubRef = System.getenv("GITHUB_EVENT_NAME")
    ?.takeIf { it == "release" }
    ?.let { System.getenv("GITHUB_REF") }
    ?.removePrefix("refs/tags/")
    ?.removePrefix("v")

version = when {
    githubRef != null -> githubRef
    else -> "1.0-SNAPSHOT"
}

logger.lifecycle("Version: $version")

val levelDbVersion = "20241128T174416Z"

val downloadLeveDBBinaries by tasks.registering(DownloadTask::class) {
    val levelDbVersion = levelDbVersion
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

val headersDir = layout.buildDirectory.dir("headers")

val extractHeaders by tasks.registering(Sync::class) {
    dependsOn(downloadLeveDBBinaries)
    from(zipTree(downloadLeveDBBinaries.map { it.downloadFile })) {
        include("**/*.h")
        eachFile { path = path.removePrefix("headers/include") }
    }
    includeEmptyDirs = false
    into(headersDir)
}

android {
    namespace = "com.github.lamba92.leveldb"
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

val currentOs: OperatingSystem = OperatingSystem.current()

kotlin {

    jvmToolchain(8)

    jvm()

    androidTarget {
        publishLibraryVariants("release")
        // KT-46452 Allow to run common tests as Android Instrumentation tests
        // https://youtrack.jetbrains.com/issue/KT-46452
        instrumentedTestVariant {
            sourceSetTree = KotlinSourceSetTree.test
        }
    }

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
            dependsOn(extractLevelDbBinariesForKotlinNative, extractHeaders)
            headers = listOf("leveldb/c.h")
            staticLibs.add("libleveldb.a")
            defFile = layout.buildDirectory.file("generated/cinterop/$defFileName")
            compilerOpts.add(headersDir.map { "-I${it.asFile.absolutePath}" })
            libraryPaths.add(levelDbBinariesForKNDir.map { it.dir(platformName).asFile.absolutePath })
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
        val logsDir = layout.buildDirectory.dir("crash-logs")
            .get()
            .asFile
            .toPath()
            .absolutePathString()
        jvmArgs("-XX:ErrorFile=${logsDir}/hs_err_pid%p.log")
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

    named<ProcessResources>("jvmProcessResources") {
        dependsOn(extractLevelDbBinariesForJvm)
    }

    val copyCppStdlibFromAndroidNdk by registering(Sync::class) {
        val ndkPath = findAndroidNdk()

        doFirst {
            if (ndkPath == null) {
                error("NDK not found, please install it.")
            }
        }
        if (ndkPath == null) return@registering

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

    val mingwX64Test = named<KotlinNativeHostTest>("mingwX64Test")
    val linuxX64Test = named<KotlinNativeHostTest>("linuxX64Test")
    val macosArm64Test = named<KotlinNativeHostTest>("macosArm64Test")
    val macosX64Test = named<KotlinNativeHostTest>("macosX64Test")
    val iosSimulatorArm64Test = named<KotlinNativeSimulatorTest>("iosSimulatorArm64Test")
    val watchosSimulatorArm64Test = named<KotlinNativeSimulatorTest>("watchosSimulatorArm64Test")
    val tvosSimulatorArm64Test = named<KotlinNativeSimulatorTest>("tvosSimulatorArm64Test")

    register("platformSpecificTest") {
        val tests = when {
            currentOs.isWindows -> listOf(mingwX64Test)
            currentOs.isLinux -> listOf(linuxX64Test)
            currentOs.isMacOsX -> listOf(
                macosArm64Test,
                macosX64Test,
                iosSimulatorArm64Test,
                watchosSimulatorArm64Test,
                tvosSimulatorArm64Test
            )

            else -> error("Unsupported OS: $currentOs")
        }
        dependsOn(tests)
    }

    // I have not found a better way...
    val winPublishTasks = listOf("publishMingwX64PublicationTo")
    val linuxPublishTasks = listOf(
        "publishAndroidNativeArm32PublicationTo",
        "publishAndroidNativeArm64PublicationTo",
        "publishAndroidNativeX64PublicationTo",
        "publishAndroidNativeX86PublicationTo",
        "publishAndroidReleasePublicationTo",
        "publishKotlinMultiplatformPublicationTo",
        "publishJvmPublicationTo",
        "publishLinuxArm64PublicationTo",
        "publishLinuxX64PublicationTo",
    )

    val macosPublishTasks = listOf(
        "publishMacosArm64PublicationTo",
        "publishMacosX64PublicationTo",
        "publishIosArm64PublicationTo",
        "publishIosSimulatorArm64PublicationTo",
        "publishIosX64PublicationTo",
        "publishTvosArm64PublicationTo",
        "publishTvosSimulatorArm64PublicationTo",
        "publishTvosX64PublicationTo",
        "publishWatchosArm64PublicationTo",
        "publishWatchosSimulatorArm64PublicationTo",
        "publishWatchosX64PublicationTo",
    )

    all {
        when {
            name.startsWithAny(winPublishTasks) -> onlyIf { currentOs.isWindows }
            name.startsWithAny(linuxPublishTasks) -> onlyIf { currentOs.isLinux }
            name.startsWithAny(macosPublishTasks) -> onlyIf { currentOs.isMacOsX }
        }
    }

    withType<AbstractPublishToMaven> {
        dependsOn(withType<Sign>())
    }
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier = "javadoc"
    from(tasks.dokkaGeneratePublicationHtml)
    destinationDirectory = layout.buildDirectory.dir("artifacts")
    archiveBaseName = "javadoc"
}

signing {
    val privateKey = System.getenv("SIGNING_PRIVATE_KEY")
        ?: project.properties["central.signing.privateKeyPath"]
            ?.let { it as? String }
            ?.let { Path(it).readText() }
        ?: return@signing
    val password = System.getenv("SIGNING_PASSWORD")
        ?: project.properties["central.signing.privateKeyPassword"] as? String
        ?: return@signing
    logger.lifecycle("Publication signing enabled")
    useInMemoryPgpKeys(privateKey, password)
    sign(publishing.publications)
}

publishing {
    repositories {
        maven(layout.buildDirectory.dir("repo")) {
            name = "test"
        }
    }
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name = "kotlin-leveldb"
                description = "LevelDB for Kotlin Multiplatform"
                url = "https://github.com/lamba92/kotlin-leveldb"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "lamba92"
                        name = "Lamberto Basti"
                        email = "basti.lamberto@gmail.com"
                    }
                }
                scm {
                    connection = "https://github.com/lamba92/kotlin-leveldb.git"
                    developerConnection = "https://github.com/lamba92/kotlin-leveldb.git"
                    url = "https://github.com/lamba92/kotlin-leveldb.git"
                }
            }
        }
    }
}

nexusPublishing {
    // repositoryDescription is used by the nexus publish plugin as identifier
    // for the repository to publish to.
    val repoDesc = System.getenv("SONATYPE_REPOSITORY_DESCRIPTION")
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

fun String.startsWithAny(strings: List<String>): Boolean = strings.any { startsWith(it) }
