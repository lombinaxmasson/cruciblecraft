#!/usr/bin/env python3
"""GT6 WorldgenGlowtus + WorldgenBushes overworld crops."""
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
SLUG = "worldgen/gt-crops"
PLAN_STEM = "GT作物世界生成详细计划.md"
STATUS = "GT_CROPS_RUNTIME_READY"
WAVE = census.TOOLS / "waves" / "worldgen" / "gt-crops"
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
DATA = census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_ICONSETS = Path("src/main/resources/assets/gregtech/textures/blocks/iconsets")
GT6_BUSH = Path("src/main/resources/assets/gregtech/textures/blocks/machines/plants/bush")
CC_ICONSETS = "assets/cruciblecraft/textures/block/gt6/iconsets"
CC_BUSH = "assets/cruciblecraft/textures/block/gt6_import/plants/bush"
ART_MANIFEST = ASSETS / "gt6_gt_crops_art_manifest.json"
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_crops"
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
GAME_TESTS = "GtCropsGameTests.java"
TEST_IDS = [
    "playerSurfaceIsRegistered",
    "biomeFilterMatchesGt6SurfaceSets",
    "glowtusPlacesOnWater",
    "bushPlacesSweetBerriesNotString",
    "bushHarvestAndBerrySet",
]
GLOWTUS = (
    "black",
    "red",
    "green",
    "brown",
    "blue",
    "purple",
    "cyan",
    "light_gray",
    "gray",
    "pink",
    "lime",
    "yellow",
    "light_blue",
    "magenta",
    "orange",
    "white",
)
GLOWTUS_BIOMES = [
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
]
BUSH_BIOMES = [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:birch_forest",
    "minecraft:old_growth_birch_forest",
    "minecraft:dark_forest",
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
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
        "src/main/java/com/masson/cruciblecraft/content/block/GlowtusBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/block/GtBushBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/GtBushBlockEntity.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/crop/**",
        "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModBlockEntities.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModFeatures.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java",
        "src/main/resources/assets/cruciblecraft/gt6_gt_crops_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6/iconsets/glowtus_*.png",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6_import/plants/bush/**",
        "src/main/resources/assets/cruciblecraft/blockstates/plant/**",
        "src/main/resources/assets/cruciblecraft/models/block/plant/**",
        "src/main/resources/assets/cruciblecraft/models/item/plant/**",
        "src/main/resources/data/cruciblecraft/loot_table/blocks/plant/**",
        "src/main/resources/data/cruciblecraft/worldgen/configured_feature/plant_*.json",
        "src/main/resources/data/cruciblecraft/worldgen/placed_feature/plant_*.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_plant_*.json",
        "src/main/resources/data/cruciblecraft_wave_worldgen_gt_crops/**",
        f"tools/capabilities/{SLUG}/**",
        "tools/waves/worldgen/gt-crops/**",
        "tools/gt_crops.py",
        "tools/build_gt_crops.py",
        "tools/tests/test_gt_crops.py",
    ]


def _plan_body(*, closed: bool) -> str:
    if closed:
        lane = (
            "lane                         = closed\n"
            f"capability_slug              = {SLUG}\n"
            "unique_active_wave           = null\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = accepted\n"
            "depends_on                   = worldgen/gt-trees\n"
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
            "depends_on                   = worldgen/gt-trees\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：unique-active，目标 `runtime_ready`。\n"
            "> 本文件位于 `card-plans/active/`。"
        )
        gate = "[ ]"
    return (
        f"# GT 作物世界生成\n\n"
        f"> 计划 slug：`{SLUG}`\n"
        f"{status}\n"
        "> 正式名称：GT 作物世界生成\n"
        "> 性质：`WorldgenGlowtus` 16 色睡莲与 `WorldgenBushes` / MTE 32759。\n"
        "> 不带 squeezer dump，不折到 `lilypad_glowtus/white_glowtus`，\n"
        "> 不掉落 string。关 `runtime_ready`，不是 `player_complete`。\n"
        ">\n"
        "> Java/tick 源：`gt6_code/gregtech6` @ "
        "`3703e40308c8c030763fd6297dea8b210d2a77b1`。\n\n"
        "```text\n"
        f"{lane}\n"
        "```\n\n"
        "## 门禁\n\n"
        f"- {gate} 16 色 glowtus BlockItem，meta 0 = black\n"
        f"- {gate} 灌木默认 sweet_berries，可设 glow_berries / plant_gt_berry\n"
        f"- {gate} 主世界 jungle / plains+woods 排除 frozen 生物群系修饰\n"
        f"- {gate} 隔离 GameTest `-PgameTestNamespaces={PACK.name}`\n"
        "- [x] 不以 string、错误睡莲或盖板顶替\n"
    )


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "depends_on": ["worldgen/gt-trees"],
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "close_target": "runtime_ready",
        "glowtus_colors": 16,
        "bush_mte": 32759,
        "default_berry": "minecraft:sweet_berries",
    }


def art_imports() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for color in GLOWTUS:
        name = f"glowtus_{color}.png"
        rows.append(
            {
                "source": "gregtech6_w",
                "gt6_source": (GT6_ICONSETS / name).as_posix(),
                "destination": f"{CC_ICONSETS}/{name}",
            }
        )
    for folder, name in (
        ("colored", "bush.png"),
        ("colored", "berries.png"),
        ("colored", "berries_immature.png"),
        ("overlay", "bush.png"),
        ("overlay", "berries.png"),
        ("overlay", "berries_immature.png"),
    ):
        rows.append(
            {
                "source": "gregtech6_w",
                "gt6_source": (GT6_BUSH / folder / name).as_posix(),
                "destination": f"{CC_BUSH}/{folder}/{name}",
            }
        )
    return rows


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


def _loot(block_id: str) -> dict[str, Any]:
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": block_id}],
                "rolls": 1.0,
            }
        ],
    }


def write_assets() -> None:
    for color in GLOWTUS:
        path = f"plant/glowtus_{color}"
        block_id = f"cruciblecraft:{path}"
        _write_json(
            ASSETS / "blockstates" / f"{path}.json",
            {"variants": {"": {"model": f"cruciblecraft:block/{path}"}}},
        )
        _write_json(
            ASSETS / "models" / "block" / f"{path}.json",
            {
                "parent": "minecraft:block/lily_pad",
                "textures": {
                    "particle": f"cruciblecraft:block/gt6/iconsets/glowtus_{color}",
                    "texture": f"cruciblecraft:block/gt6/iconsets/glowtus_{color}",
                },
            },
        )
        _write_json(
            ASSETS / "models" / "item" / f"{path}.json",
            {
                "parent": "minecraft:item/generated",
                "textures": {
                    "layer0": f"cruciblecraft:block/gt6/iconsets/glowtus_{color}"
                },
            },
        )
        _write_json(DATA / "loot_table" / "blocks" / f"{path}.json", _loot(block_id))
    variants: dict[str, dict[str, str]] = {}
    for facing in ("down", "up", "north", "south", "west", "east"):
        for stage in range(4):
            variants[f"facing={facing},stage={stage}"] = {
                "model": "cruciblecraft:block/plant/gt_bush"
            }
    _write_json(
        ASSETS / "blockstates" / "plant" / "gt_bush.json",
        {"variants": variants},
    )
    _write_json(
        ASSETS / "models" / "block" / "plant" / "gt_bush.json",
        {
            "parent": "minecraft:block/cube_all",
            "textures": {
                "all": "cruciblecraft:block/gt6_import/plants/bush/colored/bush"
            },
        },
    )
    _write_json(
        ASSETS / "models" / "item" / "plant" / "gt_bush.json",
        {"parent": "cruciblecraft:block/plant/gt_bush"},
    )
    _write_json(
        DATA / "loot_table" / "blocks" / "plant" / "gt_bush.json",
        _loot("cruciblecraft:plant/gt_bush"),
    )


def write_worldgen() -> None:
    for kind, biomes in (("glowtus", GLOWTUS_BIOMES), ("bush", BUSH_BIOMES)):
        _write_json(
            DATA / "worldgen" / "configured_feature" / f"plant_{kind}.json",
            {"config": {"kind": kind}, "type": "cruciblecraft:gt_crop"},
        )
        _write_json(
            DATA / "worldgen" / "placed_feature" / f"plant_{kind}.json",
            {"feature": f"cruciblecraft:plant_{kind}", "placement": []},
        )
        _write_json(
            DATA / "neoforge" / "biome_modifier" / f"add_plant_{kind}.json",
            {
                "biomes": biomes,
                "features": [f"cruciblecraft:plant_{kind}"],
                "step": "vegetal_decoration",
                "type": "neoforge:add_features",
            },
        )


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
    document = {
        "schema_version": 2,
        "slug": SLUG,
        "title": "GT Crops",
        "maturity": "runtime_ready",
        "workflow": "active",
        "owned_paths": _owned_paths(closed=False),
        "depends_on": ["worldgen/gt-trees"],
        "profiles": ["capability-runtime"],
        "wave_slug": SLUG,
        "required_test_ids": list(TEST_IDS),
        "identity_disposition": [
            {
                "semantic_key": "worldgen:plant.glowtus",
                "disposition": "new_distinct",
                "runtime_ids": [f"cruciblecraft:plant/glowtus_{c}" for c in GLOWTUS],
                "reason": (
                    "GT6 WorldgenGlowtus / BlockGlowtus 16 dye metas. "
                    "Not lilypad_glowtus/white_glowtus."
                ),
            },
            {
                "semantic_key": "worldgen:plant.bush",
                "disposition": "new_distinct",
                "runtime_ids": ["cruciblecraft:plant/gt_bush"],
                "reason": (
                    "GT6 WorldgenBushes / MultiTileEntityBush 32759. "
                    "Default sweet_berries, never string."
                ),
            },
        ],
        "note": (
            "Overworld glowtus and bush worldgen from local GT6. "
            "Close at runtime_ready. Do not pull squeezer dump 5322."
        ),
    }
    _write_json(cap_path, document)
    plan = census.ROOT / "docs" / "history" / "card-plans" / "active" / PLAN_STEM
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
                "GT6 WorldgenGlowtus + WorldgenBushes overworld runtime; "
                "not player_complete; no squeezer dump"
            )
        },
    )
    _write_json(
        WAVE / "denominator.json",
        {
            "bush_mte": 32759,
            "feature_count": 2,
            "features": ["plant.glowtus", "plant.bush"],
            "glowtus_colors": 16,
            "schema_version": 1,
            "source_revision": GT6_REVISION,
            "unique_active_wave": SLUG if unique_active else None,
        },
    )
    _copy_empty_nbt()
    return {
        "slug": SLUG,
        "glowtus": len(GLOWTUS),
        "art": len(art_imports()),
        "unique_active": unique_active,
    }


def check() -> list[str]:
    errors: list[str] = []
    if not GT6_W.is_dir():
        return [f"missing {census.relative(GT6_W)}"]
    if not ART_MANIFEST.is_file():
        return ["missing gt6_gt_crops_art_manifest.json"]
    manifest = census.load_json(ART_MANIFEST)
    if manifest.get("imports") != art_imports():
        errors.append("crops art manifest drifted")
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
    for color in GLOWTUS:
        path = ASSETS / "blockstates" / "plant" / f"glowtus_{color}.json"
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
    if not (ASSETS / "blockstates" / "plant" / "gt_bush.json").is_file():
        errors.append("missing gt_bush blockstate")
    if not (DATA / "worldgen" / "configured_feature" / "plant_glowtus.json").is_file():
        errors.append("missing plant_glowtus configured_feature")
    if not (DATA / "worldgen" / "configured_feature" / "plant_bush.json").is_file():
        errors.append("missing plant_bush configured_feature")
    java = (
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
    if not java.is_file():
        errors.append(f"missing {GAME_TESTS}")
    else:
        text = java.read_text(encoding="utf-8")
        if "must not reuse lilypad_glowtus/white_glowtus" not in text:
            errors.append("GameTest must keep white_glowtus unaliased")
        if "SWEET_BERRIES" not in text:
            errors.append("GameTest must assert sweet berries")
        if "Items.STRING" not in text:
            errors.append("GameTest must reject string berries")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing crops structure/empty.nbt")
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
            print("worldgen/gt-crops is current")
            return 0
        if args.write:
            print(json.dumps(write(unique_active=unique_active), indent=2))
            return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"gt crops failed: {error}", file=sys.stderr)
        return 1
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
