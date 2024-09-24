package meow0x7e.dol.plugins

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.jvm.jvmName

@Serializable(with = BeautySelectorAddonSerializer::class)
data class BeautySelectorAddon(
    /** 插件所用 `Mod` 的版本 */
    override val modVersion: String = "^2.0.0",

    /** 插件 types 参数，也可以通过 `params["types"]` 访问 */
    val types: MutableList<TypeData> = mutableListOf()
) : AbstractAddonPlugin() {
    /** 插件来源 `Mod` */
    override val modName: String = "BeautySelectorAddon"

    /** 插件来源 `Mod` 所注册的插件名称 */
    override val addonName: String = "BeautySelectorAddon"

    /** 插件参数 */
    override val params: Map<String, List<TypeData>> = mapOf("types" to types)

    @Serializable
    data class TypeData(val type: String, val imgFileListFile: String)
}

class BeautySelectorAddonSerializer : KSerializer<BeautySelectorAddon> {
    private val paramsSerializer =
        MapSerializer(String.serializer(), ListSerializer(BeautySelectorAddon.TypeData.serializer()))

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(BeautySelectorAddon::class.jvmName) {
            element<String>("modName")
            element<String>("addonName")
            element<String>("modVersion")
            element<Map<String, MutableList<BeautySelectorAddon.TypeData>>>("params")
        }

    override fun serialize(encoder: Encoder, value: BeautySelectorAddon) = encoder.beginStructure(descriptor).run {
        encodeStringElement(descriptor, 0, value.modName)
        encodeStringElement(descriptor, 1, value.addonName)
        encodeStringElement(descriptor, 2, value.modVersion)
        encodeSerializableElement(descriptor, 3, paramsSerializer, value.params)
        endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): BeautySelectorAddon {
        var modVersion: String? = null
        var params: Map<String, List<BeautySelectorAddon.TypeData>>? = null

        decoder.beginStructure(descriptor).run {
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    CompositeDecoder.UNKNOWN_NAME -> continue@loop
                    0, 1 -> check(decodeStringElement(descriptor, i) == "BeautySelectorAddon") {
                        "这不是个 BeautySelectorAddon 插件"
                    }

                    2 -> modVersion = decodeStringElement(descriptor, i)
                    3 -> params = decodeSerializableElement(descriptor, i, paramsSerializer)
                    else -> error("意外的索引: $i")
                }
            }
            endStructure(descriptor)
        }

        return BeautySelectorAddon(
            modVersion ?: throw SerializationException("缺失 modVersion"),
            (params?.getOrElse("types") { throw SerializationException("缺失 types") } as List<BeautySelectorAddon.TypeData>).toMutableList()
        )
    }
}