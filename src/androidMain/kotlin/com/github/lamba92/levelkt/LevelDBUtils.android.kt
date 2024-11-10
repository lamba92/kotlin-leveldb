package com.github.lamba92.levelkt

import com.github.lamba92.levelkt.LibLevelDB.leveldb_options_t
import com.github.lamba92.levelkt.LibLevelDB.leveldb_snapshot_t
import com.github.lamba92.levelkt.LibLevelDB.leveldb_t
import com.sun.jna.Memory
import com.sun.jna.NativeLong
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual fun LevelDB(
    path: String,
    options: LevelDBOptions
): LevelDB {
    val nativeOptions = options.toNative()
    val errPtr = PointerByReference()
    val nativeDelegate = LibLevelDB.INSTANCE.leveldb_open(nativeOptions, path, errPtr)
    val errorValue = errPtr.value?.getString(0)
    if (errorValue != null) {
        error("Failed to open database: $errorValue")
    }
    LibLevelDB.INSTANCE.leveldb_free(errPtr.value)
    return NativeLevelDB(nativeDelegate, nativeOptions)
}

class NativeLevelDB internal constructor(
    private val nativeDatabase: leveldb_t,
    private val nativeOptions: leveldb_options_t
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
        valuePointer.clear(value.length.toLong())
        keyPointer.clear(key.length.toLong())
        leveldb_writeoptions_destroy(writeOptions)
        val errorValue = errPtr.value?.getString(0)
        leveldb_free(errPtr.value)
        if (errorValue != null) {
            error("Failed to put value: $errorValue")
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
        leveldb_free(errPtr.value)
        if (errorValue != null) {
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
                        keyPointer.clear(operation.key.length.toLong())
                        valuePointer.clear(operation.value.length.toLong())
                    }

                    is LevelDBBatchOperation.Delete -> {
                        val keyPointer = operation.key.toByteArray().toPointer()
                        leveldb_writebatch_delete(
                            batch = nativeBatch,
                            key = keyPointer,
                            klen = operation.key.length.toNativeLong()
                        )
                        keyPointer.clear(operation.key.length.toLong())
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
            leveldb_free(errPtr.value)
            if (errorValue != null) {
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
            action(NativeLevelDBSnapshot(nativeDatabase, nativeSnapshot))
        } finally {
            LibLevelDB.INSTANCE.leveldb_release_snapshot(nativeDatabase, nativeSnapshot)
        }
    }

    override fun compactRange(start: String, end: String) {
        val startPointer = start.toByteArray().toPointer()
        val endPointer = end.toByteArray().toPointer()
        LibLevelDB.INSTANCE.leveldb_compact_range(
            db = nativeDatabase,
            start_key = startPointer,
            start_key_len = start.length.toNativeLong(),
            limit_key = endPointer,
            limit_key_len = end.length.toNativeLong()
        )
        startPointer.clear(start.length.toLong())
        endPointer.clear(end.length.toLong())
    }

    override fun close() {
        LibLevelDB.INSTANCE.leveldb_close(nativeDatabase)
        LibLevelDB.INSTANCE.leveldb_options_destroy(nativeOptions)
    }
}

class NativeLevelDBSnapshot internal constructor(
    private val nativeDatabase: leveldb_t,
    private val nativeSnapshot: leveldb_snapshot_t
) : LevelDBSnapshot {

    override val createdAt: Instant = Clock.System.now()

    override fun get(key: String, verifyChecksums: Boolean, fillCache: Boolean) =
        nativeDatabase.get(verifyChecksums, fillCache, key, nativeSnapshot)

    override fun <T> scan(
        from: String?,
        verifyChecksums: Boolean,
        fillCache: Boolean,
        action: (Sequence<LevelDBReader.Entry>) -> T
    ): T = nativeDatabase.sequence(verifyChecksums, fillCache, action, from, nativeSnapshot)
}

private fun ByteArray.toPointer() =
    Memory(this.size.toLong()).apply { write(0, this@toPointer, 0, this@toPointer.size) }

internal fun leveldb_t.get(
    verifyChecksums: Boolean,
    fillCache: Boolean,
    key: String,
    snapshot: leveldb_snapshot_t? = null
) =
    with(LibLevelDB.INSTANCE) {
        val errPtr = PointerByReference()
        val nativeReadOptions = leveldb_readoptions_create()
        leveldb_readoptions_set_verify_checksums(nativeReadOptions, verifyChecksums.toByte())
        leveldb_readoptions_set_fill_cache(nativeReadOptions, fillCache.toByte())
        if (snapshot != null) {
            leveldb_readoptions_set_snapshot(nativeReadOptions, snapshot)
        }
        val keyPointer = key.toByteArray().toPointer()
        val valueLengthPointer = LongByReference()
        val value = leveldb_get(
            db = this@get,
            options = nativeReadOptions,
            key = keyPointer,
            keylen = key.length.toNativeLong(),
            vallen = valueLengthPointer,
            errptr = errPtr
        )
        val valueLength = valueLengthPointer.value
        keyPointer.clear(key.length.toLong())
        leveldb_free(valueLengthPointer.pointer)
        leveldb_readoptions_destroy(nativeReadOptions)
        val errorValue = errPtr.value?.getString(0)
        if (errorValue != null) {
            error("Failed to get value: $errorValue")
        }
        value.getByteArray(0, valueLength.toInt())
            ?.toString(Charsets.UTF_8)
    }

internal fun LevelDBOptions.toNative() = with(LibLevelDB.INSTANCE) {
    val nativeOptions = leveldb_options_create()
        ?: error("Failed to create native options")
    leveldb_options_set_block_restart_interval(nativeOptions, blockRestartInterval)
    leveldb_options_set_block_size(nativeOptions, blockSize.toNativeLong())
//    leveldb_options_set_cache(nativeOptions, )
//    leveldb_options_set_comparator(nativeOptions, )
    leveldb_options_set_compression(nativeOptions, compression.ordinal)
    leveldb_options_set_create_if_missing(nativeOptions, createIfMissing.toByte())
//    leveldb_options_set_env(nativeOptions, )
    leveldb_options_set_error_if_exists(nativeOptions, errorIfExists.toByte())
//    leveldb_options_set_filter_policy(nativeOptions, )
//    leveldb_options_set_info_log(nativeOptions, null)
    leveldb_options_set_max_file_size(nativeOptions, maxFileSize.toNativeLong())
    leveldb_options_set_max_open_files(nativeOptions, maxOpenFiles)
    leveldb_options_set_paranoid_checks(nativeOptions, paranoidChecks.toByte())
    leveldb_options_set_write_buffer_size(nativeOptions, writeBufferSize.toNativeLong())
    nativeOptions
}

internal fun <T> leveldb_t.sequence(
    verifyChecksums: Boolean,
    fillCache: Boolean,
    action: (Sequence<LevelDBReader.Entry>) -> T,
    from: String? = null,
    snapshot: leveldb_snapshot_t? = null
): T = with(LibLevelDB.INSTANCE) {
    val nativeOptions = leveldb_readoptions_create()
    leveldb_readoptions_set_verify_checksums(nativeOptions, verifyChecksums.toByte())
    leveldb_readoptions_set_fill_cache(nativeOptions, fillCache.toByte())
    if (snapshot != null) {
        leveldb_readoptions_set_snapshot(nativeOptions, snapshot)
    }
    val iterator = leveldb_create_iterator(this@sequence, nativeOptions)

    when (from) {
        null -> leveldb_iter_seek_to_first(iterator)
        else -> {
            val fromPointer = from.toByteArray().toPointer()
            leveldb_iter_seek(iterator, fromPointer, from.length.toNativeLong())
            fromPointer.clear(from.length.toLong())
        }
    }

    val keyLengthPointer = LongByReference()
    val valueLengthPointer = LongByReference()
    val seq = sequence {
        while (leveldb_iter_valid(iterator) != 0.toByte()) {
            val keyPointer = leveldb_iter_key(iterator, keyLengthPointer)
            val valuePointer = leveldb_iter_value(iterator, valueLengthPointer)
            val key = keyPointer
                .getByteArray(0, keyLengthPointer.value.toInt())
                ?.toString(Charsets.UTF_8)
                ?: error("Failed to read key")
            val value = valuePointer
                .getByteArray(0, valueLengthPointer.value.toInt())
                ?.toString(Charsets.UTF_8)
                ?: error("Failed to read value for key '$key'")
            keyPointer.clear(keyLengthPointer.value)
            valuePointer.clear(valueLengthPointer.value)
            yield(LevelDBReader.Entry(key, value))
            leveldb_iter_next(iterator)
        }
    }

    return try {
        action(seq)
    } finally {
        leveldb_free(keyLengthPointer.pointer)
        leveldb_free(valueLengthPointer.pointer)
        leveldb_iter_destroy(iterator)
        leveldb_readoptions_destroy(nativeOptions)
    }
}


private fun Boolean.toByte(): Byte = if (this) 1 else 0
private fun Number.toNativeLong() = NativeLong(this.toLong())

/**
 * Repairs the database at the specified path using the provided options.
 *
 * @param path The path to the database that needs to be repaired.
 * @param options The options to configure the repair process.
 */
actual fun repairDatabase(path: String, options: LevelDBOptions) = with(LibLevelDB.INSTANCE) {
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

actual fun destroyDatabase(path: String, options: LevelDBOptions) = with(LibLevelDB.INSTANCE) {
    val nativeOptions = options.toNative()
    val errPtr = PointerByReference()
    leveldb_destroy_db(nativeOptions, path, errPtr)
    val errorValue = errPtr.value?.getString(0)
    leveldb_free(errPtr.value)
    if (errorValue != null) {
        error("Failed to destroy database: $errorValue")
    }
    leveldb_options_destroy(nativeOptions)
}