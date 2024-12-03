@file:Suppress("FunctionName", "MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")

package com.github.lamba92.leveldb.native

import cnames.structs.leveldb_iterator_t
import cnames.structs.leveldb_options_t
import cnames.structs.leveldb_snapshot_t
import cnames.structs.leveldb_t
import com.github.lamba92.leveldb.LevelDBOptions
import com.github.lamba92.leveldb.LevelDBReader
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import libleveldb.leveldb_create_iterator
import libleveldb.leveldb_get
import libleveldb.leveldb_iter_destroy
import libleveldb.leveldb_iter_key
import libleveldb.leveldb_iter_next
import libleveldb.leveldb_iter_seek
import libleveldb.leveldb_iter_seek_to_first
import libleveldb.leveldb_iter_valid
import libleveldb.leveldb_iter_value
import libleveldb.leveldb_options_create
import libleveldb.leveldb_options_set_block_restart_interval
import libleveldb.leveldb_options_set_block_size
import libleveldb.leveldb_options_set_compression
import libleveldb.leveldb_options_set_create_if_missing
import libleveldb.leveldb_options_set_error_if_exists
import libleveldb.leveldb_options_set_max_file_size
import libleveldb.leveldb_options_set_max_open_files
import libleveldb.leveldb_options_set_paranoid_checks
import libleveldb.leveldb_options_set_write_buffer_size
import libleveldb.leveldb_readoptions_create
import libleveldb.leveldb_readoptions_destroy
import libleveldb.leveldb_readoptions_set_fill_cache
import libleveldb.leveldb_readoptions_set_snapshot
import libleveldb.leveldb_readoptions_set_verify_checksums
import platform.posix.size_tVar

internal fun Boolean.toUByte(): UByte = if (this) 1u else 0u

internal fun LevelDBOptions.toNative(): CPointer<leveldb_options_t> {
    val nativeOptions = leveldb_options_create() ?: error("Failed to create native options")
    leveldb_options_set_block_restart_interval(nativeOptions, blockRestartInterval)
    leveldb_options_set_block_size(nativeOptions, blockSize.convert())
//    leveldb_options_set_cache(nativeOptions, )
//    leveldb_options_set_comparator(nativeOptions, )
    leveldb_options_set_compression(nativeOptions, compression.ordinal)
    leveldb_options_set_create_if_missing(nativeOptions, createIfMissing.toUByte())
//    leveldb_options_set_env(nativeOptions, )
    leveldb_options_set_error_if_exists(nativeOptions, errorIfExists.toUByte())
//    leveldb_options_set_filter_policy(nativeOptions, )
//    leveldb_options_set_info_log(nativeOptions, null)
    leveldb_options_set_max_file_size(nativeOptions, maxFileSize.convert())
    leveldb_options_set_max_open_files(nativeOptions, maxOpenFiles.convert())
    leveldb_options_set_paranoid_checks(nativeOptions, paranoidChecks.toUByte())
    leveldb_options_set_write_buffer_size(nativeOptions, writeBufferSize.convert())
    return nativeOptions
}

internal fun CPointer<leveldb_t>.get(
    verifyChecksums: Boolean,
    fillCache: Boolean,
    key: String,
    snapshot: CPointer<leveldb_snapshot_t>? = null
) =
    memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val valueLengthPointer = alloc<size_tVar>()
        val nativeReadOptions = leveldb_readoptions_create()
        leveldb_readoptions_set_verify_checksums(nativeReadOptions, verifyChecksums.toUByte())
        leveldb_readoptions_set_fill_cache(nativeReadOptions, fillCache.toUByte())
        if (snapshot != null) {
            leveldb_readoptions_set_snapshot(nativeReadOptions, snapshot)
        }
        val value = leveldb_get(
            db = this@get,
            options = nativeReadOptions,
            key = key,
            keylen = key.length.convert(),
            vallen = valueLengthPointer.ptr,
            errptr = errPtr.ptr
        )
        leveldb_readoptions_destroy(nativeReadOptions)
        val errorValue = errPtr.value
        if (errorValue != null) {
            error("Failed to get value: ${errorValue.toKString()}")
        }
        value?.readBytes(valueLengthPointer.value.toInt())?.toKString()
    }



internal fun <T> CPointer<leveldb_t>.sequence(
    verifyChecksums: Boolean,
    fillCache: Boolean,
    action: (Sequence<LevelDBReader.Entry>) -> T,
    from: String? = null,
    nativeSnapshot: CPointer<leveldb_snapshot_t>? = null,
): T {
    val nativeOptions = leveldb_readoptions_create()
        ?: error("Failed to create read options")
    leveldb_readoptions_set_verify_checksums(nativeOptions, verifyChecksums.toUByte())
    leveldb_readoptions_set_fill_cache(nativeOptions, fillCache.toUByte())
    if (nativeSnapshot != null) {
        leveldb_readoptions_set_snapshot(nativeOptions, nativeSnapshot)
    }
    val nativeIterator: CPointer<leveldb_iterator_t> = leveldb_create_iterator(this, nativeOptions)
        ?: error("Failed to create iterator")

    // Position iterator based on the starting point
    when (from) {
        null -> leveldb_iter_seek_to_first(nativeIterator)
        else -> leveldb_iter_seek(nativeIterator, from, from.length.convert())
    }

    val seq = sequence {
        while (leveldb_iter_valid(nativeIterator) != 0.toUByte()) {
            val keyValue = memScoped {
                val anInteger = alloc<size_tVar>()
                val key = leveldb_iter_key(nativeIterator, anInteger.ptr)
                    ?.readBytes(anInteger.value.toInt())
                    ?.toKString() ?: error("Failed to read key")
                val value = leveldb_iter_value(nativeIterator, anInteger.ptr)
                    ?.readBytes(anInteger.value.toInt())
                    ?.toKString() ?: error("Failed to read value for key '$key'")
                LevelDBReader.Entry(key, value)
            }
            yield(keyValue)
            leveldb_iter_next(nativeIterator)
        }
    }

    return try {
        val returnValue = action(seq)
        if (returnValue == seq) {
            error("Do not leak the sequence outside of the action block, it will result in a crash")
        }
        returnValue
    } finally {
        leveldb_iter_destroy(nativeIterator)
        leveldb_readoptions_destroy(nativeOptions)
    }
}

