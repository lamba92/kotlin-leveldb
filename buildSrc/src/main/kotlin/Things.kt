import org.gradle.api.file.FileSystemLocation
import java.nio.file.Path

val FileSystemLocation.asPath: Path
    get() = asFile.toPath()
