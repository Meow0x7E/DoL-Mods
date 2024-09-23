plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.24"
}

repositories {
    mavenLocal()
    maven(uri("https://maven.aliyun.com/repository/public"))
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}