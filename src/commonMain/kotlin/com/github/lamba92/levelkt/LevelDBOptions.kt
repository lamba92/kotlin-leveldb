package com.github.lamba92.levelkt

import kotlinx.serialization.Serializable

@Serializable
data class LevelDBOptions(
    val blockRestartInterval: Int = 16,
    val blockSize: Int = 4 * 1024, // 4 KB
//    val cache: ???,
//    val comparator: ???,
    val compression: CompressionType = CompressionType.SNAPPY,
    val createIfMissing: Boolean = true,
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

interface LevelDBSnapshot : AutoCloseable
