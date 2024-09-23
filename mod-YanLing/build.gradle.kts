import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import meow0x7e.dol.Boot
import meow0x7e.dol.DependenceInfo
import meow0x7e.dol.FileCopySpec
import java.util.*

version = "0.1.1"

val sourceDirectory = File(layout.projectDirectory.asFile, "src")
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
        DependenceInfo("GameVersion", "^0.5.0.6"),
        DependenceInfo("Simple Frameworks", "^1.15.3")
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


tasks.register<DefaultTask>("processScriptFileList") {
    doLast {
        mapOf(
            Pair("inject_early", boot.scriptFileList_inject_early),
            Pair("earlyload", boot.scriptFileList_earlyload),
            Pair("preload", boot.scriptFileList_preload),
            Pair("script", boot.scriptFileList)
        ).forEach { (k, v) -> compileTypeScript(k).let { v.addAll(it) } }
    }
}

tasks.register<DefaultTask>("collectTweeFile") {
    doLast {
        FileCopySpec.findFilteredFilesRelativePaths(sourceDirectory, "assets.d/twee.d/") {
            it.isFile && it.extension == "twee"
        }.let {
            boot.tweeFileList.addAll(it)
        }
    }
}

tasks.named<DefaultTask>("buildBootJson") {
    dependsOn(
        tasks.named("processScriptFileList"),
        tasks.named("collectTweeFile")
    )

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

fun compileTypeScript(name: String): List<FileCopySpec> {
    File(layout.projectDirectory.asFile, "src/typescript.d/${name}.d").let {
        if (!it.exists() && !it.isDirectory)
            return listOf()
    }

    ProcessBuilder("yarn", "workspace", "${project.name}.${name}", "tsc", "--listEmittedFiles")
        .directory(rootDir)
        .start()
        ?.let {
            logger.lifecycle(it.inputReader().readText())
            if (it.waitFor() != 0)
                throw GradleException("在编译 TypeScript workspace ${project.name}.${name} 时 yarn 返回非 0 值")
        }

    return FileCopySpec.findFilteredFilesRelativePaths(
        buildDirectory,
        "javascript.d/${name}.d"
    ) { it.isFile && it.extension.lowercase(Locale.getDefault()) == "js" }
}