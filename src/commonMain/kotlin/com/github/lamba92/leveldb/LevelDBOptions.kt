package com.github.lamba92.leveldb

import kotlinx.serialization.Serializable

/**
 * Configuration options for creating and managing a LevelDB instance.
 *
 * @property blockRestartInterval Number of keys between restart points for delta encoding of keys.
 * @property blockSize Approximate size of user data packed per block, in bytes. Default is 4 KB.
 * @property compression Compression algorithm to use for blocks.
 * @property createIfMissing Whether to create the database if it does not exist.
 * @property errorIfExists Whether to throw an error if the database already exists.
 * @property maxBytesForLevelBase Maximum bytes for level base. Default is 10 MB.
 * @property maxBytesForLevelMultiplier Multiplier for the maximum bytes for levels.
 * @property maxFileSize Maximum size of a single file, in bytes. Default is 2 MB.
 * @property maxOpenFiles Maximum number of open files that can be used by the database.
 * @property paranoidChecks Whether to perform paranoid checks for file consistency.
 * @property writeBufferSize Amount of data to build up in memory (backed by an
 * unsorted log on disk) before converting to a sorted on-disk file, in bytes.
 * Default is 4 MB.
 */
@Serializable
public data class LevelDBOptions(
    val blockRestartInterval: Int = 16,
    val blockSize: Int = 4 * 1024,
//    val cache: ???,
//    val comparator: ???,
    val compression: CompressionType = CompressionType.SNAPPY,
    val createIfMissing: Boolean = true,
//    val env: ???,
    val errorIfExists: Boolean = false,
//  val filterPolicy: ???,
    val maxBytesForLevelBase: Long = 10L * 1024 * 1024,
    val maxBytesForLevelMultiplier: Int = 10,
    val maxFileSize: Long = 2L * 1024 * 1024,
    val maxOpenFiles: Int = 1000,
    val paranoidChecks: Boolean = false,
    val writeBufferSize: Long = 4L * 1024 * 1024,
) {
    public companion object {
        public val DEFAULT: LevelDBOptions = LevelDBOptions()
    }

    /**
     * Enum representing the types of compression algorithms that can be used for
     * compressing data blocks in LevelDB.
     *
     * - `NONE`: No compression.
     * - `SNAPPY`: Compression using the Snappy algorithm.
     */
    public enum class CompressionType {
        NONE,
        SNAPPY,
    }
}
