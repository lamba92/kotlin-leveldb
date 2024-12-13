import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

val levelDbVersion = "20241128T174416Z"

val downloadLeveDBBinaries by tasks.registering(DownloadTask::class) {
    val levelDbVersion = levelDbVersion
    link = getLevelDBBuildLink(levelDbVersion)
}

val extractLevelDbBinariesForKotlinNative by registerExtractLevelDbTask(
    downloadLeveDBBinaries = downloadLeveDBBinaries,
    strategies = kotlinNativeRenamings,
    destinationDir = layout.buildDirectory.dir("binaries/leveldb/kotlinNative"),
)

val jvmLibs = layout.buildDirectory.dir("binaries/leveldb/jvm")
val extractLevelDbBinariesForJvm by registerExtractLevelDbTask(
    downloadLeveDBBinaries = downloadLeveDBBinaries,
    strategies = jvmRenamings,
    destinationDir = jvmLibs,
)

val androidLibs = layout.buildDirectory.dir("binaries/leveldb/android")
val extractLevelDbBinariesForAndroidJvm by registerExtractLevelDbTask(
    downloadLeveDBBinaries = downloadLeveDBBinaries,
    strategies = androidJvmRenamings,
    destinationDir = androidLibs,
)

val extractHeaders by tasks.registering(Sync::class) {
    dependsOn(downloadLeveDBBinaries)
    from(zipTree(downloadLeveDBBinaries.map { it.downloadFile })) {
        include("**/*.h")
        eachFile { path = path.removePrefix("headers/include") }
    }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("headers"))
}

val androidCppLibs = layout.buildDirectory.dir("binaries/stdcpp/android")
val copyCppStdlibFromAndroidNdk by tasks.registering(Sync::class) {
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
    into(androidCppLibs)
}

plugins.withId("com.android.library") {
    the<LibraryExtension>().apply {
        sourceSets {
            named("main") {
                jniLibs.srcDirs(androidLibs, androidCppLibs)
            }
        }
    }

    tasks.withType<MergeSourceSetFolders> {
        dependsOn(copyCppStdlibFromAndroidNdk, extractLevelDbBinariesForAndroidJvm)
    }
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    the<KotlinMultiplatformExtension>().apply {
        sourceSets {
            jvmMain {
                resources.srcDir(jvmLibs)
            }
        }
    }
    tasks.withType<ProcessResources> {
        dependsOn(extractLevelDbBinariesForJvm)
    }
}
