package com.github.lamba92.leveldb.app

actual val DB_PATH: String
    get() = System.getenv("DB_PATH") ?: "localdb"