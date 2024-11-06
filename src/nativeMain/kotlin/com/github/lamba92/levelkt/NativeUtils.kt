@file:Suppress("FunctionName")

package com.github.lamba92.levelkt

import cnames.structs.leveldb_options_t
import cnames.structs.leveldb_readoptions_t
import cnames.structs.leveldb_t
import cnames.structs.leveldb_writeoptions_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.UIntVarOf
import libleveldb.leveldb_get
import libleveldb.leveldb_options_create
import libleveldb.leveldb_options_set_block_restart_interval
import libleveldb.leveldb_options_set_compression
import libleveldb.leveldb_options_set_create_if_missing
import libleveldb.leveldb_options_set_error_if_exists
import libleveldb.leveldb_options_set_info_log
import libleveldb.leveldb_options_set_max_file_size
import libleveldb.leveldb_options_set_max_open_files
import libleveldb.leveldb_options_set_paranoid_checks
import libleveldb.leveldb_options_set_write_buffer_size
import libleveldb.leveldb_readoptions_create
import libleveldb.leveldb_readoptions_set_fill_cache
import libleveldb.leveldb_readoptions_set_snapshot
import libleveldb.leveldb_readoptions_set_verify_checksums
import platform.posix.size_t

internal fun Boolean.toUByte(): UByte = if (this) 1u else 0u

fun LevelDBOptions.toNative(): CPointer<leveldb_options_t> {
    val nativeOptions = leveldb_options_create() ?: error("Failed to create native options")
    leveldb_options_set_block_restart_interval(nativeOptions, blockRestartInterval)
    platform_specific_leveldb_options_set_block_size(nativeOptions, blockSize)
//    leveldb_options_set_cache(nativeOptions, )
//    leveldb_options_set_comparator(nativeOptions, )
    leveldb_options_set_compression(nativeOptions, compression.ordinal)
    leveldb_options_set_create_if_missing(nativeOptions, createIfMissing.toUByte())
//    leveldb_options_set_env(nativeOptions, )
    leveldb_options_set_error_if_exists(nativeOptions, errorIfExists.toUByte())
//    leveldb_options_set_filter_policy(nativeOptions, )
    leveldb_options_set_info_log(nativeOptions, null)
    leveldb_options_set_max_file_size(nativeOptions, maxFileSize.toUInt())
    leveldb_options_set_max_open_files(nativeOptions, maxOpenFiles)
    leveldb_options_set_paranoid_checks(nativeOptions, paranoidChecks.toUByte())
    leveldb_options_set_write_buffer_size(nativeOptions, writeBufferSize.toUInt())
    return nativeOptions
}

fun LevelDBReadOptions.toNative(): CPointer<leveldb_readoptions_t> {
    val nativeOptions = leveldb_readoptions_create()
        ?: error("Failed to create native read options")
    leveldb_readoptions_set_fill_cache(nativeOptions, fillCache.toUByte())
    leveldb_readoptions_set_verify_checksums(nativeOptions, verifyChecksums.toUByte())
    if (snapshot is NativeLevelDBSnapshot) {
        leveldb_readoptions_set_snapshot(nativeOptions, snapshot.delegate)
    }
    return nativeOptions
}

expect fun platform_specific_leveldb_options_set_block_size(nativeOptions: CPointer<leveldb_options_t>, blockSize: Int)

expect fun platform_specific_leveldb_put(
    db: CPointer<leveldb_t>,
    options: CPointer<leveldb_writeoptions_t>?,
    key: String,
    value: String,
    errptr: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>
)

expect fun platform_specific_leveldb_get(
    db: CPointer<leveldb_t>,
    options: CPointer<leveldb_readoptions_t>,
    key: String,
    errptr: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>
): String?