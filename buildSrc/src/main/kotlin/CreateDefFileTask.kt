import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty

open class CreateDefFileTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    init {
        group = "generate"
        description = "Generate a def file for a cinterop task"
    }

    @get:InputFiles
    val headers = objects.fileCollection()

    @get:Input
    val compilerOpts = objects.listProperty<String>()

    @get:Input
    val linkerOpts = objects.listProperty<String>()

    @get:Input
    val libraryPaths = objects.listProperty<String>()

    @get:OutputFile
    val defFile = objects.fileProperty()

    @get:Input
    val staticLibs = objects.listProperty<String>()

    @TaskAction
    fun generate() {
        require(!headers.isEmpty) { "No headers provided" }
        defFile.get().asFile.writeText(buildString {
            val headersString = headers.files.joinToString(" ") { "\"${it.absolutePath}\"" }
            appendLine("headers = $headersString")
            if (libraryPaths.isPresent && libraryPaths.get().isNotEmpty()) {
                appendLine("libraryPaths = ${libraryPaths.get().joinToString(" ") { "\"$it\"" }}")
            }
            if (compilerOpts.isPresent && compilerOpts.get().isNotEmpty()) {
                appendLine("compilerOpts = ${compilerOpts.get().joinToString(" ") { "\"$it\"" }}")
            }
            if (linkerOpts.isPresent && linkerOpts.get().isNotEmpty()) {
                appendLine("linkerOpts = ${linkerOpts.get().joinToString(" ") { "\"$it\"" }}")
            }
            if (staticLibs.isPresent && staticLibs.get().isNotEmpty()) {
                appendLine("staticLibraries = ${staticLibs.get().joinToString(" ") { "\"$it\"" }}")
            }
        })
    }
}
