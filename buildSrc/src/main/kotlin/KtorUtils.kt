import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.moveTo
import kotlin.io.path.outputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

var HttpTimeoutConfig.requestTimeout
    get() = requestTimeoutMillis?.milliseconds
    set(value) {
        requestTimeoutMillis = value?.inWholeMilliseconds
    }

var HttpTimeoutConfig.connectTimeout
    get() = connectTimeoutMillis?.milliseconds
    set(value) {
        connectTimeoutMillis = value?.inWholeMilliseconds
    }

var HttpTimeoutConfig.socketTimeout
    get() = socketTimeoutMillis?.milliseconds
    set(value) {
        socketTimeoutMillis = value?.inWholeMilliseconds
    }

fun HttpTimeoutConfig.allTimeouts(duration: Duration) {
    requestTimeout = duration
    connectTimeout = duration
    socketTimeout = duration
}

@Suppress("FunctionName")
fun KtorClientLogger(log: (String) -> Unit): io.ktor.client.plugins.logging.Logger =
    object : io.ktor.client.plugins.logging.Logger {
        override fun log(message: String) = log(message)
    }

fun HttpClient.downloadFile(
    url: String,
    destination: Path,
    bufferSize: Int = 8192,
): Flow<Double> =
    flow {
        destination.parent.createDirectories()
        val response = get(url)
        val contentLength = response.contentLength() ?: error("Failed to retrieve content length")
        val byteChannel = response.bodyAsChannel()
        val tmpDestination =
            destination
                .resolveSibling("${destination.fileName}.part")
        tmpDestination
            .outputStream()
            .use { output ->
                val buffer = ByteArray(bufferSize)
                var bytesRead: Long = 0
                var read: Int

                while (!byteChannel.isClosedForRead) {
                    read = byteChannel.readAvailable(buffer)
                    if (read > 0) {
                        output.write(buffer, 0, read)
                        bytesRead += read

                        // Calculate and emit the download progress as a percentage
                        val progress = ((bytesRead.toDouble() / contentLength) * 100)
                        emit(progress)
                    }
                }
            }
        tmpDestination.moveTo(destination, true)
        emit(100.0)
    }
