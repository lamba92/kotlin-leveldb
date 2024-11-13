import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.gradle.api.file.CopySpec
import kotlin.time.Duration.Companion.days

val LEVEL_DB_HEADERS_LINKS = listOf(
    "https://github.com/google/leveldb/raw/refs/heads/main/include/leveldb/c.h",
    "https://github.com/google/leveldb/raw/refs/heads/main/include/leveldb/export.h"
)

fun computeLevelDBWeeklyVersionString(): String {
    val now = Clock.System.now()
    val dayOfWeek = now.toLocalDateTime(TimeZone.UTC).dayOfWeek.value
    val daysFromMonday = dayOfWeek - 1
    val lastMonday = (now - daysFromMonday.days).toLocalDateTime(TimeZone.UTC)
    val monthString = lastMonday.monthNumber.toString().padStart(2, '0')
    val dayString = lastMonday.dayOfMonth.toString().padStart(2, '0')
    return "weekly-${lastMonday.year}-$monthString-$dayString"
}

fun getLevelDBBuildLink(version: String) =
    "https://github.com/lamba92/leveldb-builds/releases/download/$version/libleveldb-$version-all.zip"

data class RenamingStrategy(
    val from: String,
    val to: String,
    val ext: String = "a"
)

val kotlinNativeRenamings = listOf(
    RenamingStrategy("windows-static-x64", "windows-x64"),

    RenamingStrategy("linux-static-x64", "linux-x64"),
    RenamingStrategy("linux-static-arm64", "linux-arm64"),

    RenamingStrategy("macos-static-arm64", "macos-arm64"),
    RenamingStrategy("macos-static-x64", "macos-x64"),

    RenamingStrategy("android-static-arm64", "android-arm64"),
    RenamingStrategy("android-static-armv7", "androidNative-arm"),
    RenamingStrategy("android-static-x86", "android-x86"),
    RenamingStrategy("android-static-x86_64", "android-x64"),

    RenamingStrategy("ios-simulator-static-arm64", "ios-simulator-arm64"),
    RenamingStrategy("ios-simulator-static-x64", "ios-simulator-x64"),
    RenamingStrategy("ios-static-arm64", "ios-arm64"),

    RenamingStrategy("tvos-simulator-static-arm64", "tvos-simulator-arm64"),
    RenamingStrategy("tvos-simulator-static-x64", "tvos-simulator-x64"),
    RenamingStrategy("tvos-static-arm64", "tvos-arm64"),

    RenamingStrategy("watchos-simulator-static-arm64", "watchos-simulator-arm64"),
    RenamingStrategy("watchos-simulator-static-x64", "watchos-simulator-x64"),
    RenamingStrategy("watchos-static-arm64", "watchos-arm64"),
)

val androidJvmRenamings = listOf(
    RenamingStrategy("android-static-arm64", "arm64-v8a"),
    RenamingStrategy("android-static-armv7", "armeabi-v7a"),
    RenamingStrategy("android-static-x86", "x86"),
    RenamingStrategy("android-static-x86_64", "x86_64"),
)

val jvmRenamings = listOf(
    RenamingStrategy("windows-shared-x64", "win32-x86-64", "dll"),
    RenamingStrategy("windows-shared-arm64", "win32-aarch64", "dll"),

    RenamingStrategy("linux-shared-x64", "linux-x86-64", "so"),
    RenamingStrategy("linux-shared-arm64", "linux-aarch64", "so"),
    RenamingStrategy("linux-shared-armv7-a", "linux-arm", "so"),

    RenamingStrategy("macos-shared-arm64", "darwin", "dylib"),
    RenamingStrategy("macos-shared-x64", "darwin-aarch64", "dylib"),
)

fun CopySpec.forPlatform(
    strategy: RenamingStrategy,
    version: String,
) {
    val name = "libleveldb-$version-${strategy.from}"
    when (strategy.ext) {
        "dll" -> from("$name/libleveldb.dll")
        else -> from("$name.${strategy.from}.${strategy.ext}")
    }
    into("${strategy.to}/libleveldb.${strategy.ext}")
}