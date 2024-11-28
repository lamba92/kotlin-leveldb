package com.github.lamba92.levelkt

expect fun LevelDB(path: String, options: LevelDBOptions = LevelDBOptions.DEFAULT): LevelDB

/**
 * Repairs the database at the specified path using the provided options.
 *
 * @param path The path to the database that needs to be repaired.
 * @param options The options to configure the repair process.
 */
expect fun repairDatabase(path: String, options: LevelDBOptions = LevelDBOptions.DEFAULT)

/**
 * Destroys the database located at the specified path using the provided options.
 *
 * @param path The filesystem path to the database to be destroyed.
 * @param options The options for configuring the database destruction process.
 */
expect fun destroyDatabase(path: String, options: LevelDBOptions = LevelDBOptions.DEFAULT)

/**
 * Indicates that the annotated API is broken and will crash at runtime.
 *
 * This annotation is used to mark certain methods or classes within the codebase
 * as unsafe and not ready for production use. Utilization of these methods or classes
 * is strongly discouraged, as they are known to cause runtime crashes.
 *
 * Note: Ensure that you avoid using this API until it is stabilized and this annotation
 * is removed.
 */
@Suppress("unused")
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is broken and will crash at runtime"
)
internal annotation class BrokenNativeAPI(val reason: String = "")