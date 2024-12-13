package com.github.lamba92.leveldb.benchmarks

import kotlin.io.path.Path
import kotlin.io.path.writeText

actual fun writeStringToFile(
    path: String,
    content: String,
) {
    Path(path).writeText(content)
}

actual fun getenv(name: String): String? = System.getenv(name)

actual fun Double.format(arg: String): String {
    return String.format(arg, this)
}
