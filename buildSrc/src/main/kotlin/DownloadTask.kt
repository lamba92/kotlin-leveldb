import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.utils.io.core.use
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.kotlin.dsl.property

open class DownloadTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    companion object {
        fun getHttpClient(
            logLevel: LogLevel,
            logger: Logger
        ) = HttpClient(CIO) {
            install(HttpRequestRetry) {
                retryOnException(maxRetries = 100, retryOnTimeout = true)
            }
            install(HttpTimeout) {
                allTimeouts(2.minutes)
            }
            install(Logging) {
                level = logLevel
                this.logger = KtorClientLogger { logger.lifecycle(it) }
            }
            followRedirects = true
        }
    }

    init {
        group = "download"
        description = "Download file from the internet"
        outputs.upToDateWhen { downloadFile.get().asFile.exists() }
    }

    @get:Input
    val link = objects.property<String>()

    @get:Input
    val fileName = objects.property<String>()
        .convention(link.map { it.split("/").last() })

    @get:OutputFile
    val downloadFile = objects.fileProperty()
        .convention(
            link.flatMap {
                project.layout
                    .buildDirectory
                    .file("downloads/${fileName.get()}")
            }
        )

    @get:Internal
    val logLevel = objects.property<LogLevel>()
        .convention(LogLevel.NONE)

    @TaskAction
    fun download() = runBlocking {
        logging.captureStandardOutput(org.gradle.api.logging.LogLevel.LIFECYCLE)
        val progressLogger = services.get(ProgressLoggerFactory::class.java)
            .newOperation("Download ${fileName.get()}")
        progressLogger.description = "Downloading ${fileName.get()}"
        getHttpClient(logLevel.get(), logger)
            .use { client ->
                progressLogger.started("Downloading ${fileName.get()}: 0%")
                var nextUpdate = 2.5
                client.downloadFile(link.get(), downloadFile.get().asFile.toPath())
                    .collect { progress ->
                        if (progress >= nextUpdate) {
                            val formatted = String.format("%.1f", progress)
                            progressLogger.progress("Downloading ${fileName.get()}: ${formatted}%")
                            nextUpdate += 2.5
                        }
                    }
                progressLogger.completed()
            }
    }
}
