package com.github.lamba92.leveldb

/**
 * Performs a batch operation on the LevelDB database.
 *
 * @param sync Flag indicating whether to perform a synchronous batch operation, that is
 * to wait for all operations to be persisted to disk before returning. Default is false.
 * @param builder A lambda with receiver that constructs a list of batch operations to be performed.
 */
public inline fun LevelDB.batch(
    sync: Boolean = false,
    builder: LevelDBBatchBuilder.() -> Unit,
): Unit = batch(buildLevelDBBatch(builder), sync)

/**
 * Constructs and builds a batch operation for LevelDB using a customizable builder.
 *
 * @param builder A lambda with receiver on the LevelDBBatchBuilder, allowing the caller to define
 * put and delete operations that are included in the batch.
 */
public inline fun buildLevelDBBatch(builder: LevelDBBatchBuilder.() -> Unit): List<LevelDBBatchOperation> =
    LevelDBBatchBuilder().apply(builder).build()

/**
 * Constructs and manages a collection of batch operations for LevelDB.
 *
 * This class allows the construction of operations to be
 * executed in a single [LevelDB.batch] against a LevelDB database.
 * It supports adding [LevelDB.put] and [LevelDB.delete] operations for
 * specific keys, and building the final list of operations
 * to be executed.
 */
public class LevelDBBatchBuilder {
    private val operations = mutableListOf<LevelDBBatchOperation>()

    /**
     * Adds a [LevelDB.put] operation to the batch with the specified key and value.
     *
     * @param key The key associated with the value.
     * @param value The value to be stored.
     */
    public fun put(
        key: String,
        value: String,
    ) {
        operations.add(LevelDBBatchOperation.Put(key, value))
    }

    /**
     * Adds a [LevelDB.delete] operation to the batch for the specified key.
     *
     * @param key The key associated with the pair to be deleted.
     */
    public fun delete(key: String) {
        operations.add(LevelDBBatchOperation.Delete(key))
    }

    /**
     * Builds and returns a list of accumulated LevelDB batch operations.
     *
     * @return A list of LevelDBBatchOperation instances that represents
     * the operations to be executed in a batch.
     */
    public fun build(): List<LevelDBBatchOperation> = operations.toList()
}
