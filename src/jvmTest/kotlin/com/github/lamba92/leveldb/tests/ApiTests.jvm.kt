@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.github.lamba92.leveldb.tests

actual val DATABASE_PATH: String
    get() = System.getenv("LEVELDB_LOCATION")
        ?: error("LEVELDB_LOCATION environment variable not set")

actual typealias Test = org.junit.jupiter.api.Test