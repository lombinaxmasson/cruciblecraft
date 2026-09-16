#!/usr/bin/env python3
"""GT6 WorldgenRocks overworld surface pebbles (MTE 32757)."""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import io_common as io

GT6_REVISION = io.SOURCE_REVISION
SLUG = "worldgen/gt-surface-rocks"
PLAN_STEM = "GT6地表石子保真详细计划.md"
STATUS = "GT_SURFACE_ROCKS_RUNTIME_READY"
WAVE = census.TOOLS / "waves" / "worldgen" / "gt-surface-rocks"
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
DATA = census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_ROCKGT = Path(
    "src/main/resources/assets/gregtech/textures/items/materialicons/dull"
)
ART_MANIFEST = ASSETS / "gt6_gt_surface_rocks_art_manifest.json"
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_surface_rocks"
)
EMPTY_NBT = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_trees"
    / "structure"
    / "empty.nbt"
)
GAME_TESTS = "GtSurfaceRocksGameTests.java"
TEST_IDS = [
    "surfaceRockManifestResolvesLocalGt6",
    "surfaceRockPlaces32757NotCatalogTag",
    "surfaceRockLootMatchesWorldgenRocks",
    "biomeFilterMatchesGt6OverworldSets",
    "surfaceRockOutlineMatchesPositionalAabb",
    "surfaceRockPlacesOnlyWhenSneaking",
    "stoneToolHarvestFollowsGt6Quality",
]
AMOUNT = 2
PROBABILITY = 3
PLACER = "cruciblecraft:gt_surface_rock"
FEATURE_TYPE = "cruciblecraft:surface_rock_scatter"
WORLDGEN_LOOT = [
    "item:cruciblecraft:stone/rock",
    "item:minecraft:flint",
    "item:cruciblecraft:meteoric_iron/rock",
    "item:cruciblecraft:meteoric_iron/raw_ore",
]
OVERWORLD_BIOMES = [
    "minecraft:desert",
    "minecraft:badlands",
    "minecraft:wooded_badlands",
    "minecraft:eroded_badlands",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:windswept_savanna",
    "minecraft:swamp",
    "minecraft:mangrove_swamp",
    "minecraft:taiga",
    "minecraft:snowy_taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:birch_forest",
    "minecraft:old_growth_birch_forest",
    "minecraft:dark_forest",
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
    "minecraft:windswept_gravelly_hills",
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:frozen_peaks",
    "minecraft:stony_shore",
]
JUNGLE_OCEAN_BEACH = [
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
    "minecraft:ocean",
    "minecraft:beach",
]


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
        "src/main/java/com/masson/cruciblecraft/content/item/GtSurfaceRockItem.java",
        "src/main/java/com/masson/cruciblecraft/content/item/PebbleBlockItem.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/SurfaceRockAppearance.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/SurfaceRockContents.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/SurfaceRockPlacement.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/SurfaceRockFeature.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/SurfaceRockConfiguration.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/PebbleShape.java",
        "src/main/java/com/masson/cruciblecraft/client/model/PositionalPebbleGeometry.java",
        "src/main/java/com/masson/cruciblecraft/material/gen/GeneratedMaterialPack.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModFeatures.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java",
        "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java",
        "src/main/resources/assets/cruciblecraft/gt6_gt_surface_rocks_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/textures/item/material/rock.png",
        "src/main/resources/assets/cruciblecraft/textures/item/material/rock_overlay.png",
        "src/main/resources/assets/cruciblecraft/models/block/material_rock.json",
        "src/main/resources/assets/cruciblecraft/models/block/gt_surface_rock_*.json",
        "src/main/resources/assets/cruciblecraft/models/item/gt_surface_rock.json",
        "src/main/resources/assets/cruciblecraft/blockstates/gt_surface_rock.json",
        "src/main/resources/data/cruciblecraft/loot_table/blocks/gt_surface_rock.json",
        "src/main/resources/data/cruciblecraft/worldgen_catalog/surface_scatter.json",
        "src/main/resources/data/cruciblecraft/worldgen/configured_feature/surface_rock_scatter.json",
        "src/main/resources/data/cruciblecraft/worldgen/placed_feature/surface_rock_scatter.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_surface_rocks.json",
        "src/main/resources/data/cruciblecraft_wave_worldgen_gt_surface_rocks/**",
        f"tools/capabilities/{SLUG}/**",
        "tools/waves/worldgen/gt-surface-rocks/**",
        "tools/gt_surface_rocks.py",
        "tools/build_gt_surface_rocks.py",
        "tools/tests/test_gt_surface_rocks.py",
        "tools/build_worldgen_catalog.py",
        "tools/operand_reachability.py",
        "tools/tests/test_build_worldgen_catalog.py",
        "src/test/java/com/masson/cruciblecraft/worldgen/WorldgenCatalogResourceTest.java",
        "src/test/java/com/masson/cruciblecraft/worldgen/SurfaceRockFeatureConfigTest.java",
        "src/test/java/com/masson/cruciblecraft/worldgen/PebbleShapeTest.java",
        "src/test/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java",
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
        f"# GT6 地表石子保真\n\n"
        f"> 计划 slug：`{SLUG}`\n"
        f"{status}\n"
        "> 正式名称：GT6 地表石子保真\n"
        "> 性质：把 T33 `surface_rock_scatter` 从整表 `c:rocks` 1/128\n"
        "> 改成 GT6 `WorldgenRocks` `overworld.rocks`：每区块 2 条射线、1/3 命中，\n"
        "> 放置 32757（空石头 / 燧石 / 陨铁）。物品贴图用本地 `gt6_w` `rockgt`。\n"
        ">\n"
        "> Java/tick 源：`gt6_code/gregtech6` @ "
        f"`{GT6_REVISION}`。\n"
        "> 贴图源：本地 `gt6_referencable_port_code/gregtech6_w`。\n"
        "> 不重开行星岩 prep。不撒 catalog `ItemEntity`。\n\n"
        "```text\n"
        f"{lane}\n"
        "```\n\n"
        "关闭目标不是 `player_complete`。Twilight / Tropics / Atum 不在本卡。\n\n"
        "## 门禁\n\n"
        f"- {gate} `WorldgenOnSurface` amount=2 probability=3，接触草/土/沙，跳过耕地\n"
        f"- {gate} 放置 `gt_surface_rock`，不是随机铜/铁 `RockBlock`\n"
        f"- {gate} 模型与选取框共用 GT6 位置 AABB；实体碰撞保持 GT6 无碰撞\n"
        f"- {gate} 物品栏 / Jade 显示掉落材质（空石头石子 / 燧石 / 陨铁）\n"
        f"- {gate} 掉落石头石子 / 燧石 / 陨铁 rock 或 raw_ore\n"
        f"- {gate} 隔离 GameTest `-PwaveRecipes={SLUG}`\n"
        "- [x] 不以 catalog `ItemEntity` 或 `c:rocks` 整表倾倒当获得\n"
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
        "amount": AMOUNT,
        "probability": PROBABILITY,
        "placer": PLACER,
        "mte": 32757,
    }


