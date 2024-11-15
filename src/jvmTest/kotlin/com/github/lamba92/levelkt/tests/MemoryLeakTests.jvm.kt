@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.github.lamba92.levelkt.tests

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

actual fun getMemoryUsage(): MemorySize {
    val committedVirtualMemorySize =
        when (val osBean = ManagementFactory.getOperatingSystemMXBean()) {
            is OperatingSystemMXBean -> osBean.committedVirtualMemorySize
            else -> error("OperatingSystemMXBean not available")
        }

    return when {
        committedVirtualMemorySize > 0 -> committedVirtualMemorySize.bytes
        else -> Runtime.getRuntime().usedMemory().bytes
    }
}

actual typealias Test = org.junit.jupiter.api.Test