package com.github.lamba92.leveldb

import com.github.lamba92.leveldb.jvm.JvmLevelDB
import com.github.lamba92.leveldb.jvm.LibLevelDB
import com.github.lamba92.leveldb.jvm.toNative
import com.sun.jna.ptr.PointerByReference

public actual fun LevelDB(
    path: String,
    options: LevelDBOptions,
): LevelDB {
    val nativeOptions = options.toNative()
    val errPtr = PointerByReference()
    val nativeDelegate = LibLevelDB.leveldb_open(nativeOptions, path, errPtr)
    val errorValue = errPtr.value?.getString(0)
    if (errorValue != null) {
        LibLevelDB.leveldb_free(errPtr.value)
        error("Failed to open database: $errorValue")
    }
    return JvmLevelDB(nativeDelegate, nativeOptions)
}

public actual fun repairDatabase(
    path: String,
    options: LevelDBOptions,
): Unit =
    with(LibLevelDB) {
        val nativeOptions = options.toNative()
        val errPtr = PointerByReference()
        leveldb_repair_db(nativeOptions, path, errPtr)
        val errorValue = errPtr.value?.getString(0)
        leveldb_free(errPtr.value)
        if (errorValue != null) {
            error("Failed to repair database: $errorValue")
        }
        leveldb_options_destroy(nativeOptions)
    }

public actual fun destroyDatabase(
    path: String,
    options: LevelDBOptions,
): Unit =
    with(LibLevelDB) {
        val nativeOptions = options.toNative()
        val errPtr = PointerByReference()
        leveldb_destroy_db(nativeOptions, path, errPtr)
        val errorValue = errPtr.value?.getString(0)
        if (errorValue != null) {
            leveldb_free(errPtr.value)
            error("Failed to destroy database: $errorValue")
        }
        leveldb_options_destroy(nativeOptions)
    }
