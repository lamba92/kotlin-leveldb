package com.github.lamba92.leveldb.app

import com.github.lamba92.leveldb.LevelDB
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

expect val DB_PATH: String

fun main() {
    val db = LevelDB(DB_PATH)

    db.put("key1", "value1")
    db.put("key2", "value2")

    val test1 = TestOutput.Test("value1", db.get("key1"))
    db.delete("key1")
    val test2 = TestOutput.Test(null, db.get("key1"))
    db.close()
    val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
            prettyPrint = true
        }
    println(json.encodeToString(TestOutput(test1, test2)))
}

@Serializable
data class TestOutput(
    val part1: Test,
    val part2: Test,
) {
    @Serializable
    data class Test(
        val expected: String? = null,
        val actual: String? = null,
    )
}