def art_imports() -> list[dict[str, str]]:
    return [
        {
            "source": "gregtech6_w",
            "gt6_source": (GT6_ROCKGT / "rockgt.png").as_posix(),
            "destination": "assets/cruciblecraft/textures/item/material/rock.png",
        },
        {
            "source": "gregtech6_w",
            "gt6_source": (GT6_ROCKGT / "rockgt_overlay.png").as_posix(),
            "destination": (
                "assets/cruciblecraft/textures/item/material/rock_overlay.png"
            ),
        },
    ]


def copy_art() -> None:
    if not GT6_W.is_dir():
        raise FileNotFoundError(census.relative(GT6_W))
    for row in art_imports():
        source = GT6_W / row["gt6_source"]
        dest = census.ROOT / "src" / "main" / "resources" / row["destination"]
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, dest)


def _copy_empty_nbt() -> None:
    if not EMPTY_NBT.is_file():
        raise FileNotFoundError(census.relative(EMPTY_NBT))
    for relative in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = PACK / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(EMPTY_NBT, dest)


def _pebble_model(texture: str, *, tint: bool = False) -> dict[str, Any]:
    model: dict[str, Any] = {
        "loader": "cruciblecraft:positional_pebble",
        "textures": {"particle": texture, "rock": texture},
    }
    if tint:
        model["tint"] = True
    return model


