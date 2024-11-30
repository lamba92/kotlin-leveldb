@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
@file:Suppress("EXTERNAL_SERIALIZER_USELESS", "OPT_IN_USAGE", "FunctionName")

package com.github.lamba92.leveldb.benchmarks

import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.buildLevelDBBatch
import com.github.lamba92.leveldb.destroyDatabase
import kotlin.math.pow
import kotlin.time.measureTime
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.O_CREAT
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.close
import platform.posix.getenv
import platform.posix.open
import platform.posix.snprintf
import platform.posix.write

val DB_PATH: String
    get() = getenv("DB_PATH")
        ?.toKString()
        ?: error("DB_PATH not set")

val JSON_OUTPUT_PATH
    get() = getenv("JSON_OUTPUT_PATH")
        ?.toKString()
        ?: error("JSON_OUTPUT_PATH not set")

val TABLE_OUTPUT_PATH
    get() = getenv("TABLE_OUTPUT_PATH")
        ?.toKString()
        ?: error("TABLE_OUTPUT_PATH not set")

inline fun <T> withDb(block: LevelDB.() -> T): T {
    val db = LevelDB(DB_PATH)
    return try {
        block(db)
    } finally {
        db.close()
        destroyDatabase(DB_PATH)
    }
}

const val operationsCount = 500_000
const val testRepetitions = 5

fun main() {
    val singleBenchmarkOutput =
        List(testRepetitions) {
            print("Running 'singleOperationBenchmark' #$it...")
            val now = Clock.System.now()
            val r = withDb { singleOperationBenchmark(operationsCount) }
            println(" - done ${Clock.System.now() - now}")
            r
        }
            .reduce { acc, singleBenchmarkOutput ->
                SingleBenchmarkOutput(
                    putOpsSec = acc.putOpsSec + singleBenchmarkOutput.putOpsSec,
                    getOpsSec = acc.getOpsSec + singleBenchmarkOutput.getOpsSec,
                    overridePutOpsSec = acc.overridePutOpsSec + singleBenchmarkOutput.overridePutOpsSec,
                    deleteOpsSec = acc.deleteOpsSec + singleBenchmarkOutput.deleteOpsSec
                )
            }
            .let {
                SingleBenchmarkOutput(
                    putOpsSec = it.putOpsSec / testRepetitions,
                    getOpsSec = it.getOpsSec / testRepetitions,
                    overridePutOpsSec = it.overridePutOpsSec / testRepetitions,
                    deleteOpsSec = it.deleteOpsSec / testRepetitions
                )
            }
    val batchBenchmarkOutput =
        List(testRepetitions) {
            print("Running 'batchOperationBenchmark' #$it...")
            val now = Clock.System.now()
            val r = withDb { batchOperationBenchmark(operationsCount) }
            println(" - done ${Clock.System.now() - now}")
            r
        }
            .reduce { acc, batchBenchmarkOutput ->
                BatchBenchmarkOutput(
                    putOpsSec = acc.putOpsSec + batchBenchmarkOutput.putOpsSec,
                    overridePutOpsSec = acc.overridePutOpsSec + batchBenchmarkOutput.overridePutOpsSec,
                    deleteOpsSec = acc.deleteOpsSec + batchBenchmarkOutput.deleteOpsSec
                )
            }
            .let {
                BatchBenchmarkOutput(
                    putOpsSec = it.putOpsSec / testRepetitions,
                    overridePutOpsSec = it.overridePutOpsSec / testRepetitions,
                    deleteOpsSec = it.deleteOpsSec / testRepetitions
                )
            }

    val output = BenchmarkOutput(
        burstSinglePut = "${singleBenchmarkOutput.putOpsSec.div(1000).format("%.2f")}k ops/sec",
        burstSingleGet = "${singleBenchmarkOutput.getOpsSec.div(1000).format("%.2f")}k ops/sec",
        burstSingleOverridePut = "${singleBenchmarkOutput.overridePutOpsSec.div(1000).format("%.2f")}k ops/sec",
        burstSingleDelete = "${singleBenchmarkOutput.deleteOpsSec.div(1000).format("%.2f")}k ops/sec",
        batchPut = "${batchBenchmarkOutput.putOpsSec.div(1000).format("%.2f")}k ops/sec",
        batchOverridePut = "${batchBenchmarkOutput.overridePutOpsSec.div(1000).format("%.2f")}k ops/sec",
        batchDelete = "${batchBenchmarkOutput.deleteOpsSec.div(1000).format("%.2f")}k ops/sec"
    )

    val prettyJson = Json { prettyPrint = true }
    val jsonString = prettyJson.encodeToString(output)
    writeStringToFile(JSON_OUTPUT_PATH, jsonString)

    val table = MarkdownTable(
        headers = listOf("Operation", "Ops/sec (avg over $testRepetitions runs)"),
        rows = listOf(
            listOf("Burst Single Put", output.burstSinglePut.removeSuffix(" ops/sec")),
            listOf("Burst Single Get", output.burstSingleGet.removeSuffix(" ops/sec")),
            listOf("Burst Single Override Put", output.burstSingleOverridePut.removeSuffix(" ops/sec")),
            listOf("Burst Single Delete", output.burstSingleDelete.removeSuffix(" ops/sec")),
            listOf("Batch Put", output.batchPut.removeSuffix(" ops/sec")),
            listOf("Batch Override Put", output.batchOverridePut.removeSuffix(" ops/sec")),
            listOf("Batch Delete", output.batchDelete.removeSuffix(" ops/sec"))
        )
    )
    writeStringToFile(TABLE_OUTPUT_PATH, table)
}

