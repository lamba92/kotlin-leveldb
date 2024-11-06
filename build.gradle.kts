import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.0.20"
}

kotlin {

//    jvm()

    iosArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-ios-arm64")
    }

    iosX64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-ios-simulator-x64")
    }

    iosSimulatorArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-ios-simulator-arm64")
    }

    tvosArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-tvos-simulator-x64")
    }

    tvosX64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-tvos-x64")
    }

    tvosSimulatorArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-tvos-simulator-arm64")
    }

    watchosArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-watchos-arm64")
    }

    watchosX64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-watchos-simulator-x64")
    }

    watchosSimulatorArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-watchos-simulator-arm64")
    }

    androidNativeX86 {
        val libleveldb by registeringCinterop("libleveldb-1.23-android-x86")
    }

    androidNativeX64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-android-x86_64")
    }

    androidNativeArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-android-arm64")
    }

    linuxX64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-linux-x86_64")
    }

    linuxArm64 {
        val libleveldb by registeringCinterop("libleveldb-1.23-linux-arm64")
    }

    mingwX64 {
        val libleveldb by registeringCinterop(
            libraryName = "leveldb",
            libraryFolder = "libs/shared/libleveldb-1.23-windows-x64",
            generateDefTaskName = "generateLibleveldb123WindowsX64DefFile",
            defFileName = "libleveldb123WindowsX64.def"
        )
    }
}

fun KotlinNativeTarget.registeringCinterop(
    libraryName: String,
    libraryFolder: String = "libs/shared",
    packageName: String = "libleveldb",
    generateDefTaskName: String = "generate${libraryName.toCamelCase().capitalized()}DefFile",
    defFileName: String = "${libraryName.toCamelCase()}.def",
    action: DefaultCInteropSettings.() -> Unit = {},
): RegisteringDomainObjectDelegateProviderWithAction<out NamedDomainObjectContainer<DefaultCInteropSettings>, DefaultCInteropSettings> {

    val generateDefTask =
        tasks.register<CreateDefFileTask>(generateDefTaskName) {
            headers.from(rootProject.file("libs/headers/leveldb/c.h"))
            this.linkerOpts.set(listOf("-L${rootProject.file(libraryFolder).absolutePath}", "-l$libraryName"))
            defFile = layout.buildDirectory.file("generated/cinterop/$defFileName")
            compilerOpts.add("-I${rootProject.file("libs/headers").absolutePath}")
        }

    val compilation = compilations.getByName("main")

    compilation.compileTaskProvider {
        dependsOn(generateDefTask)
    }

    return compilation.cinterops.registering {
        tasks.all {
            if (name == interopProcessingTaskName) {
                dependsOn(generateDefTask)
            }
        }
        this.packageName = packageName
        definitionFile.set(generateDefTask.flatMap { it.defFile })
        action()
    }
}

fun String.toCamelCase(): String {
    return this.split("[^A-Za-z0-9]+".toRegex())
        .joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
        .replaceFirstChar(Char::lowercase)
}