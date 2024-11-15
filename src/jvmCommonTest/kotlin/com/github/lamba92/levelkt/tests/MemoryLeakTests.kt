@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.github.lamba92.levelkt.tests

@Target(AnnotationTarget.FUNCTION)
expect annotation class Test()

expect fun getMemoryUsage(): MemorySize

class MemoryLeakTests {

    @Test
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

fun Runtime.usedMemory() = totalMemory() - freeMemory()
