package com.github.lamba92.leveldb.tests

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual val DATABASE_PATH: String
    get() = getenv("LEVELDB_LOCATION")
        ?.toKString()
        ?: error("LEVELDB_LOCATION environment variable not set")