package com.github.lamba92.levelkt

actual fun LevelDB(
    path: String,
    options: LevelDBOptions
): LevelDB {
    TODO("Not yet implemented")
}

/**
 * Repairs the database at the specified path using the provided options.
 *
 * @param path The path to the database that needs to be repaired.
 * @param options The options to configure the repair process.
 */
actual fun repairDatabase(path: String, options: LevelDBOptions) {
}

/**
 * Destroys the database located at the specified path using the provided options.
 *
 * @param path The filesystem path to the database to be destroyed.
 * @param options The options for configuring the database destruction process.
 */
actual fun destroyDatabase(
    path: String,
    options: LevelDBOptions
) {
}