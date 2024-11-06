package com.github.lamba92.levelkt.tests

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getDatabaseLocation() =
    getenv("LEVELDB_LOCATION")
        ?.toKString()
        ?: error("LEVELDB_LOCATION not set")