package com.github.lamba92.leveldb.tests

actual val DATABASE_PATH: String
    get() =
        System.getenv("LEVELDB_LOCATION")
            ?: error("LEVELDB_LOCATION environment variable not set")
