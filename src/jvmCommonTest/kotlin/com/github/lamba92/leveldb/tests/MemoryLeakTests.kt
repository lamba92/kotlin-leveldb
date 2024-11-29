@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")

package com.github.lamba92.leveldb.tests

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

// for whatever reason, IntelliJ cannot see
// the kotlin-test package from this source set...
@Target(AnnotationTarget.FUNCTION)
expect annotation class Test()

expect fun getMemoryUsage(): MemorySize

val isCI
    get() = System.getenv("CI") == "true"

class MemoryLeakTests {

    @Test
    fun testDbOpenForLongTime() = withDatabase(timeout = 10.minutes) {db ->
        if (!isCI) {
            println("This test runs only in CI, skipping...")
            return@withDatabase
        }
        repeat(5) {
            db.put("key$it", "value$it")
            delay(1.minutes)
        }
    }

    @Test
//     Disabled because the library crashes randomly...
    fun testLeaks() = withDatabase { db ->
        repeat(30_000) {
            db.put("key$it", "value$it", true)
            if (it % 5000 == 0) {
                db.compactRange()
                println("Inserted ${it + 1} elements")
                checkMemoryUsage()
            }
        }
        checkMemoryUsage()

        repeat(30_000) {
            db.get("key$it")
            if (it % 5000 == 0) {
                db.compactRange()
                println("Read ${it + 1} elements")
                checkMemoryUsage()
            }
        }

        checkMemoryUsage()

        db.scan {
            it
                .withIndex()
                .onEach { (index, _) ->
                    if (index % 5000 == 0) {
                        println("Iterated over ${index + 1} elements")
                        checkMemoryUsage()
                    }
                }
                .filter { false }
                .toList()
        }
        checkMemoryUsage()

        repeat(30_000) {
            db.delete("key$it")
            if (it % 5000 == 0) {
                db.compactRange()
                println("Deleted ${it + 1} elements")
                checkMemoryUsage()
            }
        }
        checkMemoryUsage()
    }

}

fun checkMemoryUsage(maxSize: MemorySize = 700.megabytes) {
    val memoryUsage = getMemoryUsage()
    require(memoryUsage < maxSize) { "Memory usage is $memoryUsage, which is greater than $maxSize" }
    println("Memory usage: $memoryUsage")
}
