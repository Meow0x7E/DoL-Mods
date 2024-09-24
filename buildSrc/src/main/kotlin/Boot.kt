package meow0x7e.dol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import meow0x7e.dol.plugins.AbstractAddonPlugin
import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import java.io.ByteArrayOutputStream
import java.io.File

@Serializable
data class Boot(
    /** Mod 名称 */
    val name: String,
    /** Mod 版本*/
    val version: String,
    /**
     * 一个以相对路径组成的字符串数组。每个元素为提前注入的 [JavaScript](https://developer.mozilla.org/zh-CN/docs/Glossary/JavaScript) 脚本，
     * 会在当前 `Mod` 加载后立即插入到 [DOM](https://developer.mozilla.org/zh-CN/docs/Web/API/Document_Object_Model/Introduction) 当中，
     * 使浏览器按照 [&lt;script&gt;](https://developer.mozilla.org/zh-CN/docs/Web/HTML/Element/script) 脚本元素的执行方式执行
     */
    var scriptFileList_inject_early: MutableList<FileCopySpec> = mutableListOf(),

    /**
     * 提前加载的 [JavaScript](https://developer.mozilla.org/zh-CN/docs/Glossary/JavaScript) 脚本，
     * 会在当前 `Mod` 加载后，[scriptFileList_inject_early] 脚本全部插入完成后，
     * 由 [ModLoader](https://github.com/Lyoko-Jeremie/sugarcube-2-ModLoader) 执行并等待异步指令返回，
     * 可以在这里读取到未修改的 `Passage` 的内容
     */
    var scriptFileList_earlyload: MutableList<FileCopySpec> = mutableListOf(),

    /**
     * 预加载的 [JavaScript](https://developer.mozilla.org/zh-CN/docs/Glossary/JavaScript) 脚本文件，
     * 会在引擎初始化前、`Mod` 的数据文件全部加载并合并到 [HTML](https://developer.mozilla.org/zh-CN/docs/Glossary/HTML) 的 `tw-storydata` 中后，
     * 由 [ModLoader](https://github.com/Lyoko-Jeremie/sugarcube-2-ModLoader) 执行并等待异步指令返回，
     * 可以在此处调用 [ModLoader](https://github.com/Lyoko-Jeremie/sugarcube-2-ModLoader) 的 API 读取最新的 `Passage` 数据并动态修改覆盖 `Passage` 的内容
     *
     * 注意 [scriptFileList_preload] 文件有固定的格式，参见[样例](https://github.com/Lyoko-Jeremie/sugarcube-2-ModLoader/tree/master/src/insertTools/MyMod/MyMod_script_preload_example.js)
     */
    var scriptFileList_preload: MutableList<FileCopySpec> = mutableListOf(),

    /** [CSS](https://developer.mozilla.org/zh-CN/docs/Glossary/CSS) 样式文件 */
    @Required
    var styleFileList: MutableList<FileCopySpec> = mutableListOf(),

    /** [JavaScript](https://developer.mozilla.org/zh-CN/docs/Glossary/JavaScript) 脚本文件，这是游戏的一部分 */
    @Required
    var scriptFileList: MutableList<FileCopySpec> = mutableListOf(),

    /** [twee/Twine](https://twinery.org) 剧本文件 */
    @Required
    var tweeFileList: MutableList<FileCopySpec> = mutableListOf(),

    /** 图片文件，尽可能不要用容易与文件中其他字符串混淆的文件路径，否则会意外破坏文件内容 */
    @Required
    var imgFileList: MutableList<FileCopySpec> = mutableListOf(),

    /**
     * 附加文件列表，额外打包到zip中的文件，此列表中的文件不会被加载，仅作为附加文件存在
     *
     * 请注意，这里的文件会以被当作文本文件以 [UTF-8](https://developer.mozilla.org/zh-CN/docs/Glossary/UTF-8) 编码读取并保存
     *
     * 第一个以 `readme` (不区分大小写)开头的文件会被作为 `Mod` 的说明文件，会在 `Mod 管理器` 中显示
     */
    @Required
    var additionFile: MutableList<FileCopySpec> = mutableListOf(),

    /**
     * 附加二进制文件
     *
     * 如果有需要附加的二进制文件，编写在这里时 [packModZip.ts](https://github.com/Lyoko-Jeremie/sugarcube-2-ModLoader/blob/master/src/insertTools/packModZip.ts) 会将其以二进制格式保存
     */
    var additionBinaryFile: MutableList<FileCopySpec> = mutableListOf(),

    /**
     * 附加文件夹，如果有需要附加的文件夹，编写在这里时 [packModZip.ts](https://github.com/Lyoko-Jeremie/sugarcube-2-ModLoader/blob/master/src/insertTools/packModZip.ts) 会将其下所有文件以二进制格式保存
     */
    var additionDir: MutableList<FileCopySpec> = mutableListOf(),

    /**
     * 依赖的插件列表，在此声明本 `Mod` 依赖哪些插件，在此处声明后会调用对应的插件，不满足的依赖会在加载日志中产生警告
     *
     * @see AddonPlugin
     */
    var addonPlugin: MutableList<AbstractAddonPlugin> = mutableListOf(),

    /**
     * 依赖的 `Mod`列表，可以在此声明此 `Mod` 依赖哪些前置 `Mod`，不满足的依赖会在加载日志中产生警告
     *
     * @see DependenceInfo
     */
    var dependenceInfo: MutableList<DependenceInfo> = mutableListOf()
) {
    fun buildCopySpec(): Action<in CopySpec> {
        return Action<CopySpec> {
            listOf(
                scriptFileList_inject_early,
                scriptFileList_earlyload,
                scriptFileList_preload,
                styleFileList,
                scriptFileList,
                tweeFileList,
                imgFileList,
                additionFile,
                additionBinaryFile
            ).flatten().forEach {
                it.copyToSpecYesThisIsTheFile().execute(this)
            }

            additionDir.forEach {
                it.copyToSpecYesThisIsTheDirectory().execute(this)
            }
        }
    }


    fun buildBootJson(bootJsonFile: File, json: Json): String {
        // 初始缓冲区大小: 16KB
        val buf = ByteArrayOutputStream(16384)

        @OptIn(ExperimentalSerializationApi::class)
        json.encodeToStream(this, buf)

        buf.writeTo(bootJsonFile.outputStream())

        return buf.toString()
    }
}