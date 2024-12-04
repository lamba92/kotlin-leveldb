package com.github.lamba92.leveldb.tests

import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

class StressTests {
    companion object {
        const val CYCLES = 500
        const val OPERATIONS_PER_CYCLE = 5000
    }

    @Test
    fun repeatedReadWriteDelete() =
        withDatabase(timeout = 10.minutes) { db ->
            repeat(CYCLES) { cycle ->
                repeat(OPERATIONS_PER_CYCLE) { i ->
                    val index = (OPERATIONS_PER_CYCLE * cycle) + i
                    db.put("key$index", "value$index")
                }
                repeat(OPERATIONS_PER_CYCLE) { i ->
                    val index = (OPERATIONS_PER_CYCLE * cycle) + i
                    db.get("key$index")
                }
                repeat(OPERATIONS_PER_CYCLE) { i ->
                    val index = (OPERATIONS_PER_CYCLE * cycle) + i
                    db.delete("key$index")
                }
                db.compactRange()
            }
        }
}
