package com.github.lamba92.levelkt


expect fun LevelDB(path: String, options: LevelDBOptions): LevelDB

interface LevelDB : AutoCloseable {

    fun put(key: String, value: String, sync: Boolean = false)

    fun get(key: String): String?

    fun delete(key: String, sync: Boolean = false)

}