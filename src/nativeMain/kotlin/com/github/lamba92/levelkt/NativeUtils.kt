package com.github.lamba92.levelkt

import cnames.structs.leveldb_options_t
import kotlinx.cinterop.CPointer
import libleveldb.leveldb_options_create
import libleveldb.leveldb_options_set_block_restart_interval
import libleveldb.leveldb_options_set_block_size
import libleveldb.leveldb_options_set_compression
import libleveldb.leveldb_options_set_create_if_missing
import libleveldb.leveldb_options_set_error_if_exists
import libleveldb.leveldb_options_set_info_log
import libleveldb.leveldb_options_set_max_file_size
import libleveldb.leveldb_options_set_max_open_files
import libleveldb.leveldb_options_set_paranoid_checks
import libleveldb.leveldb_options_set_write_buffer_size

internal fun Boolean.toUByte(): UByte = if (this) 1u else 0u

fun LevelDBOptions.toNative(): CPointer<leveldb_options_t> {
    val nativeOptions = leveldb_options_create() ?: error("Failed to create native options")
    leveldb_options_set_block_restart_interval(nativeOptions, blockRestartInterval)
    leveldb_options_set_block_size(nativeOptions, blockSize.toUInt())
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