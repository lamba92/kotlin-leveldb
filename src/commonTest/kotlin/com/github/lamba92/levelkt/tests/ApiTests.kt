package com.github.lamba92.levelkt.tests

import com.github.lamba92.levelkt.LevelDB
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

expect fun getDatabaseLocation(): String

class ApiTests {

    @Test
    fun testGetAll() {
        val db = LevelDB(getDatabaseLocation())

        db.put("key1", "value1")
        db.put("key2", "value2")

        assertEquals("value1", db.get("key1"))
        assertEquals("value2", db.get("key2"))

        db.delete("key1")

        assertNull(db.get("key1"))

        db.close()
    }


}