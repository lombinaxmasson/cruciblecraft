#!/usr/bin/env python3
"""Prep catalog for the nine GT6 WorldgenTree* identities."""
from __future__ import annotations

import shutil
from pathlib import Path
from typing import Any

from tools import census_common as census

ROOT = census.ROOT
SOURCE_REVISION = census.SOURCE_REVISION
WAVE = ROOT / "tools" / "waves" / "prep" / "gt-trees"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
ART_MANIFEST = ASSETS / "gt6_gt_trees_art_manifest.json"
GT6_W = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_ICONSETS = Path("src/main/resources/assets/gregtech/textures/blocks/iconsets")
GT6_FOOD = Path("src/main/resources/assets/gregtech/textures/items/gt.multiitem.food")
CC_ICONSETS = "assets/cruciblecraft/textures/block/gt6/iconsets"
JAVA_PREP = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "worldgen"
    / "tree"
    / "prep"
)
LANDING_JAVA = (
    ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "registry" / "ModBlocks.java",
    ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "registry" / "ModItems.java",
    ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "registry" / "ModBlockEntities.java",
    ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "registry" / "ModFeatures.java",
)
LANDING_WORLDGEN = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "worldgen"
BIOME_MODIFIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "neoforge"
    / "biome_modifier"
)
GAMETEST = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft_wave_worldgen_gt_trees"
GAME_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "GtTreesGameTests.java"
)

SPECIES: tuple[dict[str, Any], ...] = (
    {
        "id": "rubber",
        "gt6_texture_key": "rubber",
        "feature": "tree.rubber",
        "gt6_class": "WorldgenTreeRubber",
        "amount": 1,
        "probability": 5,
        "hole_mode": "grow_rubber",
        "hole_source_id": 32762,
        "hole_item": "rubber_resin",
        "hole_fluid": "FL.Resin_Rubber",
        "overworld_biomes": [
            "minecraft:taiga",
            "minecraft:snowy_taiga",
            "minecraft:old_growth_pine_taiga",
            "minecraft:old_growth_spruce_taiga",
        ],
    },
    {
        "id": "maple",
        "gt6_texture_key": "maple",
        "feature": "tree.maple",
        "gt6_class": "WorldgenTreeMaple",
        "amount": 1,
        "probability": 5,
        "hole_mode": "drill_maple",
        "hole_source_id": 32761,
        "hole_item": "",
        "hole_fluid": "FL.Sap_Maple",
        "overworld_biomes": [
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest",
        ],
    },
    {
        "id": "willow",
        "gt6_texture_key": "willow",
        "feature": "tree.willow",
        "gt6_class": "WorldgenTreeWillow",
        "amount": 1,
        "probability": 4,
        "hole_mode": "none",
        "hole_source_id": 0,
        "hole_item": "",
        "hole_fluid": "",
        "overworld_biomes": ["minecraft:swamp", "minecraft:mangrove_swamp"],
    },
    {
        "id": "blue_mahoe",
        "gt6_texture_key": "bluemahoe",
        "feature": "tree.bluemahoe",
        "gt6_class": "WorldgenTreeBlueMahoe",
        "amount": 1,
        "probability": 3,
        "hole_mode": "none",
        "hole_source_id": 0,
        "hole_item": "",
        "hole_fluid": "",
        "overworld_biomes": [
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle",
        ],
    },
    {
        "id": "hazel",
        "gt6_texture_key": "hazel",
        "feature": "tree.hazel",
        "gt6_class": "WorldgenTreeHazel",
        "amount": 1,
        "probability": 32,
        "hole_mode": "none",
        "hole_source_id": 0,
        "hole_item": "",
        "hole_fluid": "",
        "overworld_biomes": [
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:meadow",
        ],
    },
    {
        "id": "cinnamon",
        "gt6_texture_key": "cinnamon",
        "feature": "tree.cinnamon",
        "gt6_class": "WorldgenTreeCinnamon",
        "amount": 1,
        "probability": 3,
        "hole_mode": "none",
        "hole_source_id": 0,
        "hole_item": "",
        "hole_fluid": "",
        "overworld_biomes": [
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle",
        ],
    },
    {
        "id": "coconut",
        "gt6_texture_key": "coconut",
        "feature": "tree.coconut",
        "gt6_class": "WorldgenTreeCoconut",
        "amount": 1,
        "probability": 1,
        "hole_mode": "none",
        "hole_source_id": 0,
        "hole_item": "",
        "hole_fluid": "",
        "overworld_biomes": ["minecraft:beach"],
    },
    {
        "id": "rainbowood",
        "gt6_texture_key": "rainbowood",
        "feature": "tree.rainbowood",
        "gt6_class": "WorldgenTreeRainbowood",
        "amount": 1,
        "probability": 4,
        "hole_mode": "drill_rainbowood",
        "hole_source_id": 32760,
        "hole_item": "",
        "hole_fluid": "FL.Sap_Rainbow",
        "overworld_biomes": [],
    },
    {
        "id": "blue_spruce",
        "gt6_texture_key": "bluespruce",
        "feature": "tree.bluespruce",
        "gt6_class": "WorldgenTreeBlueSpruce",
        "amount": 1,
        "probability": 32,
        "hole_mode": "none",
        "hole_source_id": 0,
        "hole_item": "",
        "hole_fluid": "",
        "overworld_biomes": [
            "minecraft:windswept_hills",
            "minecraft:windswept_forest",
            "minecraft:windswept_gravelly_hills",
            "minecraft:stony_peaks",
            "minecraft:jagged_peaks",
            "minecraft:stony_shore",
        ],
    },
)

