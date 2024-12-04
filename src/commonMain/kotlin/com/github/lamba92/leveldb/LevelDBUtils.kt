package com.github.lamba92.leveldb

/**
 * Creates a new instance of a LevelDB database at the specified file path.
 *
 * @param path The file system path where the LevelDB database is located or will be created. It has to a directory.
 * If the directory does not exist, it will be created only if [LevelDBOptions.createIfMissing] is set to `true`.
 * @param options Configuration options for creating and managing the LevelDB instance, defaults to [LevelDBOptions.DEFAULT].
 * @return A `LevelDB` instance for interacting with the database.
 */
public expect fun LevelDB(
    path: String,
    options: LevelDBOptions = LevelDBOptions.DEFAULT,
): LevelDB

/**
 * Repairs the database at the specified path using the provided options.
 *
 * @param path The path to the database that needs to be repaired.
 * @param options The options to configure the repair process.
 */
public expect fun repairDatabase(
    path: String,
    options: LevelDBOptions = LevelDBOptions.DEFAULT,
)

/**
 * Destroys the database located at the specified path using the provided options.
 *
 * @param path The filesystem path to the database to be destroyed.
 * @param options The options for configuring the database destruction process.
 */
public expect fun destroyDatabase(
    path: String,
    options: LevelDBOptions = LevelDBOptions.DEFAULT,
)

/**
 * Indicates that the annotated API is broken and will crash at runtime.
 *
 * This annotation is used to mark certain methods or classes within the codebase
 * as unsafe and not ready for production use. Utilization of these methods or classes
 * is strongly discouraged, as they are known to cause runtime crashes.
 *
 * Note: Ensure that you avoid using this API until it is stabilized and this annotation
 * is removed.
 */
@Suppress("unused")
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is broken and will crash at runtime",
)
internal annotation class BrokenNativeAPI(val reason: String = "")

/**
 * Represents a sequence of elements that can be closed to release any underlying resources.
 *
 * This interface extends the standard [Sequence] interface and adds the [close] method
 * to allow for explicit resource management.
 *
 * @param <T> the type of elements produced by this sequence
 */
public interface CloseableSequence<T> : Sequence<T>, AutoCloseable {
    /**
     * Closes this sequence and releases any underlying resources.
     *
     * This method should be called when the sequence is no longer needed to prevent
     * resource leaks.
     */
    override fun close()
}

/**
 * Wraps a [Sequence] into a [CloseableSequence] with a custom close action.
 *
 * This function allows you to associate a specific action to be executed when the
 * resulting [CloseableSequence] is closed.
 *
 * @param T the type of elements produced by the sequence
 * @param onClose the action to be executed when the sequence is closed
 * @return a [CloseableSequence] that wraps the original sequence and executes the
 *         specified action on close
 */
public fun <T> Sequence<T>.asCloseable(onClose: () -> Unit): CloseableSequence<T> =
    object : CloseableSequence<T>, Sequence<T> by this {
        override fun close() = onClose()
    }
