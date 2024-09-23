package meow0x7e.dol

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

data class FileLocation(
    val file: File,
    val relativeFilePath: Path
) {
    constructor(file: File, relativeFilePath: String) : this(file, Path(relativeFilePath))

    fun toFileCopySpec() = FileCopySpec(this)
}