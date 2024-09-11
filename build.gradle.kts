import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    kotlin("jvm") version "2.0.20"

    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

group = "ru.kainlight.lightcutter"
version = "1.3.0"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io/")
}

dependencies {
    implementation(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.zaxxer:HikariCP:5.1.0")

    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")

    implementation(files(
        "C:/Users/danny/IdeaProjects/.Kotlin/.private/LightLibrary/bukkit/build/libs/LightLibraryBukkit-PUBLIC-1.0.jar"
    ))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
kotlin {
    jvmToolchain(17)
}

tasks {
    // Настройка для задачи сборки Shadow JAR
    named<ShadowJar>("shadowJar") {
        // Настройки для Shadow JAR
        archiveBaseName.set(project.name)
        archiveFileName.set("${project.name}-${project.version}.jar")

        // Исключения и переименование пакетов
        exclude("META-INF/maven/**")
        exclude("META-INF/INFO_BIN")
        exclude("META-INF/INFO_SRC")
        //exclude("kotlin")
        //exclude("org/jetbrains/kotlin/**")

        val shadedPath = "ru.kainlight.lightcutter.shaded"

        //relocate("net.kyori", "$shadedPath.net.kyori")
        relocate("ru.kainlight.lightlibrary", "$shadedPath.lightlibrary")
    }
}