HOLES: tuple[dict[str, Any], ...] = (
    {
        "source_id": 32762,
        "species": "rubber",
        "gt6_name": "Rubber Resin Hole",
        "mode": "grow_rubber",
        "item": "rubber_resin",
        "fluid": "FL.Resin_Rubber",
        "cc_fluid": "",
        "fluid_status": "explicitly_blocked",
        "fluid_reason": "FL.Resin_Rubber / fluidrubbertreesap is not a CC fluid; latex is not a stand-in",
        "millibuckets": 250,
        "nearby_xz": 256,
        "regen_ticks": 600,
    },
    {
        "source_id": 32761,
        "species": "maple",
        "gt6_name": "Tapped Maple",
        "mode": "drill_maple",
        "item": "",
        "fluid": "FL.Sap_Maple",
        "cc_fluid": "",
        "fluid_status": "explicitly_blocked",
        "fluid_reason": "FL.Sap_Maple is missing; do not fill maple holes with latex or rainbow sap",
        "millibuckets": 250,
        "tool": "drill",
        "log_meta": "LogA maple",
        "regen_ticks": 600,
    },
    {
        "source_id": 32760,
        "species": "rainbowood",
        "gt6_name": "Tapped Rainbowood",
        "mode": "drill_rainbowood",
        "item": "",
        "fluid": "FL.Sap_Rainbow",
        "cc_fluid": "cruciblecraft:rainbow_sap",
        "fluid_status": "reuse_canonical",
        "fluid_reason": "bath_remainder_fluid_mapping already names rainbow_sap",
        "millibuckets": 250,
        "tool": "drill",
        "log_meta": "LogB rainbowood",
        "regen_ticks": 600,
    },
)

BLOCKED: tuple[dict[str, str], ...] = (
    {
        "id": "maple_sap_fluid",
        "status": "explicitly_blocked",
        "reason": "FL.Sap_Maple has no CC fluid; maple hole blocks still freeze",
    },
    {
        "id": "rubber_resin_fluid",
        "status": "explicitly_blocked",
        "reason": "FL.Resin_Rubber is missing; latex is not a stand-in",
    },
    {
        "id": "resin_to_rubber",
        "status": "explicitly_blocked",
        "reason": "GT6 uses extractor/juicer/squeezer; CC has no squeezer; coagulator eats latex not resin",
    },
    {
        "id": "slime_ball_to_rubber_plate",
        "status": "keep",
        "reason": "press/slime_ball_to_rubber_plate stays until a later player_complete card",
    },
    {
        "id": "hazelnut_coconut_drops",
        "status": "explicitly_blocked",
        "reason": "food identities belong to Crops/Food; leaves must not emit stand-in food",
    },
    {
        "id": "seasonal_leaves",
        "status": "out_of_scope",
        "reason": "maple brown/orange/red/yellow and blue-spruce xmas overlays are not this card",
    },
    {
        "id": "extra_dimensions",
        "status": "out_of_scope",
        "reason": "DESIGN_POLICY_OVERWORLD_ACCESS; Twilight/Erebus/Alfheim/Aether/Tropics stay off",
    },
)


