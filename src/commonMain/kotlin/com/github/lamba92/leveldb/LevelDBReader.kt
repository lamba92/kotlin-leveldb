package com.github.lamba92.leveldb

import kotlinx.serialization.Serializable

/**
 * Interface for reading from a LevelDB database.
 * Provides methods for getting values and iterating over key-value pairs.
 */
public interface LevelDBReader {

    /**
     * Retrieves the value associated with a given key in the LevelDB database.
     *
     * @param key The key for which the value is to be retrieved.
     * @param verifyChecksums Indicates whether checksums should be verified during iteration to ensure
     * data integrity, default is `false`.
     * @param fillCache Indicates whether the cache should be populated during iteration, that is to cache
     * in memory the data read from disk, default is `true`.
     * @return The value associated with the specified key, or null if the key does not exist.
     */
    public fun get(key: String, verifyChecksums: Boolean = false, fillCache: Boolean = true): String?

    /**
     * Creates an [Iterable] to traverse all key-value pairs in the LevelDB database.
     *
     * @param verifyChecksums Indicates whether checksums should be verified during iteration to ensure data integrity.
     * @param fillCache Indicates whether the cache should be populated during iteration, that is to cache in memory the data read from disk.
     * @param from The key from which to start the iteration, default is `null`.
     * @return An iterator over key-value pairs, where each pair consists of a key and its associated value.
     */
    public fun <T> scan(
        from: String? = null,
        verifyChecksums: Boolean = false,
        fillCache: Boolean = true,
        action: (Sequence<Entry>) -> T
    ): T

    @Serializable
    public data class Entry(val key: String, val value: String)

}