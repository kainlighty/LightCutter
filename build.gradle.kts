import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    kotlin("jvm") version "2.1.10"

    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.gradleup.shadow").version("9.0.0-beta4")
}

group = "ru.kainlight.lightcutter"
version = "1.3.3"

val kotlinVersion = "2.1.10"
val adventureVersion = "4.19.0"
val adventureBukkitVersion = "4.3.4"
val hikariCPVersion = "6.2.1"
val mysqlConnectorVersion = "9.2.0"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    compileOnly("net.kyori:adventure-platform-bukkit:$adventureBukkitVersion")
    compileOnly("com.zaxxer:HikariCP:$hikariCPVersion")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")

    compileOnly("com.mysql:mysql-connector-j:$mysqlConnectorVersion")

    implementation(project(":API"))
    implementation(files(
        "C:/Users/danny/IdeaProjects/.Kotlin/.private/LightLibrary/bukkit/build/libs/LightLibraryBukkit-PUBLIC-1.0.jar"
    ))
}

val javaVersion = 17
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}
kotlin {
    jvmToolchain(javaVersion)
}

tasks {
    processResources {
        val libraries = listOf(
            "org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}",
            "com.zaxxer:HikariCP:${hikariCPVersion}",
            "net.kyori:adventure-text-minimessage:${adventureVersion}",
            "net.kyori:adventure-platform-bukkit:${adventureBukkitVersion}",
            "net.kyori:adventure-text-minimessage:${adventureVersion}",
            "com.mysql:mysql-connector-j:${mysqlConnectorVersion}"
        )
        val props = mapOf(
            "pluginVersion" to version,
            "description" to description,
            "kotlinVersion" to kotlinVersion,
            "adventureVersion" to adventureVersion,
            "adventureBukkitVersion" to adventureBukkitVersion,
            "libraries" to libraries
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    named<ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        archiveFileName.set("${project.name}-${project.version}.jar")

        exclude("META-INF/maven/**",
                "META-INF/INFO_BIN",
                "META-INF/INFO_SRC",
                "kotlin/**"
        )
        mergeServiceFiles()

        val shadedPath = "ru.kainlight.${project.name.lowercase()}.shaded"
        relocate("ru.kainlight.lightlibrary", "$shadedPath.lightlibrary")
        relocate("org.mariadb.jdbc", "$shadedPath.database")
    }
}

