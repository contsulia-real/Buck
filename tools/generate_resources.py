from pathlib import Path
from zipfile import ZipFile
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft"
RES = ROOT / "src/main/resources"
VERSIONS = ("1.20.1", "1.21.1", "26.3")


def minecraft_jar(version: str) -> Path:
    return next(
        p for p in CACHE.rglob("*.jar")
        if version in p.name and "sources" not in p.name
    )


JARS = {version: minecraft_jar(version) for version in VERSIONS}


def read_slabs(version: str):
    result = {}
    with ZipFile(JARS[version]) as jar:
        for name in jar.namelist():
            if not (name.startswith("assets/minecraft/blockstates/") and name.endswith("_slab.json")):
                continue
            source = Path(name).stem
            state = json.loads(jar.read(name))
            result[source] = {
                "bottom": state["variants"]["type=bottom"]["model"],
                "top": state["variants"]["type=top"]["model"],
                "double": state["variants"]["type=double"]["model"],
            }
    return result


SLABS = {version: read_slabs(version) for version in VERSIONS}
assert len(SLABS["1.20.1"]) == 57
assert len(SLABS["1.21.1"]) == 60
assert len(SLABS["26.3"]) == 101

MODELS = {}
for version in VERSIONS:
    for source, models in SLABS[version].items():
        if source in MODELS and MODELS[source] != models:
            raise RuntimeError(f"model mapping changed for {source}")
        MODELS[source] = models

ALL_SLABS = sorted(MODELS)
assert len(ALL_SLABS) == 101

ZH_MATERIALS = {
    "acacia": "金合欢木",
    "andesite": "安山岩",
    "bamboo_mosaic": "竹马赛克",
    "bamboo": "竹",
    "birch": "白桦木",
    "blackstone": "黑石",
    "brick": "砖",
    "cherry": "樱花木",
    "cinnabar_brick": "辰砂砖",
    "cinnabar": "辰砂",
    "cobbled_deepslate": "深板岩圆石",
    "cobblestone": "圆石",
    "crimson": "绯红木",
    "cut_copper": "切制铜",
    "cut_red_sandstone": "切制红砂岩",
    "cut_sandstone": "切制砂岩",
    "dark_oak": "深色橡木",
    "dark_prismarine": "暗海晶石",
    "deepslate_brick": "深板岩砖",
    "deepslate_tile": "深板岩瓦",
    "diorite": "闪长岩",
    "end_stone_brick": "末地石砖",
    "exposed_cut_copper": "斑驳切制铜",
    "granite": "花岗岩",
    "jungle": "丛林木",
    "mangrove": "红树木",
    "mossy_cobblestone": "苔石",
    "mossy_stone_brick": "苔石砖",
    "mud_brick": "泥砖",
    "nether_brick": "下界砖",
    "oak": "橡木",
    "oxidized_cut_copper": "氧化切制铜",
    "pale_oak": "苍白橡木",
    "petrified_oak": "石化橡木",
    "polished_andesite": "磨制安山岩",
    "polished_blackstone_brick": "磨制黑石砖",
    "polished_blackstone": "磨制黑石",
    "polished_cinnabar": "磨制辰砂",
    "polished_deepslate": "磨制深板岩",
    "polished_diorite": "磨制闪长岩",
    "polished_granite": "磨制花岗岩",
    "polished_sulfur": "磨制硫磺",
    "polished_tuff": "磨制凝灰岩",
    "poplar": "杨木",
    "prismarine_brick": "海晶石砖",
    "prismarine": "海晶石",
    "purpur": "紫珀",
    "quartz": "石英",
    "red_nether_brick": "红色下界砖",
    "red_sandstone": "红砂岩",
    "resin_brick": "树脂砖",
    "sandstone": "砂岩",
    "smooth_quartz": "平滑石英",
    "smooth_red_sandstone": "平滑红砂岩",
    "smooth_sandstone": "平滑砂岩",
    "smooth_stone": "平滑石头",
    "spruce": "云杉木",
    "stone_brick": "石砖",
    "stone": "石头",
    "sulfur_brick": "硫磺砖",
    "sulfur": "硫磺",
    "tuff_brick": "凝灰岩砖",
    "tuff": "凝灰岩",
    "warped": "诡异木",
    "waxed_cut_copper": "涂蜡切制铜",
    "waxed_exposed_cut_copper": "涂蜡斑驳切制铜",
    "waxed_oxidized_cut_copper": "涂蜡氧化切制铜",
    "waxed_weathered_cut_copper": "涂蜡锈蚀切制铜",
    "weathered_cut_copper": "锈蚀切制铜",
}
COLORS = {
    "black": "黑色", "blue": "蓝色", "brown": "棕色", "cyan": "青色",
    "gray": "灰色", "green": "绿色", "light_blue": "淡蓝色",
    "light_gray": "淡灰色", "lime": "黄绿色", "magenta": "品红色",
    "orange": "橙色", "pink": "粉红色", "purple": "紫色", "red": "红色",
    "white": "白色", "yellow": "黄色",
}


