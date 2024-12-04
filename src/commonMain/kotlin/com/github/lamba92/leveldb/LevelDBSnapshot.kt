package com.github.lamba92.leveldb

import kotlinx.datetime.Instant

/**
 * Represents a snapshot of the LevelDB database at a specific point in time.
 * A snapshot provides a consistent view of the database, unaffected by subsequent changes.
 */
public interface LevelDBSnapshot : LevelDBReader {
    public val createdAt: Instant
}
