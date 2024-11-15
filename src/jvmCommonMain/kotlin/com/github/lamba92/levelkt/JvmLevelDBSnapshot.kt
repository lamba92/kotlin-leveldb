package com.github.lamba92.levelkt

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class JvmLevelDBSnapshot internal constructor(
    private val nativeDatabase: LibLevelDB.leveldb_t,
    private val nativeSnapshot: LibLevelDB.leveldb_snapshot_t
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