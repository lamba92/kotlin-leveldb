@file:OptIn(BrokenNativeAPI::class)
@file:Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")

package com.github.lamba92.leveldb.native

import cnames.structs.leveldb_options_t
import cnames.structs.leveldb_t
import com.github.lamba92.leveldb.BrokenNativeAPI
import com.github.lamba92.leveldb.CloseableSequence
import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.LevelDBBatchOperation
import com.github.lamba92.leveldb.LevelDBReader
import com.github.lamba92.leveldb.LevelDBSnapshot
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import libleveldb.leveldb_close
import libleveldb.leveldb_compact_range
import libleveldb.leveldb_create_snapshot
import libleveldb.leveldb_delete
import libleveldb.leveldb_free
import libleveldb.leveldb_options_destroy
import libleveldb.leveldb_put
import libleveldb.leveldb_release_snapshot
import libleveldb.leveldb_write
import libleveldb.leveldb_writebatch_create
import libleveldb.leveldb_writebatch_delete
import libleveldb.leveldb_writebatch_destroy
import libleveldb.leveldb_writebatch_put
import libleveldb.leveldb_writeoptions_create
import libleveldb.leveldb_writeoptions_destroy
import libleveldb.leveldb_writeoptions_set_sync

public class NativeLevelDB internal constructor(
    private val nativeDatabase: CPointer<leveldb_t>,
    private val nativeOptions: CPointer<leveldb_options_t>
) : LevelDB {

    override fun put(key: String, value: String, sync: Boolean): Unit = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val writeOptions = leveldb_writeoptions_create()
        leveldb_writeoptions_set_sync(writeOptions, sync.toUByte())
        leveldb_put(
            db = nativeDatabase,
            options = writeOptions,
            key = key,
            keylen = key.length.convert(),
            `val` = value,
            vallen = value.length.convert(),
            errptr = errPtr.ptr
        )
        leveldb_writeoptions_destroy(writeOptions)
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to put value: ${errorValue.toKString()}")
        }
        leveldb_free(errPtr.value)
    }

    override fun get(key: String, verifyChecksums: Boolean, fillCache: Boolean): String? =
        nativeDatabase.get(verifyChecksums, fillCache, key)

    override fun delete(key: String, sync: Boolean): Unit = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val writeOptions = leveldb_writeoptions_create()
        leveldb_writeoptions_set_sync(writeOptions, sync.toUByte())
        leveldb_delete(
            db = nativeDatabase,
            options = writeOptions,
            key = key,
            keylen = key.length.convert(),
            errptr = errPtr.ptr
        )
        leveldb_writeoptions_destroy(writeOptions)
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to delete value: ${errorValue.toKString()}")
        }
    }

    override fun batch(operations: List<LevelDBBatchOperation>, sync: Boolean): Unit = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val nativeBatch = leveldb_writebatch_create()
        for (operation in operations) {
            when (operation) {
                is LevelDBBatchOperation.Delete -> leveldb_writebatch_delete(
                    nativeBatch,
                    operation.key,
                    operation.key.length.convert(),
                )

                is LevelDBBatchOperation.Put -> leveldb_writebatch_put(
                    nativeBatch,
                    operation.key,
                    operation.key.length.convert(),
                    operation.value,
                    operation.value.length.convert()
                )
            }
            val errorValue = errPtr.value
            if (errorValue != null) {
                error("Failed to put values: ${errorValue.toKString()}")
            }
        }
        val writeOptions = leveldb_writeoptions_create()
        leveldb_write(
            db = nativeDatabase,
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

    override fun scan(
        from: String?,
        verifyChecksums: Boolean,
        fillCache: Boolean,
    ): CloseableSequence<LevelDBReader.LazyEntry> =
        nativeDatabase.asSequence(verifyChecksums, fillCache, from)

    override fun <T> withSnapshot(action: LevelDBSnapshot.() -> T): T {
        val nativeSnapshot = leveldb_create_snapshot(nativeDatabase)
            ?: error("Failed to create snapshot")
        return NativeLevelDBSnapshot(nativeSnapshot, nativeDatabase)
            .action()
            .also { leveldb_release_snapshot(nativeDatabase, nativeSnapshot) }
    }

    override fun close() {
        leveldb_close(nativeDatabase)
        leveldb_options_destroy(nativeOptions)
    }

    override fun compactRange(start: String, end: String): Unit = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        leveldb_compact_range(
            db = nativeDatabase,
            start_key = start,
            start_key_len = start.length.convert(),
            limit_key = end,
            limit_key_len = end.length.convert()
        )
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to compact range: ${errorValue.toKString()}")
        }
    }
}
