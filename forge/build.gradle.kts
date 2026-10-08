plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val minecraft = stonecutter.current.version
val common = requireNotNull(stonecutter.node?.sibling("")?.project) { "No common project for $project" }
val modId = project.property("modId").toString()
val modVersion = project.property("modVersion").toString()

version = "$modVersion+$minecraft"
base { archivesName.set("$modId-forge") }

architectury {
    platformSetupLoomIde()
    forge()
}

val commonBundle = configurations.create("commonBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val shadowBundle = configurations.create("shadowBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    named("developmentForge").get().extendsFrom(commonBundle)
}

loom {
    silentMojangMappingsLicense()
    forge {
        mixinConfig("buck.mixins.json")
    }
}

val forgeVersion = common.property("forgeVersion") as String
val architecturyApiVersion = common.property("architecturyApiVersion") as String

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())
    "forge"("net.minecraftforge:forge:$forgeVersion")
    modImplementation("dev.architectury:architectury-forge:$architecturyApiVersion")
    commonBundle(project(path = common.path, configuration = "namedElements")) { isTransitive = false }
    shadowBundle(project(path = common.path, configuration = "transformProductionForge")) { isTransitive = false }
}

java {
    withSourcesJar()
    val javaVersion = common.property("javaVersion").toString().toInt()
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val modName = project.property("modName").toString()
    val modDescription = project.property("modDescription").toString()
    val modLicense = project.property("modLicense").toString()
    val props = mapOf(
        "mod_id" to modId,
        "mod_version" to modVersion,
        "mod_name" to modName,
        "mod_description" to modDescription,
        "mod_license" to modLicense,
        "minecraft_version" to minecraft,
        "architectury_version" to architecturyApiVersion,
    )
    inputs.properties(props)
    filesMatching("META-INF/mods.toml") { expand(props) }
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
    exclude("fabric.mod.json", "architectury.common.json")
    manifest.attributes["MixinConfigs"] = "buck.mixins.json"
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier.set("dev")
}