def write_assets() -> None:
    appearances = {
        "stone": "minecraft:block/stone",
        "sandstone": "minecraft:block/sandstone",
        "cobble": "minecraft:block/cobblestone",
    }
    contents = ("empty", "flint", "meteoric_rock", "meteoric_raw")
    variants: dict[str, dict[str, str]] = {}
    for appearance, texture in appearances.items():
        _write_json(
            ASSETS / "models" / "block" / f"gt_surface_rock_{appearance}.json",
            _pebble_model(texture),
        )
        for content in contents:
            variants[f"appearance={appearance},contents={content}"] = {
                "model": f"cruciblecraft:block/gt_surface_rock_{appearance}"
            }
    _write_json(
        ASSETS / "blockstates" / "gt_surface_rock.json",
        {"variants": variants},
    )
    _write_json(
        ASSETS / "models" / "item" / "gt_surface_rock.json",
        {"parent": "cruciblecraft:block/gt_surface_rock_stone"},
    )
    _write_json(
        ASSETS / "models" / "block" / "material_rock.json",
        _pebble_model("minecraft:block/stone", tint=True),
    )
    _write_json(
        DATA / "loot_table" / "blocks" / "gt_surface_rock.json",
        {
            "type": "minecraft:block",
            "pools": [
                {
                    "bonus_rolls": 0.0,
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                    "entries": [
                        {
                            "type": "minecraft:alternatives",
                            "children": [
                                {
                                    "type": "minecraft:item",
                                    "name": "minecraft:flint",
                                    "conditions": [
                                        {
                                            "condition": "minecraft:block_state_property",
                                            "block": "cruciblecraft:gt_surface_rock",
                                            "properties": {"contents": "flint"},
                                        }
                                    ],
                                },
                                {
                                    "type": "minecraft:item",
                                    "name": "cruciblecraft:meteoric_iron/rock",
                                    "conditions": [
                                        {
                                            "condition": "minecraft:block_state_property",
                                            "block": "cruciblecraft:gt_surface_rock",
                                            "properties": {
                                                "contents": "meteoric_rock"
                                            },
                                        }
                                    ],
                                },
                                {
                                    "type": "minecraft:item",
                                    "name": "cruciblecraft:meteoric_iron/raw_ore",
                                    "conditions": [
                                        {
                                            "condition": "minecraft:block_state_property",
                                            "block": "cruciblecraft:gt_surface_rock",
                                            "properties": {
                                                "contents": "meteoric_raw"
                                            },
                                        }
                                    ],
                                },
                                {
                                    "type": "minecraft:item",
                                    "name": "cruciblecraft:stone/rock",
                                },
                            ],
                        }
                    ],
                    "rolls": 1.0,
                }
            ],
        },
    )


def surface_scatter_declaration() -> dict[str, Any]:
    return {
        "schema_version": 1,
        "id": "surface_rock_scatter",
        "feature_type": FEATURE_TYPE,
        "config": {
            "amount": AMOUNT,
            "probability": PROBABILITY,
        },
        "placed_feature": "cruciblecraft:surface_rock_scatter",
        "placer": PLACER,
        "worldgen_loot": list(WORLDGEN_LOOT),
        "biome_modifier": {
            "id": "add_surface_rocks",
            "type": "neoforge:add_features",
            "biomes": list(OVERWORLD_BIOMES),
            "features": ["cruciblecraft:surface_rock_scatter"],
            "step": "top_layer_modification",
        },
        "provenance": (
            "GT6 WorldgenRocks overworld.rocks amount=2 probability=3; "
            "MTE 32757 loot; DESIGN_POLICY 1.21 biome mapping of GT6 vanilla cores"
        ),
        "design_policy": "DESIGN_POLICY",
        "rock_tag_source": {
            "material_gate": (
                "src/main/resources/data/cruciblecraft/"
                "material_registration_gate.json"
            ),
            "prefix_definition": (
                "src/main/resources/data/cruciblecraft/material_prefixes/rock.json"
            ),
            "prefix": "rock",
            "generation_flag": "cruciblecraft:generates_rock",
            "tag_namespace": "c",
            "tag_directory": "rocks",
            "runtime_pack": "GeneratedMaterialPack",
            "note": (
                "c:rocks is the placeable OP.rockGt material form tag. "
                "Worldgen samples gt_surface_rock, not this tag."
            ),
        },
    }


