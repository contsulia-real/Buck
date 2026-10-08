from pathlib import Path
from zipfile import ZipFile
import json

break_source = Path("src/main/java/dev/buck/mixin/MultiPlayerGameModeMixin.java").read_text(encoding="utf-8")
if "addDestroyBlockEffect" not in break_source or "playLocalSound" not in break_source:
    raise SystemExit("source: local mixed-slab break effects missing")

weather_source = Path("src/main/java/dev/buck/block/WeatheringVerticalSlabBlock.java").read_text(encoding="utf-8")
if "implements WeatheringCopper" not in weather_source or "getNext(BlockState state)" not in weather_source:
    raise SystemExit("source: weathering vertical slab linkage missing")

if "return getAge() != WeatherState.OXIDIZED;" not in weather_source:
    raise SystemExit("source: copper random-tick eligibility must not depend on a registry map")
registry_source = Path("src/main/java/dev/buck/registry/BuckContent.java").read_text(encoding="utf-8")
if ("weathering.getAge() != WeatheringCopper.WeatherState.OXIDIZED" not in registry_source
        or "properties.randomTicks();" not in registry_source):
    raise SystemExit("source: copper random-tick registration must be enabled before cache initialization")

mixed_source = Path("src/main/java/dev/buck/block/MixedSlabBlock.java").read_text(encoding="utf-8")
if ("void randomTick(" not in mixed_source
        or "WeatheringCopper.getNext(" not in mixed_source
        or "mixed.setSources(" not in mixed_source
        or "BlockPos.withinManhattan(" not in mixed_source):
    raise SystemExit("source: mixed-slab copper oxidation must tick and persist each component")
if "BlockBehaviour.Properties.of().strength(1.5F).noOcclusion().randomTicks()" not in registry_source:
    raise SystemExit("source: mixed-slab random ticks must be enabled")

targets = [
    ("fabric-1.20.1", Path("fabric/versions/1.20.1/build/libs/buck-fabric-0.1.0+1.20.1.jar"), 57, "recipes", "loot_tables", "fabric.mod.json"),
    ("forge-1.20.1", Path("forge/versions/1.20.1/build/libs/buck-forge-0.1.0+1.20.1.jar"), 57, "recipes", "loot_tables", "META-INF/mods.toml"),
    ("fabric-1.21.1", Path("fabric/versions/1.21.1/build/libs/buck-fabric-0.1.0+1.21.1.jar"), 60, "recipe", "loot_table", "fabric.mod.json"),
    ("neoforge-1.21.1", Path("neoforge/versions/1.21.1/build/libs/buck-neoforge-0.1.0+1.21.1.jar"), 60, "recipe", "loot_table", "META-INF/neoforge.mods.toml"),
    ("fabric-26.3", Path("fabric/versions/26.3/build/libs/buck-fabric-0.1.0+26.3.jar"), 101, "recipe", "loot_table", "fabric.mod.json"),
    ("neoforge-26.3", Path("neoforge/versions/26.3/build/libs/buck-neoforge-0.1.0+26.3.jar"), 101, "recipe", "loot_table", "META-INF/neoforge.mods.toml"),
]