def zh_name(source: str) -> str:
    stem = source.removesuffix("_slab")
    if stem.endswith("_concrete"):
        return f"竖直{COLORS[stem.removesuffix('_concrete')]}混凝土台阶"
    if stem.endswith("_wool"):
        return f"竖直{COLORS[stem.removesuffix('_wool')]}羊毛台阶"
    return f"竖直{ZH_MATERIALS.get(stem, stem)}台阶"


def write_json(path: Path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if RES.exists():
    shutil.rmtree(RES)
RES.mkdir(parents=True)

write_json(RES / "buck.mixins.json", {
    "required": True,
    "minVersion": "0.8",
    "package": "dev.buck.mixin",
    "mixins": [
        "BlockItemMixin",
        "ServerPlayerGameModeMixin",
    ],
    "client": [
        "MultiPlayerGameModeMixin",
    ],
    "injectors": {"defaultRequire": 1},
})

manifest_dir = ROOT / "slabs"
manifest_dir.mkdir(exist_ok=True)
for version in VERSIONS:
    (manifest_dir / f"{version}.txt").write_text(
        "\n".join(sorted(SLABS[version])) + "\n", encoding="utf-8"
    )
write_json(manifest_dir / "manifest.json", {
    "versions": {version: sorted(SLABS[version]) for version in VERSIONS},
    "models": MODELS,
})

with ZipFile(JARS["26.3"]) as jar:
    en_us = json.loads(jar.read("assets/minecraft/lang/en_us.json"))

en = {"block.buck.mixed_slab": "Mixed Slab"}
zh = {"block.buck.mixed_slab": "双拼台阶"}
for source in ALL_SLABS:
    vanilla_key = f"block.minecraft.{source}"
    english = en_us.get(vanilla_key, source.replace("_", " ").title())
    name = f"vertical_{source}"
    for kind in ("block", "item"):
        en[f"{kind}.buck.{name}"] = f"Vertical {english}"
        zh[f"{kind}.buck.{name}"] = zh_name(source)
write_json(RES / "assets/buck/lang/en_us.json", en)
write_json(RES / "assets/buck/lang/zh_cn.json", zh)

element = {
    "from": [0, 0, 0],
    "to": [8, 16, 16],
    "faces": {
        "down": {"texture": "#bottom", "cullface": "down"},
        "up": {"texture": "#top", "cullface": "up"},
        "north": {"texture": "#side", "cullface": "north"},
        "south": {"texture": "#side", "cullface": "south"},
        "west": {"texture": "#side", "cullface": "west"},
        "east": {"texture": "#side"},
    },
}
item_element = {
    "from": [4, 0, 0],
    "to": [12, 16, 16],
    "faces": {
        "down": {"texture": "#bottom"},
        "up": {"texture": "#top"},
        "north": {"texture": "#side"},
        "south": {"texture": "#side"},
        "west": {"texture": "#side"},
        "east": {"texture": "#side"},
    },
}
rotations = {"west": 0, "north": 90, "east": 180, "south": 270}

for source in ALL_SLABS:
    name = f"vertical_{source}"
    models = MODELS[source]
    write_json(RES / f"assets/buck/models/block/{name}.json", {
        "parent": models["bottom"],
        "elements": [element],
    })

    variants = {}
    for facing, rotation in rotations.items():
        single = {"model": f"buck:block/{name}"}
        if rotation:
            single.update({"y": rotation, "uvlock": True})
        variants[f"facing={facing},type=bottom"] = single
        variants[f"facing={facing},type=top"] = dict(single)
        variants[f"facing={facing},type=double"] = {"model": models["double"]}
    write_json(RES / f"assets/buck/blockstates/{name}.json", {"variants": variants})

    write_json(RES / f"assets/buck/models/item/{name}.json", {
        "parent": models["bottom"],
        "elements": [item_element],
    })
    write_json(RES / f"assets/buck/items/{name}.json", {
        "model": {"type": "minecraft:model", "model": f"buck:item/{name}"},
    })


write_json(RES / "assets/buck/blockstates/mixed_slab.json", {
    "variants": {"": {"model": "buck:block/mixed_slab"}}
})
write_json(RES / "assets/buck/models/block/mixed_slab.json", {
    "parent": "minecraft:block/stone"
})

def tag_graph(version: str, registry: str):
    folder = registry if version != "1.20.1" else registry + "s"
    prefix = f"data/minecraft/tags/{folder}/"
    graph = {}
    with ZipFile(JARS[version]) as jar:
        for name in jar.namelist():
            if not (name.startswith(prefix) and name.endswith(".json")):
                continue
            tag = "minecraft:" + name[len(prefix):-5]
            data = json.loads(jar.read(name))
            values = []
            for raw in data.get("values", []):
                value = raw.get("id") if isinstance(raw, dict) else raw
                if value:
                    values.append(value)
            graph[tag] = values
    return graph


def contains(graph, tag, value, memo, visiting):
    key = (tag, value)
    if key in memo:
        return memo[key]
    if tag in visiting:
        return False
    visiting.add(tag)
    result = False
    for entry in graph.get(tag, []):
        if entry.startswith("#"):
            ref = entry[1:]
            if ":" not in ref:
                ref = "minecraft:" + ref
            if contains(graph, ref, value, memo, visiting):
                result = True
                break
        else:
            full = entry if ":" in entry else "minecraft:" + entry
            if full == value:
                result = True
                break
    visiting.remove(tag)
    memo[key] = result
    return result


def memberships(version: str, registry: str):
    graph = tag_graph(version, registry)
    memo = {}
    out = {}
    for tag in graph:
        members = [
            source for source in sorted(SLABS[version])
            if contains(graph, tag, f"minecraft:{source}", memo, set())
        ]
        if members:
            out[tag] = members
    return out


for registry in ("block", "item"):
    old = memberships("1.20.1", registry)
    modern_a = memberships("1.21.1", registry)
    modern_b = memberships("26.3", registry)

    old_folder = registry + "s"
    for tag, members in old.items():
        write_json(
            RES / f"data/minecraft/tags/{old_folder}/{tag.split(':', 1)[1]}.json",
            {
                "replace": False,
                "values": [
                    {"id": f"buck:vertical_{source}", "required": False}
                    for source in members
                ],
            },
        )

    for tag in sorted(set(modern_a) | set(modern_b)):
        members = sorted(set(modern_a.get(tag, [])) | set(modern_b.get(tag, [])))
        write_json(
            RES / f"data/minecraft/tags/{registry}/{tag.split(':', 1)[1]}.json",
            {
                "replace": False,
                "values": [
                    {"id": f"buck:vertical_{source}", "required": False}
                    for source in members
                ],
            },
        )

print(f"assets: {len(ALL_SLABS)} slab sets")
print(f"files: {sum(1 for path in RES.rglob('*') if path.is_file())}")
for version in VERSIONS:
    print(f"{version}: {len(SLABS[version])}")
