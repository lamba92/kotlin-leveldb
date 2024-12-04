package com.github.lamba92.leveldb.tests

import com.github.lamba92.leveldb.BrokenNativeAPI
import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.batch
import com.github.lamba92.leveldb.destroyDatabase
import com.github.lamba92.leveldb.resolve
import com.github.lamba92.leveldb.scan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

expect val DATABASE_PATH: String

class ApiTests {
    @Test
    fun baseTest() =
        withDatabase { db ->
            db.put("key1", "value1")
            db.put("key2", "value2")

            assertEquals("value1", db.get("key1"))
            assertEquals("value2", db.get("key2"))

            db.delete("key1")

            assertNull(db.get("key1"))

            db.delete("key2")
            assertNull(db.get("key2"))
        }

    @Test
    @Ignore
    @BrokenNativeAPI
    fun testSnapshots() =
        withDatabase { db ->
            db.put("key1", "value1")
            println("key1 inserted")
            launch {
                delay(1.seconds)
                db.withSnapshot {
                    assertEquals("value1", get("key1"))
                }
            }
            db.put("key1", "value2")
        }

    @Test
    fun batchTest() =
        withDatabase { db ->
            db.batch {
                put("key1", "value1")
                put("key2", "value2")
            }

            assertEquals("value1", db.get("key1"))

            db.batch {
                delete("key1")
                delete("key2")
            }

            assertNull(db.get("key1"))
            assertNull(db.get("key2"))
        }

    @Test
    fun iteratorTest() =
        withDatabase { db ->
            repeat(100) {
                db.put("key$it", "value$it")
            }

            val dbList = db.scan { it.map { it.resolve() }.toList() }

            assertEquals(100, dbList.size)

            dbList.iterator().forEach { (key, value) ->
                val keyNumber = key.removePrefix("key").toInt()
                val valueNumber = value.removePrefix("value").toInt()
                assertEquals(keyNumber, valueNumber)
            }
        }

    @Test
    fun testKeyPartitioningIterator() =
        withDatabase { db ->
            val aSize = 70
            repeat(aSize) {
                db.put("a:key$it", "value$it")
            }

            val bSize = 80
            repeat(bSize) {
                db.put("b:key$it", "value$it")
            }

            val cSize = 90
            repeat(cSize) {
                db.put("c:key$it", "value$it")
            }
            val aList =
                db.scan("a:") {
                    it.map { it.resolve() }
                        .takeWhile { it.key.startsWith("a:") }
                        .toList()
                }
            assertEquals(aSize, aList.size)

            val bList =
                db.scan("b:") {
                    it.map { it.resolve() }.takeWhile { it.key.startsWith("b:") }.toList()
                }
            assertEquals(bSize, bList.size)

            val cList =
                db.scan("c:") {
                    it.map { it.resolve() }.takeWhile { it.key.startsWith("c:") }.toList()
                }
            assertEquals(cSize, cList.size)

            aList.forEach { (key, value) ->
                assertTrue(key.startsWith("a:"), "key: $key does not start with 'a:'")
                val keyNumber = key.removePrefix("a:key").toInt()
                val valueNumber = value.removePrefix("value").toInt()
                assertEquals(keyNumber, valueNumber)
            }

            bList.forEach { (key, value) ->
                assertTrue(key.startsWith("b:"), "key: $key does not start with 'b:'")
                val keyNumber = key.removePrefix("b:key").toInt()
                val valueNumber = value.removePrefix("value").toInt()
                assertEquals(keyNumber, valueNumber)
            }

            cList.forEach { (key, value) ->
                assertTrue(key.startsWith("c:"), "key: $key does not start with 'c:'")
                val keyNumber = key.removePrefix("c:key").toInt()
                val valueNumber = value.removePrefix("value").toInt()
                assertEquals(keyNumber, valueNumber)
            }

            val allList = db.scan { it.map { it.resolve() }.toList() }
            assertEquals(aSize + bSize + cSize, allList.size)

            allList.forEachIndexed { index, (key, value) ->
                val prefix =
                    when {
                        index < aSize -> "a:"
                        index >= aSize && index < aSize + bSize -> "b:"
                        else -> "c:"
                    }
                require(key.startsWith(prefix)) { "key: $key does not start with '$prefix'" }
                val keyNumber = key.removePrefix("${prefix}key").toInt()
                val valueNumber = value.removePrefix("value").toInt()
                assertEquals(keyNumber, valueNumber)
            }
        }
}

fun withDatabase(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration = 1.minutes,
    block: suspend TestScope.(database: LevelDB) -> Unit,
) = runTest(context, timeout) {
    destroyDatabase(DATABASE_PATH)
    val db = LevelDB(DATABASE_PATH)
    try {
        block(db)
    } finally {
        db.close()
    }
}
