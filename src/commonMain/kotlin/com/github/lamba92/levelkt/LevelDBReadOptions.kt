package com.github.lamba92.levelkt

/**
 * Options to customize read operations in LevelDB.
 *
 * @property verifyChecksums If true, the read will verify checksums for data blocks. Defaults to `false`.
 * @property fillCache If true, the block read for this iteration will be cached in memory for future reads. Defaults to `true`.
 * @property snapshot A specific snapshot to use for the read operation. If null, the read will operate on the latest state of the database.
 */
class LevelDBReadOptions(
    val verifyChecksums: Boolean = false,
    val fillCache: Boolean = true,
    val snapshot: LevelDBSnapshot? = null
) {
    companion object {
        val DEFAULT = LevelDBReadOptions()
    }
}