fun Double.format(arg: String, bufferSize: Int = 128): String = memScoped {
    val buffer = allocArray<ByteVar>(bufferSize)
    snprintf(buffer, bufferSize.toULong(), arg, this@format)
    buffer.toKString()
}

fun LevelDB.batchOperationBenchmark(operationsCount: Int): BatchBenchmarkOutput {
    val insertions = buildLevelDBBatch {
        repeat(operationsCount) {
            put("key$it", "value$it")
        }
    }

    val batchPut = measureTime {
        batch(insertions)
        compactRange()
    }

    val batchOverridePutInsertions = buildLevelDBBatch {
        repeat(operationsCount) {
            put("key$it", "value${it + operationsCount}")
        }
    }

    val batchOverridePut = measureTime {
        batch(batchOverridePutInsertions)
        compactRange()
    }

    val batchDeleteInsertions = buildLevelDBBatch {
        repeat(operationsCount) {
            delete("key$it")
        }
    }

    val batchDelete = measureTime {
        batch(batchDeleteInsertions)
        compactRange()
    }
    return BatchBenchmarkOutput(
        putOpsSec = operationsCount / batchPut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        overridePutOpsSec = operationsCount / batchOverridePut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        deleteOpsSec = operationsCount / batchDelete.inWholeMicroseconds.toDouble() * 10.0.pow(6)
    )
}

data class SingleBenchmarkOutput(
    val putOpsSec: Double,
    val getOpsSec: Double,
    val overridePutOpsSec: Double,
    val deleteOpsSec: Double
)

fun LevelDB.singleOperationBenchmark(operationsCount: Int): SingleBenchmarkOutput {
    val burstSinglePut = measureTime {
        repeat(operationsCount) {
            put("key$it", "value$it")
        }
        compactRange()
    }

    val burstSingleGet = measureTime {
        repeat(operationsCount) {
            get("key$it")
        }
    }

    val burstSingleOverridePut = measureTime {
        repeat(operationsCount) {
            put("key$it", "value${it + operationsCount}")
        }
        compactRange()
    }

    val burstSingleDelete = measureTime {
        repeat(operationsCount) {
            delete("key$it")
        }
        compactRange()
    }
    return SingleBenchmarkOutput(
        putOpsSec = operationsCount / burstSinglePut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        getOpsSec = operationsCount / burstSingleGet.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        overridePutOpsSec = operationsCount / burstSingleOverridePut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        deleteOpsSec = operationsCount / burstSingleDelete.inWholeMicroseconds.toDouble() * 10.0.pow(6)
    )
}

data class BatchBenchmarkOutput(
    val putOpsSec: Double,
    val overridePutOpsSec: Double,
    val deleteOpsSec: Double
)

@Serializable
data class BenchmarkOutput(
    val burstSinglePut: String,
    val burstSingleGet: String,
    val burstSingleOverridePut: String,
    val burstSingleDelete: String,
    val batchPut: String,
    val batchOverridePut: String,
    val batchDelete: String
)

fun MarkdownTable(headers: List<String>, rows: List<List<String>>): String {
    val colWidths = headers.mapIndexed { index, _ ->
        maxOf(
            headers[index].length,
            rows.maxOfOrNull { it[index].length } ?: 0
        )
    }

    fun formatRow(row: List<String>) =
        row.mapIndexed { index, cell -> cell.padEnd(colWidths[index]) }.joinToString(" | ", "| ", " |")

    val headerRow = formatRow(headers)
    val separatorRow = colWidths.joinToString(" | ", "| ", " |") { "-".repeat(it) }
    val dataRows = rows.joinToString("\n", transform = ::formatRow)

    return "$headerRow\n$separatorRow\n$dataRows"
}

fun writeStringToFile(path: String, content: String) {
    println("Writing to file $path")
    val fd = open(path, O_WRONLY or O_CREAT or O_TRUNC, S_IRUSR or S_IWUSR)
    if (fd == -1) {
        error("Error opening file")
    }

    try {
        val written: Int = write(fd, content.encodeToByteArray().refTo(0), content.length.convert()).convert()
        if (written == -1) error("Error writing to file")
    } finally {
        close(fd)
    }
}