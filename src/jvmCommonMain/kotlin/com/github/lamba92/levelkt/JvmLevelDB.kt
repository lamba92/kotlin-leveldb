package com.github.lamba92.levelkt

import com.sun.jna.ptr.PointerByReference

class JvmLevelDB internal constructor(
    private val nativeDatabase: LibLevelDB.leveldb_t,
    private val nativeOptions: LibLevelDB.leveldb_options_t
) : LevelDB {
    override fun put(key: String, value: String, sync: Boolean): Unit = with(LibLevelDB.INSTANCE) {
        val errPtr = PointerByReference()
        val writeOptions = leveldb_writeoptions_create()
        leveldb_writeoptions_set_sync(writeOptions, sync.toByte())
        val keyPointer = key.toByteArray().toPointer()
        val valuePointer = value.toByteArray().toPointer()
        leveldb_put(
            db = nativeDatabase,
            options = writeOptions,
            key = keyPointer,
            keylen = key.length.toNativeLong(),
            value = valuePointer,
            vallen = value.length.toNativeLong(),
            errptr = errPtr
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

    override fun get(key: String, verifyChecksums: Boolean, fillCache: Boolean) =
        nativeDatabase.get(verifyChecksums, fillCache, key)

    override fun delete(key: String, sync: Boolean) = with(LibLevelDB.INSTANCE) {
        val errPtr = PointerByReference()
        val writeOptions = leveldb_writeoptions_create()
        leveldb_writeoptions_set_sync(writeOptions, sync.toByte())
        val keyPointer = key.toByteArray().toPointer()
        leveldb_delete(
            db = nativeDatabase,
            options = writeOptions,
            key = keyPointer,
            keylen = key.length.toNativeLong(),
            errptr = errPtr
        )
        keyPointer.clear(key.length.toLong())
        leveldb_writeoptions_destroy(writeOptions)
        val errorValue = errPtr.value?.getString(0)
        if (errorValue != null) {
            leveldb_free(errPtr.value)
            error("Failed to delete value: $errorValue")
        }
    }

    override fun batch(operations: List<LevelDBBatchOperation>, sync: Boolean) =
        with(LibLevelDB.INSTANCE) {
            val errPtr = PointerByReference()
            val nativeBatch = leveldb_writebatch_create()
            for (operation in operations) {
                when (operation) {
                    is LevelDBBatchOperation.Put -> {
                        val keyPointer = operation.key.toByteArray().toPointer()
                        val valuePointer = operation.value.toByteArray().toPointer()
                        leveldb_writebatch_put(
                            batch = nativeBatch,
                            key = keyPointer,
                            klen = operation.key.length.toNativeLong(),
                            value = valuePointer,
                            vlen = operation.value.length.toNativeLong()
                        )
                        keyPointer.close()
                        valuePointer.close()
                    }

                    is LevelDBBatchOperation.Delete -> {
                        val keyPointer = operation.key.toByteArray().toPointer()
                        leveldb_writebatch_delete(
                            batch = nativeBatch,
                            key = keyPointer,
                            klen = operation.key.length.toNativeLong()
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
                errptr = errPtr
            )
            leveldb_writeoptions_destroy(writeOptions)
            leveldb_writebatch_destroy(nativeBatch)
            val errorValue = errPtr.value?.getString(0)
            if (errorValue != null) {
                leveldb_free(errPtr.value)
                error("Failed to put values: $errorValue")
            }
        }

    override fun <T> scan(
        from: String?,
        verifyChecksums: Boolean,
        fillCache: Boolean,
        action: (Sequence<LevelDBReader.Entry>) -> T
    ): T = nativeDatabase.sequence(verifyChecksums, fillCache, action, from)

    @BrokenNativeAPI
    override fun <T> withSnapshot(action: LevelDBSnapshot.() -> T): T {
        val nativeSnapshot = LibLevelDB.INSTANCE.leveldb_create_snapshot(nativeDatabase)
        return try {
            action(JvmLevelDBSnapshot(nativeDatabase, nativeSnapshot))
        } finally {
            LibLevelDB.INSTANCE.leveldb_release_snapshot(nativeDatabase, nativeSnapshot)
        }
    }

    override fun compactRange(start: String, end: String) {
        val startPointer = start
            .takeIf { it.isNotEmpty() }
            ?.toByteArray()
            ?.toPointer()
        val endPointer = end
            .takeIf { it.isNotEmpty() }
            ?.toByteArray()
            ?.toPointer()
        LibLevelDB.INSTANCE.leveldb_compact_range(
            db = nativeDatabase,
            start_key = startPointer,
            start_key_len = start
                .takeIf { it.isNotEmpty() }
                ?.length
                ?.toNativeLong()
                ?: 0.toNativeLong(),
            limit_key = endPointer,
            limit_key_len = end
                .takeIf { it.isNotEmpty() }
                ?.length
                ?.toNativeLong()
                ?: 0.toNativeLong()
        )
        startPointer?.close()
        endPointer?.close()
    }

    override fun close() {
        LibLevelDB.INSTANCE.leveldb_close(nativeDatabase)
        LibLevelDB.INSTANCE.leveldb_options_destroy(nativeOptions)
    }
}