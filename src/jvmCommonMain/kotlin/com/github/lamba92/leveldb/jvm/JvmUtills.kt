package com.github.lamba92.leveldb.jvm

import com.github.lamba92.leveldb.CloseableSequence
import com.github.lamba92.leveldb.LevelDBOptions
import com.github.lamba92.leveldb.LevelDBReader
import com.github.lamba92.leveldb.asCloseable
import com.sun.jna.Memory
import com.sun.jna.NativeLong
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference

internal fun ByteArray.toPointer() = Memory(size.toLong()).apply { write(0, this@toPointer, 0, this@toPointer.size) }

internal fun LibLevelDB.leveldb_t.get(
    verifyChecksums: Boolean,
    fillCache: Boolean,
    key: String,
    snapshot: LibLevelDB.leveldb_snapshot_t? = null,
) = with(LibLevelDB.INSTANCE) {
    val errPtr = PointerByReference()
    val nativeReadOptions = leveldb_readoptions_create()
    leveldb_readoptions_set_verify_checksums(nativeReadOptions, verifyChecksums.toByte())
    leveldb_readoptions_set_fill_cache(nativeReadOptions, fillCache.toByte())
    if (snapshot != null) {
        leveldb_readoptions_set_snapshot(nativeReadOptions, snapshot)
    }
    val keyPointer = key.toByteArray().toPointer()
    val valueLengthPointer = LongByReference()
    val value =
        leveldb_get(
            db = this@get,
            options = nativeReadOptions,
            key = keyPointer,
            keylen = key.length.toNativeLong(),
            vallen = valueLengthPointer,
            errptr = errPtr,
        )
    val valueLength = valueLengthPointer.value
    keyPointer.close()
    leveldb_readoptions_destroy(nativeReadOptions)
    val errorValue = errPtr.value?.getString(0)
    if (errorValue != null) {
        error("Failed to get value: $errorValue")
    }
    value?.getByteArray(0, valueLength.toInt())
        ?.toString(Charsets.UTF_8)
}

internal fun LevelDBOptions.toNative() =
    with(LibLevelDB.INSTANCE) {
        val nativeOptions = leveldb_options_create()
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

internal fun LibLevelDB.leveldb_t.asSequence(
    verifyChecksums: Boolean,
    fillCache: Boolean,
    from: String? = null,
    snapshot: LibLevelDB.leveldb_snapshot_t? = null,
): CloseableSequence<LevelDBReader.LazyEntry> =
    with(LibLevelDB.INSTANCE) {
        val nativeOptions = leveldb_readoptions_create()
        leveldb_readoptions_set_verify_checksums(nativeOptions, verifyChecksums.toByte())
        leveldb_readoptions_set_fill_cache(nativeOptions, fillCache.toByte())

        if (snapshot != null) {
            leveldb_readoptions_set_snapshot(nativeOptions, snapshot)
        }
        val iterator = leveldb_create_iterator(this@asSequence, nativeOptions)

        when (from) {
            null -> leveldb_iter_seek_to_first(iterator)
            else -> {
                val fromPointer = from.toByteArray().toPointer()
                leveldb_iter_seek(iterator, fromPointer, from.length.toNativeLong())
                fromPointer.close()
            }
        }

        val keyLengthPointer = LongByReference()
        val valueLengthPointer = LongByReference()
        val seq =
            sequence {
                while (leveldb_iter_valid(iterator) != 0.toByte()) {
                    val key =
                        lazy {
                            val keyPointer = leveldb_iter_key(iterator, keyLengthPointer)
                            keyPointer.getByteArray(0, keyLengthPointer.value.toInt())
                                ?.toString(Charsets.UTF_8)
                                ?: error("Failed to read key")
                        }
                    val value =
                        lazy {
                            val valuePointer = leveldb_iter_value(iterator, valueLengthPointer)
                            valuePointer.getByteArray(0, valueLengthPointer.value.toInt())
                                ?.toString(Charsets.UTF_8)
                                ?: error("Failed to read value for key '${key.value}'")
                        }
                    yield(LevelDBReader.LazyEntry(key, value))
                    leveldb_iter_next(iterator)
                }
            }

        return seq.asCloseable {
            leveldb_iter_destroy(iterator)
            leveldb_readoptions_destroy(nativeOptions)
        }
    }

internal fun Boolean.toByte(): Byte = if (this) 1 else 0

internal fun Number.toNativeLong() = NativeLong(toLong())
