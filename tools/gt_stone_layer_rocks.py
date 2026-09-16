#!/usr/bin/env python3
"""GT6 WorldgenStoneLayers overworld pebbles (MTE 32757 / tLastRock)."""
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

GT6_REVISION = io.SOURCE_REVISION
SLUG = "worldgen/gt-stone-layer-rocks"
PLAN_STEM = "GT6石层石子详细计划.md"
STATUS = "GT_STONE_LAYER_ROCKS_RUNTIME_READY"
WAVE = census.TOOLS / "waves" / "worldgen" / "gt-stone-layer-rocks"
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
DATA = census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
GT6 = census.ROOT / "gt6_code" / "gregtech6"
LOADER = GT6 / "src" / "main" / "java" / "gregtech" / "loaders" / "b" / "Loader_Worldgen.java"
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
    "stoneLayerOneIn128AndNoDeep",
    "stoneLayerDoesNotDumpCatalog",
    "stoneLayerManifestResolvesLocalGt6",
]
PLACER = "cruciblecraft:gt_surface_rock"
FEATURE_TYPE = "cruciblecraft:stone_layer_rocks"
PROBABILITY = 128
NO_DEEP_Y = 24
DEEPSLATE = "deepslate"
LAYER_COUNT = 123
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
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_STONES = Path("src/main/resources/assets/gregtech/textures/blocks/stones")
CC_STONES = "assets/cruciblecraft/textures/block/gt6/stones"


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
        "src/main/java/com/masson/cruciblecraft/content/block/StoneLayerStoneBlock.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/StoneLayerRockConfiguration.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModFeatures.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModBlockEntities.java",
        "src/main/java/com/masson/cruciblecraft/compat/jade/CrucibleJadePlugin.java",
        "src/main/resources/data/cruciblecraft/worldgen_catalog/stone_layer_rocks.json",
        "src/main/resources/data/cruciblecraft/worldgen/configured_feature/stone_layer_rocks.json",
        "src/main/resources/data/cruciblecraft/worldgen/placed_feature/stone_layer_rocks.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_stone_layer_rocks.json",
        "src/main/resources/data/minecraft/tags/block/needs_iron_tool.json",
        "src/main/resources/data/cruciblecraft_wave_worldgen_gt_stone_layer_rocks/**",
        "src/main/resources/assets/cruciblecraft/gt6_gt_stone_layer_rocks_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/stones/**/stone.png",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/stones/**/cobble.png",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/stones/**/cobble_mossy.png",
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
        "> 32757（`tLastRock`）。不生成 `StoneLayerOres`，不撒 catalog\n"
        "> `ItemEntity`，不重开行星岩 prep。\n"
        ">\n"
        "> Java/tick 源：`gt6_code/gregtech6` @ "
        f"`{GT6_REVISION}`。\n"
        "> 贴图：从 `gregtech6_w` `blocks/stones/<folder>/"
        "{stone,cobble,cobble_mossy}.png` 拷进已有族 `block/gt6/stones/`。\n\n"
        "```text\n"
        f"{lane}\n"
        "```\n\n"
        "关闭目标不是 `player_complete`。月/火/行星岩仍在 prep。\n\n"
        "## 门禁\n\n"
        f"- {gate} 无模组 GT6 `StoneLayer.LAYERS` 加权 123 条，噪声选层\n"
        f"- {gate} 列扫描替换石头/圆石/深板岩为层立方体（{STONE_BLOCK_COUNT} 个 GT 方块）\n"
        f"- {gate} 列扫描 1/{PROBABILITY} 在石/基岩/圆石面空气放置 `gt_surface_rock`\n"
        f"- {gate} 掉落 `tLastRock` 的 `rock`（granite_black 等），不是整表 `c:rocks`\n"
        f"- {gate} 不写 `StoneLayerOres`，不替换原版矿石格\n"
        f"- {gate} y<{NO_DEEP_Y} 的 `setNoDeep` 层用 deepslate\n"
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
        "stone_block_count": STONE_BLOCK_COUNT,
        "no_deep_y": NO_DEEP_Y,
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


