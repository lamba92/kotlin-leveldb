package com.github.lamba92.levelkt.tests

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

actual fun getMemoryUsage(): MemorySize {
    val osBean = ManagementFactory.getOperatingSystemMXBean()
    val committedVirtualMemorySize =
        when {
            osBean is OperatingSystemMXBean -> osBean.committedVirtualMemorySize
            else -> error("OperatingSystemMXBean not available")
        }

    return when {
        OperatingSystem.current.isMacOs -> (committedVirtualMemorySize / 1024).bytes
        else -> committedVirtualMemorySize.bytes
    }
}
