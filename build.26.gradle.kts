plugins {
    id("dev.architectury.loom-no-remap")
    id("architectury-plugin")
}

val minecraft = stonecutter.current.version
val modId = project.property("modId").toString()
val modVersion = project.property("modVersion").toString()

version = "$modVersion+$minecraft"
base { archivesName.set("$modId-common") }

architectury.common(listOf("fabric", "neoforge"))

val fabricLoaderVersion = project.property("fabricLoaderVersion").toString()
val architecturyApiVersion = project.property("architecturyApiVersion").toString()

dependencies {
    minecraft("net.minecraft:minecraft:$minecraft")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("dev.architectury:architectury:$architecturyApiVersion")
}

java {
    withSourcesJar()
    val javaVersion = project.property("javaVersion").toString()
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}


val buckRecipeOutput = layout.buildDirectory.dir("generated/buck-recipes")

val generateBuckRecipes = tasks.register("generateBuckRecipes") {
    val slabList = rootProject.file("slabs/$minecraft.txt")
    inputs.file(slabList)
    inputs.file(rootProject.file("slabs/$minecraft-vanilla-recipes.json"))
    outputs.dir(buckRecipeOutput)

    doLast {
        val output = buckRecipeOutput.get().asFile
        output.deleteRecursively()

        val recipeRoot = output.resolve("data/$modId/recipe")
        val lootRoot = output.resolve("data/$modId/loot_table/blocks")
        recipeRoot.mkdirs()
        lootRoot.mkdirs()

        val slabs = slabList.readLines()
            .map(String::trim)
            .filter(String::isNotEmpty)

        val vanillaRecipes = rootProject.file("slabs/$minecraft-vanilla-recipes.json")
        val entries = groovy.json.JsonSlurper().parse(vanillaRecipes) as List<*>
        entries.forEach { raw ->
            val entry = raw as Map<*, *>
            val name = entry["id"] as String
            val source = entry["source"] as String
            val recipe = (entry["recipe"] as Map<*, *>).toMutableMap()
            val result = recipe["result"]
            recipe["result"] = if (result is Map<*, *>) {
                result.toMutableMap().also {
                    it[if (minecraft == "1.20.1") "item" else "id"] = "$modId:vertical_$source"
                }
            } else {
                "$modId:vertical_$source"
            }
            val prefix = if (recipe["type"] == "minecraft:stonecutting") "cut_vertical_" else "crafted_vertical_"
            recipeRoot.resolve("$prefix$name.json").writeText(
                groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(recipe))
            )
        }

        slabs.forEach { source ->
                val vertical = "vertical_$source"

                recipeRoot.resolve(vertical + ".json").writeText(
                    """{
  "type": "minecraft:crafting_shapeless",
  "category": "building",
  "ingredients": ["minecraft:$source"],
  "result": {"id": "$modId:$vertical"}
}
"""
                )

                recipeRoot.resolve("horizontal_" + source + ".json").writeText(
                    """{
  "type": "minecraft:crafting_shapeless",
  "category": "building",
  "ingredients": ["$modId:$vertical"],
  "result": {"id": "minecraft:$source"}
}
"""
                )

                lootRoot.resolve(vertical + ".json").writeText(
                    """{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "$modId:$vertical",
          "modifier": [
            {
              "type": "minecraft:set_count",
              "condition": {
                "type": "minecraft:match_block",
                "blocks": "$modId:$vertical",
                "state": {"type": "double"}
              },
              "count": 2
            },
            {"type": "minecraft:explosion_decay"}
          ]
        }
      ]
    }
  ]
}
"""
                )
            }

    }
}

sourceSets.main {
    resources.srcDir(buckRecipeOutput)
}

tasks.processResources {
    dependsOn(generateBuckRecipes)
}

tasks.named("sourcesJar") {
    dependsOn(generateBuckRecipes)
}
