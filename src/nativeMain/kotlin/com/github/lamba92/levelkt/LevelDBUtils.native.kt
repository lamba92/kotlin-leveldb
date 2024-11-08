package com.github.lamba92.levelkt

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import libleveldb.leveldb_destroy_db
import libleveldb.leveldb_open
import libleveldb.leveldb_repair_db

actual fun LevelDB(path: String, options: LevelDBOptions): LevelDB = memScoped {
    val errPtr = allocPointerTo<ByteVar>()
    val nativeOptions = options.toNative()
    val nativeDelegate = leveldb_open(nativeOptions, path, errPtr.ptr)
    val errorValue = errPtr.value
    if (errorValue != null) {
        error("Failed to open database: ${errorValue.toKString()}")
    }
    if (nativeDelegate == null) {
        error("Failed to open database")
    }
    NativeLevelDB(nativeDelegate, nativeOptions)
}

actual fun repairDatabase(path: String, options: LevelDBOptions) = memScoped {
    val nativeOptions = options.toNative()
    val errorPtr = allocPointerTo<ByteVar>()
    leveldb_repair_db(
        options = nativeOptions,
        name = path,
        errptr = errorPtr.ptr
    )
    val errorValue = errorPtr.value
    if (errorValue != null) {
        error("Failed to repair database: ${errorValue.toKString()}")
    }
}

actual fun destroyDatabase(path: String, options: LevelDBOptions) = memScoped {
    val nativeOptions = options.toNative()
    val errorPtr = allocPointerTo<ByteVar>()
    leveldb_destroy_db(
        options = nativeOptions,
        name = path,
        errptr = errorPtr.ptr
    )
    val errorValue = errorPtr.value
    if (errorValue != null) {
        error("Failed to destroy database: ${errorValue.toKString()}")
    }
}