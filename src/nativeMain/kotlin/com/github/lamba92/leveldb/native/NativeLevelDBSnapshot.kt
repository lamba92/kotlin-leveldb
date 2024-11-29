package com.github.lamba92.leveldb.native

import cnames.structs.leveldb_snapshot_t
import cnames.structs.leveldb_t
import com.github.lamba92.leveldb.LevelDBReader
import com.github.lamba92.leveldb.LevelDBSnapshot
import kotlinx.cinterop.CPointer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant


class NativeLevelDBSnapshot(
    private val nativeSnapshot: CPointer<leveldb_snapshot_t>,
    private val nativeDatabase: CPointer<leveldb_t>
) : LevelDBSnapshot {

    override val createdAt: Instant = Clock.System.now()

    override fun get(key: String, verifyChecksums: Boolean, fillCache: Boolean): String? =
        nativeDatabase.get(verifyChecksums, fillCache, key, nativeSnapshot)

    override fun <T> scan(
        from: String?,
        verifyChecksums: Boolean,
        fillCache: Boolean,
        action: (Sequence<LevelDBReader.Entry>) -> T
    ): T = nativeDatabase.sequence(verifyChecksums, fillCache, action, from, nativeSnapshot)
}