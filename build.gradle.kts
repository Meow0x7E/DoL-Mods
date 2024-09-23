allprojects {
    repositories {
        mavenLocal()
        maven(uri("https://maven.aliyun.com/repository/public"))
        mavenCentral()
        google()
    }

    buildscript {
        repositories {
            mavenLocal()
            maven(uri("https://maven.aliyun.com/repository/public"))
            mavenCentral()
            google()
        }

        dependencies {
            classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        }
    }
}

subprojects.onEach { project ->
    project.tasks.register<DefaultTask>("buildBootJson")

    project.tasks.register<Zip>("package") {
        dependsOn(project.tasks.named("buildBootJson"))

        archiveFileName.set("Meow0x7E-${project.name.substring(4)}-v${project.version}.mod.zip")
        destinationDirectory.set(layout.buildDirectory)

        entryCompression = ZipEntryCompression.DEFLATED
        includeEmptyDirs = false
        setMetadataCharset("UTF-8")
        isZip64 = true

        eachFile { logger.lifecycle("压缩 -> $path") }
    }

    project.tasks.register<DefaultTask>("clean") {
        doLast { project.layout.buildDirectory.get().asFile.deleteRecursively() }
    }
}

hashSetOf("package", "clean").forEach { taskName ->
    tasks.register<DefaultTask>(taskName) {
        dependsOn(*subprojects.map { project -> project.tasks.named(taskName) }.toTypedArray())
    }
}