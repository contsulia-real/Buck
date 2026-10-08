pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.minecraftforge.net/") { name = "Forge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForge" }
        maven("https://maven.kikugie.dev/releases") { name = "Stonecutter" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Buck"

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true

    create(rootProject) {
        versions("1.20.1", "1.21.1")
        versions("26.3").buildscript("build.26.gradle.kts")

        branch("fabric") {
            inherit()
            versions("26.3").buildscript("build.26.gradle.kts")
        }

        branch("neoforge") {
            versions("1.21.1")
            versions("26.3").buildscript("build.26.gradle.kts")
        }

        branch("forge") {
            versions("1.20.1")
        }

        vcsVersion = "1.21.1"
    }
}
