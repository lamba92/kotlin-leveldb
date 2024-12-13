@file:Suppress(
    "ClassName",
    "ConstPropertyName",
    "unused",
    "FunctionName",
    "PropertyName",
    "LocalVariableName",
)

package com.github.lamba92.leveldb.jvm

import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.PointerType
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference

/**
 * Direct mapping bindings for LevelDB using JNA.
 */
public class LibLevelDB {
    // Opaque pointer types for LevelDB structs
    public class leveldb_t : PointerType()

    public class leveldb_cache_t : PointerType()

    public class leveldb_comparator_t : PointerType()

    public class leveldb_env_t : PointerType()

    public class leveldb_filelock_t : PointerType()

    public class leveldb_filterpolicy_t : PointerType()

    public class leveldb_iterator_t : PointerType()

    public class leveldb_logger_t : PointerType()

    public class leveldb_options_t : PointerType()

    public class leveldb_randomfile_t : PointerType()

    public class leveldb_readoptions_t : PointerType()

    public class leveldb_seqfile_t : PointerType()

    public class leveldb_snapshot_t : PointerType()

    public class leveldb_writablefile_t : PointerType()

    public class leveldb_writebatch_t : PointerType()

    public class leveldb_writeoptions_t : PointerType()

    public companion object {
        init {
            Native.register(LibLevelDB::class.java, "leveldb")
        }

        /********** Constants **********/
        @JvmStatic
        public val leveldb_no_compression: Int = 0

        @JvmStatic
        public val leveldb_snappy_compression: Int = 1

        /********** DB operations **********/
        @JvmStatic
        public external fun leveldb_open(
            options: leveldb_options_t,
            name: String,
            errptr: PointerByReference,
        ): leveldb_t

        @JvmStatic
        public external fun leveldb_close(db: leveldb_t)

        @JvmStatic
        public external fun leveldb_put(
            db: leveldb_t,
            options: leveldb_writeoptions_t,
            key: Pointer,
            keylen: NativeLong,
            value: Pointer,
            vallen: NativeLong,
            errptr: PointerByReference,
        )

        @JvmStatic
        public external fun leveldb_delete(
            db: leveldb_t,
            options: leveldb_writeoptions_t,
            key: Pointer,
            keylen: NativeLong,
            errptr: PointerByReference,
        )

        @JvmStatic
        public external fun leveldb_write(
            db: leveldb_t,
            options: leveldb_writeoptions_t,
            batch: leveldb_writebatch_t,
            errptr: PointerByReference,
        )

        @JvmStatic
        public external fun leveldb_get(
            db: leveldb_t,
            options: leveldb_readoptions_t,
            key: Pointer,
            keylen: NativeLong,
            vallen: LongByReference,
            errptr: PointerByReference,
        ): Pointer?

        @JvmStatic
        public external fun leveldb_create_iterator(
            db: leveldb_t,
            options: leveldb_readoptions_t,
        ): leveldb_iterator_t

        @JvmStatic
        public external fun leveldb_create_snapshot(db: leveldb_t): leveldb_snapshot_t

        @JvmStatic
        public external fun leveldb_release_snapshot(
            db: leveldb_t,
            snapshot: leveldb_snapshot_t,
        )

        @JvmStatic
        public external fun leveldb_property_value(
            db: leveldb_t,
            propname: String,
        ): Pointer

        // Cannot be used in direct mapping because of Array<Pointer>
//    @JvmStatic
//    public external fun leveldb_approximate_sizes(
//        db: leveldb_t,
//        num_ranges: Int,
//        range_start_key: Array<Pointer>,
//        range_start_key_len: LongArray,
//        range_limit_key: Array<Pointer>,
//        range_limit_key_len: LongArray,
//        sizes: LongArray
//    )

        @JvmStatic
        public external fun leveldb_compact_range(
            db: leveldb_t,
            start_key: Pointer?,
            start_key_len: NativeLong,
            limit_key: Pointer?,
            limit_key_len: NativeLong,
        )

        /********** Management operations **********/
        @JvmStatic
        public external fun leveldb_destroy_db(
            options: leveldb_options_t,
            name: String,
            errptr: PointerByReference,
        )

        @JvmStatic
        public external fun leveldb_repair_db(
            options: leveldb_options_t,
            name: String,
            errptr: PointerByReference,
        )

        /********** Iterator **********/
        @JvmStatic
        public external fun leveldb_iter_destroy(iter: leveldb_iterator_t)

        @JvmStatic
        public external fun leveldb_iter_valid(iter: leveldb_iterator_t): Byte

        @JvmStatic
        public external fun leveldb_iter_seek_to_first(iter: leveldb_iterator_t)

        @JvmStatic
        public external fun leveldb_iter_seek_to_last(iter: leveldb_iterator_t)

        @JvmStatic
        public external fun leveldb_iter_seek(
            iter: leveldb_iterator_t,
            key: Pointer,
            keylen: NativeLong,
        )

        @JvmStatic
        public external fun leveldb_iter_next(iter: leveldb_iterator_t)

        @JvmStatic
        public external fun leveldb_iter_prev(iter: leveldb_iterator_t)

        @JvmStatic
        public external fun leveldb_iter_key(
            iter: leveldb_iterator_t,
            keylen: LongByReference,
        ): Pointer

        @JvmStatic
        public external fun leveldb_iter_value(
            iter: leveldb_iterator_t,
            vallen: LongByReference,
        ): Pointer

        @JvmStatic
        public external fun leveldb_iter_get_error(
            iter: leveldb_iterator_t,
            errptr: PointerByReference,
        )

        /********** Write batch **********/
        @JvmStatic
        public external fun leveldb_writebatch_create(): leveldb_writebatch_t

        @JvmStatic
        public external fun leveldb_writebatch_destroy(batch: leveldb_writebatch_t)

        @JvmStatic
        public external fun leveldb_writebatch_clear(batch: leveldb_writebatch_t)

        @JvmStatic
        public external fun leveldb_writebatch_put(
            batch: leveldb_writebatch_t,
            key: Pointer,
            klen: NativeLong,
            value: Pointer,
            vlen: NativeLong,
        )

        @JvmStatic
        public external fun leveldb_writebatch_delete(
            batch: leveldb_writebatch_t,
            key: Pointer,
            klen: NativeLong,
        )

        @JvmStatic
        public external fun leveldb_writebatch_iterate(
            batch: leveldb_writebatch_t,
            state: Pointer,
            put: WriteBatchPutFunction,
            deleted: WriteBatchDeleteFunction,
        )

        @JvmStatic
        public external fun leveldb_writebatch_append(
            destination: leveldb_writebatch_t,
            source: leveldb_writebatch_t,
        )

        /********** Options **********/
        @JvmStatic
        public external fun leveldb_options_create(): leveldb_options_t

        @JvmStatic
        public external fun leveldb_options_destroy(options: leveldb_options_t)

        @JvmStatic
        public external fun leveldb_options_set_comparator(
            options: leveldb_options_t,
            comparator: leveldb_comparator_t,
        )

        @JvmStatic
        public external fun leveldb_options_set_filter_policy(
            options: leveldb_options_t,
            filterpolicy: leveldb_filterpolicy_t,
        )

        @JvmStatic
        public external fun leveldb_options_set_create_if_missing(
            options: leveldb_options_t,
            value: Byte,
        )

        @JvmStatic
        public external fun leveldb_options_set_error_if_exists(
            options: leveldb_options_t,
            value: Byte,
        )

        @JvmStatic
        public external fun leveldb_options_set_paranoid_checks(
            options: leveldb_options_t,
            value: Byte,
        )

        @JvmStatic
        public external fun leveldb_options_set_env(
            options: leveldb_options_t,
            env: leveldb_env_t,
        )

        @JvmStatic
        public external fun leveldb_options_set_info_log(
            options: leveldb_options_t,
            logger: leveldb_logger_t,
        )

        @JvmStatic
        public external fun leveldb_options_set_write_buffer_size(
            options: leveldb_options_t,
            size: NativeLong,
        )

        @JvmStatic
        public external fun leveldb_options_set_max_open_files(
            options: leveldb_options_t,
            max_open_files: Int,
        )

        @JvmStatic
        public external fun leveldb_options_set_cache(
            options: leveldb_options_t,
            cache: leveldb_cache_t,
        )

        @JvmStatic
        public external fun leveldb_options_set_block_size(
            options: leveldb_options_t,
            block_size: NativeLong,
        )

        @JvmStatic
        public external fun leveldb_options_set_block_restart_interval(
            options: leveldb_options_t,
            interval: Int,
        )

        @JvmStatic
        public external fun leveldb_options_set_max_file_size(
            options: leveldb_options_t,
            size: NativeLong,
        )

        @JvmStatic
        public external fun leveldb_options_set_compression(
            options: leveldb_options_t,
            compression: Int,
        )

        /********** Comparator **********/
        @JvmStatic
        public external fun leveldb_comparator_create(
            state: Pointer?,
            destructor: Callback?,
            compare: ComparatorCompareFunction,
            name: ComparatorNameFunction,
        ): leveldb_comparator_t

        @JvmStatic
        public external fun leveldb_comparator_destroy(comparator: leveldb_comparator_t)

        /********** Filter policy **********/
        @JvmStatic
        public external fun leveldb_filterpolicy_create(
            state: Pointer?,
            destructor: Callback?,
            create_filter: FilterPolicyCreateFunction,
            key_may_match: FilterPolicyKeyMayMatchFunction,
            name: FilterPolicyNameFunction,
        ): leveldb_filterpolicy_t

        @JvmStatic
        public external fun leveldb_filterpolicy_destroy(filterpolicy: leveldb_filterpolicy_t)

        @JvmStatic
        public external fun leveldb_filterpolicy_create_bloom(bits_per_key: Int): leveldb_filterpolicy_t

        /********** Read options **********/
        @JvmStatic
        public external fun leveldb_readoptions_create(): leveldb_readoptions_t

        @JvmStatic
        public external fun leveldb_readoptions_destroy(options: leveldb_readoptions_t)

        @JvmStatic
        public external fun leveldb_readoptions_set_verify_checksums(
            options: leveldb_readoptions_t,
            value: Byte,
        )

        @JvmStatic
        public external fun leveldb_readoptions_set_fill_cache(
            options: leveldb_readoptions_t,
            value: Byte,
        )

        @JvmStatic
        public external fun leveldb_readoptions_set_snapshot(
            options: leveldb_readoptions_t,
            snapshot: leveldb_snapshot_t,
        )

        /********** Write options **********/
        @JvmStatic
        public external fun leveldb_writeoptions_create(): leveldb_writeoptions_t

        @JvmStatic
        public external fun leveldb_writeoptions_destroy(options: leveldb_writeoptions_t)

        @JvmStatic
        public external fun leveldb_writeoptions_set_sync(
            options: leveldb_writeoptions_t,
            value: Byte,
        )

        /********** Cache **********/
        @JvmStatic
        public external fun leveldb_cache_create_lru(capacity: NativeLong): leveldb_cache_t

        @JvmStatic
        public external fun leveldb_cache_destroy(cache: leveldb_cache_t)

        /********** Env **********/
        @JvmStatic
        public external fun leveldb_create_default_env(): leveldb_env_t

        @JvmStatic
        public external fun leveldb_env_destroy(env: leveldb_env_t)

        @JvmStatic
        public external fun leveldb_env_get_test_directory(env: leveldb_env_t): Pointer

        /********** Utility **********/
        @JvmStatic
        public external fun leveldb_free(ptr: Pointer)

        @JvmStatic
        public external fun leveldb_major_version(): Int

        @JvmStatic
        public external fun leveldb_minor_version(): Int

        /********** Callback Interfaces **********/

        public fun interface WriteBatchPutFunction : Callback {
            public fun invoke(
                state: Pointer?,
                key: Pointer?,
                klen: NativeLong,
                value: Pointer?,
                vlen: NativeLong,
            )
        }

        public fun interface WriteBatchDeleteFunction : Callback {
            public fun invoke(
                state: Pointer?,
                key: Pointer?,
                klen: NativeLong,
            )
        }

        public fun interface ComparatorCompareFunction : Callback {
            public fun invoke(
                state: Pointer?,
                a: Pointer?,
                alen: NativeLong,
                b: Pointer?,
                blen: NativeLong,
            ): Int
        }

        public fun interface ComparatorNameFunction : Callback {
            public fun invoke(state: Pointer?): String?
        }

        public fun interface FilterPolicyCreateFunction : Callback {
            public fun invoke(
                state: Pointer?,
                keys: Pointer?,
                key_lengths: Pointer?,
                num_keys: Int,
                filter_length: PointerByReference,
            ): Pointer?
        }

        public fun interface FilterPolicyKeyMayMatchFunction : Callback {
            public fun invoke(
                state: Pointer?,
                key: Pointer?,
                length: NativeLong,
                filter: Pointer?,
                filter_length: NativeLong,
            ): Byte
        }

        public fun interface FilterPolicyNameFunction : Callback {
            public fun invoke(state: Pointer?): String?
        }
    }
}
