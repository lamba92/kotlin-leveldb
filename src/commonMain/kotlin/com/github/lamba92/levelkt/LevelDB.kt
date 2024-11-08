package com.github.lamba92.levelkt


expect fun LevelDB(path: String, options: LevelDBOptions = LevelDBOptions()): LevelDB

/**
 * A simple interface for interacting with a LevelDB database.
 * Provides methods to perform various operations such as put, get, delete,
 * batch operations, iteration, snapshot creation, and compaction.
 */
interface LevelDB : AutoCloseable, Iterable<Pair<String, String>> {

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
     * Retrieves the value associated with a key from the LevelDB database.
     *
     * @param key The key to look up.
     * @param options Options to customize the read operation.
     * @return The value associated with the key, or null if the key does not exist.
     */
    fun get(key: String, options: LevelDBReadOptions = LevelDBReadOptions.DEFAULT): String?

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
     * Creates an iterator to traverse all key-value pairs in the LevelDB database.
     *
     * @param options Options to customize the read operation for the iterator.
     * @return An iterator over key-value pairs, where each pair consists of a key and its associated value.
     */
    fun iterator(options: LevelDBReadOptions = LevelDBReadOptions.DEFAULT): Iterator<Pair<String, String>>

    override fun iterator() =
        iterator(options = LevelDBReadOptions.DEFAULT)


    /**
     * Creates a snapshot of the current state of the LevelDB database.
     *
     * @return A LevelDBSnapshot representing the current state. It can be used to perform read operations on a
     * consistent view of the database.
     */
    fun createSnapshot(): LevelDBSnapshot


    /**
     * Compacts the range of keys between the specified start and end keys in the LevelDB database.
     *
     * @param start The starting key of the range to compact (inclusive).
     * @param end The ending key of the range to compact (exclusive).
     */
    fun compactRange(start: String, end: String)

}

/**
 * Repairs the database at the specified path using the provided options.
 *
 * @param path The path to the database that needs to be repaired.
 * @param options The options to configure the repair process.
 */
expect fun repairDatabase(path: String, options: LevelDBOptions)

/**
 * Destroys the database located at the specified path using the provided options.
 *
 * @param path The filesystem path to the database to be destroyed.
 * @param options The options for configuring the database destruction process.
 */
expect fun destroyDatabase(path: String, options: LevelDBOptions)

