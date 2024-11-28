@file:Suppress("unused")

package com.github.lamba92.levelkt.tests

sealed interface OperatingSystem {

    val arch: Arch

    companion object {
        val current by lazy {
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()

            val arch = when {
                "arm" in osArch || "aarch64" in osArch -> Arch.ARM64
                "x86_64" in osArch || "amd64" in osArch -> Arch.X64
                else -> error("Unknown architecture: $osArch")
            }

            when {
                "mac" in osName || "darwin" in osName -> MacOs(arch)
                "win" in osName -> Windows(arch)
                "nux" in osName || "linux" in osName -> Linux(arch)
                else -> error("Unknown operating system: $osName")
            }
        }
    }

    enum class Arch { ARM64, X64 }
    data class MacOs(override val arch: Arch) : OperatingSystem
    data class Linux(override val arch: Arch) : OperatingSystem
    data class Windows(override val arch: Arch) : OperatingSystem
}

val OperatingSystem.isMacOs
    get() = this is OperatingSystem.MacOs

val OperatingSystem.isLinux
    get() = this is OperatingSystem.Linux

val OperatingSystem.isWindows
    get() = this is OperatingSystem.Windows