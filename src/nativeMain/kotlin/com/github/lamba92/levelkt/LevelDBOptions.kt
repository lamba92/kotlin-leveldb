package com.github.lamba92.levelkt

import cnames.structs.leveldb_options_t
import kotlinx.cinterop.CPointer
import kotlinx.serialization.Serializable
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

@Serializable
data class LevelDBOptions(
    val blockRestartInterval: Int = 16,
    val blockSize: Int = 4 * 1024, // 4 KB
//    val cache: ???,
//    val comparator: ???,
    val compression: CompressionType = CompressionType.SNAPPY,
    val createIfMissing: Boolean = false,
//    val env: ???,
    val errorIfExists: Boolean = false,
//  val filterPolicy: ???,
    val maxBytesForLevelBase: Long = 10L * 1024 * 1024, // 10 MB
    val maxBytesForLevelMultiplier: Int = 10,
    val maxFileSize: Long = 2L * 1024 * 1024, // 2 MB
    val maxOpenFiles: Int = 1000,
    val paranoidChecks: Boolean = false,
    val writeBufferSize: Long = 4L * 1024 * 1024, // 4 MB
) {
    enum class CompressionType {
        NONE, SNAPPY
    }
}

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