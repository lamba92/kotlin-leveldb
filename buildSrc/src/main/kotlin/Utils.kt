import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.registering

fun getLevelDBBuildLink(version: String) =
    "https://github.com/lamba92/leveldb-builds/releases/download/$version/leveldb.zip"

data class RenamingStrategy(
    val fromPath: String,
    val toPath: String = fromPath,
    val ext: String = "a",
    val fromName: String = "libleveldb",
    val toName: String = "libleveldb",
)

val kotlinNativeRenamings = listOf(
    RenamingStrategy("windows/static/x64", "mingw-x64"),

    RenamingStrategy("linux/static/x64", "linux-x64"),
    RenamingStrategy("linux/static/arm64", "linux-arm64"),

    RenamingStrategy("macosx/static/x86_64", "macos-x64"),
    RenamingStrategy("macosx/static/arm64", "macos-arm64"),

    RenamingStrategy("android/static/arm64", "android-arm64"),
    RenamingStrategy("android/static/x64", "android-x64"),
    RenamingStrategy("android/static/x86", "android-x86"),
    RenamingStrategy("android/static/armv7a", "android-arm32"),

    RenamingStrategy("iphoneos/static/arm64", "ios-arm64"),
    RenamingStrategy("iphonesimulator/static/x86_64", "ios-simulator-x64"),
    RenamingStrategy("iphonesimulator/static/arm64", "ios-simulator-arm64"),

    RenamingStrategy("appletvos/static/arm64", "tvos-arm64"),
    RenamingStrategy("appletvsimulator/static/arm64", "tvos-simulator-arm64"),
    RenamingStrategy("appletvsimulator/static/x86_64", "tvos-simulator-x64"),

    RenamingStrategy("watchos/static/arm64", "watchos-arm64"),
    RenamingStrategy("watchsimulator/static/x86_64", "watchos-simulator-x64"),
    RenamingStrategy("watchsimulator/static/arm64", "watchos-simulator-arm64")
)

val androidJvmRenamings = listOf(
    RenamingStrategy("android/shared/arm64", "arm64-v8a", "so"),
    RenamingStrategy("android/shared/armv7", "armeabi-v7a", "so"),
    RenamingStrategy("android/shared/x86", "x86", "so"),
    RenamingStrategy("android/shared/x64", "x86_64", "so"),
)

val jvmRenamings = listOf(
    RenamingStrategy("windows/shared/x64", "win32-x86-64", "dll", toName = "leveldb"),
    RenamingStrategy("windows/shared/arm64", "win32-aarch64", "dll", toName = "leveldb"),

    RenamingStrategy("linux/shared/x64", "linux-x86-64", "so"),
    RenamingStrategy("linux/shared/arm64", "linux-aarch64", "so"),
    RenamingStrategy("linux/shared/armv7a", "linux-arm", "so"),

    RenamingStrategy("macosx/shared/arm64", "darwin-aarch64", "dylib"),
    RenamingStrategy("macosx/shared/x64", "darwin", "dylib"),
)

fun CopySpec.forPlatform(
    strategy: RenamingStrategy,
    isRelease: Boolean
) {
    include(buildString {
        append(strategy.fromPath)
        when {
            isRelease -> append("/release")
            else -> append("/debug")
        }
        append("/${strategy.fromName}.${strategy.ext}")
    })
    eachFile { path = "${strategy.toPath}/${strategy.toName}.${strategy.ext}" }
}

fun Project.registerExtractLevelDbTask(
    downloadLeveDBBinaries: TaskProvider<DownloadTask>,
    strategies: List<RenamingStrategy>,
    destinationDir: Provider<Directory>,
    isRelease: Boolean = project.properties["leveldb.release"]
        ?.let { it as? String }
        ?.toBooleanStrictOrNull()
        ?: true
) = tasks.registering(Sync::class) {
    dependsOn(downloadLeveDBBinaries)
    strategies.forEach {
        from(zipTree(downloadLeveDBBinaries.map { it.downloadFile })) {
            forPlatform(it, isRelease)
        }
    }
    includeEmptyDirs = false
    into(destinationDir)
}

fun String.toCamelCase() =
    split("[^A-Za-z0-9]+".toRegex())
        .joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
        .replaceFirstChar(Char::lowercase)

val Project.localProperties: Map<String, String>
    get() {
        val localPropertiesFile = rootProject.file("local.properties")
        if (!localPropertiesFile.exists()) {
            return emptyMap()
        }
        val p = Properties()
        localPropertiesFile
            .inputStream()
            .use { p.load(it) }
        return p.entries.associate { it.key.toString() to it.value.toString() }
    }

fun Project.findAndroidSdk() =
    getAndroidSdkPathString()?.let { Path(it) }

private fun Project.getAndroidSdkPathString(): String? =
    project.findProperty("sdk.dir") as String?
        ?: project.localProperties["sdk.dir"]
        ?: System.getenv("ANDROID_SDK_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_SDK")

fun Project.findAndroidNdk() =
    getAndroidNdkPathString()
        ?.let { Path(it) }
        ?.takeIf { it.exists() }
        ?: findAndroidSdk()
            ?.resolve("ndk")
            ?.listDirectoryEntries()
            ?.find { it.isDirectory() }

private fun Project.getAndroidNdkPathString(): String? =
    project.findProperty("ndk.dir") as String?
        ?: project.localProperties["ndk.dir"]
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: System.getenv("ANDROID_NDK_ROOT")
        ?: System.getenv("ANDROID_NDK")