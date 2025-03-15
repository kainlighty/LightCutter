plugins {
    id("java")
    kotlin("jvm") version "2.1.10"
    id("maven-publish")
}

val pluginName: String = parent!!.name
val artifactName: String = project.name.lowercase()

val parentGroup: String = parent!!.group.toString()
val parentVersion: String = parent!!.version.toString()

group = parentGroup
version = parentVersion

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

val javaVersion = JavaLanguageVersion.of(17)
java {
    toolchain.languageVersion.set(javaVersion)
    withSourcesJar()
    withJavadocJar()
}
kotlin {
    jvmToolchain { javaVersion }
}

publishing {
    val gitUrl = "github.com/kainlighty/$pluginName"

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            tasks.kotlinSourcesJar
            tasks.javadoc

            pom {
                artifactId = artifactName
                version = parentVersion

                name.set(pluginName)
                description.set(parent!!.description)
                url.set("https://$gitUrl")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://$gitUrl?tab=MIT-1-ov-file#")
                    }
                }

                developers {
                    developer {
                        id.set("kainlight")
                        name.set("Danil Panov")
                        organization.set("kainlighty")
                        url.set("https://github.com/kainlighty")
                    }
                }

                scm {
                    connection.set("scm:git:git://$gitUrl.git")
                    developerConnection.set("scm:git:git@github.com:kainlighty/$pluginName.git")
                    url.set("https://$gitUrl")
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://$gitUrl/issues")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.$gitUrl")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}