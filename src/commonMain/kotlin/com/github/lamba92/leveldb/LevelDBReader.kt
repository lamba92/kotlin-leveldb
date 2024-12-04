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
    public fun get(
        key: String,
        verifyChecksums: Boolean = false,
        fillCache: Boolean = true,
    ): String?

    /**
     * Creates an [Iterable] to traverse all key-value pairs in the LevelDB database. The sequence will
     * start from the specified key, or from the first key if no key is specified. LevelDB sorts keys
     * in lexicographic order.
     *
     * **IMPORTANT**: The returned sequence must be closed after use to release any underlying resources.
     *
     * @param verifyChecksums Indicates whether checksums should be verified during iteration to ensure data integrity.
     * @param fillCache Indicates whether the cache should be populated during iteration, that is to cache in memory the data read from disk.
     * @param from The key from which to start the iteration, default is `null`.
     * @return A [CloseableSequence] over key-value pairs, where each pair consists of a key and its associated value.
     */
    public fun scan(
        from: String? = null,
        verifyChecksums: Boolean = false,
        fillCache: Boolean = true,
    ): CloseableSequence<LazyEntry>

    /**
     * Represents a key-value pair in a LevelDB database. The key and value are both lazy-initialized
     * and read from the database only when accessed for the first time.
     *
     * @property key The key of the pair.
     * @property value The value of the pair.
     */
    @Serializable
    public data class LazyEntry(val key: Lazy<String>, val value: Lazy<String>)

    /**
     * Represents a key-value pair in a LevelDB database. The key and value are both eagerly initialized
     * and read from the database when the pair is created.
     *
     * @property key The key of the pair.
     * @property value The value of the pair.
     */
    @Serializable
    public data class Entry(val key: String, val value: String)
}

/**
 * Retrieves the value associated with a given key in the LevelDB database.
 * This is a convenience method that returns the value as a [LevelDBReader.Entry] object.
 * This method can be invoked only if the sequence is not closed.
 *
 */
public fun LevelDBReader.LazyEntry.resolve(): LevelDBReader.Entry = LevelDBReader.Entry(key.value, value.value)

/**
 * Creates an [Iterable] to traverse all key-value pairs in the LevelDB database. The sequence will
 * start from the specified key, or from the first key if no key is specified. LevelDB sorts keys
 * in lexicographic order.
 *
 * The provided sequence inside the [action] lambda is lazily evaluated, and the underlying resources are
 * automatically released after the lambda completes execution.
 *
 * **IMPORTANT**: Do not store the sequence outside the lambda, as it will be closed automatically after the lambda completes execution.
 *
 * See [LevelDBReader.scan] for more information.
 *
 * @param verifyChecksums Indicates whether checksums should be verified during iteration to ensure data integrity.
 * @param fillCache Indicates whether the cache should be populated during iteration, that is to cache in memory the data read from disk.
 * @param from The key from which to start the iteration, default is `null`.
 * @param action The action to be performed on the sequence of key-value pairs.
 * @return A [CloseableSequence] over key-value pairs, where each pair consists of a key and its associated value.
 */
public inline fun <T> LevelDB.scan(
    from: String? = null,
    verifyChecksums: Boolean = false,
    fillCache: Boolean = true,
    action: (Sequence<LevelDBReader.LazyEntry>) -> T,
): T = scan(from, verifyChecksums, fillCache).use { action(it) }