for label, jar_path, expected, recipe_dir, loot_dir, metadata_path in targets:
    with ZipFile(jar_path) as jar:
        names = jar.namelist()
        resolver_bytecode = jar.read("dev/buck/registry/BuckContent.class")
        entity_bytecode = jar.read("dev/buck/block/entity/MixedSlabBlockEntity.class")
        if b"findFirst" not in resolver_bytecode:
            raise SystemExit(f"{label}: packaged external slab source lookup missing")
        if b"savedFirstId" not in entity_bytecode or b"savedSecondId" not in entity_bytecode:
            raise SystemExit(f"{label}: missing-mod slab IDs are not preserved")
        mixed_bytecode = jar.read("dev/buck/block/MixedSlabBlock.class")
        if (b"nextOxidizedSlab" not in mixed_bytecode
                or (label == "neoforge-26.3" and b"randomTick" not in mixed_bytecode)):
            raise SystemExit(f"{label}: packaged mixed-slab random oxidation logic missing")

        if label.endswith("26.3"):
            render_class = "dev/buck/client/MixedSlabRenderState$NeighborAwareMovingBlockRenderState.class"
            if render_class not in names:
                raise SystemExit(f"{label}: neighborhood-aware mixed renderer missing")
            render_bytecode = jar.read(render_class)
            for method in (b"getBlockState", b"getFluidState", b"getBlockTint", b"getHeight", b"getMinY"):
                if method not in render_bytecode:
                    raise SystemExit(f"{label}: mixed-slab render context missing {method.decode()}")
            if b"capture" not in jar.read("dev/buck/client/MixedSlabBlockEntityRenderer.class"):
                raise SystemExit(f"{label}: neighborhood capture not wired to renderer")

        if label.startswith("neoforge-"):
            oxidation_path = "data/neoforge/data_maps/block/oxidizables.json"
            expected_oxidation = {
                "buck:vertical_cut_copper_slab": {
                    "next_oxidation_stage": "buck:vertical_exposed_cut_copper_slab"
                },
                "buck:vertical_exposed_cut_copper_slab": {
                    "next_oxidation_stage": "buck:vertical_weathered_cut_copper_slab"
                },
                "buck:vertical_weathered_cut_copper_slab": {
                    "next_oxidation_stage": "buck:vertical_oxidized_cut_copper_slab"
                },
            }
            if oxidation_path not in names:
                raise SystemExit(f"{label}: NeoForge copper oxidation data map missing")
            if json.loads(jar.read(oxidation_path)).get("values") != expected_oxidation:
                raise SystemExit(f"{label}: NeoForge copper oxidation data map mismatch")

        en_lang = json.loads(jar.read("assets/buck/lang/en_us.json"))
        zh_lang = json.loads(jar.read("assets/buck/lang/zh_cn.json"))
        if en_lang.get("block.buck.mixed_slab") != "Mixed Slab":
            raise SystemExit(f"{label}: mixed slab English display name missing")
        if zh_lang.get("block.buck.mixed_slab") != "双拼台阶":
            raise SystemExit(f"{label}: mixed slab Chinese display name missing")

        metadata = jar.read(metadata_path).decode("utf-8")
        if metadata_path.endswith(".json"):
            metadata_json = json.loads(metadata)
            if metadata_json["name"] != "蹶蹄子" or metadata_json["description"] != "为 Minecraft 添加竖半砖。":
                raise SystemExit(f"{label}: metadata text encoding mismatch")
        elif 'displayName = "蹶蹄子"' not in metadata or 'description = "为 Minecraft 添加竖半砖。"' not in metadata:
            raise SystemExit(f"{label}: metadata text encoding mismatch")

        states = [n for n in names if n.startswith("assets/buck/blockstates/vertical_") and n.endswith(".json")]
        block_models = [n for n in names if n.startswith("assets/buck/models/block/vertical_") and n.endswith(".json")]
        item_models = [n for n in names if n.startswith("assets/buck/models/item/vertical_") and n.endswith(".json")]
        recipes = [n for n in names if n.startswith(f"data/buck/{recipe_dir}/vertical_") and n.endswith(".json")]
        reverse = [n for n in names if n.startswith(f"data/buck/{recipe_dir}/horizontal_") and n.endswith(".json")]
        crafted = [n for n in names if n.startswith(f"data/buck/{recipe_dir}/crafted_vertical_") and n.endswith(".json")]
        cut = [n for n in names if n.startswith(f"data/buck/{recipe_dir}/cut_vertical_") and n.endswith(".json")]
        loot = [n for n in names if n.startswith(f"data/buck/{loot_dir}/blocks/vertical_") and n.endswith(".json")]
        mixed_states = [n for n in names if n.startswith("assets/buck/blockstates/mixed_") and n.endswith(".json")]
        mixed_models = [n for n in names if n.startswith("assets/buck/models/block/mixed_") and n.endswith(".json")]
        pair_assets = [n for n in mixed_states + mixed_models if "__" in n]
        has_mixin = "buck.mixins.json" in names
        has_mixed_entity = "dev/buck/block/entity/MixedSlabBlockEntity.class" in names
        has_break_helper = "dev/buck/block/MixedSlabBreak.class" in names
        has_old_break_helper = "dev/buck/block/MixedSlabBreaking.class" in names
        has_weathering_vertical = "dev/buck/block/WeatheringVerticalSlabBlock.class" in names

        print(label, "states", len(states), "models", len(block_models), "items", len(item_models),
              "conversions", len(recipes), "crafted", len(crafted), "stonecut", len(cut), "reverse", len(reverse), "loot", len(loot), "mixed", len(mixed_states))

        if len(states) != 101 or len(block_models) != 101 or len(item_models) != 101:
            raise SystemExit(f"{label}: static asset superset missing")
        if len(recipes) != expected or len(reverse) != expected or len(loot) != expected:
            raise SystemExit(f"{label}: version data count mismatch")

        import copy
        minecraft_version = label.split("-")[-1]
        vanilla_specs = json.loads(Path(f"slabs/{minecraft_version}-vanilla-recipes.json").read_text(encoding="utf-8"))
        expected_crafted = sum(x["recipe"]["type"] == "minecraft:crafting_shaped" for x in vanilla_specs)
        expected_cut = sum(x["recipe"]["type"] == "minecraft:stonecutting" for x in vanilla_specs)
        if (len(crafted), len(cut)) != (expected_crafted, expected_cut):
            raise SystemExit(f"{label}: shaped or stonecutting recipe count mismatch")

        for spec in vanilla_specs:
            original = copy.deepcopy(spec["recipe"])
            prefix = "cut_vertical_" if original["type"] == "minecraft:stonecutting" else "crafted_vertical_"
            recipe_file = f"data/buck/{recipe_dir}/{prefix}{spec['id']}.json"
            transformed = json.loads(jar.read(recipe_file))
            result = original["result"]
            if isinstance(result, dict):
                result["item" if minecraft_version == "1.20.1" else "id"] = f"buck:vertical_{spec['source']}"
            else:
                original["result"] = f"buck:vertical_{spec['source']}"
            if original != transformed:
                raise SystemExit(f"{label}: vanilla-equivalent recipe changed: {recipe_file}")
            if original["type"] == "minecraft:crafting_shaped" and transformed["pattern"] != ["#", "#", "#"]:
                raise SystemExit(f"{label}: crafted vertical slabs not arranged in a vertical column")

        for model_path in block_models:
            element = json.loads(jar.read(model_path))["elements"][0]
            if element["from"] != [0, 0, 0] or element["to"] != [8, 16, 16]:
                raise SystemExit(f"{label}: placed block model geometry changed: {model_path}")

        for model_path in item_models:
            element = json.loads(jar.read(model_path))["elements"][0]
            if element["from"] != [4, 0, 0] or element["to"] != [12, 16, 16]:
                raise SystemExit(f"{label}: inventory model is not centered: {model_path}")

        if label.endswith("26.3"):
            item_defs = [n for n in names if n.startswith("assets/buck/items/vertical_") and n.endswith(".json")]
            if len(item_defs) != 101:
                raise SystemExit(f"{label}: item definitions missing")
            for item_path in item_defs:
                model = json.loads(jar.read(item_path))["model"]["model"]
                if not model.startswith("buck:item/vertical_"):
                    raise SystemExit(f"{label}: item definition bypasses centered item model: {item_path}")

        if mixed_states != ["assets/buck/blockstates/mixed_slab.json"]:
            raise SystemExit(f"{label}: mixed slab blockstate registry surface expanded")
        if mixed_models != ["assets/buck/models/block/mixed_slab.json"]:
            raise SystemExit(f"{label}: mixed slab model registry surface expanded")
        mixed_model = json.loads(jar.read("assets/buck/models/block/mixed_slab.json"))
        if mixed_model.get("parent") != "minecraft:block/stone":
            raise SystemExit(f"{label}: mixed slab particle fallback is missing")

        if not has_mixin:
            raise SystemExit(f"{label}: mixin config missing")
        mixin_config = json.loads(jar.read("buck.mixins.json"))
        common_mixins = set(mixin_config.get("mixins", []))
        client_mixins = set(mixin_config.get("client", []))
        if not {"BlockItemMixin", "ServerPlayerGameModeMixin"}.issubset(common_mixins):
            raise SystemExit(f"{label}: mixed-slab common mixins missing")
        if "MultiPlayerGameModeMixin" not in client_mixins:
            raise SystemExit(f"{label}: mixed-slab client mixin missing")
        retired = {
            "BlockStateBaseMixin", "FenceBlockMixin", "WallBlockAccessor", "WallBlockMixin",
            "TorchSupportMixin", "TorchParticleMixin", "LanternSupportMixin", "SlabAttachmentRenderMixin"
        }
        if retired.intersection(common_mixins | client_mixins):
            raise SystemExit(f"{label}: removed slab-surface feature still has mixins")
        if any(
            n.startswith("dev/buck/block/SlabAttachment")
            or n.startswith("dev/buck/mixin/SlabAttachment")
            for n in names
        ):
            raise SystemExit(f"{label}: removed slab-surface feature still has classes")
        if pair_assets or not has_mixed_entity or not has_break_helper or has_old_break_helper:
            raise SystemExit(f"{label}: mixed slab packaging mismatch")

        if not has_weathering_vertical:
            raise SystemExit(f"{label}: weathering vertical slab implementation missing")

        if "dev/buck/mixin/MultiPlayerGameModeMixin.class" not in names:
            raise SystemExit(f"{label}: client mixed-slab break mixin missing")

print("all jar, material-shape recipes, stonecutting variants, copper-weathering, registry and half-break checks OK")
