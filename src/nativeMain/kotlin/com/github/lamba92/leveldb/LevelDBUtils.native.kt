@file:Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")

package com.github.lamba92.leveldb

import com.github.lamba92.leveldb.native.NativeLevelDB
import com.github.lamba92.leveldb.native.toNative
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import libleveldb.leveldb_destroy_db
import libleveldb.leveldb_open
import libleveldb.leveldb_options_destroy
import libleveldb.leveldb_repair_db

public actual fun LevelDB(
    path: String,
    options: LevelDBOptions,
): LevelDB =
    memScoped {
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

public actual fun repairDatabase(
    path: String,
    options: LevelDBOptions,
): Unit =
    memScoped {
        val nativeOptions = options.toNative()
        val errorPtr = allocPointerTo<ByteVar>()
        leveldb_repair_db(
            options = nativeOptions,
            name = path,
            errptr = errorPtr.ptr,
        )
        leveldb_options_destroy(nativeOptions)
        val errorValue = errorPtr.value
        if (errorValue != null) {
            error("Failed to repair database: ${errorValue.toKString()}")
        }
    }

public actual fun destroyDatabase(
    path: String,
    options: LevelDBOptions,
): Unit =
    memScoped {
        val nativeOptions = options.toNative()
        val errorPtr = allocPointerTo<ByteVar>()
        leveldb_destroy_db(
            options = nativeOptions,
            name = path,
            errptr = errorPtr.ptr,
        )
        val errorValue = errorPtr.value
        if (errorValue != null) {
            error("Failed to destroy database: ${errorValue.toKString()}")
        }
        leveldb_options_destroy(nativeOptions)
    }
