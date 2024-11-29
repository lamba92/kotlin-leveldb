@file:OptIn(ExperimentalForeignApi::class)

package com.github.lamba92.leveldb.app

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

actual val DB_PATH: String
    get() = getenv("DB_PATH")?.toKString() ?: "localdb"