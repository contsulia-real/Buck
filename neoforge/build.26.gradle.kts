plugins {
    id("dev.architectury.loom-no-remap")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val minecraft = stonecutter.current.version
val common = requireNotNull(stonecutter.node?.sibling("")?.project) { "No common project for $project" }
val modId = project.property("modId").toString()
val modVersion = project.property("modVersion").toString()

version = "$modVersion+$minecraft"
base { archivesName.set("$modId-neoforge") }

architectury {
    platformSetupLoomIde()
    neoForge()
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
    named("developmentNeoForge").get().extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

val neoforgeVersion = common.property("neoforgeVersion") as String
val architecturyApiVersion = common.property("architecturyApiVersion") as String

dependencies {
    minecraft("net.minecraft:minecraft:$minecraft")
    "neoForge"("net.neoforged:neoforge:$neoforgeVersion")
    implementation("dev.architectury:architectury-neoforge:$architecturyApiVersion")
    commonBundle(project(common.path)) { isTransitive = false }
    shadowBundle(project(path = common.path, configuration = "transformProductionNeoForge")) { isTransitive = false }
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
        "neoforge_version" to neoforgeVersion,
        "architectury_version" to architecturyApiVersion,
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("")
    exclude("fabric.mod.json", "architectury.common.json")
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
