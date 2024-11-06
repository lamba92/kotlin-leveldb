package com.github.lamba92.levelkt

import cnames.structs.leveldb_options_t
import kotlinx.cinterop.CPointer
import libleveldb.leveldb_options_set_block_size

actual fun platform_specific_leveldb_options_set_block_size(
    nativeOptions: CPointer<leveldb_options_t>,
    blockSize: Int
) {
    leveldb_options_set_block_size(nativeOptions, blockSize.toULong())
}