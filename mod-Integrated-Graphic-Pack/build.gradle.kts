import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import meow0x7e.dol.*
import meow0x7e.dol.plugins.BeautySelectorAddon

version = "1.0.0-Alpha"

val buildDirectory = layout.buildDirectory.get().asFile

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
    mutableMapOf(
        "name" to "GameOriginalImagePack",
        "enable" to true,
        "repoUrl" to ""
    ),
    mutableMapOf(
        "name" to "Degrees of Lewdity Graphics Mod",
        "enable" to true,
        "repoUrl" to "https://gitgud.io/BEEESSS/degrees-of-lewdity-graphics-mod"
    ),
    mutableMapOf(
        "name" to "BEEESSS Community Sprite Compilation",
        "enable" to true,
        "repoUrl" to "https://gitgud.io/Kaervek/kaervek-beeesss-community-sprite-compilation"
    ),
    mutableMapOf(
        "name" to "BEEESSS Wax",
        "enable" to true,
        "repoUrl" to "https://gitgud.io/GTXMEGADUDE/beeesss-wax"
    ),
    mutableMapOf(
        "name" to "通用战斗美化",
        "enable" to true,
        "repoUrl" to "https://github.com/site098/mysterious"
    ),
    mutableMapOf(
        "name" to "Saver Meal",
        "enable" to true,
        "repoUrl" to "https://gitgud.io/GTXMEGADUDE/double-cheeseburger"
    ),
    mutableMapOf(
        "name" to "DOL_BJ_hair_extend",
        "enable" to true,
        "repoUrl" to "https://github.com/zubonko/DOL_BJ_hair_extend"
    ),
    mutableMapOf(
        "name" to "Tattoo",
        "enable" to true,
        "repoUrl" to "https://github.com/zubonko/DOL_BJ_hair_extend"
    )
)

tasks.register<DefaultTask>("processImgFile") {
    val imgTypes = mutableListOf("gif", "svg", "png")

    doLast {
        imgPackList.reversed().map { map ->
            val name = map["name"] as String

            val baseDirectoryRelativePath = "img.d/${name}"
            val imgFileListJson = "img.d/${name}/imgFileList.json"

            val baseDirectory = layout.projectDirectory.dir(baseDirectoryRelativePath)
            val imgFileListFile = layout.buildDirectory.get().file(imgFileListJson).asFile.apply { parentFile::mkdirs }

            val fileCopySpecList = FileCopySpec.findFilteredFilesRelativePaths(baseDirectory, "img") {
                it.isFile && it.extension.let(imgTypes::contains)
            }

            @OptIn(ExperimentalSerializationApi::class)
            json.encodeToStream(fileCopySpecList, imgFileListFile.outputStream())

            FileLocation(imgFileListFile, imgFileListJson)
                .toFileCopySpec()
                .let(boot.additionFile::add)

            FileLocation(baseDirectory.asFile, baseDirectoryRelativePath)
                .toFileCopySpec()
                .let(boot.additionDir::add)

            BeautySelectorAddon.TypeData(name, imgFileListJson)
        }.let { list ->
            (boot.addonPlugin.find { it.modName == "BeautySelectorAddon" } as? BeautySelectorAddon)!!.types += list
        }
    }
}

tasks.named<DefaultTask>("buildBootJson") {
    dependsOn(tasks.named("processImgFile"))

    doLast {
        val bootJsonFile = File(buildDirectory, "boot.json")

        boot.buildBootJson(bootJsonFile, json).let(logger::lifecycle)

        zipCopySpec.run {
            from(bootJsonFile)
            boot.buildCopySpec().execute(this)
        }
    }
}

tasks.named<Zip>("package") { with(zipCopySpec) }