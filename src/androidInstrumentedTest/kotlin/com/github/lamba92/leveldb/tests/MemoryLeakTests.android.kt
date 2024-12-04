@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.github.lamba92.leveldb.tests

import android.os.Debug

actual fun getMemoryUsage() = MemorySize(Runtime.getRuntime().usedMemory() + Debug.getNativeHeapAllocatedSize())

private fun Runtime.usedMemory() = totalMemory() - freeMemory()

actual typealias Test = org.junit.Test