def denominator() -> dict[str, Any]:
    return {
        "feature_count": len(SPECIES),
        "features": [
            {
                "id": row["id"],
                "feature": row["feature"],
                "gt6_class": row["gt6_class"],
                "amount": row["amount"],
                "probability": row["probability"],
                "hole_mode": row["hole_mode"],
                "hole_source_id": row["hole_source_id"],
            }
            for row in SPECIES
        ],
        "generated_by": "tools/gt_trees.py",
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "PREP_GT_TREES_DENOMINATOR",
        "tree_hole_count": len(HOLES),
        "unique_active_wave": None,
        "wave_slug": "prep/gt-trees",
    }


def biome_map() -> dict[str, Any]:
    return {
        "generated_by": "tools/gt_trees.py",
        "rainbowood_rare_chance": 8192,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "PREP_GT_TREES_BIOME_MAP",
        "trees": [
            {
                "feature": row["feature"],
                "id": row["id"],
                "overworld_biomes": list(row["overworld_biomes"]),
            }
            for row in SPECIES
        ],
    }


def tree_holes() -> dict[str, Any]:
    return {
        "generated_by": "tools/gt_trees.py",
        "holes": [dict(row) for row in HOLES],
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "PREP_GT_TREES_HOLES",
    }


def blocked() -> dict[str, Any]:
    return {
        "blocked": [dict(row) for row in BLOCKED],
        "generated_by": "tools/gt_trees.py",
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "PREP_GT_TREES_BLOCKED",
    }


def _import_row(
    destination: str,
    gt6_source: Path,
    *,
    already_present: bool = False,
) -> dict[str, str]:
    row = {
        "destination": destination.replace("\\", "/"),
        "gt6_source": gt6_source.as_posix(),
        "source": "gt6_referencable_port_code/gregtech6_w",
    }
    if already_present:
        row["status"] = "already_present"
    return row


def art_imports() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for row in SPECIES:
        key = str(row["gt6_texture_key"])
        ident = str(row["id"])
        rows.append(
            _import_row(
                f"assets/cruciblecraft/textures/block/tree/{ident}/sapling.png",
                GT6_ICONSETS / f"sapling_small_{key}.png",
            )
        )
        rows.append(
            _import_row(
                f"assets/cruciblecraft/textures/block/tree/{ident}/leaves.png",
                GT6_ICONSETS / f"leaves_{key}.png",
            )
        )
        for face in ("side", "top"):
            rows.append(
                _import_row(
                    f"{CC_ICONSETS}/log_{face}_{key}.png",
                    GT6_ICONSETS / f"log_{face}_{key}.png",
                    already_present=True,
                )
            )
    hole_files = (
        ("rubber", "log_hole_rubber.png", "log_hole.png"),
        ("rubber", "log_resin_rubber.png", "log_resin.png"),
        ("maple", "log_hole_maple.png", "log_hole.png"),
        ("maple", "log_sap_maple.png", "log_sap.png"),
        ("rainbowood", "log_hole_rainbowood.png", "log_hole.png"),
        ("rainbowood", "log_sap_rainbowood.png", "log_sap.png"),
    )
    for ident, source_name, dest_name in hole_files:
        rows.append(
            _import_row(
                f"assets/cruciblecraft/textures/block/tree/{ident}/{dest_name}",
                GT6_ICONSETS / source_name,
            )
        )
    rows.append(
        _import_row(
            "assets/cruciblecraft/textures/item/tree/rubber_resin.png",
            GT6_FOOD / "12050.png",
        )
    )
    return rows


def art_manifest() -> dict[str, Any]:
    return {
        "generated_by": "tools/gt_trees.py",
        "imports": art_imports(),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "PREP_GT_TREES_ART",
    }


def copy_art(imports: list[dict[str, str]] | None = None) -> None:
    gt6_root = GT6_W
    for row in imports or art_imports():
        if row.get("status") == "already_present":
            continue
        destination = ROOT / "src" / "main" / "resources" / row["destination"]
        source = gt6_root / row["gt6_source"]
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)


def freeze_payloads() -> dict[Path, dict[str, Any]]:
    return {
        WAVE / "denominator.json": denominator(),
        WAVE / "biome_map.json": biome_map(),
        WAVE / "tree_holes.json": tree_holes(),
        WAVE / "blocked.json": blocked(),
        ART_MANIFEST: art_manifest(),
    }


