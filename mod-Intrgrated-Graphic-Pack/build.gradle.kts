import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import meow0x7e.dol.Boot
import meow0x7e.dol.DependenceInfo
import meow0x7e.dol.FileCopySpec
import meow0x7e.dol.FileLocation

version = "1.0.0-Alpha"

val projectDirectory = layout.projectDirectory.asFile
val buildDirectory = layout.buildDirectory.get().asFile

/** boot.json */
val boot = Boot(
    name = project.name,
    version = project.version.toString(),
    scriptFileList_inject_early = hashSetOf(),
    styleFileList = hashSetOf(),
    scriptFileList = hashSetOf(),
    tweeFileList = hashSetOf(),
    imgFileList = hashSetOf(),
    additionFile = hashSetOf(),
    dependenceInfo = hashSetOf(
        DependenceInfo("ModLoader", "^2.18.3"),
        DependenceInfo("GameVersion", "^0.5.0.6")
    )
)

/** package 打包时使用的 copySpec */
var zipCopySpec = copySpec()

/** 配置序列化 Json 时使用的格式 */
@OptIn(ExperimentalSerializationApi::class)
val jsonConfig = Json {
    prettyPrint = true
    prettyPrintIndent = "\t"
}

val imgPackList = listOf<Map<String, Any>>(
    hashMapOf(
        Pair("name", "GameOriginalImagePack"),
        Pair("enable", true),
        Pair("repoUrl", "")
    ),
    hashMapOf(
        Pair("name", "Degrees of Lewdity Graphics Mod"),
        Pair("enable", true),
        Pair("repoUrl", "https://gitgud.io/BEEESSS/degrees-of-lewdity-graphics-mod")
    ),
    hashMapOf(
        Pair("name", "BEEESSS Community Sprite Compilation"),
        Pair("enable", true),
        Pair("repoUrl", "https://gitgud.io/Kaervek/kaervek-beeesss-community-sprite-compilation")
    ),
    hashMapOf(
        Pair("name", "BEEESSS Wax"),
        Pair("enable", true),
        Pair("repoUrl", "https://gitgud.io/GTXMEGADUDE/beeesss-wax")
    ),
    hashMapOf(
        Pair("name", "通用战斗美化"),
        Pair("enable", true),
        Pair("repoUrl", "https://github.com/site098/mysterious")
    ),
    hashMapOf(
        Pair("name", "Saver Meal"),
        Pair("enable", true),
        Pair("repoUrl", "https://gitgud.io/GTXMEGADUDE/double-cheeseburger")
    ),
    hashMapOf(
        Pair("name", "DOL_BJ_hair_extend"),
        Pair("enable", true),
        Pair("repoUrl", "https://github.com/zubonko/DOL_BJ_hair_extend")
    ),
    hashMapOf(
        Pair("name", "Tattoo"),
        Pair("enable", true),
        Pair("repoUrl", "https://github.com/zubonko/DOL_BJ_hair_extend")
    )
)

tasks.register<DefaultTask>("processImgFile") {
    val imgTypes = hashSetOf("gif", "svg", "png")

    doLast {
        imgPackList.forEach { map ->
            val name = map["name"] as String
            val relativeDirectoryPath = "img.d/${name}/img/"

            FileCopySpec.findFilteredFilesRelativePaths(projectDirectory, relativeDirectoryPath) { file ->
                file.isFile && imgTypes.contains(file.extension)
            }.let {
                val relativeFilePath = "img.d/${name}/imgFileList.json"
                val imgFileDirectory = File(projectDirectory, relativeDirectoryPath)
                val imgFileListFile = File(buildDirectory, relativeFilePath).apply {
                    parentFile.mkdirs()
                }

                @OptIn(ExperimentalSerializationApi::class)
                jsonConfig.encodeToStream(it, imgFileListFile.outputStream())

                FileLocation(imgFileListFile, relativeFilePath)
                    .toFileCopySpec()
                    .let(boot.additionFile::add)

                FileLocation(imgFileDirectory, relativeDirectoryPath)
                    .toFileCopySpec()
                    .let(boot.additionDir::add)
            }
        }
    }
}

tasks.named<DefaultTask>("buildBootJson") {
    dependsOn(tasks.named("processImgFile"))

    doLast {
        val bootJsonFile = File(buildDirectory, "boot.json")

        boot.buildBootJson(bootJsonFile, jsonConfig).let(logger::lifecycle)

        zipCopySpec.run {
            from(bootJsonFile)
            boot.buildCopySpec().execute(this)
        }
    }
}

tasks.named<Zip>("package") { with(zipCopySpec) }