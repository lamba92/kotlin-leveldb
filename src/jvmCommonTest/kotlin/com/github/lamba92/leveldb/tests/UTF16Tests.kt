@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")

package com.github.lamba92.leveldb.tests

import kotlin.test.assertEquals

@Target(AnnotationTarget.FUNCTION)
expect annotation class Test()

class UTF16Tests {
    @Test
    fun emoji() =
        withDatabase { db ->
            val key = "👋🌍"
            val value = "👋🌍"
            db.put(key, value)
            assertEquals(value, db.get(key))
        }

    @Test
    fun accentedChars() =
        withDatabase { db ->
            val key = "áéíóú"
            val value = "áéíóú"
            db.put(key, value)
            assertEquals(value, db.get(key))
        }

    @Test
    fun chineseChars() =
        withDatabase { db ->
            val key = "你好"
            val value = "你好"
            db.put(key, value)
            assertEquals(value, db.get(key))
        }

    @Test
    fun japaneseChars() =
        withDatabase { db ->
            val key = "こんにちは"
            val value = "こんにちは"
            db.put(key, value)
            assertEquals(value, db.get(key))
        }

    @Test
    fun cyrillicChars() =
        withDatabase { db ->
            val key = "Здравствуйте"
            val value = "Здравствуйте"
            db.put(key, value)
            assertEquals(value, db.get(key))
        }

    @Test
    fun dieresis() =
        withDatabase { db ->
            val key = "äëïöü"
            val value = "äëïöü"
            db.put(key, value)
            assertEquals(value, db.get(key))
        }

    @Test
    fun greekChars() =
        withDatabase { db ->
            val key = "Γειά σας"
            val value = "Γειά σας"
            db.put(key, value)
            assertEquals(value, db.get(key))
        }
}