def write_worldgen() -> None:
    declaration = surface_scatter_declaration()
    _write_json(DATA / "worldgen_catalog" / "surface_scatter.json", declaration)
    _write_json(
        DATA / "worldgen" / "configured_feature" / "surface_rock_scatter.json",
        {
            "type": FEATURE_TYPE,
            "config": declaration["config"],
        },
    )
    _write_json(
        DATA / "worldgen" / "placed_feature" / "surface_rock_scatter.json",
        {"feature": "cruciblecraft:surface_rock_scatter", "placement": []},
    )
    _write_json(
        DATA / "neoforge" / "biome_modifier" / "add_surface_rocks.json",
        {
            "type": "neoforge:add_features",
            "biomes": list(OVERWORLD_BIOMES),
            "features": ["cruciblecraft:surface_rock_scatter"],
            "step": "top_layer_modification",
        },
    )


def capability_document(*, closed: bool) -> dict[str, Any]:
    return {
        "schema_version": 2,
        "slug": SLUG,
        "title": "GT Surface Rocks",
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
                "semantic_key": "worldgen:overworld.rocks",
                "disposition": "new_distinct",
                "runtime_ids": [PLACER, "cruciblecraft:surface_rock_scatter"],
                "reason": (
                    "GT6 WorldgenRocks / MultiTileEntityRock 32757 overworld placer. "
                    "Not c:rocks catalog dump. Not moon/mars/planet rocks."
                ),
            }
        ],
        "note": (
            "Overworld WorldgenRocks amount=2 probability=3. Close at "
            "runtime_ready. Do not scatter catalog ItemEntity. Do not land "
            "worldgen/gt-planet-rocks."
        ),
    }


