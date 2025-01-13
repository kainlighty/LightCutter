import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    id("java")
    kotlin("jvm") version "2.0.20"

    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.gradleup.shadow").version("9.0.0-beta4")
}

group = "ru.kainlight.lightcutter"
version = "1.3.2"

val kotlinVersion = "2.0.20"
val adventureVersion = "4.18.0"
val adventureBukkitVersion = "4.3.4"
val hikariCPVersion = "6.2.1"
val mysqlConnectorVersion = "9.1.0"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    compileOnly("net.kyori:adventure-platform-bukkit:$adventureBukkitVersion")
    compileOnly("com.zaxxer:HikariCP:$hikariCPVersion")

    compileOnly("com.mysql:mysql-connector-j:$mysqlConnectorVersion")

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
    processResources {
        filesMatching("plugin.yml") {
            expand(
                mapOf(
                    "pluginVersion" to version,
                    "kotlinVersion" to kotlinVersion,
                    "adventureVersion" to adventureVersion,
                    "adventureBukkitVersion" to adventureBukkitVersion,
                    "hikariCPVersion" to hikariCPVersion,
                    "mysqlVersion" to mysqlConnectorVersion
                )
            )
        }
    }

    // Настройка для задачи сборки Shadow JAR
    named<ShadowJar>("shadowJar") {
        // Настройки для Shadow JAR
        archiveBaseName.set(project.name)
        archiveFileName.set("${project.name}-${project.version}.jar")

        // Исключения и переименование пакетов
        exclude("META-INF/maven/**",
                "META-INF/INFO_BIN",
                "META-INF/INFO_SRC"
        )

        //exclude("META-INF/maven/**",
        //        "META-INF/INFO_BIN",
        //        "META-INF/INFO_SRC",
        //        "kotlin/**",
        //        "org/jetbrains/kotlin/**",
        //        "net/kyori/**")

        mergeServiceFiles()

        val shadedPath = "ru.kainlight.lightcutter.shaded"
        relocate("ru.kainlight.lightlibrary", "$shadedPath.lightlibrary")
        relocate("org.mariadb.jdbc", "$shadedPath.database")
    }
}

