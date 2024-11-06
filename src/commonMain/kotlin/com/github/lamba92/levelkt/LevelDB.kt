package com.github.lamba92.levelkt

import kotlinx.serialization.Serializable


expect fun LevelDB(path: String, options: LevelDBOptions = LevelDBOptions()): LevelDB

expect fun repairDatabase(path: String, options: LevelDBOptions)
expect fun destroyDatabase(path: String, options: LevelDBOptions)

interface LevelDB : AutoCloseable, Iterable<Pair<String, String>> {

    fun put(key: String, value: String, sync: Boolean = false)
    fun get(key: String, options: LevelDBReadOptions = LevelDBReadOptions.DEFAULT): String?
    fun delete(key: String, sync: Boolean = false)

    fun batch(operations: List<LevelDBBatchOperation>, sync: Boolean = false)

    fun iterator(options: LevelDBReadOptions = LevelDBReadOptions()): Iterator<Pair<String, String>>

    override fun iterator() =
        iterator(options = LevelDBReadOptions.DEFAULT)

    fun createSnapshot(): LevelDBSnapshot

    fun compactRange(start: String, end: String)

}

class LevelDBReadOptions(
    val verifyChecksums: Boolean = false,
    val fillCache: Boolean = true,
    val snapshot: LevelDBSnapshot? = null
) {
    companion object {
        val DEFAULT = LevelDBReadOptions()
    }
}

@Serializable
sealed interface LevelDBBatchOperation {

    val key: String

    @Serializable
    data class Put(override val key: String, val value: String) : LevelDBBatchOperation

    @Serializable
    data class Delete(override val key: String) : LevelDBBatchOperation
}
