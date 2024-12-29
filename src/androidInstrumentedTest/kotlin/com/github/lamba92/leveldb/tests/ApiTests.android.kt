@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.github.lamba92.leveldb.tests

import androidx.test.platform.app.InstrumentationRegistry

actual val DATABASE_PATH: String
    get() =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .filesDir
            .resolve("testdb")
            .absolutePath

actual typealias Test = org.junit.Test
