package com.github.lamba92.levelkt

import cnames.structs.leveldb_options_t
import cnames.structs.leveldb_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import libleveldb.leveldb_close
import libleveldb.leveldb_delete
import libleveldb.leveldb_free
import libleveldb.leveldb_get
import libleveldb.leveldb_open
import libleveldb.leveldb_options_destroy
import libleveldb.leveldb_put
import libleveldb.leveldb_writeoptions_create
import libleveldb.leveldb_writeoptions_destroy
import libleveldb.leveldb_writeoptions_set_sync
import platform.posix.size_tVar


fun LevelDB(path: String, options: LevelDBOptions): LevelDB = memScoped {
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
    LevelDB(nativeDelegate, nativeOptions)
}

class LevelDB internal constructor(
    private val delegate: CPointer<leveldb_t>,
    private val nativeOptions: CPointer<leveldb_options_t>
) : AutoCloseable {

    fun put(key: String, value: String, sync: Boolean = false) = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        if (sync) {
            val writeOptions = leveldb_writeoptions_create()
            leveldb_writeoptions_set_sync(writeOptions, 1u)
            leveldb_put(
                db = delegate,
                options = writeOptions,
                key = key,
                keylen = key.length.toUInt(),
                `val` = value,
                vallen = value.length.toUInt(),
                errptr = errPtr.ptr
            )
            leveldb_writeoptions_destroy(writeOptions)
        } else {
            leveldb_put(
                db = delegate,
                options = null,
                key = key,
                keylen = key.length.toUInt(),
                `val` = value,
                vallen = value.length.toUInt(),
                errptr = errPtr.ptr
            )
        }
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to put value: ${errorValue.toKString()}")
        }
    }

    fun get(key: String): String? = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val valLen = alloc<size_tVar>()
        val valuePtr = leveldb_get(
            db = delegate,
            options = null,
            key = key,
            keylen = key.length.toUInt(),
            vallen = valLen.ptr,
            errptr = errPtr.ptr
        )
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to get value: ${errorValue.toKString()}")
        }
        if (valuePtr == null) {
            return null
        }
        val value = valuePtr.readBytes(valLen.value.toInt()).toKString()
        leveldb_free(valuePtr)
        return value
    }

    fun delete(key: String, sync: Boolean = false) = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        if (sync) {
            val writeOptions = leveldb_writeoptions_create()
            leveldb_writeoptions_set_sync(writeOptions, 1u)
            leveldb_delete(
                db = delegate,
                options = writeOptions,
                key = key,
                keylen = key.length.toUInt(),
                errptr = errPtr.ptr
            )
            leveldb_writeoptions_destroy(writeOptions)
        } else {
            leveldb_delete(
                db = delegate,
                options = null,
                key = key,
                keylen = key.length.toUInt(),
                errptr = errPtr.ptr
            )
        }
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to delete value: ${errorValue.toKString()}")
        }
    }

    override fun close() {
        leveldb_close(delegate)
        leveldb_options_destroy(nativeOptions)
    }

}