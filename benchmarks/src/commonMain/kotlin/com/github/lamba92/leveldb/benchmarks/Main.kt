@file:Suppress("EXTERNAL_SERIALIZER_USELESS", "OPT_IN_USAGE", "FunctionName")

package com.github.lamba92.leveldb.benchmarks

import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.buildLevelDBBatch
import com.github.lamba92.leveldb.destroyDatabase
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.time.measureTime

val DB_PATH: String
    get() =
        getenv("DB_PATH")
            ?: error("DB_PATH not set")

val JSON_OUTPUT_PATH
    get() =
        getenv("JSON_OUTPUT_PATH")
            ?: error("JSON_OUTPUT_PATH not set")

val TABLE_OUTPUT_PATH
    get() =
        getenv("TABLE_OUTPUT_PATH")
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

val operationsCount
    get() =
        getenv("OPERATIONS_COUNT")
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: 500_000

val testRepetitions
    get() =
        getenv("TEST_REPETITIONS")
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: 5

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
                    deleteOpsSec = acc.deleteOpsSec + singleBenchmarkOutput.deleteOpsSec,
                )
            }
            .let {
                SingleBenchmarkOutput(
                    putOpsSec = it.putOpsSec / testRepetitions,
                    getOpsSec = it.getOpsSec / testRepetitions,
                    overridePutOpsSec = it.overridePutOpsSec / testRepetitions,
                    deleteOpsSec = it.deleteOpsSec / testRepetitions,
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
                    deleteOpsSec = acc.deleteOpsSec + batchBenchmarkOutput.deleteOpsSec,
                )
            }
            .let {
                BatchBenchmarkOutput(
                    putOpsSec = it.putOpsSec / testRepetitions,
                    overridePutOpsSec = it.overridePutOpsSec / testRepetitions,
                    deleteOpsSec = it.deleteOpsSec / testRepetitions,
                )
            }

    val output =
        BenchmarkOutput(
            burstSinglePut = "${singleBenchmarkOutput.putOpsSec.div(1000).format("%.2f")}k ops/sec",
            burstSingleGet = "${singleBenchmarkOutput.getOpsSec.div(1000).format("%.2f")}k ops/sec",
            burstSingleOverridePut = "${singleBenchmarkOutput.overridePutOpsSec.div(1000).format("%.2f")}k ops/sec",
            burstSingleDelete = "${singleBenchmarkOutput.deleteOpsSec.div(1000).format("%.2f")}k ops/sec",
            batchPut = "${batchBenchmarkOutput.putOpsSec.div(1000).format("%.2f")}k ops/sec",
            batchOverridePut = "${batchBenchmarkOutput.overridePutOpsSec.div(1000).format("%.2f")}k ops/sec",
            batchDelete = "${batchBenchmarkOutput.deleteOpsSec.div(1000).format("%.2f")}k ops/sec",
        )

    val prettyJson = Json { prettyPrint = true }
    val jsonString = prettyJson.encodeToString(output)
    writeStringToFile(JSON_OUTPUT_PATH, jsonString)

    val table =
        MarkdownTable(
            headers = listOf("Operation", "Ops/sec (avg over $testRepetitions runs)"),
            rows =
                listOf(
                    listOf("Burst Single Put", output.burstSinglePut.removeSuffix(" ops/sec")),
                    listOf("Burst Single Get", output.burstSingleGet.removeSuffix(" ops/sec")),
                    listOf("Burst Single Override Put", output.burstSingleOverridePut.removeSuffix(" ops/sec")),
                    listOf("Burst Single Delete", output.burstSingleDelete.removeSuffix(" ops/sec")),
                    listOf("Batch Put", output.batchPut.removeSuffix(" ops/sec")),
                    listOf("Batch Override Put", output.batchOverridePut.removeSuffix(" ops/sec")),
                    listOf("Batch Delete", output.batchDelete.removeSuffix(" ops/sec")),
                ),
        )
    writeStringToFile(TABLE_OUTPUT_PATH, table)
}

expect fun Double.format(arg: String): String

fun LevelDB.batchOperationBenchmark(operationsCount: Int): BatchBenchmarkOutput {
    val insertions =
        buildLevelDBBatch {
            repeat(operationsCount) {
                put("key$it", "value$it")
            }
        }

    val batchPut =
        measureTime {
            batch(insertions)
            compactRange()
        }

    val batchOverridePutInsertions =
        buildLevelDBBatch {
            repeat(operationsCount) {
                put("key$it", "value${it + operationsCount}")
            }
        }

    val batchOverridePut =
        measureTime {
            batch(batchOverridePutInsertions)
            compactRange()
        }

    val batchDeleteInsertions =
        buildLevelDBBatch {
            repeat(operationsCount) {
                delete("key$it")
            }
        }

    val batchDelete =
        measureTime {
            batch(batchDeleteInsertions)
            compactRange()
        }
    return BatchBenchmarkOutput(
        putOpsSec = operationsCount / batchPut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        overridePutOpsSec = operationsCount / batchOverridePut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        deleteOpsSec = operationsCount / batchDelete.inWholeMicroseconds.toDouble() * 10.0.pow(6),
    )
}

data class SingleBenchmarkOutput(
    val putOpsSec: Double,
    val getOpsSec: Double,
    val overridePutOpsSec: Double,
    val deleteOpsSec: Double,
)

fun LevelDB.singleOperationBenchmark(operationsCount: Int): SingleBenchmarkOutput {
    val burstSinglePut =
        measureTime {
            repeat(operationsCount) {
                put("key$it", "value$it")
            }
            compactRange()
        }

    val burstSingleGet =
        measureTime {
            repeat(operationsCount) {
                get("key$it")
            }
        }

    val burstSingleOverridePut =
        measureTime {
            repeat(operationsCount) {
                put("key$it", "value${it + operationsCount}")
            }
            compactRange()
        }

    val burstSingleDelete =
        measureTime {
            repeat(operationsCount) {
                delete("key$it")
            }
            compactRange()
        }
    return SingleBenchmarkOutput(
        putOpsSec = operationsCount / burstSinglePut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        getOpsSec = operationsCount / burstSingleGet.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        overridePutOpsSec = operationsCount / burstSingleOverridePut.inWholeMicroseconds.toDouble() * 10.0.pow(6),
        deleteOpsSec = operationsCount / burstSingleDelete.inWholeMicroseconds.toDouble() * 10.0.pow(6),
    )
}

data class BatchBenchmarkOutput(
    val putOpsSec: Double,
    val overridePutOpsSec: Double,
    val deleteOpsSec: Double,
)

@Serializable
data class BenchmarkOutput(
    val burstSinglePut: String,
    val burstSingleGet: String,
    val burstSingleOverridePut: String,
    val burstSingleDelete: String,
    val batchPut: String,
    val batchOverridePut: String,
    val batchDelete: String,
)

fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
): String {
    val colWidths =
        List(headers.size) { index ->
            maxOf(
                headers[index].length,
                rows.maxOfOrNull { it[index].length } ?: 0,
            )
        }

    fun formatRow(row: List<String>) = row.mapIndexed { index, cell -> cell.padEnd(colWidths[index]) }.joinToString(" | ", "| ", " |")

    val headerRow = formatRow(headers)
    val separatorRow = colWidths.joinToString(" | ", "| ", " |") { "-".repeat(it) }
    val dataRows = rows.joinToString("\n", transform = ::formatRow)

    return "$headerRow\n$separatorRow\n$dataRows"
}

expect fun writeStringToFile(
    path: String,
    content: String,
)

expect fun getenv(name: String): String?
