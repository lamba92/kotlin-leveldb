package com.github.lamba92.levelkt.tests

import androidx.test.platform.app.InstrumentationRegistry

actual val DATABASE_PATH: String
    get() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .filesDir
        .resolve("testdb")
        .absolutePath