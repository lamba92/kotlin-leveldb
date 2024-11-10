package com.github.lamba92.levelkt.tests

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual val DATABASE_PATH: String
    get() = getenv("LEVELDB_LOCATION")
        ?.toKString()
        ?: error("Please set LEVELDB_LOCATION environment variable to the path of the database to test")