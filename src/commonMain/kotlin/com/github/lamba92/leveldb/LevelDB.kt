package com.github.lamba92.leveldb


/**
 * A simple interface for interacting with a LevelDB database.
 * Provides methods to perform various operations such as put, get, delete,
 * batch operations, iteration, snapshot creation, and compaction.
 */
interface LevelDB : AutoCloseable, LevelDBReader {

    /**
     * Inserts or updates a key-value pair in the LevelDB database.
     *
     * @param key The key associated with the value.
     * @param value The value to be stored.
     * @param sync Flag indicating whether to perform a synchronous write, that is
     * to wait for the write to be persisted to disk before returning.
     */
    fun put(key: String, value: String, sync: Boolean = false)

    /**
     * Deletes a key-value pair from the LevelDB database.
     *
     * @param key The key associated with the pair to be deleted.
     * @param sync Flag indicating whether to perform a synchronous delete, that is
     * to wait for the delete operation to be persisted to disk before returning.
     */
    fun delete(key: String, sync: Boolean = false)

    /**
     * Performs a batch operation on the LevelDB database.
     *
     * @param operations A list of batch operations to be performed. Each operation can either be a put or delete action.
     * @param sync Flag indicating whether to perform a synchronous batch operation, that is
     * to wait for all operations to be persisted to disk before returning. Default is false.
     */
    fun batch(operations: List<LevelDBBatchOperation>, sync: Boolean = false)

    /**
     * Executes a provided action within the context of a LevelDB snapshot.
     * This method ensures that the action runs with a consistent view of the database
     * at the time the snapshot is created.
     *
     * @param action The action to execute within the snapshot's context. This is a
     * lambda with receiver that operates on a `LevelDBSnapshot` instance.
     */
    @BrokenNativeAPI("Native function `leveldb_create_snapshot` crashes upon invocation on all platforms.")
    fun <T> withSnapshot(action: LevelDBSnapshot.() -> T): T

    /**
     * Compacts the range of keys between the specified start and end keys in the LevelDB database.
     *
     * @param start The starting key of the range to compact (inclusive).
     * @param end The ending key of the range to compact (exclusive).
     */
    fun compactRange(start: String = "", end: String = "")

}

