package meow0x7e.dol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import meow0x7e.dol.plugins.AbstractAddonPlugin
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * `Boot` 类表示一个 `DoL Mod` 的基本配置和文件信息
 *
 * @property name Mod 的名称
 * @property version Mod 的版本
 * @property scriptFileList_inject_early 提前注入的 JavaScript 脚本列表，Mod 加载后立即执行
 * @property scriptFileList_earlyload 提前加载的 JavaScript 脚本列表，在inject_early脚本后执行
 * @property scriptFileList_preload 预加载的 JavaScript 脚本列表，在引擎初始化前执行
 * @property styleFileList CSS样式文件列表
 * @property scriptFileList JavaScript 脚本文件列表，是游戏的一部分
 * @property tweeFileList Twine剧本文件列表
 * @property imgFileList 图片文件列表
 * @property additionFile 附加文件列表，仅作为额外文件存在
 * @property additionBinaryFile 附加二进制文件列表
 * @property additionDir 附加文件夹列表，包含所有文件以二进制格式保存
 * @property addonPlugin 依赖的插件列表
 * @property dependenceInfo 依赖的 Mod 列表
 */
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
     * @see AbstractAddonPlugin
     */
    var addonPlugin: MutableList<AbstractAddonPlugin> = mutableListOf(),

    /**
     * 依赖的 `Mod`列表，可以在此声明此 `Mod` 依赖哪些前置 `Mod`，不满足的依赖会在加载日志中产生警告
     *
     * @see DependenceInfo
     */
    var dependenceInfo: MutableList<DependenceInfo> = mutableListOf()
) {
    /**
     * 构造一个新的 `Boot` 实例
     *
     * @param project [Project]对象，用于获取 Mod 的名称和版本
     * @param scriptFileList_inject_early 提前注入的 JavaScript 脚本列表，Mod 加载后立即执行 脚本列表
     * @param scriptFileList_earlyload 提前加载的 JavaScript 脚本列表，在inject_early脚本后执行列表
     * @param scriptFileList_preload 预加载的 JavaScript 脚本列表，在引擎初始化前执行
     * @param styleFileList CSS样式文件列表
     * @param scriptFileList JavaScript 脚本文件列表，是游戏的一部分
     * @param tweeFileList Twine剧本文件列表
     * @param imgFileList 图片文件列表
     * @param additionFile 附加文件列表，仅作为额外文件存在
     * @param additionBinaryFile 附加二进制文件列表
     * @param additionDir 附加文件夹列表，包含所有文件以二进制格式保存
     * @param addonPlugin 依赖的插件列表
     * @param dependenceInfo 依赖的 Mod 列表
     */
    constructor(
        project: Project,
        scriptFileList_inject_early: MutableList<FileCopySpec> = mutableListOf(),
        scriptFileList_earlyload: MutableList<FileCopySpec> = mutableListOf(),
        scriptFileList_preload: MutableList<FileCopySpec> = mutableListOf(),
        styleFileList: MutableList<FileCopySpec> = mutableListOf(),
        scriptFileList: MutableList<FileCopySpec> = mutableListOf(),
        tweeFileList: MutableList<FileCopySpec> = mutableListOf(),
        imgFileList: MutableList<FileCopySpec> = mutableListOf(),
        additionFile: MutableList<FileCopySpec> = mutableListOf(),
        additionBinaryFile: MutableList<FileCopySpec> = mutableListOf(),
        additionDir: MutableList<FileCopySpec> = mutableListOf(),
        addonPlugin: MutableList<AbstractAddonPlugin> = mutableListOf(),
        dependenceInfo: MutableList<DependenceInfo> = mutableListOf()
    ) : this(
        project.name.substring(4),
        project.version.toString(),
        scriptFileList_inject_early,
        scriptFileList_earlyload,
        scriptFileList_preload,
        styleFileList,
        scriptFileList,
        tweeFileList,
        imgFileList,
        additionFile,
        additionBinaryFile,
        additionDir,
        addonPlugin,
        dependenceInfo
    )

    /**
     * 构建一个用于复制文件的 [CopySpec] 操作。
     *
     * 此方法通过遍历所有需要被复制的文件和目录列表(包括 JavaScript 脚本、CSS 样式文件、图片文件等)，
     * 并为每个文件或目录创建一个 [CopySpec] 操作，以便在 Gradle 构建过程中进行复制。
     *
     * @return 一个 [Action] 对象，该对象包含了所有必要的复制操作。
     */
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


    /**
     * 将 `Boot` 类的实例序列化为 JSON 格式，并写入到指定的文件中。
     *
     * 此方法使用 `Kotlinx.serialization` 库将 `Boot` 类的实例(即 Mod 的配置信息)序列化为 JSON 格式，
     * 然后将序列化后的数据写入到提供的文件中。
     *
     * @param bootJsonFile 目标文件，序列化后的 JSON 数据将被写入此文件。
     * @param json 用于序列化的 Json 对象。
     * @return 序列化后的 JSON 字符串
     */
    fun buildBootJson(bootJsonFile: File, json: Json): String {
        // 初始缓冲区大小: 16KB
        val buf = ByteArrayOutputStream(16384)

        @OptIn(ExperimentalSerializationApi::class)
        json.encodeToStream(this, buf)

        buf.writeTo(bootJsonFile.outputStream())

        return buf.toString()
    }
}