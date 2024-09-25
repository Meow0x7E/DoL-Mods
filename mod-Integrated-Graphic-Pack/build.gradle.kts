import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import meow0x7e.dol.Boot
import meow0x7e.dol.DependenceInfo
import meow0x7e.dol.FileCopySpec
import meow0x7e.dol.FileLocation
import meow0x7e.dol.plugins.BeautySelectorAddon
import meow0x7e.text.getTerminalDisplayLength

version = "1.0.0-Bata"

/** boot.json */
val boot = Boot(
    name = project.name,
    version = project.version.toString(),
    scriptFileList_inject_early = mutableListOf(),
    styleFileList = mutableListOf(),
    scriptFileList = mutableListOf(),
    tweeFileList = mutableListOf(),
    imgFileList = mutableListOf(),
    additionFile = mutableListOf(),
    addonPlugin = mutableListOf(
        BeautySelectorAddon(modVersion = "v2.0.0")
    ),
    dependenceInfo = mutableListOf(
        DependenceInfo("ModLoader", "^2.18.3"),
        DependenceInfo("GameVersion", "^0.5.0.6"),
        DependenceInfo("BeautySelectorAddon", "^2.0.0")
    )
)

/** package 打包时使用的 copySpec */
var zipCopySpec = copySpec()

/** 配置序列化 Json 时使用的格式 */
@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    prettyPrintIndent = "\t"
}

val imgPackList = listOf<Map<String, Any>>(
    mapOf(
        "name" to "GameOriginalImagePack",
        "include" to true,
        "repoUrl" to "https://github.com/Eltirosto/Degrees-of-Lewdity-Chinese-Localization"
    ),
    mapOf(
        "name" to "Degrees of Lewdity Graphics Mod",
        "include" to true,
        "repoUrl" to "https://gitgud.io/BEEESSS/degrees-of-lewdity-graphics-mod"
    ),
    mapOf(
        "name" to "BEEESSS Community Sprite Compilation",
        "include" to true,
        "repoUrl" to "https://gitgud.io/Kaervek/kaervek-beeesss-community-sprite-compilation"
    ),
    mapOf(
        "name" to "BEEESSS Wax",
        "include" to true,
        "repoUrl" to "https://gitgud.io/GTXMEGADUDE/beeesss-wax"
    ),
    mapOf(
        "name" to "通用战斗美化",
        "include" to true,
        "repoUrl" to "https://github.com/site098/mysterious"
    ),
    mapOf(
        "name" to "Saver Meal",
        "include" to true,
        "repoUrl" to "https://gitgud.io/GTXMEGADUDE/double-cheeseburger"
    ),
    mapOf(
        "name" to "DOL_BJ_hair_extend",
        "include" to true,
        "repoUrl" to "https://github.com/zubonko/DOL_BJ_hair_extend"
    ),
    mapOf(
        "name" to "Tattoo",
        "include" to true,
        "repoUrl" to "https://github.com/zubonko/DOL_BJ_hair_extend"
    )
).filter { it["include"] as Boolean }

tasks.register<DefaultTask>("buildMarkdown") {
    val readme = layout.buildDirectory.file("Readme.md")
    outputs.file(readme)

    doLast {
        val f = readme.get().asFile
        val l = imgPackList.map { it["name"] as String to (it["repoUrl"] as String).let { "[点击跳转](${it})" } }
        val targetLength = l.maxOfOrNull { it.first.getTerminalDisplayLength() }!! + 2 to
                l.maxOfOrNull { it.second.getTerminalDisplayLength() }!! + 2

        f.bufferedWriter().apply {
            appendLine(buildTableLine("美化名称" to "仓库链接", targetLength))
            appendLine(buildTableLine(":---:" to ":---:", targetLength))

            l.forEach { appendLine(buildTableLine(it, targetLength)) }

            flush()
            close()
        }

        boot.additionFile.add(FileLocation(f, f.name).toFileCopySpec())
    }
}

tasks.register<DefaultTask>("processImgFile") {
    val imgTypes = setOf("gif", "svg", "png")
    val filter = { it: File -> it.isFile && it.extension.let(imgTypes::contains) }

    doLast {
        imgPackList.reversed()
            .map {
                val type = it["name"] as String

                val baseDirectoryRelativePath = "img.d/${type}"
                val imgFileListJson = "img.d/${type}/imgFileList.json"

                val baseDirectory = layout.projectDirectory.dir(baseDirectoryRelativePath)
                val imgFileListFile = layout.buildDirectory.file(imgFileListJson).get().asFile
                    .apply { parentFile.mkdirs() }

                @OptIn(ExperimentalSerializationApi::class)
                json.encodeToStream(
                    FileCopySpec.findFilteredFilesRelativePaths(baseDirectory, "img", filter),
                    imgFileListFile.outputStream()
                )

                FileLocation(imgFileListFile, imgFileListJson)
                    .toFileCopySpec()
                    .let(boot.additionFile::add)

                FileLocation(baseDirectory.asFile, baseDirectoryRelativePath)
                    .toFileCopySpec()
                    .let(boot.additionDir::add)

                BeautySelectorAddon.TypeData(type, imgFileListJson)
            }.let { typeDataList ->
                // 如果是 Null 那还构建个球，直接断言
                (boot.addonPlugin.find { it.modName == "BeautySelectorAddon" } as? BeautySelectorAddon)!!.types += typeDataList
            }
    }
}

tasks.named<DefaultTask>("buildBootJson") {
    dependsOn(
        tasks.named("processImgFile"),
        tasks.named("buildMarkdown")
    )

    val bootJsonFile = File(layout.buildDirectory.get().asFile, "boot.json")

    doLast {
        boot.buildBootJson(bootJsonFile, json).let(logger::lifecycle)

        zipCopySpec.run {
            from(bootJsonFile)
            boot.buildCopySpec().execute(this)
        }
    }
}

tasks.named<Zip>("package") { with(zipCopySpec) }

fun centerString(str: String, targetLength: Int): String {
    val length = str.getTerminalDisplayLength()
    val before = (targetLength - length) / 2
    val after = targetLength - length - before

    return "${" ".repeat(before)}${str}${" ".repeat(after)}"
}

fun buildTableLine(pair: Pair<String, String>, len: Pair<Int, Int>): String =
    "|${centerString(pair.first, len.first)}|${centerString(pair.second, len.second)}|"