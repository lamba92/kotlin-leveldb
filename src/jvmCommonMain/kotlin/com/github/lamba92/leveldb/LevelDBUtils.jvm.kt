package com.github.lamba92.leveldb

import com.github.lamba92.leveldb.jvm.JvmLevelDB
import com.github.lamba92.leveldb.jvm.LibLevelDB
import com.github.lamba92.leveldb.jvm.toNative
import com.sun.jna.ptr.PointerByReference
import java.nio.charset.Charset

public actual fun LevelDB(
    path: String,
    options: LevelDBOptions,
): LevelDB = LevelDB(path, options, Charsets.UTF_8)

/**
 * Creates a new instance of a LevelDB database at the specified file path.
 *
 * @param path The file system path where the LevelDB database is located or will be created. It has to a directory.
 * If the directory does not exist, it will be created only if [LevelDBOptions.createIfMissing] is set to `true`.
 * @param options Configuration options for creating and managing the LevelDB instance, defaults to [LevelDBOptions.DEFAULT].
 * @param charset The character set to use for encoding and decoding strings. Defaults to [Charsets.UTF_8].
 * @return A `LevelDB` instance for interacting with the database.
 */
public fun LevelDB(
    path: String,
    options: LevelDBOptions,
    charset: Charset,
): LevelDB {
    val nativeOptions = options.toNative()
    val errPtr = PointerByReference()
    val nativeDelegate = LibLevelDB.leveldb_open(nativeOptions, path, errPtr)
    val errorValue = errPtr.value?.getString(0)
    if (errorValue != null) {
        LibLevelDB.leveldb_free(errPtr.value)
        error("Failed to open database: $errorValue")
    }
    return JvmLevelDB(nativeDelegate, nativeOptions, charset)
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
