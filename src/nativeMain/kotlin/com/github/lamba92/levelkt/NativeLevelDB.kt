package com.github.lamba92.levelkt

import cnames.structs.leveldb_options_t
import cnames.structs.leveldb_readoptions_t
import cnames.structs.leveldb_snapshot_t
import cnames.structs.leveldb_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.UIntVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import libleveldb.leveldb_close
import libleveldb.leveldb_compact_range
import libleveldb.leveldb_create_iterator
import libleveldb.leveldb_create_snapshot
import libleveldb.leveldb_delete
import libleveldb.leveldb_destroy_db
import libleveldb.leveldb_free
import libleveldb.leveldb_get
import libleveldb.leveldb_iter_destroy
import libleveldb.leveldb_iter_key
import libleveldb.leveldb_iter_next
import libleveldb.leveldb_iter_valid
import libleveldb.leveldb_iter_value
import libleveldb.leveldb_open
import libleveldb.leveldb_options_destroy
import libleveldb.leveldb_readoptions_create
import libleveldb.leveldb_readoptions_destroy
import libleveldb.leveldb_readoptions_set_fill_cache
import libleveldb.leveldb_readoptions_set_snapshot
import libleveldb.leveldb_readoptions_set_verify_checksums
import libleveldb.leveldb_release_snapshot
import libleveldb.leveldb_repair_db
import libleveldb.leveldb_write
import libleveldb.leveldb_writebatch_create
import libleveldb.leveldb_writebatch_delete
import libleveldb.leveldb_writebatch_destroy
import libleveldb.leveldb_writebatch_put
import libleveldb.leveldb_writeoptions_create
import libleveldb.leveldb_writeoptions_destroy
import libleveldb.leveldb_writeoptions_set_sync
import platform.posix.size_t
import platform.posix.size_tVar

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

class NativeLevelDB internal constructor(
    private val delegate: CPointer<leveldb_t>,
    private val nativeOptions: CPointer<leveldb_options_t>
) : LevelDB {

    override fun put(key: String, value: String, sync: Boolean) = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        if (sync) {
            val writeOptions = leveldb_writeoptions_create()
            leveldb_writeoptions_set_sync(writeOptions, 1u)
            platform_specific_leveldb_put(
                db = delegate,
                options = writeOptions,
                key = key,
                value = value,
                errptr = errPtr.ptr
            )
            leveldb_writeoptions_destroy(writeOptions)
        } else {
            platform_specific_leveldb_put(
                db = delegate,
                options = null,
                key = key,
                value = value,
                errptr = errPtr.ptr
            )
        }
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to put value: ${errorValue.toKString()}")
        }
    }

    override fun get(key: String, options: LevelDBReadOptions): String? = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val nativeReadOptions = options.toNative()
        val value = platform_specific_leveldb_get(
            db = delegate,
            options = nativeReadOptions,
            key = key,
            errptr = errPtr.ptr
        )
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to get value: ${errorValue.toKString()}")
        }
        return value
    }

    override fun delete(key: String, sync: Boolean) = memScoped {
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

    override fun batch(operations: List<LevelDBBatchOperation>, sync: Boolean) = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val nativeBatch = leveldb_writebatch_create()
        for (operation in operations) {
            when (operation) {
                is LevelDBBatchOperation.Delete -> leveldb_writebatch_delete(
                    nativeBatch,
                    operation.key,
                    operation.key.length.toUInt(),
                )

                is LevelDBBatchOperation.Put -> leveldb_writebatch_put(
                    nativeBatch,
                    operation.key,
                    operation.key.length.toUInt(),
                    operation.value,
                    operation.value.length.toUInt()
                )
            }
            val errorValue = errPtr.value
            if (errorValue != null) {
                error("Failed to put values: ${errorValue.toKString()}")
            }
        }
        val writeOptions = leveldb_writeoptions_create()
        leveldb_write(
            db = delegate,
            options = writeOptions,
            batch = nativeBatch,
            errptr = errPtr.ptr
        )
        leveldb_writeoptions_destroy(writeOptions)
        leveldb_writebatch_destroy(nativeBatch)
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed commit batch: ${errorValue.toKString()}")
        }
    }

    override fun iterator(options: LevelDBReadOptions): Iterator<Pair<String, String>> = iterator {
        val nativeOptions = leveldb_readoptions_create()
        leveldb_readoptions_set_verify_checksums(nativeOptions, options.verifyChecksums.toUByte())
        leveldb_readoptions_set_fill_cache(nativeOptions, options.fillCache.toUByte())
        if (options.snapshot is NativeLevelDBSnapshot) {
            leveldb_readoptions_set_snapshot(nativeOptions, options.snapshot.delegate)
        }
        val nativeIterator = leveldb_create_iterator(delegate, nativeOptions)
            ?: error("Failed to create iterator")

        while (leveldb_iter_valid(nativeIterator) != 0.toUByte()) {
            val keyValue = memScoped {
                val anInteger = alloc<size_tVar>()
                val key = leveldb_iter_key(nativeIterator, anInteger.ptr)
                    ?.readBytes(anInteger.value.toInt())
                    ?.toKString()
                    ?: error("Failed to read key")
                val value = leveldb_iter_value(nativeIterator, anInteger.ptr)
                    ?.readBytes(anInteger.value.toInt())
                    ?.toKString()
                    ?: error("Failed to read value from key $key")
                key to value
            }
            yield(keyValue)
            leveldb_iter_next(nativeIterator)
        }
        leveldb_iter_destroy(nativeIterator)
        leveldb_readoptions_destroy(nativeOptions)
    }

    override fun createSnapshot() = NativeLevelDBSnapshot(
        delegate = leveldb_create_snapshot(delegate)
            ?: error("Failed to create snapshot"),
        db = delegate
    )

    override fun close() {
        leveldb_close(delegate)
        leveldb_options_destroy(nativeOptions)
    }

    override fun compactRange(start: String, end: String) = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        leveldb_compact_range(
            db = delegate,
            start_key = start,
            start_key_len = start.length.toUInt(),
            limit_key = end,
            limit_key_len = end.length.toUInt()
        )
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to compact range: ${errorValue.toKString()}")
        }
    }
}

class NativeLevelDBSnapshot(
    internal val delegate: CPointer<leveldb_snapshot_t>,
    private val db: CPointer<leveldb_t>
) : LevelDBSnapshot {
    override fun close() {
        leveldb_release_snapshot(db, delegate)
    }
}