@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
@file:Suppress("EXTERNAL_SERIALIZER_USELESS", "FunctionName")

package com.github.lamba92.leveldb.benchmarks

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.O_CREAT
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.close
import platform.posix.getenv
import platform.posix.open
import platform.posix.snprintf
import platform.posix.write

actual fun writeStringToFile(
    path: String,
    content: String,
) {
    println("Writing to file $path")
    val fd = open(path, O_WRONLY or O_CREAT or O_TRUNC, S_IRUSR or S_IWUSR)
    if (fd == -1) {
        error("Error opening file")
    }

    try {
        val written: Int = write(fd, content.encodeToByteArray().refTo(0), content.length.convert()).convert()
        if (written == -1) error("Error writing to file")
    } finally {
        close(fd)
    }
}

actual fun getenv(name: String): String? = getenv(name)?.toKString()

actual fun Double.format(arg: String): String =
    memScoped {
        val bufferSize = 128
        val buffer = allocArray<ByteVar>(bufferSize)
        snprintf(buffer, bufferSize.convert(), arg, this@format)
        buffer.toKString()
    }
