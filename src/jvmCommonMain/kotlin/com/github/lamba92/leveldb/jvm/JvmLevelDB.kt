package com.github.lamba92.leveldb.jvm

import com.github.lamba92.leveldb.BrokenNativeAPI
import com.github.lamba92.leveldb.CloseableSequence
import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.LevelDBBatchOperation
import com.github.lamba92.leveldb.LevelDBReader
import com.github.lamba92.leveldb.LevelDBSnapshot
import com.sun.jna.ptr.PointerByReference
import java.nio.charset.Charset

public class JvmLevelDB internal constructor(
    private val nativeDatabase: LibLevelDB.leveldb_t,
    private val nativeOptions: LibLevelDB.leveldb_options_t,
    private val charset: Charset,
) : LevelDB {
    override fun put(
        key: String,
        value: String,
        sync: Boolean,
    ): Unit =
        with(LibLevelDB) {
            val errPtr = PointerByReference()
            val writeOptions = leveldb_writeoptions_create()
            leveldb_writeoptions_set_sync(writeOptions, sync.toByte())
            val keyPointer = key.toPointer(charset)
            val valuePointer = value.toPointer(charset)
            leveldb_put(
                db = nativeDatabase,
                options = writeOptions,
                key = keyPointer,
                keylen = keyPointer.size().toNativeLong(),
                value = valuePointer,
                vallen = valuePointer.size().toNativeLong(),
                errptr = errPtr,
            )
            valuePointer.close()
            keyPointer.close()
            leveldb_writeoptions_destroy(writeOptions)
            val errorValue = errPtr.value?.getString(0)
            if (errorValue != null) {
                error("Failed to put value: $errorValue")
//            leveldb_free(errPtr.value)
            }
        }

    override fun get(
        key: String,
        verifyChecksums: Boolean,
        fillCache: Boolean,
    ): String? = nativeDatabase.get(verifyChecksums, fillCache, key, charset = charset)

    override fun delete(
        key: String,
        sync: Boolean,
    ): Unit =
        with(LibLevelDB) {
            val errPtr = PointerByReference()
            val writeOptions = leveldb_writeoptions_create()
            leveldb_writeoptions_set_sync(writeOptions, sync.toByte())
            val keyPointer = key.toPointer(charset)
            leveldb_delete(
                db = nativeDatabase,
                options = writeOptions,
                key = keyPointer,
                keylen = keyPointer.size().toNativeLong(),
                errptr = errPtr,
            )
            keyPointer.clear(key.length.toLong())
            leveldb_writeoptions_destroy(writeOptions)
            val errorValue = errPtr.value?.getString(0)
            if (errorValue != null) {
                leveldb_free(errPtr.value)
                error("Failed to delete value: $errorValue")
            }
        }

    override fun batch(
        operations: List<LevelDBBatchOperation>,
        sync: Boolean,
    ): Unit =
        with(LibLevelDB) {
            val errPtr = PointerByReference()
            val nativeBatch = leveldb_writebatch_create()
            for (operation in operations) {
                when (operation) {
                    is LevelDBBatchOperation.Put -> {
                        val keyPointer = operation.key.toPointer(charset)
                        val valuePointer = operation.value.toPointer(charset)
                        leveldb_writebatch_put(
                            batch = nativeBatch,
                            key = keyPointer,
                            klen = keyPointer.size().toNativeLong(),
                            value = valuePointer,
                            vlen = valuePointer.size().toNativeLong(),
                        )
                        keyPointer.close()
                        valuePointer.close()
                    }

                    is LevelDBBatchOperation.Delete -> {
                        val keyPointer = operation.key.toPointer(charset)
                        leveldb_writebatch_delete(
                            batch = nativeBatch,
                            key = keyPointer,
                            klen = keyPointer.size().toNativeLong(),
                        )
                        keyPointer.close()
                    }
                }
            }
            val writeOptions = leveldb_writeoptions_create()
            leveldb_writeoptions_set_sync(writeOptions, sync.toByte())
            leveldb_write(
                db = nativeDatabase,
                options = writeOptions,
                batch = nativeBatch,
                errptr = errPtr,
            )
            leveldb_writeoptions_destroy(writeOptions)
            leveldb_writebatch_destroy(nativeBatch)
            val errorValue = errPtr.value?.getString(0)
            if (errorValue != null) {
                leveldb_free(errPtr.value)
                error("Failed to put values: $errorValue")
            }
        }

    override fun scan(
        from: String?,
        verifyChecksums: Boolean,
        fillCache: Boolean,
    ): CloseableSequence<LevelDBReader.LazyEntry> =
        nativeDatabase.asSequence(
            verifyChecksums,
            fillCache,
            from,
            charset = charset,
        )

    @BrokenNativeAPI
    override fun <T> withSnapshot(action: LevelDBSnapshot.() -> T): T {
        val nativeSnapshot = LibLevelDB.leveldb_create_snapshot(nativeDatabase)
        return try {
            action(JvmLevelDBSnapshot(nativeDatabase, nativeSnapshot, charset))
        } finally {
            LibLevelDB.leveldb_release_snapshot(nativeDatabase, nativeSnapshot)
        }
    }

    override fun compactRange(
        start: String,
        end: String,
    ) {
        val startPointer =
            start
                .takeIf { it.isNotEmpty() }
                ?.toPointer(charset)
        val endPointer =
            end
                .takeIf { it.isNotEmpty() }
                ?.toPointer(charset)
        LibLevelDB.leveldb_compact_range(
            db = nativeDatabase,
            start_key = startPointer,
            start_key_len =
                start
                    .takeIf { it.isNotEmpty() }
                    ?.length
                    ?.toNativeLong()
                    ?: 0.toNativeLong(),
            limit_key = endPointer,
            limit_key_len =
                end
                    .takeIf { it.isNotEmpty() }
                    ?.length
                    ?.toNativeLong()
                    ?: 0.toNativeLong(),
        )
        startPointer?.close()
        endPointer?.close()
    }

    override fun close() {
        LibLevelDB.leveldb_close(nativeDatabase)
        LibLevelDB.leveldb_options_destroy(nativeOptions)
    }
}
