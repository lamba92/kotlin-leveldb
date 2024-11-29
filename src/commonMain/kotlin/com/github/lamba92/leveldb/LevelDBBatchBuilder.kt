package com.github.lamba92.leveldb

inline fun LevelDB.batch(sync: Boolean = false, builder: LevelDBBatchBuilder.() -> Unit) =
    batch(buildLevelDBBatch(builder), sync)

inline fun buildLevelDBBatch(builder: LevelDBBatchBuilder.() -> Unit) =
    LevelDBBatchBuilder().apply(builder).build()

class LevelDBBatchBuilder {
    private val operations = mutableListOf<LevelDBBatchOperation>()

    fun put(key: String, value: String) {
        operations.add(LevelDBBatchOperation.Put(key, value))
    }

    fun delete(key: String) {
        operations.add(LevelDBBatchOperation.Delete(key))
    }

    fun build(): List<LevelDBBatchOperation> = operations.toList()
}