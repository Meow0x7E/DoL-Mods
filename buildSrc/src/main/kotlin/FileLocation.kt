package meow0x7e.dol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.jvm.jvmName

@Serializable(with = FileLocationSerializer::class)
data class FileLocation(
    val file: File,
    val relativeFilePath: Path
) {
    constructor(file: File, relativeFilePath: String) : this(file, Path(relativeFilePath))

    fun toFileCopySpec() = FileCopySpec(this)
}

/**
 * 专门用于序列化 [FileLocation] 的序列化器。
 * 它仅支持序列化操作，因为反序列化操作在丢失了原始信息的情况下是不完整的。
 */
class FileLocationSerializer : KSerializer<FileLocation> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(FileLocation::class.jvmName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FileLocation) =
        encoder.encodeString(value.relativeFilePath.toString())

    /**
     * @throws SerializationException 总是抛出，反序列化操作不被支持
     */
    override fun deserialize(decoder: Decoder): Nothing =
        throw SerializationException("这个方法永远无法反序列化一段已经丢失了信息的数据，这是不可能的")
}