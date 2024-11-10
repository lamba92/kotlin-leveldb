package com.github.lamba92.levelkt.tests

import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.createDirectoryAtURL

actual val DATABASE_PATH: String
    get() = createTestFilePath()
        ?: error("Failed to create test file for database")

fun createTestFilePath(): String? {
    // Use the temporary directory for test files
    val tmpDir = NSURL.fileURLWithPath(NSTemporaryDirectory())

    // Create a subdirectory for organization, if desired
    val testDir = tmpDir.URLByAppendingPathComponent("testDir")
    NSFileManager.defaultManager.createDirectoryAtURL(testDir, withIntermediateDirectories = true, attributes = null, error = null)

    // Create the test file within this directory
    val testFile = testDir.URLByAppendingPathComponent("testFile.txt")

    // Write some text to the file (helper function or additional implementation may be needed)
    testFile.path?.let {
        // Assuming a function `writeTextToPath` to handle actual text writing
        writeTextToPath(it, "Hello, iOS Test!")
    }

    // Return the absolute path as a String
    return testFile.path
}

// Helper function to write text to a file path in iOS
fun writeTextToPath(path: String, text: String) {
    val fileManager = NSFileManager.defaultManager
    fileManager.createFileAtPath(path, text.encodeToByteArray(), attributes = null)
}