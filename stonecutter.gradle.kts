plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id("dev.architectury.loom-no-remap") version "1.17-SNAPSHOT" apply false
    id("architectury-plugin") version "3.5-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.4.3" apply false
}

stonecutter active "1.21.1" /* [SC] DO NOT EDIT */

tasks.register("chiseledBuild") {
    group = "project"
    description = "Build every configured Minecraft version and loader."
    dependsOn(stonecutter.tasks.named("build").map { it.values })
}
