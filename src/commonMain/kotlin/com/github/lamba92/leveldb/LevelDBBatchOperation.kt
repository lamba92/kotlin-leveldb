package com.github.lamba92.leveldb

import kotlinx.serialization.Serializable

/**
 * Represents a batch operation to be performed on a LevelDB database.
 * This sealed interface encapsulates different types of operations that can be executed in a batch.
 */
@Serializable
sealed interface LevelDBBatchOperation {

    /**
     * The key associated with a LevelDB database operation.
     * Used in various batch operations to specify the target key for put or delete actions.
     */
    val key: String

    /**
     * Represents a 'put' operation in a batch for a LevelDB database.
     *
     * This operation encapsulates inserting or updating a key-value pair within
     * a batch context in LevelDB.
     *
     * @property key The key associated with the value.
     * @property value The value to be stored.
     */
    @Serializable
    data class Put(override val key: String, val value: String) : LevelDBBatchOperation

    /**
     * Represents a 'delete' operation in a batch for a LevelDB database.
     *
     * This operation encapsulates the removal of a key-value pair within
     * a batch context in LevelDB.
     *
     * @property key The key associated with the pair to be deleted.
     */
    @Serializable
    data class Delete(override val key: String) : LevelDBBatchOperation
}