def extract_layers(text: str | None = None) -> list[dict[str, Any]]:
    source = text if text is not None else LOADER.read_text(encoding="utf-8")
    needle = "StoneLayer.LAYERS.add("
    rows: list[dict[str, Any]] = []
    cursor = 0
    while True:
        start = source.find(needle, cursor)
        if start < 0:
            break
        open_idx = start + len(needle) - 1
        end = _matching_paren(source, open_idx)
        body = source[open_idx + 1 : end]
        no_deep = ".setNoDeep()" in body.replace(" ", "")
        ctor = body.find("new StoneLayer(")
        if ctor < 0:
            raise ValueError("LAYERS.add without StoneLayer constructor")
        arg_start = body.find("(", ctor) + 1
        while arg_start < len(body) and body[arg_start].isspace():
            arg_start += 1
        if body.startswith("null", arg_start):
            material = "stone"
            gt_block = None
        else:
            match = re.match(r"BlocksGT\.([A-Za-z0-9_]+)", body[arg_start:])
            if match is None:
                raise ValueError(
                    f"unmapped StoneLayer first arg: {body[arg_start:arg_start + 48]!r}"
                )
            gt_block = match.group(1)
            material = BLOCKSGT_TO_CC.get(gt_block)
            if material is None:
                raise ValueError(f"unmapped BlocksGT.{gt_block}")
        rows.append(
            {
                "material": material,
                "no_deep": no_deep,
                "gt_block": gt_block,
            }
        )
        cursor = end
    return rows


def layer_catalog() -> dict[str, Any]:
    layers = extract_layers()
    if len(layers) != LAYER_COUNT:
        raise ValueError(f"expected {LAYER_COUNT} LAYERS, got {len(layers)}")
    materials = {row["material"] for row in layers}
    materials.add(DEEPSLATE)
    missing = [name for name in NATIVE_MATERIALS if name not in materials]
    if missing:
        raise ValueError(f"native materials missing from LAYERS: {missing}")
    blocks = stone_blocks()
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
        "deepslate": DEEPSLATE,
        "layer_count": len(layers),
        "stone_block_count": len(blocks),
        "layers": [
            {"material": row["material"], "no_deep": row["no_deep"]}
            for row in layers
        ],
        "stone_blocks": blocks,
        "biome_modifier": {
            "id": "add_stone_layer_rocks",
            "type": "neoforge:add_features",
            "biomes": "#minecraft:is_overworld",
            "features": ["cruciblecraft:stone_layer_rocks"],
            "step": "underground_decoration",
        },
        "provenance": (
            "GT6 WorldgenStoneLayers overworld cube replace plus 32757 "
            "tLastRock pebbles; no-mod LAYERS weights; no StoneLayerOres"
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
                ],
                "reason": (
                    "GT6 WorldgenStoneLayers 32757 tLastRock pebbles and "
                    "stone-cube replace on overworld stone/cobble/deepslate. "
                    "Not WorldgenRocks grass scatter, not catalog dump, "
                    "not moon/mars/planet rocks, not StoneLayerOres."
                ),
            }
        ],
        "note": (
            "Overworld WorldgenStoneLayers cubes plus pebbles. Close at runtime_ready. "
            "Do not scatter catalog ItemEntity. Do not land worldgen/gt-planet-rocks. "
            "Do not emit StoneLayerOres or eat vanilla ore cells."
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
                "GT6 WorldgenStoneLayers overworld cube replace plus 32757 "
                "tLastRock pebbles; not player_complete; not planet rocks; "
                "not StoneLayerOres"
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
            "stone_block_count": declaration["stone_block_count"],
            "no_deep_y": NO_DEEP_Y,
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
    try:
        expected = layer_catalog()
    except ValueError as error:
        return [str(error)]
    if not CATALOG.is_file():
        return ["missing stone_layer_rocks.json"]
    actual = census.load_json(CATALOG)
    if actual != expected:
        errors.append("stone_layer_rocks.json drifted from Loader_Worldgen LAYERS")
    if actual.get("layer_count") != LAYER_COUNT:
        errors.append("layer_count drifted from no-mod GT6 LAYERS")
    if actual.get("stone_block_count") != STONE_BLOCK_COUNT:
        errors.append("stone_block_count drifted from BlocksGT native cubes")
    cube_materials = {row["material"] for row in actual.get("stone_blocks", [])}
    native_gt = {stone["material"] for stone in NATIVE_STONES}
    if cube_materials != native_gt:
        errors.append("stone_blocks materials drifted from BlocksGT")
    if "granite_black" not in {
        row["material"] for row in actual.get("layers", [])
    }:
        errors.append("granite_black must remain a StoneLayer surface material")
    imports = art_imports()
    if len(imports) != STONE_BLOCK_COUNT:
        errors.append("art manifest must copy stone/cobble/mossy for each GT cube")
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
        if "canEntityDestroy" not in (
            java_root / "content" / "block" / "StoneLayerStoneBlock.java"
        ).read_text(encoding="utf-8"):
            errors.append("harvest-3 cubes must keep GT6 wither proof")
        if "StoneLayerOres" in text and "placeBlock" in text:
            errors.append("this card must not emit StoneLayerOres")
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
