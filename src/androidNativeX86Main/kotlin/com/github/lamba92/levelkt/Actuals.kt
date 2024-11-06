package com.github.lamba92.levelkt

import cnames.structs.leveldb_options_t
import cnames.structs.leveldb_readoptions_t
import cnames.structs.leveldb_t
import cnames.structs.leveldb_writeoptions_t
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.value
import libleveldb.leveldb_get
import libleveldb.leveldb_options_set_block_size
import libleveldb.leveldb_put
import platform.posix.size_tVar

actual fun platform_specific_leveldb_options_set_block_size(
    nativeOptions: CPointer<leveldb_options_t>,
    blockSize: Int
) {
    leveldb_options_set_block_size(nativeOptions, blockSize.toUInt())
}

actual fun platform_specific_leveldb_put(
    db: CPointer<leveldb_t>,
    options: CPointer<leveldb_writeoptions_t>?,
    key: String,
    value : String,
    errptr: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>
) {
    leveldb_put(
        db = db,
        options = options,
        key = key,
        keylen = key.length.toUInt(),
        `val` = value,
        vallen = value.length.toUInt(),
        errptr = errptr
    )
}

actual fun platform_specific_leveldb_get(
    db: CPointer<leveldb_t>,
    options: CPointer<leveldb_readoptions_t>,
    key: String,
    errptr: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>
): String? = memScoped {
    val vallen = alloc<size_tVar>()
    val ptr = leveldb_get(
        db = db,
        options = options,
        key = key,
        keylen = key.length.toUInt(),
        vallen = vallen.ptr,
        errptr = errptr
    )

    if (ptr == null) {
        return null
    }

    ptr.readBytes(vallen.value.toInt()).decodeToString()
}