package meow0x7e.dol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import java.io.File
import kotlin.reflect.jvm.jvmName


/**
 * 它基本就是在描述一个文件处理前的位置和处理后的位置，如果不需要处理，那处理前和处理后可以一样
 *
 * @property original 文件的原始位置，使用 [FileLocation] 表示
 * @property destination 文件的目标位置，也使用 [FileLocation] 表示，并可以修改
 * @see FileLocation 表示文件位置的类
 * @see FileCopySpecSerializer 专门用于序列化 [FileCopySpec] 的序列化器
 */
@Serializable(FileCopySpecSerializer::class)
data class FileCopySpec(
    val original: FileLocation,
    var destination: FileLocation
) {
    companion object {
        /**
         * 在指定的基础目录下，根据相对文件路径和过滤条件，查找并创建 [FileCopySpec] 实例的列表
         *
         * @param baseDir 基础目录，用于解析相对路径
         * @param relativeFilePath 要搜索的文件的相对路径
         * @param filter 一个函数，用于确定哪些文件应该被包含在内
         * @return 包含找到的文件的 [FileCopySpec] 实例列表
         */
        fun findFilteredFilesRelativePaths(
            baseDir: Directory,
            relativeFilePath: String,
            filter: (File) -> Boolean
        ): List<FileCopySpec> {
            val baseDirPath = baseDir.asFile.toPath()

            return baseDir.file(relativeFilePath).asFile
                .walkTopDown()
                .filter(filter)
                .map { file ->
                    FileLocation(file, baseDirPath.relativize(file.toPath())).let(::FileCopySpec)
                }
                .toList()
        }
    }

    /**
     * 构造函数，仅设置文件的原始位置，目标位置默认为原始位置
     *
     * @param original 文件的原始位置
     */
    constructor(original: FileLocation) : this(original, original)

    /**
     * 创建一个Gradle的 [Action]，用于将文件复制到指定的目录中
     *
     * @param into 目标目录的路径
     * @return 一个Gradle的 [Action]，可以添加到 Gradle 的 [CopySpec] 中
     */
    private fun copyToSpec(into: String): Action<in CopySpec> = Action<CopySpec> {
        from(destination.file) {
            into(into)
        }
    }

    // 别问为什么这个方法的名称长这样，我就问你用不用吧
    /**
     * 创建一个 Gradle 的 [Action]，用于将文件复制到目标文件所在的目录中
     *
     * @return 一个 Gradle 的 [Action]，可以添加到 Gradle 的 [CopySpec] 中
     */
    fun copyToSpecYesThisIsTheDirectory(): Action<in CopySpec> = Action<CopySpec> {
        copyToSpec(destination.relativeFilePath.toString()).execute(this)
    }

    // 别问为什么这个方法的名称长这样，我就问你用不用吧
    /**
     * 创建一个 Gradle 的 [Action]，用于将文件复制到目标文件所在目录的父目录中
     *
     * @return 一个 Gradle 的 [Action]，可以添加到Gradle的[CopySpec]中
     */
    fun copyToSpecYesThisIsTheFile(): Action<in CopySpec> = Action<CopySpec> {
        copyToSpec(destination.relativeFilePath.parent.toString()).execute(this)
    }
}

/**
 * 专门用于序列化 [FileCopySpec] 的序列化器。
 * 它仅支持序列化操作，因为反序列化操作在丢失了原始信息的情况下是不完整的。
 */
class FileCopySpecSerializer : KSerializer<FileCopySpec> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(FileCopySpec::class.jvmName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FileCopySpec) =
        encoder.encodeSerializableValue(FileLocation.serializer(), value.destination)

    /**
     * @throws SerializationException 总是抛出，反序列化操作不被支持
     */
    override fun deserialize(decoder: Decoder): Nothing =
        throw SerializationException("这个方法永远无法反序列化一段已经丢失了信息的数据，这是不可能的")
}