def issue_active() -> dict[str, Any]:
    active_dir = census.ROOT / "docs" / "history" / "card-plans" / "active"
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
    plan.parent.mkdir(parents=True, exist_ok=True)
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
        for path in sorted(active_dir.glob("*.md")):
            if path.name != PLAN_STEM:
                raise ValueError(f"unique-active already occupied by {path.name}")
    copy_art()
    write_assets()
    write_worldgen()
    _write_json(ART_MANIFEST, {"imports": art_imports(), "schema_version": 1})
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": (
                "GT6 WorldgenRocks overworld runtime; not player_complete; "
                "not planet rocks; not c:rocks catalog dump"
            )
        },
    )
    _write_json(
        WAVE / "denominator.json",
        {
            "amount": AMOUNT,
            "probability": PROBABILITY,
            "placer": PLACER,
            "mte": 32757,
            "loot": list(WORLDGEN_LOOT),
            "biome_count": len(OVERWORLD_BIOMES),
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
        "art": len(art_imports()),
        "biomes": len(OVERWORLD_BIOMES),
        "unique_active": unique_active,
    }


def check() -> list[str]:
    errors: list[str] = []
    if not GT6_W.is_dir():
        return [f"missing {census.relative(GT6_W)}"]
    if not ART_MANIFEST.is_file():
        return ["missing gt6_gt_surface_rocks_art_manifest.json"]
    manifest = census.load_json(ART_MANIFEST)
    if manifest.get("imports") != art_imports():
        errors.append("surface rock art manifest drifted")
    for row in art_imports():
        dest = census.ROOT / "src" / "main" / "resources" / row["destination"]
        source = GT6_W / row["gt6_source"]
        if not dest.is_file():
            errors.append(f"missing destination {row['destination']}")
            continue
        if not source.is_file():
            errors.append(f"missing gt6 source {row['gt6_source']}")
            continue
        if dest.read_bytes() != source.read_bytes():
            errors.append(f"byte mismatch {row['destination']}")
        if "multiblock_casing" in row["destination"] or "conveyor_cover" in row["destination"]:
            errors.append(f"aliased art {row['destination']}")
    declaration = census.load_json(DATA / "worldgen_catalog" / "surface_scatter.json")
    if declaration != surface_scatter_declaration():
        errors.append("surface_scatter.json drifted from WorldgenRocks contract")
    if declaration.get("config", {}).get("rock_tag") == "c:rocks":
        errors.append("surface scatter must not sample c:rocks")
    biomes = declaration.get("biome_modifier", {}).get("biomes")
    if biomes == "#minecraft:is_overworld":
        errors.append("surface scatter must not use #minecraft:is_overworld")
    for biome in JUNGLE_OCEAN_BEACH:
        if isinstance(biomes, list) and biome in biomes:
            errors.append(f"surface scatter includes out-of-scope biome {biome}")
    java_root = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
    )
    feature = (java_root / "worldgen" / "SurfaceRockFeature.java").read_text(
        encoding="utf-8"
    )
    if "c:rocks" in feature or "RockBlock" in feature:
        errors.append("SurfaceRockFeature must not sample RockBlock / c:rocks")
    if "nextInt(mAmount)" not in feature and "config.amount()" not in feature:
        errors.append("SurfaceRockFeature must keep WorldgenRocks amount roll")
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
    ).read_text(encoding="utf-8")
    if "cruciblecraft_wave_worldgen_gt_surface_rocks" not in tests:
        errors.append("GameTest namespace drifted")
    if "must not place RockBlock" not in tests:
        errors.append("GameTest must reject catalog RockBlock placement")
    if "surfaceRockPlacesOnlyWhenSneaking" not in tests:
        errors.append("GameTest must lock sneak-only pebble placement")
    if "GtItemScatterFeature" in tests or "ItemEntity" in tests:
        errors.append("GameTest must not reintroduce ItemEntity scatter")
    item = (java_root / "content" / "item" / "GtSurfaceRockItem.java").read_text(
        encoding="utf-8"
    )
    if "surface_rock.material" not in item:
        errors.append("surface-rock item tooltip must expose its material")
    block = (java_root / "content" / "block" / "GtSurfaceRockBlock.java").read_text(
        encoding="utf-8"
    )
    if "PebbleShape" not in block:
        errors.append("surface rock outline must use PebbleShape")
    if "noCollission" not in block:
        errors.append("surface rock must keep GT6 empty collision")
    pebble_shape = (java_root / "worldgen" / "PebbleShape.java").read_text(
        encoding="utf-8"
    )
    if "IntegerProperty.create" in pebble_shape:
        errors.append("pebble AABB must not be a 0-1023 blockstate (registry OOM)")
    model = census.load_json(
        ASSETS / "models" / "block" / "gt_surface_rock_stone.json"
    )
    if model.get("loader") != "cruciblecraft:positional_pebble":
        errors.append("surface rock model must use positional_pebble loader")
    states = census.load_json(ASSETS / "blockstates" / "gt_surface_rock.json")
    if "multipart" in states:
        errors.append("surface rock blockstate must not use multipart (mesh RNG)")
    if "variants" not in states:
        errors.append("surface rock blockstate must enumerate appearance variants")
    geometry = (java_root / "client" / "model" / "PositionalPebbleGeometry.java").read_text(
        encoding="utf-8"
    )
    if "getModelData" not in geometry:
        errors.append("pebble mesh must read AABB from ModelData / pos")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing surface-rocks structure/empty.nbt")
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
            print("worldgen/gt-surface-rocks is current")
            return 0
        if args.write:
            print(json.dumps(write(unique_active=unique_active), indent=2))
            return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"gt surface rocks failed: {error}", file=sys.stderr)
        return 1
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