def worldgen_payloads() -> dict[Path, dict[str, Any]]:
    payloads: dict[Path, dict[str, Any]] = {}
    for row in SPECIES:
        ident = str(row["id"])
        payloads[LANDING_WORLDGEN / "configured_feature" / f"tree_{ident}.json"] = {
            "type": "cruciblecraft:gt_tree",
            "config": {"species": ident},
        }
        payloads[LANDING_WORLDGEN / "placed_feature" / f"tree_{ident}.json"] = {
            "feature": f"cruciblecraft:tree_{ident}",
            "placement": [],
        }
        modifier: dict[str, Any] = {
            "type": "neoforge:add_features",
            "features": [f"cruciblecraft:tree_{ident}"],
            "step": "vegetal_decoration",
        }
        if ident == "rainbowood":
            modifier["biomes"] = "#minecraft:is_overworld"
        else:
            modifier["biomes"] = list(row["overworld_biomes"])
        payloads[BIOME_MODIFIERS / f"add_tree_{ident}.json"] = modifier
    return payloads


def write() -> dict[str, Any]:
    WAVE.mkdir(parents=True, exist_ok=True)
    copy_art()
    payloads = {**freeze_payloads(), **worldgen_payloads()}
    for path, document in payloads.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        census.write_stable(path, document)
    return {
        "art_imports": len(art_imports()),
        "features": len(SPECIES),
        "holes": len(HOLES),
        "worldgen": len(worldgen_payloads()),
    }


def _bytes_match(row: dict[str, str]) -> str | None:
    destination = ROOT / "src" / "main" / "resources" / row["destination"]
    source = GT6_W / row["gt6_source"]
    if not destination.is_file():
        return f"missing destination {row['destination']}"
    if not source.is_file():
        return f"missing gt6 source {row['gt6_source']}"
    if destination.read_bytes() != source.read_bytes():
        return f"byte mismatch {row['destination']}"
    dest = row["destination"]
    if "multiblock_casing" in dest or "pipe_filter_cover" in dest:
        return f"aliased art {dest}"
    return None


def check() -> list[str]:
    errors: list[str] = []
    if not GT6_W.is_dir():
        return [f"missing {census.relative(GT6_W)}"]
    expected = freeze_payloads()
    for path, document in expected.items():
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        try:
            actual = census.load_json(path)
        except ValueError as exc:
            errors.append(f"{census.relative(path)}: {exc}")
            continue
        if actual != document:
            errors.append(f"{census.relative(path)} drifted from tools/gt_trees.py")
    for row in art_imports():
        mismatch = _bytes_match(row)
        if mismatch:
            errors.append(mismatch)
    for name in (
        "GtTreeGrower.java",
        "GtTreeSpecies.java",
        "GtTreePlacement.java",
        "GtTreeWorld.java",
        "HorizontalFacing.java",
    ):
        path = JAVA_PREP / name
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
    if (ROOT / "src" / "recipe_generated").joinpath(
        "resources", "data", "cruciblecraft", "recipe"
    ).exists():
        live = ROOT / "src" / "recipe_generated" / "resources" / "data" / "cruciblecraft" / "recipe"
        errors.extend(
            f"live recipe mentions tree prep {census.relative(path)}"
            for path in live.rglob("*")
            if "gt-trees" in path.name.lower() or "gt_tree" in path.name.lower()
        )
    landing_needles = (
        (LANDING_JAVA[0], ("GtTreeSpecies", "GtTreeSaplingBlock", "GtTreeLogBlock")),
        (LANDING_JAVA[1], ("GtTreeSpecies", "tree/rubber_resin")),
        (LANDING_JAVA[2], ("GtTreeHoleBlockEntity",)),
        (LANDING_JAVA[3], ("GtTreeFeature", "GtTreeGrower", "GtTreeSpecies")),
    )
    for path, needles in landing_needles:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        text = path.read_text(encoding="utf-8")
        for needle in needles:
            if needle not in text:
                errors.append(f"{path.name} missing {needle}")
    if not GAME_TESTS.is_file():
        errors.append(f"missing {census.relative(GAME_TESTS)}")
    for name in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        if not (GAMETEST / name).is_file():
            errors.append(f"missing {census.relative(GAMETEST / name)}")
    expected_worldgen = worldgen_payloads()
    for path, document in expected_worldgen.items():
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        try:
            actual = census.load_json(path)
        except ValueError as exc:
            errors.append(f"{census.relative(path)}: {exc}")
            continue
        if actual != document:
            errors.append(f"{census.relative(path)} drifted from tools/gt_trees.py")
    return errors
