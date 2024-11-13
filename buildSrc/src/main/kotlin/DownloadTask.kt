import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class DownloadTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    companion object {
        fun getHttpClient(logLevel: LogLevel) = HttpClient(CIO) {
            install(HttpRequestRetry) {
                retryOnException(maxRetries = 5, retryOnTimeout = true)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
            install(Logging) {
                level = logLevel
            }
        }
    }

    @get:Input
    val link = objects.property<String>()

    @get:OutputFile
    val downloadFile = objects.fileProperty()
        .convention(
            link.flatMap {
                val fileName = it.split("/").last()
                project.layout
                    .buildDirectory
                    .file("dowloads/$fileName")
            }
        )

    @get:Internal
    val logLevel = objects.property<LogLevel>()
        .convention(LogLevel.INFO)

    @TaskAction
    fun download() = runBlocking {
        val httpClient = getHttpClient(logLevel.get())
        downloadFile.get().asFile.outputStream().use { output ->
            httpClient.get(link.get())
                .bodyAsChannel()
                .copyTo(output)
        }
    }
}

fun Project.registerDownload(link: String, directory: String) {

}