import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import meow0x7e.dol.Boot
import meow0x7e.dol.DependenceInfo
import meow0x7e.dol.FileCopySpec
import java.util.*

version = "0.1.1-Alpha"

val sourceDirectory = layout.projectDirectory.dir("src")

/** boot.json */
val boot = Boot(
    project,
    dependenceInfo = mutableListOf(
        DependenceInfo("ModLoader", "^2.18.3"),
        DependenceInfo("GameVersion", "^0.5.0.6"),
        DependenceInfo("Simple Frameworks", "^1.15.3")
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

tasks.register<DefaultTask>("processScriptFileList") {
    doLast {
        mapOf(
            "inject_early" to boot.scriptFileList_inject_early,
            "earlyload" to boot.scriptFileList_earlyload,
            "preload" to boot.scriptFileList_preload,
            "script" to boot.scriptFileList
        ).forEach { (k, v) -> compileTypeScript(k).let { v += it } }
    }
}

tasks.register<DefaultTask>("collectTweeFile") {
    doLast {
        FileCopySpec.findFilteredFilesRelativePaths(sourceDirectory, "assets.d/twee.d/") {
            it.isFile && it.extension == "twee"
        }.let { boot.tweeFileList += it }
    }
}

tasks.named<DefaultTask>("buildBootJson") {
    dependsOn(
        tasks.named("processScriptFileList"),
        tasks.named("collectTweeFile")
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

fun compileTypeScript(name: String): List<FileCopySpec> {
    layout.projectDirectory.dir("src/typescript.d/${name}.d").asFile
        .run { exists() && isDirectory }
        .let { if (!it) return listOf() }

    ProcessBuilder("yarn", "workspace", "${project.name}.${name}", "tsc", "--listEmittedFiles")
        .directory(rootDir)
        .start()
        // 把 Yarn 的日志输出到 lifecycle 级别的日志中
        .also { logger.lifecycle(it.inputReader().readText()) }
        .waitFor()
        // 如果 Yarn 的返回值不为 0 则终止 Gradle 构建
        .let { if (it != 0) throw GradleException("在 Workspace ${project.name}.${name} 编译 TypeScript 时 Yarn 返回非 0 值！") }

    return FileCopySpec.findFilteredFilesRelativePaths(
        layout.buildDirectory.get(),
        "javascript.d/${name}.d"
    ) { it.isFile && it.extension.lowercase(Locale.getDefault()) == "js" }
}