#!/usr/bin/env python3
"""GT6 WorldgenStoneLayers overworld cubes, pebbles, and StoneLayerOres."""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import io_common as io
from tools.gt6_resolve import form_exists, resolve_material

GT6_REVISION = io.SOURCE_REVISION
SLUG = "worldgen/gt-stone-layer-rocks"
PLAN_STEM = "GT6石层石子详细计划.md"
STATUS = "GT_STONE_LAYER_ROCKS_RUNTIME_READY"
WAVE = census.TOOLS / "waves" / "worldgen" / "gt-stone-layer-rocks"
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
DATA = census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
GT6 = census.ROOT / "gt6_code" / "gregtech6"
LOADER = GT6 / "src" / "main" / "java" / "gregtech" / "loaders" / "b" / "Loader_Worldgen.java"
ROCK_ORES_JAVA = (
    GT6 / "src" / "main" / "java" / "gregtech" / "blocks" / "stone" / "BlockRockOres.java"
)
NOISE_SOURCE = GT6 / "src" / "main" / "java" / "gregtech" / "worldgen" / "NoiseGenerator.java"
NOISE_DEST = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "worldgen"
    / "StoneLayerNoise.java"
)
CATALOG = DATA / "worldgen_catalog" / "stone_layer_rocks.json"
ART_MANIFEST = ASSETS / "gt6_gt_stone_layer_rocks_art_manifest.json"
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_stone_layer_rocks"
)
EMPTY_NBT = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_surface_rocks"
    / "structure"
    / "empty.nbt"
)
GAME_TESTS = "GtStoneLayerRocksGameTests.java"
TEST_IDS = [
    "stoneLayerCatalogMatchesLoaderWorldgen",
    "stoneLayerPlaces32757WithLayerLoot",
    "stoneLayerReplacesVanillaStone",
    "stoneLayerPlacesLayerOres",
    "stoneLayerPlacesDenseRockOres",
    "stoneLayerPlacesNetherQuartz",
    "stoneLayerOneIn128AndNoDeep",
    "stoneLayerDoesNotDumpCatalog",
    "stoneLayerManifestResolvesLocalGt6",
]
NETHER_QUARTZ_JAVA = (
    GT6 / "src" / "main" / "java" / "gregtech" / "worldgen" / "nether"
    / "WorldgenNetherQuartz.java"
)
NETHER_QUARTZ_FEATURE_TYPE = "cruciblecraft:nether_netherquartz"
NETHER_QUARTZ_META = 8
NETHER_QUARTZ_ICON = "ore_netherquartz.png"
NETHER_QUARTZ_LABELS = ("Nether Quartz", "下界石英")
NETHER_QUARTZ_BASE_Y = 40
NETHER_QUARTZ_SPAN = 200
NETHER_QUARTZ_SAMPLE_YS = (0, 64)
NETHER_NOISE_OFFSET = -512
VANILLA_NETHER_QUARTZ = (
    "minecraft:ore_quartz_nether",
    "minecraft:ore_quartz_deltas",
)
PLACER = "cruciblecraft:gt_surface_rock"
FEATURE_TYPE = "cruciblecraft:stone_layer_rocks"
PROBABILITY = 128
NO_DEEP_Y = 24
DEEPSLATE = "deepslate"
LOADER_LAYER_COUNT = 123
ROCK_ORE_COUNT = 8
LAYER_COUNT = LOADER_LAYER_COUNT + ROCK_ORE_COUNT
UNIT = 648648000
CHANCE_TOKENS = {f"U{n}": UNIT // n for n in (
    2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 16, 24, 32, 48, 64, 96, 128
)}
CHANCE_TOKENS["U"] = UNIT
VANILLA_ORE_BLOCKS = {
    "Blocks.emerald_ore": "minecraft:emerald_ore",
    "Blocks.diamond_ore": "minecraft:diamond_ore",
    "Blocks.lapis_ore": "minecraft:lapis_ore",
    "Blocks.redstone_ore": "minecraft:redstone_ore",
    "Blocks.gold_ore": "minecraft:gold_ore",
    "Blocks.iron_ore": "minecraft:iron_ore",
    "Blocks.coal_ore": "minecraft:coal_ore",
    "Blocks.monster_egg": "minecraft:infested_stone",
}
BIOME_SETS = {
    "BIOMES_MOUNTAINS": ["#c:is_mountain"],
    "BIOMES_JUNGLE": ["#c:is_jungle"],
    "BIOMES_DESERT": ["#c:is_desert"],
    "BIOMES_PLAINS": ["#c:is_plains"],
    "BIOMES_FOREST": ["#c:is_forest"],
    "BIOMES_WOODS": ["#c:is_forest"],
    "BIOMES_TAIGA": ["#c:is_taiga"],
    "BIOMES_SWAMP": ["#c:is_swamp"],
    "BIOMES_SAVANNA": ["#c:is_savanna"],
    "BIOMES_OCEAN": ["#c:is_ocean"],
    "BIOMES_OCEAN_BEACH": ["#c:is_ocean", "#c:is_beach"],
    "BIOMES_RIVER_LAKE": ["#c:is_river"],
    "BIOMES_LAKE": ["#c:is_river"],
    "BIOMES_MESA": ["#c:is_badlands"],
    "BIOMES_FROZEN": ["#c:is_snowy"],
    "BIOMES_SHROOM": ["#c:is_mushroom"],
    "BIOMES_DARK_FOREST": ["minecraft:dark_forest"],
    "BIOMES_VOLCANIC": None,
    "BIOMES_MAGICAL": None,
    "BIOMES_MAGICAL_GOOD": None,
    "BIOMES_RADIOACTIVE": None,
}
BLOCKSGT_TO_CC = {
    "GraniteBlack": "granite_black",
    "GraniteRed": "granite_red",
    "Basalt": "basalt",
    "Marble": "marble",
    "Limestone": "limestone",
    "Granite": "granite",
    "Diorite": "diorite",
    "Andesite": "andesite",
    "Komatiite": "komatiite",
    "SchistGreen": "greenschist",
    "SchistBlue": "blueschist",
    "Kimberlite": "kimberlite",
    "Quartzite": "quartzite",
    "Slate": "slate",
    "Shale": "shale",
}
NATIVE_MATERIALS = tuple(sorted({*BLOCKSGT_TO_CC.values(), "stone", DEEPSLATE}))
# BlocksGT.stones harvest/hardness from Loader_Rocks. Vanilla stone/deepslate
# stay minecraft blocks; only GT BlockStones cubes are registered here.
NATIVE_STONES = (
    {
        "material": "granite_black",
        "gt_folder": "gt.stone.granite.black",
        "english": "Black Granite",
        "chinese": "黑花岗岩",
        "harvest": 3,
        "hardness_mul": 3.00,
        "resistance_mul": 6.00,
    },
    {
        "material": "granite_red",
        "gt_folder": "gt.stone.granite.red",
        "english": "Red Granite",
        "chinese": "红花岗岩",
        "harvest": 3,
        "hardness_mul": 3.00,
        "resistance_mul": 6.00,
    },
    {
        "material": "basalt",
        "gt_folder": "gt.stone.basalt",
        "english": "Basalt",
        "chinese": "玄武岩",
        "harvest": 2,
        "hardness_mul": 2.00,
        "resistance_mul": 3.00,
    },
    {
        "material": "marble",
        "gt_folder": "gt.stone.marble",
        "english": "Marble",
        "chinese": "大理石",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "limestone",
        "gt_folder": "gt.stone.limestone",
        "english": "Limestone",
        "chinese": "石灰岩",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "granite",
        "gt_folder": "gt.stone.granite",
        "english": "Granite",
        "chinese": "花岗岩",
        "harvest": 1,
        "hardness_mul": 1.00,
        "resistance_mul": 2.00,
    },
    {
        "material": "diorite",
        "gt_folder": "gt.stone.diorite",
        "english": "Diorite",
        "chinese": "闪长岩",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "andesite",
        "gt_folder": "gt.stone.andesite",
        "english": "Andesite",
        "chinese": "安山岩",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "komatiite",
        "gt_folder": "gt.stone.komatiite",
        "english": "Komatiite",
        "chinese": "科马提岩",
        "harvest": 2,
        "hardness_mul": 2.00,
        "resistance_mul": 3.00,
    },
    {
        "material": "greenschist",
        "gt_folder": "gt.stone.greenschist",
        "english": "Green Schist",
        "chinese": "绿片岩",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "blueschist",
        "gt_folder": "gt.stone.blueschist",
        "english": "Blue Schist",
        "chinese": "蓝片岩",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "kimberlite",
        "gt_folder": "gt.stone.kimberlite",
        "english": "Kimberlite",
        "chinese": "金伯利岩",
        "harvest": 2,
        "hardness_mul": 2.00,
        "resistance_mul": 3.00,
    },
    {
        "material": "quartzite",
        "gt_folder": "gt.stone.quartzite",
        "english": "Quartzite",
        "chinese": "石英岩",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "slate",
        "gt_folder": "gt.stone.slate",
        "english": "Slate",
        "chinese": "板岩",
        "harvest": 1,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
    {
        "material": "shale",
        "gt_folder": "gt.stone.shale",
        "english": "Shale",
        "chinese": "页岩",
        "harvest": 0,
        "hardness_mul": 0.50,
        "resistance_mul": 0.75,
    },
)
STONE_ROLES = (
    {
        "role": "stone",
        "gt_file": "stone.png",
        "english_suffix": "",
        "chinese_suffix": "",
        "english_prefix": "",
        "chinese_prefix": "",
    },
    {
        "role": "cobble",
        "gt_file": "cobble.png",
        "english_suffix": " Cobblestone",
        "chinese_suffix": "圆石",
        "english_prefix": "",
        "chinese_prefix": "",
    },
    {
        "role": "mossy_cobble",
        "gt_file": "cobble_mossy.png",
        "english_suffix": " Cobblestone",
        "chinese_suffix": "圆石",
        "english_prefix": "Mossy ",
        "chinese_prefix": "苔石",
    },
)
STONE_BLOCK_COUNT = len(NATIVE_STONES) * len(STONE_ROLES)
# Textures.BlockIcons.ROCK_ORES / BlockRockOres LH names, metas 0–7.
ROCK_ORE_ICONS = (
    "ore_anthracite.png",
    "ore_lignite.png",
    "ore_salt.png",
    "ore_rocksalt.png",
    "ore_bauxite.png",
    "ore_oil.png",
    "ore_gypsum.png",
    "ore_milkyquartz.png",
)
ROCK_ORE_LABELS = (
    ("Anthracite Coal", "无烟煤"),
    ("Lignite Coal", "褐煤"),
    ("Salt", "盐"),
    ("Sylvite", "钾盐"),
    ("Bauxite", "铝土矿"),
    ("Oil Shale", "油页岩"),
    ("Gypsum", "石膏"),
    ("Milky Quartz", "乳白石英"),
)
OVERWORLD_LARGE_VEINS = (
    "cruciblecraft:large_copper_vein",
    "cruciblecraft:large_gold_vein",
    "cruciblecraft:large_iron_vein",
    "cruciblecraft:large_tin_vein",
    "cruciblecraft:large_tungsten_vein",
)
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_STONES = Path("src/main/resources/assets/gregtech/textures/blocks/stones")
GT6_ICONS = Path("src/main/resources/assets/gregtech/textures/blocks/iconsets")
CC_STONES = "assets/cruciblecraft/textures/block/gt6/stones"
CC_ROCK_ORES = "assets/cruciblecraft/textures/block/gt6/rock_ores"


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _owned_paths(*, closed: bool) -> list[str]:
    lane = "closed" if closed else "active"
    return [
        f"docs/history/card-plans/{lane}/{PLAN_STEM}",
        f"src/test/java/com/masson/cruciblecraft/gametest/{GAME_TESTS}",
        "src/main/java/com/masson/cruciblecraft/content/block/GtSurfaceRockBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/GtSurfaceRockBlockEntity.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/StoneLayerNoise.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/StoneLayerCatalog.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/StoneLayerStones.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/StoneLayerRockFeature.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/NetherQuartzLayerFeature.java",
        "src/main/java/com/masson/cruciblecraft/content/block/StoneLayerStoneBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/block/StoneLayerRockOreBlock.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/StoneLayerRockConfiguration.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModFeatures.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModBlockEntities.java",
        "src/main/java/com/masson/cruciblecraft/compat/jade/CrucibleJadePlugin.java",
        "src/main/resources/data/cruciblecraft/worldgen_catalog/stone_layer_rocks.json",
        "src/main/resources/data/cruciblecraft/worldgen/configured_feature/stone_layer_rocks.json",
        "src/main/resources/data/cruciblecraft/worldgen/placed_feature/stone_layer_rocks.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_stone_layer_rocks.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/remove_overworld_large_veins.json",
        "src/main/resources/data/cruciblecraft/worldgen/configured_feature/nether_netherquartz.json",
        "src/main/resources/data/cruciblecraft/worldgen/placed_feature/nether_netherquartz.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_nether_netherquartz.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/remove_vanilla_nether_quartz.json",
        "src/main/resources/data/minecraft/tags/block/needs_iron_tool.json",
        "src/main/resources/data/cruciblecraft_wave_worldgen_gt_stone_layer_rocks/**",
        "src/main/resources/assets/cruciblecraft/gt6_gt_stone_layer_rocks_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/stones/**/stone.png",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/stones/**/cobble.png",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/stones/**/cobble_mossy.png",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/rock_ores/**",
        f"tools/capabilities/{SLUG}/**",
        "tools/waves/worldgen/gt-stone-layer-rocks/**",
        "tools/gt_stone_layer_rocks.py",
        "tools/build_gt_stone_layer_rocks.py",
        "tools/tests/test_gt_stone_layer_rocks.py",
        "src/test/java/com/masson/cruciblecraft/worldgen/StoneLayerRockFeatureConfigTest.java",
        "docs/current/player-guide.md",
    ]


def _plan_body(*, closed: bool) -> str:
    if closed:
        lane = (
            "lane                         = closed\n"
            f"capability_slug              = {SLUG}\n"
            "unique_active_wave           = null\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = accepted\n"
            "depends_on                   =\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：已关闭 `runtime_ready` / `workflow=accepted`。\n"
            "> 本文件位于 `card-plans/closed/`。"
        )
        gate = "[x]"
    else:
        lane = (
            "lane                         = unique-active\n"
            f"capability_slug              = {SLUG}\n"
            f"unique_active_wave           = {SLUG}\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = active\n"
            "depends_on                   =\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：unique-active，目标 `runtime_ready`。\n"
            "> 本文件位于 `card-plans/active/`。"
        )
        gate = "[ ]"
    return (
        f"# GT6 石层石子\n\n"
        f"> 计划 slug：`{SLUG}`\n"
        f"{status}\n"
        "> 正式名称：GT6 石层石子\n"
        "> 性质：把 GT6 `WorldgenStoneLayers` 接到主世界：按噪声把原版石头/\n"
        "> 圆石/深板岩换成层立方体（黑色花岗岩等），并在不透明石面 1/128 放\n"
        "> 32757（`tLastRock`）。同一扫描写无模组 `StoneLayerOres`（层内 /\n"
        "> 交界 / 1/100 随机小宝石），并吃原版矿格。`BlockRockOres` 8 层致密\n"
        "> 立方体计入 LAYERS 权重；meta 8 下界石英不进主世界 LAYERS，由\n"
        "> `WorldgenNetherQuartz` 在下界岩里铺。`GENERATE_STONE` 时关掉主世界\n"
        "> 大矿脉；GT6 `PREVENTED_ORES QUARTZ` 关掉原版下界石英矿。\n"
        "> 不撒 catalog `ItemEntity`，不重开行星岩 prep。\n"
        ">\n"
        "> Java/tick 源：`gt6_code/gregtech6` @ "
        f"`{GT6_REVISION}`。\n"
        "> 贴图：层石从 `gregtech6_w` `blocks/stones/<folder>/` 拷进\n"
        "> `block/gt6/stones/`；致密矿从 `iconsets/ore_*.png` 拷进\n"
        "> `block/gt6/rock_ores/`。\n\n"
        "```text\n"
        f"{lane}\n"
        "```\n\n"
        "关闭目标不是 `player_complete`。月/火/行星岩仍在 prep。\n\n"
        "## 门禁\n\n"
        f"- {gate} 无模组 GT6 `StoneLayer.LAYERS` 加权 {LAYER_COUNT} 条"
        f"（Loader {LOADER_LAYER_COUNT} + BlockRockOres {ROCK_ORE_COUNT}）\n"
        f"- {gate} 列扫描替换石头/圆石/深板岩为层立方体（{STONE_BLOCK_COUNT} 个 GT 方块）\n"
        f"- {gate} BlockRockOres 8 个致密立方体（煤/褐煤/盐/钾盐/铝土/油页岩/石膏/乳白石英）\n"
        f"- {gate} `WorldgenNetherQuartz` 在下界岩写 meta 8 下界石英；不进主世界 LAYERS\n"
        f"- {gate} 关掉原版下界石英矿（GT6 `PREVENTED_ORES` QUARTZ）\n"
        f"- {gate} 列扫描 1/{PROBABILITY} 在石/基岩/圆石面空气放置 `gt_surface_rock`\n"
        f"- {gate} 掉落 `tLastRock` 的 `rock`（granite_black 等），不是整表 `c:rocks`\n"
        f"- {gate} 7 格扫描写无模组 `StoneLayerOres`（层内 / 交界 MAP / 1/100 宝石）\n"
        f"- {gate} 原版矿格可被层石或层矿替换（GT6 REPLACEABLE_BLOCKS）\n"
        f"- {gate} `setNoDeep` 在 `minBuildHeight+{NO_DEEP_Y}` 以下用 deepslate\n"
        f"- {gate} 主世界大矿脉在石层开启时移除（GT6 GENERATE_STONE）\n"
        f"- {gate} 隔离 GameTest `-PwaveRecipes={SLUG}`\n"
        "- [x] 不以 catalog `ItemEntity` 当获得；不写 moon/mars/planet.rocks\n"
    )


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "depends_on": [],
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "close_target": "runtime_ready",
        "probability": PROBABILITY,
        "placer": PLACER,
        "mte": 32757,
        "layer_count": LAYER_COUNT,
        "loader_layer_count": LOADER_LAYER_COUNT,
        "rock_ore_count": ROCK_ORE_COUNT,
        "nether_rock_ore_count": 1,
        "stone_block_count": STONE_BLOCK_COUNT,
        "no_deep_y": NO_DEEP_Y,
        "no_deep_offset": NO_DEEP_Y,
    }


def art_imports() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for stone in NATIVE_STONES:
        for role in STONE_ROLES:
            rows.append(
                {
                    "source": "gregtech6_w",
                    "gt6_source": (
                        GT6_STONES / stone["gt_folder"] / role["gt_file"]
                    ).as_posix(),
                    "destination": (
                        f"{CC_STONES}/{stone['gt_folder']}/{role['gt_file']}"
                    ),
                }
            )
    for icon in ROCK_ORE_ICONS:
        rows.append(
            {
                "source": "gregtech6_w",
                "gt6_source": (GT6_ICONS / icon).as_posix(),
                "destination": f"{CC_ROCK_ORES}/{icon}",
            }
        )
    rows.append(
        {
            "source": "gregtech6_w",
            "gt6_source": (GT6_ICONS / NETHER_QUARTZ_ICON).as_posix(),
            "destination": f"{CC_ROCK_ORES}/{NETHER_QUARTZ_ICON}",
        }
    )
    return rows


def copy_art() -> None:
    if not GT6_W.is_dir():
        raise FileNotFoundError(census.relative(GT6_W))
    for row in art_imports():
        source = GT6_W / row["gt6_source"]
        dest = (
            census.ROOT
            / "src"
            / "main"
            / "resources"
            / row["destination"]
        )
        if not source.is_file():
            raise FileNotFoundError(row["gt6_source"])
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, dest)


def stone_blocks() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for stone in NATIVE_STONES:
        hardness = round(stone["hardness_mul"] * 1.5, 4)
        resistance = round(stone["resistance_mul"] * 10.0, 4)
        for role in STONE_ROLES:
            rows.append(
                {
                    "material": stone["material"],
                    "role": role["role"],
                    "registry_path": f"{stone['material']}/{role['role']}",
                    "texture": (
                        "cruciblecraft:block/gt6/stones/"
                        f"{stone['gt_folder']}/{role['gt_file'][:-4]}"
                    ),
                    "gt_folder": stone["gt_folder"],
                    "gt_file": role["gt_file"],
                    "harvest_level": stone["harvest"],
                    "hardness": hardness,
                    "resistance": resistance,
                    "english": (
                        f"{role['english_prefix']}{stone['english']}"
                        f"{role['english_suffix']}"
                    ),
                    "chinese": (
                        f"{role['chinese_prefix']}{stone['chinese']}"
                        f"{role['chinese_suffix']}"
                    ),
                }
            )
    if len(rows) != STONE_BLOCK_COUNT:
        raise ValueError(
            f"expected {STONE_BLOCK_COUNT} stone cubes, got {len(rows)}"
        )
    return rows


def _parse_java_array(source: str, name: str) -> list[str]:
    match = re.search(
        rf"{re.escape(name)}\s*=\s*\{{([^}}]+)\}}",
        source,
    )
    if not match:
        raise ValueError(f"missing {name}")
    return [
        part.strip().rstrip("Ff")
        for part in match.group(1).split(",")
        if part.strip()
    ]


def extract_rock_ores(text: str | None = None) -> list[dict[str, Any]]:
    source = text if text is not None else ROCK_ORES_JAVA.read_text(encoding="utf-8")
    harvests = [int(value) for value in _parse_java_array(source, "HARVEST_LEVELS")]
    burns = [int(value) for value in _parse_java_array(source, "BURN_LEVELS")]
    hardness = [float(value) for value in _parse_java_array(source, "HARDNESS_LEVELS")]
    materials = _parse_java_array(source, "ORE_MATERIALS")
    rows: list[dict[str, Any]] = []
    needle = "StoneLayer.LAYERS.add("
    cursor = 0
    while True:
        start = source.find(needle, cursor)
        if start < 0:
            break
        if _commented_at(source, start):
            cursor = start + len(needle)
            continue
        open_idx = start + len(needle) - 1
        end = _matching_paren(source, open_idx)
        body = source[open_idx + 1 : end]
        ctor = body.find("new StoneLayer(")
        if ctor < 0:
            raise ValueError("BlockRockOres LAYERS.add without StoneLayer")
        ctor_open = body.find("(", ctor)
        args = _top_level_args(
            body[ctor_open + 1 : _matching_paren(body, ctor_open)]
        )
        if len(args) < 3:
            raise ValueError(f"short BlockRockOres StoneLayer: {body[:80]}")
        meta = int(args[1].strip())
        if meta < 0 or meta >= len(ROCK_ORE_ICONS):
            raise ValueError(f"BlockRockOres meta {meta} out of overworld range")
        mapped = _mt_to_cc(materials[meta])
        if not mapped:
            raise ValueError(f"unmapped BlockRockOres material {materials[meta]}")
        if not form_exists(mapped, "raw_ore"):
            raise ValueError(f"BlockRockOres {mapped} is missing raw_ore")
        english, chinese = ROCK_ORE_LABELS[meta]
        hardness_mul = hardness[meta]
        rows.append(
            {
                "material": mapped,
                "layer_material": mapped,
                "no_deep": ".setNoDeep()" in body.replace(" ", ""),
                "ores": [],
                "dense_ore": True,
                "meta": meta,
                "icon": ROCK_ORE_ICONS[meta],
                "english": english,
                "chinese": chinese,
                "harvest": harvests[meta],
                "hardness_mul": hardness_mul,
                "flammability": burns[meta],
            }
        )
        cursor = end
    if len(rows) != ROCK_ORE_COUNT:
        raise ValueError(
            f"expected {ROCK_ORE_COUNT} BlockRockOres overworld layers, got {len(rows)}"
        )
    return rows


def extract_nether_quartz_ore(text: str | None = None) -> dict[str, Any]:
    source = text if text is not None else ROCK_ORES_JAVA.read_text(encoding="utf-8")
    harvests = [int(value) for value in _parse_java_array(source, "HARVEST_LEVELS")]
    burns = [int(value) for value in _parse_java_array(source, "BURN_LEVELS")]
    hardness = [float(value) for value in _parse_java_array(source, "HARDNESS_LEVELS")]
    materials = _parse_java_array(source, "ORE_MATERIALS")
    if "Nope, Nether Quartz is not for the Overworld" not in source:
        raise ValueError(
            "BlockRockOres must keep the commented-out Nether Quartz LAYERS.add"
        )
    token = materials[NETHER_QUARTZ_META]
    mapped = _mt_to_cc(token)
    if mapped != "nether_quartz":
        raise ValueError(f"BlockRockOres meta 8 must be NetherQuartz, got {token}")
    if not form_exists(mapped, "raw_ore"):
        raise ValueError("BlockRockOres nether_quartz is missing raw_ore")
    english, chinese = NETHER_QUARTZ_LABELS
    return {
        "material": mapped,
        "layer_material": mapped,
        "no_deep": True,
        "ores": [],
        "dense_ore": True,
        "meta": NETHER_QUARTZ_META,
        "icon": NETHER_QUARTZ_ICON,
        "english": english,
        "chinese": chinese,
        "harvest": harvests[NETHER_QUARTZ_META],
        "hardness_mul": hardness[NETHER_QUARTZ_META],
        "flammability": burns[NETHER_QUARTZ_META],
    }


def _dense_cube(ore: dict[str, Any]) -> dict[str, Any]:
    hardness = round(ore["hardness_mul"] * 1.5, 4)
    return {
        "material": ore["material"],
        "role": "stone",
        "dense_ore": True,
        "registry_path": f"{ore['material']}/dense_ore",
        "texture": (
            "cruciblecraft:block/gt6/rock_ores/"
            f"{ore['icon'][:-4]}"
        ),
        "gt_file": ore["icon"],
        "harvest_level": ore["harvest"],
        "hardness": hardness,
        "resistance": 6.0,
        "flammability": ore["flammability"],
        "english": ore["english"],
        "chinese": ore["chinese"],
    }


def rock_ore_blocks() -> list[dict[str, Any]]:
    rows = [_dense_cube(ore) for ore in extract_rock_ores()]
    rows.append(_dense_cube(extract_nether_quartz_ore()))
    if len(rows) != ROCK_ORE_COUNT + 1:
        raise ValueError(
            f"expected {ROCK_ORE_COUNT + 1} dense cubes, got {len(rows)}"
        )
    return rows


def _matching_paren(text: str, open_idx: int) -> int:
    depth = 0
    for index in range(open_idx, len(text)):
        char = text[index]
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return index
    raise ValueError("unbalanced StoneLayer.LAYERS.add")


def _top_level_args(text: str) -> list[str]:
    args: list[str] = []
    depth = 0
    start = 0
    for index, char in enumerate(text):
        if char in "({[":
            depth += 1
        elif char in ")}]":
            depth -= 1
        elif char == "," and depth == 0:
            args.append(text[start:index].strip())
            start = index + 1
    tail = text[start:].strip()
    if tail:
        args.append(tail)
    return args


def _commented_at(source: str, index: int) -> bool:
    line_start = source.rfind("\n", 0, index) + 1
    return source[line_start:index].lstrip().startswith("//")


def _mt_to_cc(token: str) -> str | None:
    name = token[3:] if token.startswith("MT.") else token
    for prefix in ("OREMATS.", "STONES.", "UNUSED."):
        if name.startswith(prefix):
            name = name[len(prefix):]
            break
    row = resolve_material(name)
    if row.get("status") != "ok":
        return None
    return row.get("cc_material")


def _include_no_mod(arg: str) -> str | None:
    arg = re.sub(r"\s+", " ", arg.strip())
    if arg in {"null", ""}:
        return None
    if "mLoaded ?" in arg:
        if re.search(r"mLoaded \? null :", arg):
            return arg.split(":", 1)[1].strip()
        return None
    if "mHidden ?" in arg:
        if arg.startswith("!"):
            return arg.split(":", 1)[0].split("?", 1)[1].strip()
        return None
    return arg


def _parse_ore(expr: str) -> dict[str, Any]:
    expr = expr.strip()
    if not expr.startswith("new StoneLayerOres("):
        raise ValueError(f"not a StoneLayerOres: {expr[:80]}")
    open_idx = expr.find("(")
    args = _top_level_args(expr[open_idx + 1 : _matching_paren(expr, open_idx)])
    if len(args) < 4:
        raise ValueError(f"short StoneLayerOres: {expr[:80]}")
    material_tok = args[0].strip()
    index = 1
    indicators = True
    if args[index] in {"T", "F"}:
        indicators = args[index] == "T"
        index += 1
    chance_tok = args[index].replace(" ", "")
    min_y = int(args[index + 1])
    max_y = int(args[index + 2])
    rest = args[index + 3 :]
    vanilla = None
    biomes: list[str] = []
    never = False
    for item in rest:
        item = item.strip()
        if item in VANILLA_ORE_BLOCKS:
            vanilla = VANILLA_ORE_BLOCKS[item]
        elif item.startswith("Blocks."):
            raise ValueError(f"unmapped vanilla block {item}")
        elif (
            item.startswith("ST.block")
            or item.startswith("MD.")
            or item.startswith("IL.")
            or item.isdigit()
            or re.fullmatch(r"\d+\s*\+\s*\d+", item)
        ):
            continue
        elif item.startswith("BIOMES_"):
            mapped = BIOME_SETS[item]
            if mapped is None:
                never = True
            else:
                biomes.extend(mapped)
        else:
            raise ValueError(f"unmapped ore arg {item!r}")
    chance = CHANCE_TOKENS.get(chance_tok)
    if chance is None:
        raise ValueError(f"unmapped chance {chance_tok}")
    return {
        "gt": material_tok,
        "material": _mt_to_cc(material_tok),
        "indicators": indicators,
        "chance": chance,
        "min_y": min_y,
        "max_y": max_y,
        "vanilla_block": vanilla,
        "biomes": biomes,
        "never_biome": never,
    }


def _slice_ore_expr(expr: str) -> str:
    start = expr.find("new StoneLayerOres(")
    return expr[start : _matching_paren(expr, start + len("new StoneLayerOres")) + 1]


def _parse_ctor(body: str) -> dict[str, Any]:
    no_deep = ".setNoDeep()" in body.replace(" ", "")
    ctor = body.find("new StoneLayer(")
    if ctor < 0:
        raise ValueError("LAYERS.add without StoneLayer constructor")
    open_idx = body.find("(", ctor)
    args = _top_level_args(body[open_idx + 1 : _matching_paren(body, open_idx)])
    first = args[0].strip()
    cube = "stone"
    layer = "stone"
    gt_block = None
    if first.startswith("BlocksGT."):
        gt_block = first.split(".", 1)[1]
        cube = BLOCKSGT_TO_CC.get(gt_block)
        if cube is None:
            raise ValueError(f"unmapped BlocksGT.{gt_block}")
        layer = cube
    elif first != "null":
        raise ValueError(f"unmapped StoneLayer first arg: {first[:48]!r}")
    ore_start = 1
    if len(args) > 1 and args[1].strip().startswith("MT."):
        mapped = _mt_to_cc(args[1].strip())
        if mapped:
            layer = mapped
        ore_start = 2
        while ore_start < len(args) and "StoneLayerOres" not in args[ore_start]:
            ore_start += 1
    ores: list[dict[str, Any]] = []
    for arg in args[ore_start:]:
        expr = _include_no_mod(arg)
        if expr is None or "new StoneLayerOres(" not in expr:
            continue
        ores.append(_parse_ore(_slice_ore_expr(expr)))
    return {
        "material": cube,
        "layer_material": layer,
        "no_deep": no_deep,
        "gt_block": gt_block,
        "ores": ores,
    }


def extract_layers(text: str | None = None) -> list[dict[str, Any]]:
    source = text if text is not None else LOADER.read_text(encoding="utf-8")
    needle = "StoneLayer.LAYERS.add("
    rows: list[dict[str, Any]] = []
    cursor = 0
    while True:
        start = source.find(needle, cursor)
        if start < 0:
            break
        if _commented_at(source, start):
            cursor = start + len(needle)
            continue
        open_idx = start + len(needle) - 1
        end = _matching_paren(source, open_idx)
        rows.append(_parse_ctor(source[open_idx + 1 : end]))
        cursor = end
    return rows


def extract_deepslate(text: str | None = None) -> dict[str, Any]:
    source = text if text is not None else LOADER.read_text(encoding="utf-8")
    needle = "StoneLayer.DEEPSLATE = new StoneLayer("
    start = source.find(needle)
    if start < 0:
        raise ValueError("missing StoneLayer.DEEPSLATE")
    end = _matching_paren(source, start + len(needle) - 1)
    parsed = _parse_ctor(source[start + len("StoneLayer.DEEPSLATE = ") : end + 1])
    parsed["material"] = DEEPSLATE
    parsed["layer_material"] = parsed.get("layer_material") or DEEPSLATE
    return parsed


def extract_boundaries(text: str | None = None) -> list[dict[str, Any]]:
    source = text if text is not None else LOADER.read_text(encoding="utf-8")
    rows: list[dict[str, Any]] = []
    for kind in ("bothsides", "topbottom"):
        needle = f"StoneLayer.{kind}("
        cursor = 0
        while True:
            start = source.find(needle, cursor)
            if start < 0:
                break
            if _commented_at(source, start):
                cursor = start + len(needle)
                continue
            end = _matching_paren(source, start + len(needle) - 1)
            args = _top_level_args(source[start + len(needle) : end])
            ores: list[dict[str, Any]] = []
            for arg in args[2:]:
                expr = _include_no_mod(arg)
                if expr is None or "new StoneLayerOres(" not in expr:
                    continue
                ores.append(_parse_ore(_slice_ore_expr(expr)))
            rows.append(
                {
                    "kind": kind,
                    "top": _mt_to_cc(args[0].strip()),
                    "bottom": _mt_to_cc(args[1].strip()),
                    "ores": ores,
                }
            )
            cursor = end
    return rows


def extract_random_small_gems() -> list[str]:
    mt_java = GT6 / "src" / "main" / "java" / "gregapi" / "data" / "MT.java"
    text = mt_java.read_text(encoding="utf-8")
    names: list[str] = []
    for match in re.finditer(
            r"^\s+([A-Za-z][A-Za-z0-9_]*)\s*=.*RANDOM_SMALL_GEM_ORE",
            text,
            re.MULTILINE,
    ):
        mapped = _mt_to_cc(match.group(1))
        if mapped and form_exists(mapped, "ore"):
            names.append(mapped)
    if "emerald" not in names and form_exists("emerald", "ore"):
        names.insert(0, "emerald")
    seen: set[str] = set()
    ordered: list[str] = []
    for name in names:
        if name in seen:
            continue
        seen.add(name)
        ordered.append(name)
    return ordered


def _layer_json(row: dict[str, Any]) -> dict[str, Any]:
    document: dict[str, Any] = {
        "material": row["material"],
        "layer_material": row["layer_material"],
        "no_deep": row["no_deep"],
        "ores": [_ore_json(ore) for ore in row["ores"]],
    }
    if row.get("dense_ore"):
        document["dense_ore"] = True
    return document


def _ore_json(ore: dict[str, Any]) -> dict[str, Any]:
    row: dict[str, Any] = {
        "material": ore["material"],
        "chance": ore["chance"],
        "min_y": ore["min_y"],
        "max_y": ore["max_y"],
    }
    if not ore["indicators"]:
        row["indicators"] = False
    if ore.get("vanilla_block"):
        row["vanilla_block"] = ore["vanilla_block"]
    if ore.get("biomes"):
        row["biomes"] = ore["biomes"]
    if ore.get("never_biome"):
        row["never_biome"] = True
    return row


def layer_catalog() -> dict[str, Any]:
    rock_ores = extract_rock_ores()
    loader_layers = extract_layers()
    if len(loader_layers) != LOADER_LAYER_COUNT:
        raise ValueError(
            f"expected {LOADER_LAYER_COUNT} Loader_Worldgen LAYERS, "
            f"got {len(loader_layers)}"
        )
    layers = rock_ores + loader_layers
    if len(layers) != LAYER_COUNT:
        raise ValueError(f"expected {LAYER_COUNT} LAYERS, got {len(layers)}")
    deepslate = extract_deepslate()
    boundaries = extract_boundaries()
    gems = extract_random_small_gems()
    materials = {row["material"] for row in loader_layers}
    materials.add(DEEPSLATE)
    missing = [name for name in NATIVE_MATERIALS if name not in materials]
    if missing:
        raise ValueError(f"native materials missing from LAYERS: {missing}")
    blocks = stone_blocks()
    dense = rock_ore_blocks()
    return {
        "schema_version": 1,
        "id": "stone_layer_rocks",
        "feature_type": FEATURE_TYPE,
        "config": {"probability": PROBABILITY},
        "placed_feature": "cruciblecraft:stone_layer_rocks",
        "placer": PLACER,
        "mte": 32757,
        "probability": PROBABILITY,
        "no_deep_y": NO_DEEP_Y,
        "no_deep_offset": NO_DEEP_Y,
        "deepslate_material": DEEPSLATE,
        "layer_count": len(layers),
        "loader_layer_count": len(loader_layers),
        "rock_ore_count": len(rock_ores),
        "stone_block_count": len(blocks),
        "unit": UNIT,
        "layers": [_layer_json(row) for row in layers],
        "deepslate_layer": {
            "material": DEEPSLATE,
            "layer_material": deepslate["layer_material"],
            "ores": [_ore_json(ore) for ore in deepslate["ores"]],
        },
        "boundaries": [
            {
                "kind": row["kind"],
                "top": row["top"],
                "bottom": row["bottom"],
                "ores": [_ore_json(ore) for ore in row["ores"]],
            }
            for row in boundaries
        ],
        "random_small_gems": gems,
        "stone_blocks": blocks,
        "rock_ores": dense,
        "nether_rock_ore_count": 1,
        "nether_quartz": {
            "feature_type": NETHER_QUARTZ_FEATURE_TYPE,
            "placed_feature": "cruciblecraft:nether_netherquartz",
            "material": "nether_quartz",
            "meta": NETHER_QUARTZ_META,
            "base_y": NETHER_QUARTZ_BASE_Y,
            "span": NETHER_QUARTZ_SPAN,
            "sample_ys": list(NETHER_QUARTZ_SAMPLE_YS),
            "noise_offset": NETHER_NOISE_OFFSET,
            "host": "minecraft:netherrack",
            "vanilla_remove": list(VANILLA_NETHER_QUARTZ),
        },
        "biome_modifier": {
            "id": "add_stone_layer_rocks",
            "type": "neoforge:add_features",
            "biomes": "#minecraft:is_overworld",
            "features": ["cruciblecraft:stone_layer_rocks"],
            "step": "underground_decoration",
        },
        "remove_overworld_large_veins": {
            "id": "remove_overworld_large_veins",
            "type": "neoforge:remove_features",
            "biomes": "#minecraft:is_overworld",
            "features": list(OVERWORLD_LARGE_VEINS),
            "steps": ["underground_ores"],
        },
        "provenance": (
            "GT6 WorldgenStoneLayers overworld cube replace, 32757 tLastRock "
            "pebbles, BlockRockOres dense layers, WorldgenNetherQuartz, and "
            "no-mod StoneLayerOres / double-layer MAP"
        ),
        "design_policy": "DESIGN_POLICY",
    }


def _copy_empty_nbt() -> None:
    if not EMPTY_NBT.is_file():
        raise FileNotFoundError(census.relative(EMPTY_NBT))
    for relative in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = PACK / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(EMPTY_NBT, dest)


def write_noise() -> None:
    if not NOISE_SOURCE.is_file():
        raise FileNotFoundError(census.relative(NOISE_SOURCE))
    text = NOISE_SOURCE.read_text(encoding="utf-8")
    text = text.replace(
        "package gregtech.worldgen;",
        "package com.masson.cruciblecraft.worldgen;",
    )
    text = text.replace("import net.minecraft.world.World;\n\n", "")
    text = text.replace("public class NoiseGenerator", "public final class StoneLayerNoise")
    text = text.replace(
        "\tpublic NoiseGenerator(long aSeed) {\n"
        "\t\tmSeed = (int)aSeed;\n"
        "\t}\n"
        "\tpublic NoiseGenerator(World aWorld) {\n"
        "\t\tmOffsetY = 512 * aWorld.provider.dimensionId;\n"
        "\t\tmSeed = (int)aWorld.getSeed();\n"
        "\t}\n"
        "\tpublic NoiseGenerator setFrequency(float aFrequency) {",
        "\tpublic StoneLayerNoise(long aSeed) {\n"
        "\t\tthis((int) aSeed, 0);\n"
        "\t}\n"
        "\tpublic StoneLayerNoise(int aSeed, int aOffsetY) {\n"
        "\t\tmSeed = aSeed;\n"
        "\t\tmOffsetY = aOffsetY;\n"
        "\t}\n"
        "\tpublic StoneLayerNoise setFrequency(float aFrequency) {",
    )
    text = text.replace(
        "\tpublic NoiseGenerator setFrequency(float aX, float aY, float aZ) {",
        "\tpublic StoneLayerNoise setFrequency(float aX, float aY, float aZ) {",
    )
    if "class NoiseGenerator" in text or "World aWorld" in text:
        raise ValueError("StoneLayerNoise rewrite left GT6 World constructor")
    NOISE_DEST.parent.mkdir(parents=True, exist_ok=True)
    NOISE_DEST.write_text(text, encoding="utf-8")


def write_worldgen() -> dict[str, Any]:
    declaration = layer_catalog()
    _write_json(CATALOG, declaration)
    _write_json(
        DATA / "worldgen" / "configured_feature" / "stone_layer_rocks.json",
        {
            "type": FEATURE_TYPE,
            "config": declaration["config"],
        },
    )
    _write_json(
        DATA / "worldgen" / "placed_feature" / "stone_layer_rocks.json",
        {"feature": "cruciblecraft:stone_layer_rocks", "placement": []},
    )
    _write_json(
        DATA / "neoforge" / "biome_modifier" / "add_stone_layer_rocks.json",
        {
            "type": "neoforge:add_features",
            "biomes": "#minecraft:is_overworld",
            "features": ["cruciblecraft:stone_layer_rocks"],
            "step": "underground_decoration",
        },
    )
    _write_json(
        DATA / "neoforge" / "biome_modifier" / "remove_overworld_large_veins.json",
        {
            "type": "neoforge:remove_features",
            "biomes": "#minecraft:is_overworld",
            "features": list(OVERWORLD_LARGE_VEINS),
            "steps": ["underground_ores"],
        },
    )
    _write_json(
        DATA / "worldgen" / "configured_feature" / "nether_netherquartz.json",
        {
            "type": NETHER_QUARTZ_FEATURE_TYPE,
            "config": {},
        },
    )
    _write_json(
        DATA / "worldgen" / "placed_feature" / "nether_netherquartz.json",
        {"feature": "cruciblecraft:nether_netherquartz", "placement": []},
    )
    _write_json(
        DATA / "neoforge" / "biome_modifier" / "add_nether_netherquartz.json",
        {
            "type": "neoforge:add_features",
            "biomes": "#minecraft:is_nether",
            "features": ["cruciblecraft:nether_netherquartz"],
            "step": "underground_decoration",
        },
    )
    _write_json(
        DATA / "neoforge" / "biome_modifier" / "remove_vanilla_nether_quartz.json",
        {
            "type": "neoforge:remove_features",
            "biomes": "#minecraft:is_nether",
            "features": list(VANILLA_NETHER_QUARTZ),
            "steps": ["underground_ores", "underground_decoration"],
        },
    )
    return declaration


def capability_document(*, closed: bool) -> dict[str, Any]:
    return {
        "schema_version": 2,
        "slug": SLUG,
        "title": "GT Stone Layer Rocks",
        "maturity": "runtime_ready",
        "workflow": "accepted" if closed else "active",
        "survival_access": "partial",
        "owned_paths": _owned_paths(closed=closed),
        "depends_on": [],
        "profiles": ["capability-runtime"],
        "wave_slug": SLUG,
        "required_test_ids": list(TEST_IDS),
        "identity_disposition": [
            {
                "semantic_key": "worldgen:overworld.stonelayers.rocks",
                "disposition": "new_distinct",
                "runtime_ids": [
                    PLACER,
                    FEATURE_TYPE,
                    "cruciblecraft:granite_black/stone",
                    "cruciblecraft:coal/dense_ore",
                    "cruciblecraft:nether_quartz/dense_ore",
                    NETHER_QUARTZ_FEATURE_TYPE,
                ],
                "reason": (
                    "GT6 WorldgenStoneLayers cubes, 32757 tLastRock pebbles, "
                    "BlockRockOres dense layers, WorldgenNetherQuartz, and "
                    "no-mod StoneLayerOres. Not WorldgenRocks grass scatter, "
                    "not catalog dump, not moon/mars/planet rocks."
                ),
            }
        ],
        "note": (
            "Overworld WorldgenStoneLayers cubes, pebbles, BlockRockOres, "
            "WorldgenNetherQuartz, and StoneLayerOres. Close at runtime_ready. "
            "Do not scatter catalog ItemEntity. Do not land "
            "worldgen/gt-planet-rocks. Per-stone oreSmall hosts stay out. "
            "Bedrock veins stay out."
        ),
    }


def issue_active() -> dict[str, Any]:
    active_dir = census.ROOT / "docs" / "history" / "card-plans" / "active"
    active_dir.mkdir(parents=True, exist_ok=True)
    for path in sorted(active_dir.glob("*.md")):
        if path.name != PLAN_STEM:
            raise ValueError(f"unique-active already occupied by {path.name}")
    cap_path = census.TOOLS / "capabilities" / SLUG / "capability.json"
    if cap_path.is_file():
        existing = census.load_json(cap_path)
        if existing.get("workflow") == "accepted":
            raise ValueError(f"{SLUG} already closed")
    cap_path.parent.mkdir(parents=True, exist_ok=True)
    _write_json(cap_path, capability_document(closed=False))
    plan = active_dir / PLAN_STEM
    plan.write_text(_plan_body(closed=False), encoding="utf-8")
    _copy_empty_nbt()
    return {"capability": census.relative(cap_path), "plan": census.relative(plan)}


def prepare_close() -> dict[str, Any]:
    cap_path = census.TOOLS / "capabilities" / SLUG / "capability.json"
    document = census.load_json(cap_path)
    document["owned_paths"] = _owned_paths(closed=True)
    _write_json(cap_path, document)
    active = census.ROOT / "docs" / "history" / "card-plans" / "active" / PLAN_STEM
    if active.is_file():
        active.write_text(_plan_body(closed=True), encoding="utf-8")
    _write_json(WAVE / "topology.json", topology(False))
    _write_json(WAVE / "readiness.json", readiness(False))
    return {"owned_paths": document["owned_paths"]}


def write(*, unique_active: bool) -> dict[str, Any]:
    WAVE.mkdir(parents=True, exist_ok=True)
    if unique_active:
        active_dir = census.ROOT / "docs" / "history" / "card-plans" / "active"
        active_dir.mkdir(parents=True, exist_ok=True)
        for path in sorted(active_dir.glob("*.md")):
            if path.name != PLAN_STEM:
                raise ValueError(f"unique-active already occupied by {path.name}")
    write_noise()
    copy_art()
    declaration = write_worldgen()
    _write_json(ART_MANIFEST, {"imports": art_imports(), "schema_version": 1})
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": (
                "GT6 WorldgenStoneLayers cubes, pebbles, BlockRockOres dense "
                "layers, WorldgenNetherQuartz, and no-mod StoneLayerOres; "
                "not player_complete; not planet rocks; not bedrock veins"
            )
        },
    )
    counts = Counter(row["material"] for row in declaration["layers"])
    _write_json(
        WAVE / "denominator.json",
        {
            "probability": PROBABILITY,
            "placer": PLACER,
            "mte": 32757,
            "layer_count": declaration["layer_count"],
            "loader_layer_count": declaration["loader_layer_count"],
            "rock_ore_count": declaration["rock_ore_count"],
            "nether_rock_ore_count": declaration["nether_rock_ore_count"],
            "stone_block_count": declaration["stone_block_count"],
            "no_deep_y": NO_DEEP_Y,
            "unit": UNIT,
            "layer_ore_count": sum(len(row["ores"]) for row in declaration["layers"]),
            "boundary_count": len(declaration["boundaries"]),
            "material_counts": dict(counts),
            "schema_version": 1,
            "source_revision": GT6_REVISION,
            "unique_active_wave": SLUG if unique_active else None,
        },
    )
    _copy_empty_nbt()
    cap_path = census.TOOLS / "capabilities" / SLUG / "capability.json"
    if unique_active:
        cap_path.parent.mkdir(parents=True, exist_ok=True)
        _write_json(cap_path, capability_document(closed=False))
        plan = (
            census.ROOT / "docs" / "history" / "card-plans" / "active" / PLAN_STEM
        )
        plan.write_text(_plan_body(closed=False), encoding="utf-8")
    return {
        "slug": SLUG,
        "layers": declaration["layer_count"],
        "unique_active": unique_active,
    }


def check() -> list[str]:
    errors: list[str] = []
    if not LOADER.is_file():
        return [f"missing {census.relative(LOADER)}"]
    if not NOISE_SOURCE.is_file():
        return [f"missing {census.relative(NOISE_SOURCE)}"]
    if not ROCK_ORES_JAVA.is_file():
        return [f"missing {census.relative(ROCK_ORES_JAVA)}"]
    if not NETHER_QUARTZ_JAVA.is_file():
        return [f"missing {census.relative(NETHER_QUARTZ_JAVA)}"]
    try:
        expected = layer_catalog()
    except ValueError as error:
        return [str(error)]
    if not CATALOG.is_file():
        return ["missing stone_layer_rocks.json"]
    actual = census.load_json(CATALOG)
    if actual != expected:
        errors.append(
            "stone_layer_rocks.json drifted from Loader_Worldgen + BlockRockOres"
        )
    if actual.get("unit") != UNIT:
        errors.append("catalog unit must stay GT6 CS.U")
    if not actual.get("deepslate_layer") or not actual["deepslate_layer"].get("ores"):
        errors.append("DEEPSLATE layer ores missing")
    if not actual.get("boundaries"):
        errors.append("StoneLayer MAP boundaries missing")
    if actual.get("layer_count") != LAYER_COUNT:
        errors.append("layer_count drifted from no-mod GT6 LAYERS")
    if actual.get("loader_layer_count") != LOADER_LAYER_COUNT:
        errors.append("loader_layer_count drifted from Loader_Worldgen")
    if actual.get("rock_ore_count") != ROCK_ORE_COUNT:
        errors.append("rock_ore_count drifted from BlockRockOres")
    if actual.get("stone_block_count") != STONE_BLOCK_COUNT:
        errors.append("stone_block_count drifted from BlocksGT native cubes")
    cube_materials = {row["material"] for row in actual.get("stone_blocks", [])}
    native_gt = {stone["material"] for stone in NATIVE_STONES}
    if cube_materials != native_gt:
        errors.append("stone_blocks materials drifted from BlocksGT")
    dense_materials = [row["material"] for row in actual.get("rock_ores", [])]
    overworld_dense = [row["material"] for row in extract_rock_ores()]
    nether_dense = extract_nether_quartz_ore()["material"]
    if dense_materials != overworld_dense + [nether_dense]:
        errors.append("rock_ores drifted from BlockRockOres")
    layer_materials = [row["material"] for row in actual.get("layers", [])]
    if layer_materials[:ROCK_ORE_COUNT] != overworld_dense:
        errors.append("BlockRockOres must prepend Loader_Worldgen LAYERS")
    if nether_dense in set(layer_materials):
        errors.append("Nether Quartz must not enter overworld LAYERS")
    if actual.get("nether_rock_ore_count") != 1:
        errors.append("nether_rock_ore_count drifted from BlockRockOres meta 8")
    nether = actual.get("nether_quartz") or {}
    if nether.get("feature_type") != NETHER_QUARTZ_FEATURE_TYPE:
        errors.append("nether quartz feature_type drifted")
    if nether.get("meta") != NETHER_QUARTZ_META:
        errors.append("nether quartz must stay BlockRockOres meta 8")
    if nether.get("base_y") != NETHER_QUARTZ_BASE_Y or nether.get("span") != NETHER_QUARTZ_SPAN:
        errors.append("WorldgenNetherQuartz Y formula drifted")
    if nether.get("sample_ys") != list(NETHER_QUARTZ_SAMPLE_YS):
        errors.append("WorldgenNetherQuartz noise sample Y drifted")
    if nether.get("noise_offset") != NETHER_NOISE_OFFSET:
        errors.append("nether NoiseGenerator offset must stay 512 * -1")
    if nether.get("vanilla_remove") != list(VANILLA_NETHER_QUARTZ):
        errors.append("vanilla nether quartz remove list drifted")
    if "granite_black" not in set(layer_materials):
        errors.append("granite_black must remain a StoneLayer surface material")
    if "coal" not in set(layer_materials):
        errors.append("MT.Coal BlockRockOres layer must remain in LAYERS")
    imports = art_imports()
    if len(imports) != STONE_BLOCK_COUNT + ROCK_ORE_COUNT + 1:
        errors.append(
            "art manifest must copy stone cubes and BlockRockOres iconsets"
        )
    manifest = census.load_json(ART_MANIFEST) if ART_MANIFEST.is_file() else {}
    if manifest.get("imports") != imports:
        errors.append("gt6_gt_stone_layer_rocks_art_manifest.json drifted")
    for row in imports:
        dest = census.ROOT / "src" / "main" / "resources" / row["destination"]
        if not dest.is_file():
            errors.append(f"missing imported texture {row['destination']}")
    materials_dir = (
        census.ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "materials"
    )
    for material in NATIVE_MATERIALS:
        path = materials_dir / f"{material}.json"
        if not path.is_file():
            errors.append(f"missing material {material}.json")
            continue
        blob = path.read_text(encoding="utf-8")
        if "cruciblecraft:generates_rock" not in blob:
            errors.append(f"{material} must generate rock form")
    java_root = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
    )
    feature = java_root / "worldgen" / "StoneLayerRockFeature.java"
    if not feature.is_file():
        errors.append("missing StoneLayerRockFeature.java")
    else:
        text = feature.read_text(encoding="utf-8")
        if "c:rocks" in text or "ItemEntity" in text or "GtItemScatterFeature" in text:
            errors.append("stone-layer feature must not dump catalog ItemEntity")
        if "instanceof RockBlock" in text or "new RockBlock" in text:
            errors.append("stone-layer pebbles must be gt_surface_rock, not RockBlock")
        if "nextInt" not in text:
            errors.append("stone-layer feature must keep 1/128 roll")
        if "tryReplace" not in text or "Blocks.STONE" not in text:
            errors.append("stone-layer feature must replace vanilla stone cubes")
        if "Blocks.TUFF" not in text:
            errors.append("stone-layer feature must replace 1.21 tuff")
        if "tryPlaceOre" not in text or "UNIT" not in text:
            errors.append("stone-layer feature must emit StoneLayerOres")
        if "isVanillaOre" not in text:
            errors.append("stone-layer feature must treat vanilla ores as replaceable")
        if "minBuildHeight" not in text:
            errors.append("setNoDeep must use minBuildHeight+24")
        if "scan[6] == scan[0]" not in text and "scan[6]==scan[0]" not in text:
            errors.append("in-layer ores must keep GT6 scan[6]==scan[0] small/normal")
        if "canEntityDestroy" not in (
            java_root / "content" / "block" / "StoneLayerStoneBlock.java"
        ).read_text(encoding="utf-8"):
            errors.append("harvest-3 cubes must keep GT6 wither proof")
        rock_ore = java_root / "content" / "block" / "StoneLayerRockOreBlock.java"
        if not rock_ore.is_file():
            errors.append("missing StoneLayerRockOreBlock.java")
        else:
            rock_text = rock_ore.read_text(encoding="utf-8")
            if "getFlammability" not in rock_text:
                errors.append("BlockRockOres must keep GT6 flammability")
            if "spawnAfterBreak" not in rock_text:
                errors.append("BlockRockOres must keep GT6 1/8 experience drop")
        loot_java = java_root / "datagen" / "ModBlockLootTables.java"
        if loot_java.is_file():
            loot_text = loot_java.read_text(encoding="utf-8")
            if "lookupOrThrow(Registries.ENCHANTMENT)" not in loot_text:
                errors.append("dense ore loot must look up fortune from the enchantment registry")
            if "RAW_ORE" not in loot_text:
                errors.append("BlockRockOres must drop oreRaw")
        states = java_root / "datagen" / "ModBlockStateProvider.java"
        if states.is_file() and "simpleBlockWithItem" not in states.read_text(
            encoding="utf-8"
        ):
            errors.append("layer cubes must emit block and item models together")
        tags_java = java_root / "datagen" / "ModBlockTagProvider.java"
        if tags_java.is_file() and "NEEDS_IRON_TOOL" not in tags_java.read_text(
            encoding="utf-8"
        ):
            errors.append("harvest-2 cubes must stay on needs_iron_tool")
        generated = census.ROOT / "src" / "generated" / "resources"
        for ore in extract_rock_ores() + [extract_nether_quartz_ore()]:
            rel = f"{ore['material']}/dense_ore.json"
            for folder in (
                generated / "assets" / "cruciblecraft" / "blockstates",
                generated / "assets" / "cruciblecraft" / "models",
                generated / "assets" / "cruciblecraft" / "models" / "item",
                generated / "data" / "cruciblecraft" / "loot_table" / "blocks",
            ):
                dest = folder / rel
                if not dest.is_file():
                    errors.append(f"missing generated {census.relative(dest)}")
        pickaxe = (
            generated / "data" / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json"
        )
        if pickaxe.is_file():
            pickaxe_text = pickaxe.read_text(encoding="utf-8")
            if "cruciblecraft:coal/dense_ore" not in pickaxe_text:
                errors.append("dense cubes must be mineable with pickaxe")
            if "cruciblecraft:nether_quartz/dense_ore" not in pickaxe_text:
                errors.append("nether quartz dense cube must be mineable with pickaxe")
        else:
            errors.append("missing generated pickaxe tag")
        loot_coal = (
            generated
            / "data"
            / "cruciblecraft"
            / "loot_table"
            / "blocks"
            / "coal"
            / "dense_ore.json"
        )
        if loot_coal.is_file() and "cruciblecraft:coal/raw_ore" not in loot_coal.read_text(
            encoding="utf-8"
        ):
            errors.append("coal dense cube loot must drop coal/raw_ore")
        for forbidden in ("moon.rocks", "mars.rocks", "planet.rocks"):
            if forbidden in text:
                errors.append(f"must not land {forbidden}")
    stones = java_root / "worldgen" / "StoneLayerStones.java"
    if not stones.is_file():
        errors.append("missing StoneLayerStones.java")
    blocks_java = java_root / "registry" / "ModBlocks.java"
    if not blocks_java.is_file():
        errors.append("missing ModBlocks.java")
    else:
        text = blocks_java.read_text(encoding="utf-8")
        if "GT_STONE_BLOCKS.containsKey" not in text:
            errors.append("layer cubes must reuse live gt_stone_catalog ids")
    cube = java_root / "content" / "block" / "StoneLayerStoneBlock.java"
    if not cube.is_file():
        errors.append("missing StoneLayerStoneBlock.java")
    tests = (
        census.ROOT
        / "src"
        / "test"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "gametest"
        / GAME_TESTS
    )
    if not tests.is_file():
        errors.append(f"missing {GAME_TESTS}")
    else:
        text = tests.read_text(encoding="utf-8")
        if "cruciblecraft_wave_worldgen_gt_stone_layer_rocks" not in text:
            errors.append("GameTest namespace drifted")
        if "granite_black" not in text:
            errors.append("GameTest must cover granite_black loot")
        if "stoneLayerReplacesVanillaStone" not in text:
            errors.append("GameTest must cover stone-cube replace")
        if "stoneLayerPlacesLayerOres" not in text:
            errors.append("GameTest must cover StoneLayerOres placement")
        if "stoneLayerPlacesDenseRockOres" not in text:
            errors.append("GameTest must cover BlockRockOres dense cubes")
        if "stoneLayerPlacesNetherQuartz" not in text:
            errors.append("GameTest must cover WorldgenNetherQuartz")
        if "ore_quartz_nether" not in text:
            errors.append("GameTest must cover vanilla nether quartz removal")
        if "stoneLayerManifestResolvesLocalGt6" not in text:
            errors.append("GameTest must cover local GT6 stone textures")
        if "GtItemScatterFeature" in text:
            errors.append("GameTest must not reintroduce catalog scatter")
    block = (
        java_root / "content" / "block" / "GtSurfaceRockBlock.java"
    ).read_text(encoding="utf-8")
    if "EntityBlock" not in block:
        errors.append("gt_surface_rock must keep a BlockEntity for layer material")
    if "noCollission" not in block:
        errors.append("surface rock must keep GT6 empty collision")
    noise = NOISE_DEST.read_text(encoding="utf-8") if NOISE_DEST.is_file() else ""
    if "CELL_3D" not in noise or "Hash3D" not in noise:
        errors.append("StoneLayerNoise must keep GT6 CELL_3D / Hash3D")
    if "dimensionId" in noise or "net.minecraft.world.World" in noise:
        errors.append("StoneLayerNoise must not keep 1.7.10 World")
    biome = census.load_json(
        DATA / "neoforge" / "biome_modifier" / "add_stone_layer_rocks.json"
    )
    if biome.get("biomes") != "#minecraft:is_overworld":
        errors.append("stone-layer rocks are overworld-wide, not WorldgenRocks cores")
    if biome.get("step") != "underground_decoration":
        errors.append("stone-layer pebbles must run underground_decoration")
    remove = census.load_json(
        DATA / "neoforge" / "biome_modifier" / "remove_overworld_large_veins.json"
    )
    if remove.get("type") != "neoforge:remove_features":
        errors.append("GENERATE_STONE must remove overworld large veins")
    if remove.get("features") != list(OVERWORLD_LARGE_VEINS):
        errors.append("removed large veins drifted from add_large_veins")
    nether_biome = DATA / "neoforge" / "biome_modifier" / "add_nether_netherquartz.json"
    if not nether_biome.is_file():
        errors.append("missing add_nether_netherquartz.json")
    else:
        nether_add = census.load_json(nether_biome)
        if nether_add.get("biomes") != "#minecraft:is_nether":
            errors.append("WorldgenNetherQuartz must hang on is_nether")
        if nether_add.get("features") != ["cruciblecraft:nether_netherquartz"]:
            errors.append("nether quartz placed feature drifted")
    vanilla_remove_path = (
        DATA / "neoforge" / "biome_modifier" / "remove_vanilla_nether_quartz.json"
    )
    if not vanilla_remove_path.is_file():
        errors.append("missing remove_vanilla_nether_quartz.json")
    else:
        vanilla_remove = census.load_json(vanilla_remove_path)
        if vanilla_remove.get("features") != list(VANILLA_NETHER_QUARTZ):
            errors.append("vanilla nether quartz remove list drifted")
        if vanilla_remove.get("biomes") != "#minecraft:is_nether":
            errors.append("vanilla quartz remove must stay nether-only")
    nether_java = java_root / "worldgen" / "NetherQuartzLayerFeature.java"
    if not nether_java.is_file():
        errors.append("missing NetherQuartzLayerFeature.java")
    else:
        nether_text = nether_java.read_text(encoding="utf-8")
        if "Blocks.NETHERRACK" not in nether_text:
            errors.append("WorldgenNetherQuartz must only replace netherrack")
        if "NETHER_NOISE_OFFSET" not in nether_text or "-512" not in nether_text:
            errors.append("nether quartz must keep 512 * dimensionId offset")
        if "SAMPLE_Y_HIGH = 64" not in nether_text:
            errors.append("nether quartz must sample noise Y 0 and 64")
        if "BASE_Y = 40" not in nether_text or "SPAN = 200" not in nether_text:
            errors.append("nether quartz Y must stay 40 + noise 0..199")
        if "Level.NETHER" not in nether_text:
            errors.append("nether quartz feature must refuse non-nether dimensions")
    gt6_nether = NETHER_QUARTZ_JAVA.read_text(encoding="utf-8")
    compact = re.sub(r"\s+", "", gt6_nether)
    if "BlocksGT.RockOres,8" not in compact:
        errors.append("GT6 WorldgenNetherQuartz must still write RockOres meta 8")
    if "40+tNoise.get" not in compact:
        errors.append("GT6 WorldgenNetherQuartz Y formula drifted")
    features_java = java_root / "registry" / "ModFeatures.java"
    if features_java.is_file():
        features_text = features_java.read_text(encoding="utf-8")
        if "nether_netherquartz" not in features_text:
            errors.append("ModFeatures must register nether.netherquartz")
    blob = json.dumps(actual)
    for forbidden in ("moon.rocks", "mars.rocks", "planet.rocks", "GtItemScatterFeature"):
        if forbidden in blob:
            errors.append(f"catalog must not mention {forbidden}")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing stone-layer-rocks structure/empty.nbt")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--issue", action="store_true")
    parser.add_argument("--prepare-close", action="store_true")
    parser.add_argument("--unique-active", action="store_true")
    parser.add_argument("--no-unique-active", action="store_true")
    args = parser.parse_args(argv)
    unique_active = True
    if args.no_unique_active:
        unique_active = False
    if args.unique_active:
        unique_active = True
    try:
        if args.issue:
            print(json.dumps(issue_active(), indent=2))
            return 0
        if args.prepare_close:
            print(json.dumps(prepare_close(), indent=2))
            return 0
        if args.check:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("worldgen/gt-stone-layer-rocks is current")
            return 0
        if args.write:
            print(json.dumps(write(unique_active=unique_active), indent=2))
            return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"gt stone layer rocks failed: {error}", file=sys.stderr)
        return 1
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
