package com.github.lamba92.leveldb.jvm

import com.github.lamba92.leveldb.CloseableSequence
import com.github.lamba92.leveldb.LevelDBReader
import com.github.lamba92.leveldb.LevelDBSnapshot
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.nio.charset.Charset

public class JvmLevelDBSnapshot internal constructor(
    private val nativeDatabase: LibLevelDB.leveldb_t,
    private val nativeSnapshot: LibLevelDB.leveldb_snapshot_t,
    private val charset: Charset,
) : LevelDBSnapshot {
    override val createdAt: Instant = Clock.System.now()

    override fun get(
        key: String,
        verifyChecksums: Boolean,
        fillCache: Boolean,
    ): String? =
        nativeDatabase.get(
            verifyChecksums = verifyChecksums,
            fillCache = fillCache,
            key = key,
            snapshot = nativeSnapshot,
            charset = charset,
        )

    override fun scan(
        from: String?,
        verifyChecksums: Boolean,
        fillCache: Boolean,
    ): CloseableSequence<LevelDBReader.LazyEntry> =
        nativeDatabase.asSequence(
            verifyChecksums = verifyChecksums,
            fillCache = fillCache,
            from = from,
            snapshot = nativeSnapshot,
            charset = charset,
        )
}
