#!/usr/bin/env python3
"""GT6 WorldgenOresSmall + WorldgenColtan, and GENERATE_STONE vein removal."""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import io_common as io

GT6_REVISION = io.SOURCE_REVISION
SLUG = "worldgen/gt-small-ores"
PLAN_STEM = "GT6小矿世界生成详细计划.md"
STATUS = "GT_SMALL_ORES_RUNTIME_READY"
WAVE = census.TOOLS / "waves" / "worldgen" / "gt-small-ores"
DATA = census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
GT6 = census.ROOT / "gt6_code" / "gregtech6"
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
LOADER = GT6 / "src" / "main" / "java" / "gregtech" / "loaders" / "b" / "Loader_Worldgen.java"
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
GT6_ORE_SMALL_SRC = (
    GT6_W
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "materialicons"
    / "METALLIC"
    / "oresmall.png"
)
ORE_SMALL_PNG = (
    ASSETS / "textures" / "block" / "gt6_import" / "materialicons" / "ore_small.png"
)
ORE_SMALL_TEX = "cruciblecraft:block/gt6_import/materialicons/ore_small"
ORE_FLECKS_TEX = "cruciblecraft:block/material/ore_flecks"
ART_MANIFEST = ASSETS / "gt6_gt_small_ores_art_manifest.json"
MATERIAL_SMALL_ORE = ASSETS / "models" / "block" / "material_small_ore.json"
ORE_SMALL_HOST = ASSETS / "models" / "block" / "ore_small_host"
SMALL_ORE_STATE = ASSETS / "blockstates" / "gt_small_ore.json"
HOSTED_ORE_STATE = ASSETS / "blockstates" / "gt_hosted_ore.json"
SMALL_ORE_MODELS = (
    ("gt_small_ore_stone.json", "minecraft:block/stone"),
    ("gt_small_ore_deepslate.json", "minecraft:block/deepslate"),
    ("gt_small_ore_netherrack.json", "minecraft:block/netherrack"),
    ("gt_small_bedrock_ore.json", "minecraft:block/bedrock"),
)
CATALOG = DATA / "worldgen_catalog" / "small_ores.json"
ORE_VEINS = DATA / "worldgen_catalog" / "ore_veins.json"
REMOVE_REMAINING = (
    DATA / "neoforge" / "biome_modifier" / "remove_remaining_overworld_large_veins.json"
)
REMOVE_FIVE = DATA / "neoforge" / "biome_modifier" / "remove_overworld_large_veins.json"
ADD_CATALOG = (
    census.ROOT
    / "src"
    / "worldgen_catalog_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "neoforge"
    / "biome_modifier"
    / "add_worldgen_catalog.json"
)
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_small_ores"
)
EMPTY_NBT_CANDIDATES = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_surface_rocks"
    / "structure"
    / "empty.nbt",
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_trees"
    / "structure"
    / "empty.nbt",
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_crops"
    / "structure"
    / "empty.nbt",
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_worldgen_gt_stone_layer_rocks"
    / "structure"
    / "empty.nbt",
)
GAME_TESTS = "GtSmallOresGameTests.java"
FEATURE_TYPE = "cruciblecraft:small_ores"
STEP = "fluid_springs"
OVERWORLD_COUNT = 37
NETHER_COUNT = 19
UNIQUE_NAME_COUNT = 41
COLTAN_AMOUNT = 32
COLTAN_MIN_Y = 20
COLTAN_MAX_Y = 40
COLTAN_RANGE = 480
COLTAN_SEED_OFFSET = 5
COLTAN_GAUSSIAN = 1500
COLTAN_LARGE_RANGE_SQUARED = 4096
FIVE_REMOVED_VEINS = (
    "cruciblecraft:large_copper_vein",
    "cruciblecraft:large_gold_vein",
    "cruciblecraft:large_iron_vein",
    "cruciblecraft:large_tin_vein",
    "cruciblecraft:large_tungsten_vein",
)
CC_BY_GT6_NAME = {
    "ore.small.copper": "copper",
    "ore.small.chalcopyrite": "chalcopyrite",
    "ore.small.malachite": "malachite",
    "ore.small.tin": "tin",
    "ore.small.cassiterite": "cassiterite",
    "ore.small.zinc": "zinc",
    "ore.small.sphalerite": "sphalerite",
    "ore.small.smithsonite": "smithsonite",
    "ore.small.stibnite": "stibnite",
    "ore.small.bismuth": "bismuth",
    "ore.small.lead": "lead",
    "ore.small.galena": "galena",
    "ore.small.silver": "silver",
    "ore.small.gold": "gold",
    "ore.small.pyrite": "pyrite",
    "ore.small.hematite": "hematite",
    "ore.small.pyrolusite": "pyrolusite",
    "ore.small.garnierite": "garnierite",
    "ore.small.pentlandite": "pentlandite",
    "ore.small.scheelite": "scheelite",
    "ore.small.salt": "salt",
    "ore.small.rocksalt": "sylvite",
    "ore.small.borax": "borax",
    "ore.small.asbestos": "asbestos",
    "ore.small.diamond": "diamond",
    "ore.small.amber": "amber",
    "ore.small.craponite": "craponite",
    "ore.small.redstone": "redstone",
    "ore.small.redcinnabar": "cinnabar",
    "ore.small.lapis": "lapis",
    "ore.small.eudialyte": "eudialyte",
    "ore.small.azurite": "azurite",
    "ore.small.coal": "coal",
    "ore.small.graphite": "graphite",
    "ore.small.pollucite": "pollucite",
    "ore.small.zeolite": "zeolite",
    "ore.small.sulfur": "sulfur",
    "ore.small.coltan": "coltan",
    "ore.small.niter": "niter",
    "ore.small.efrine": "efrine",
    "ore.small.cinnabar": "cinnabar",
}
TEST_IDS = [
    "smallOreCatalogMatchesLoaderWorldgen",
    "smallOrePlacesGtSmallOreBlock",
    "smallOreCountFollowsGt6Amount",
    "coltanHotspotUsesSeedPlusFive",
    "remainingOverworldLargeVeinsAreRemoved",
    "smallOreDoesNotDumpCatalog",
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
        "src/main/java/com/masson/cruciblecraft/worldgen/SmallOreCatalog.java",
        "src/main/java/com/masson/cruciblecraft/worldgen/SmallOreFeature.java",
        "src/main/java/com/masson/cruciblecraft/registry/ModFeatures.java",
        "src/main/resources/data/cruciblecraft/worldgen_catalog/small_ores.json",
        "src/main/resources/data/cruciblecraft/worldgen/configured_feature/small_ores.json",
        "src/main/resources/data/cruciblecraft/worldgen/placed_feature/small_ores.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_small_ores.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/add_nether_small_ores.json",
        "src/main/resources/data/cruciblecraft/neoforge/biome_modifier/remove_remaining_overworld_large_veins.json",
        "src/main/resources/data/cruciblecraft_wave_worldgen_gt_small_ores/**",
        f"tools/capabilities/{SLUG}/**",
        "tools/waves/worldgen/gt-small-ores/**",
        "tools/gt_small_ores.py",
        "tools/build_gt_small_ores.py",
        "tools/tests/test_gt_small_ores.py",
        "docs/current/player-guide.md",
        "src/main/resources/assets/cruciblecraft/gt6_gt_small_ores_art_manifest.json",
        "src/main/resources/assets/cruciblecraft/textures/block/gt6_import/materialicons/ore_small.png",
        "src/main/resources/assets/cruciblecraft/models/block/material_small_ore.json",
        "src/main/resources/assets/cruciblecraft/models/block/ore_small_host/**",
        "src/main/resources/assets/cruciblecraft/blockstates/gt_small_ore.json",
        "src/main/resources/assets/cruciblecraft/models/block/gt_small_ore_stone.json",
        "src/main/resources/assets/cruciblecraft/models/block/gt_small_ore_deepslate.json",
        "src/main/resources/assets/cruciblecraft/models/block/gt_small_ore_netherrack.json",
        "src/main/resources/assets/cruciblecraft/models/block/gt_small_bedrock_ore.json",
    ]


def _plan_body(*, closed: bool) -> str:
    if closed:
        lane = (
            "lane                         = closed\n"
            f"capability_slug              = {SLUG}\n"
            "unique_active_wave           = null\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = accepted\n"
            "depends_on                   = worldgen/gt-stone-layer-rocks\n"
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
            "depends_on                   = worldgen/gt-stone-layer-rocks\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：unique-active，目标 `runtime_ready`。\n"
            "> 本文件位于 `card-plans/active/`。"
        )
        gate = "[ ]"
    return (
        f"# GT6 小矿世界生成\n\n"
        f"> 计划 slug：`{SLUG}`\n"
        f"{status}\n"
        "> 正式名称：GT6 小矿世界生成\n"
        "> 性质：独立 `WorldgenOresSmall` 散点（主世界 GEN_GT + 下界 GEN_NETHER）\n"
        "> 与 `WorldgenColtan` 热点。GENERATE_STONE 时关掉剩余 catalog 主世界大矿脉。\n"
        "> 不改 T20 `add_worldgen_catalog.json`，不撒 catalog `ItemEntity`，\n"
        "> 不开 End / 行星 / GEN_GEMS 独立散点。关 `runtime_ready`，不是 `player_complete`。\n"
        ">\n"
        "> Java/tick 源：`gt6_code/gregtech6` @ "
        f"`{GT6_REVISION}`。\n\n"
        "```text\n"
        f"{lane}\n"
        "```\n\n"
        "## 门禁\n\n"
        f"- {gate} Loader 无模组 GEN_GT 37 行 + GEN_NETHER 19 行，41 个名字；"
        "rocksalt→sylvite\n"
        f"- {gate} 石层之后 `fluid_springs` 放置 `gt_small_ore`，公式 "
        "`max(1, amount/2 + random(1+amount)/2)`\n"
        f"- {gate} `WorldgenColtan` seed+5 gaussian×1500、range 480、"
        "3:1:1 coltan/columbite/tantalite\n"
        f"- {gate} 第二份 remove_features 关掉 catalog 129 条主世界大矿脉；"
        "不改已关石层卡的 5 脉名单\n"
        f"- {gate} 隔离 GameTest `-PwaveRecipes={SLUG}`\n"
        "- [x] 不以 catalog 掉落物、stand-in 配料或 T20 椭球顶小矿\n"
    )


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "depends_on": ["worldgen/gt-stone-layer-rocks"],
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "close_target": "runtime_ready",
        "overworld_count": OVERWORLD_COUNT,
        "nether_count": NETHER_COUNT,
        "unique_name_count": UNIQUE_NAME_COUNT,
        "feature": FEATURE_TYPE,
        "step": STEP,
    }


def extract_calls(src: str, name: str) -> list[str]:
    calls: list[str] = []
    pattern = re.compile(rf"new {re.escape(name)}\s*\(")
    index = 0
    while True:
        match = pattern.search(src, index)
        if match is None:
            break
        start = match.end() - 1
        depth = 0
        cursor = start
        while cursor < len(src):
            char = src[cursor]
            if char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0:
                    calls.append(src[start + 1 : cursor])
                    index = cursor + 1
                    break
            cursor += 1
        else:
            break
    return calls


def _first_string(body: str) -> str:
    match = re.search(r'"([^"]+)"', body)
    if match is None:
        raise ValueError(f"missing name in {body[:80]}")
    return match.group(1)


def _enabled_token(body: str) -> str:
    parts = [part.strip() for part in body.split(",")]
    return parts[1] if len(parts) > 1 else ""


def _ints(body: str, count: int) -> list[int]:
    parts = [part.strip() for part in body.split(",")]
    values: list[int] = []
    for part in parts[2:]:
        if re.fullmatch(r"-?\d+", part):
            values.append(int(part))
        if len(values) == count:
            break
    if len(values) != count:
        raise ValueError(f"expected {count} ints in {body[:80]}")
    return values


def parse_loader() -> dict[str, Any]:
    if not LOADER.is_file():
        raise FileNotFoundError(census.relative(LOADER))
    source = LOADER.read_text(encoding="utf-8")
    by_name: dict[str, dict[str, Any]] = {}
    order: list[str] = []
    for body in extract_calls(source, "WorldgenOresSmall"):
        name = _first_string(body)
        if _enabled_token(body) != "T":
            continue
        if "custom" in name:
            continue
        gens = re.findall(r"\bGEN_[A-Za-z0-9_]+", body)
        overworld = "GEN_GT" in gens
        nether = "GEN_NETHER" in gens
        if not overworld and not nether:
            continue
        min_y, max_y, amount = _ints(body, 3)
        material = CC_BY_GT6_NAME.get(name)
        if material is None:
            raise ValueError(f"unmapped {name}")
        by_name[name] = {
            "gt6_name": name,
            "material": material,
            "min_y": min_y,
            "max_y": max_y,
            "amount": amount,
            "overworld": overworld,
            "nether": nether,
        }
        order.append(name)
    entries = [by_name[name] for name in order]
    coltan_bodies = extract_calls(source, "WorldgenColtan")
    if len(coltan_bodies) != 1:
        raise ValueError("expected one WorldgenColtan")
    coltan_name = _first_string(coltan_bodies[0])
    min_y, max_y, amount, rng = _ints(coltan_bodies[0], 4)
    return {
        "entries": entries,
        "overworld_count": sum(1 for row in entries if row["overworld"]),
        "nether_count": sum(1 for row in entries if row["nether"]),
        "unique_name_count": len(entries),
        "coltan": {
            "gt6_name": coltan_name,
            "min_y": min_y,
            "max_y": max_y,
            "amount": amount,
            "range": rng,
            "seed_offset": COLTAN_SEED_OFFSET,
            "gaussian_scale": COLTAN_GAUSSIAN,
            "large_ore_range_squared": COLTAN_LARGE_RANGE_SQUARED,
            "materials": ["coltan", "columbite", "tantalite"],
        },
    }


def catalog_vein_ids() -> list[str]:
    document = census.load_json(ORE_VEINS)
    return [str(vein["id"]) for vein in document["veins"]]


def catalog_document() -> dict[str, Any]:
    parsed = parse_loader()
    veins = catalog_vein_ids()
    return {
        "schema_version": 1,
        "source_revision": GT6_REVISION,
        "entries": parsed["entries"],
        "overworld_count": parsed["overworld_count"],
        "nether_count": parsed["nether_count"],
        "unique_name_count": parsed["unique_name_count"],
        "coltan": parsed["coltan"],
        "removed_overworld_large_veins": veins,
    }


def _ore_host_names() -> list[str]:
    host_dir = ASSETS / "models" / "block" / "ore_host"
    return sorted(
        path.stem
        for path in host_dir.glob("*.json")
        if not path.stem.endswith("_cobble")
    )


def art_manifest_document() -> dict[str, Any]:
    return {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "source_present": True,
        "source_revision": GT6_REVISION,
        "imports": [
            {
                "destination": (
                    "assets/cruciblecraft/textures/block/gt6_import/"
                    "materialicons/ore_small.png"
                ),
                "gt6_source": (
                    "assets/gregtech/textures/blocks/materialicons/"
                    "METALLIC/oresmall.png"
                ),
                "note": (
                    "GT6 OP.oreSmall METALLIC overlay. Unique/hosted/broken ores "
                    "keep the CC ore_flecks overlay."
                ),
                "source": "gt6_referencable_port_code/gregtech6_w",
            }
        ],
    }


def _small_ore_model(base: str) -> dict[str, Any]:
    return {
        "parent": "cruciblecraft:block/material_small_ore",
        "textures": {"base": base},
    }


def _small_host_model(host: str) -> dict[str, Any]:
    return {
        "parent": f"cruciblecraft:block/ore_host/{host}",
        "textures": {"flecks": ORE_SMALL_TEX},
    }


def write_art() -> None:
    if not GT6_ORE_SMALL_SRC.is_file():
        raise FileNotFoundError(census.relative(GT6_ORE_SMALL_SRC))
    ORE_SMALL_PNG.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(GT6_ORE_SMALL_SRC, ORE_SMALL_PNG)
    _write_json(ART_MANIFEST, art_manifest_document())
    _write_json(
        MATERIAL_SMALL_ORE,
        {
            "parent": "cruciblecraft:block/material_ore",
            "textures": {"flecks": ORE_SMALL_TEX},
        },
    )
    ORE_SMALL_HOST.mkdir(parents=True, exist_ok=True)
    for host in _ore_host_names():
        _write_json(ORE_SMALL_HOST / f"{host}.json", _small_host_model(host))
    state = census.load_json(SMALL_ORE_STATE)
    for variant in state["variants"].values():
        model = variant["model"]
        variant["model"] = model.replace(
            ":block/ore_host/",
            ":block/ore_small_host/",
        )
    _write_json(SMALL_ORE_STATE, state)
    for name, base in SMALL_ORE_MODELS:
        _write_json(ASSETS / "models" / "block" / name, _small_ore_model(base))


def check_art() -> list[str]:
    errors: list[str] = []
    if not ORE_SMALL_PNG.is_file():
        errors.append("missing GT6 oreSmall overlay copy")
    if not ART_MANIFEST.is_file():
        errors.append("missing gt6_gt_small_ores_art_manifest.json")
    else:
        actual = census.load_json(ART_MANIFEST)
        if actual != art_manifest_document():
            errors.append("small-ore art manifest drifted")
    if not MATERIAL_SMALL_ORE.is_file():
        errors.append("missing material_small_ore.json")
    else:
        model = census.load_json(MATERIAL_SMALL_ORE)
        if model.get("textures", {}).get("flecks") != ORE_SMALL_TEX:
            errors.append("material_small_ore must use GT6 oreSmall flecks")
        if ORE_FLECKS_TEX in json.dumps(model):
            errors.append("material_small_ore must not keep ore_flecks")
    if not SMALL_ORE_STATE.is_file():
        errors.append("missing gt_small_ore.json")
    else:
        state = census.load_json(SMALL_ORE_STATE)
        hosts = _ore_host_names()
        variants = state.get("variants") or {}
        expected_keys = {f"host={host}" for host in hosts}
        if set(variants) != expected_keys:
            errors.append("gt_small_ore blockstate hosts drifted")
        for host in hosts:
            variant = variants.get(f"host={host}") or {}
            expected = f"cruciblecraft:block/ore_small_host/{host}"
            if variant.get("model") != expected:
                errors.append(f"gt_small_ore host={host} must use {expected}")
            path = ORE_SMALL_HOST / f"{host}.json"
            if not path.is_file():
                errors.append(f"missing {census.relative(path)}")
            else:
                model = census.load_json(path)
                if model != _small_host_model(host):
                    errors.append(f"{host} small-host model drifted")
        blob = json.dumps(state)
        if ":block/ore_host/" in blob:
            errors.append("gt_small_ore must not share ore_host models with large ores")
        if ORE_FLECKS_TEX in blob:
            errors.append("gt_small_ore blockstate must not mention ore_flecks")
    for name, base in SMALL_ORE_MODELS:
        path = ASSETS / "models" / "block" / name
        if not path.is_file():
            errors.append(f"missing {name}")
            continue
        model = census.load_json(path)
        if model != _small_ore_model(base):
            errors.append(f"{name} must parent material_small_ore")
        if ORE_FLECKS_TEX in json.dumps(model):
            errors.append(f"{name} must not keep ore_flecks")
    if HOSTED_ORE_STATE.is_file():
        hosted = json.dumps(census.load_json(HOSTED_ORE_STATE))
        if ":block/ore_small_host/" in hosted:
            errors.append("gt_hosted_ore must keep ore_host models")
    return errors


def write_worldgen() -> None:
    _write_json(
        DATA / "worldgen" / "configured_feature" / "small_ores.json",
        {"type": FEATURE_TYPE, "config": {}},
    )
    _write_json(
        DATA / "worldgen" / "placed_feature" / "small_ores.json",
        {"feature": FEATURE_TYPE, "placement": []},
    )
    _write_json(
        DATA / "neoforge" / "biome_modifier" / "add_small_ores.json",
        {
            "type": "neoforge:add_features",
            "biomes": "#minecraft:is_overworld",
            "features": [FEATURE_TYPE],
            "step": STEP,
        },
    )
    _write_json(
        DATA / "neoforge" / "biome_modifier" / "add_nether_small_ores.json",
        {
            "type": "neoforge:add_features",
            "biomes": "#minecraft:is_nether",
            "features": [FEATURE_TYPE],
            "step": STEP,
        },
    )
    _write_json(
        REMOVE_REMAINING,
        {
            "type": "neoforge:remove_features",
            "biomes": "#minecraft:is_overworld",
            "features": [f"cruciblecraft:{vein_id}" for vein_id in catalog_vein_ids()],
            "steps": ["underground_ores"],
        },
    )


def _empty_nbt() -> Path:
    for path in EMPTY_NBT_CANDIDATES:
        if path.is_file():
            return path
    raise FileNotFoundError("missing GameTest empty.nbt donor")


def _copy_empty_nbt() -> None:
    source = _empty_nbt()
    for relative in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = PACK / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, dest)


def _assert_unique_active_free(plan_stem: str) -> None:
    active_dir = census.ROOT / "docs" / "history" / "card-plans" / "active"
    active_dir.mkdir(parents=True, exist_ok=True)
    for path in sorted(active_dir.glob("*.md")):
        if path.name != plan_stem:
            raise ValueError(f"unique-active already occupied by {path.name}")


def capability_document(*, closed: bool) -> dict[str, Any]:
    return {
        "schema_version": 2,
        "slug": SLUG,
        "title": "GT Small Ores",
        "maturity": "runtime_ready",
        "workflow": "accepted" if closed else "active",
        "survival_access": "partial",
        "owned_paths": _owned_paths(closed=closed),
        "depends_on": ["worldgen/gt-stone-layer-rocks"],
        "profiles": ["capability-runtime"],
        "wave_slug": SLUG,
        "required_test_ids": list(TEST_IDS),
        "identity_disposition": [
            {
                "semantic_key": "worldgen:overworld.ores.small",
                "disposition": "new_distinct",
                "runtime_ids": [
                    "cruciblecraft:small_ores",
                    "cruciblecraft:gt_small_ore",
                ],
                "reason": (
                    "GT6 WorldgenOresSmall independent scatter on GEN_GT / GEN_NETHER "
                    "plus WorldgenColtan. Not T20 LargeVeinFeature ellipsoids, not "
                    "catalog ItemEntity scatter."
                ),
            }
        ],
        "note": (
            "Independent small-ore scatter after stone layers. Remaining overworld "
            "catalog large veins removed for GENERATE_STONE. Close at runtime_ready. "
            "Do not scatter catalog ItemEntity."
        ),
    }


def issue_active() -> dict[str, Any]:
    _assert_unique_active_free(PLAN_STEM)
    cap_path = census.TOOLS / "capabilities" / SLUG / "capability.json"
    if cap_path.is_file():
        existing = census.load_json(cap_path)
        if existing.get("workflow") == "accepted":
            raise ValueError(f"{SLUG} already closed")
    cap_path.parent.mkdir(parents=True, exist_ok=True)
    _write_json(cap_path, capability_document(closed=False))
    plan = census.ROOT / "docs" / "history" / "card-plans" / "active" / PLAN_STEM
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
        _assert_unique_active_free(PLAN_STEM)
    document = catalog_document()
    if document["overworld_count"] != OVERWORLD_COUNT:
        raise ValueError(f"overworld count {document['overworld_count']}")
    if document["nether_count"] != NETHER_COUNT:
        raise ValueError(f"nether count {document['nether_count']}")
    if document["unique_name_count"] != UNIQUE_NAME_COUNT:
        raise ValueError(f"unique count {document['unique_name_count']}")
    coltan = document["coltan"]
    if (
        coltan["amount"] != COLTAN_AMOUNT
        or coltan["range"] != COLTAN_RANGE
        or coltan["min_y"] != COLTAN_MIN_Y
        or coltan["max_y"] != COLTAN_MAX_Y
    ):
        raise ValueError("WorldgenColtan constants drifted")
    _write_json(CATALOG, document)
    write_worldgen()
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": (
                "GT6 WorldgenOresSmall + WorldgenColtan; GENERATE_STONE removes "
                "remaining overworld catalog large veins; not player_complete"
            )
        },
    )
    _write_json(
        WAVE / "denominator.json",
        {
            "schema_version": 1,
            "source_revision": GT6_REVISION,
            "unique_active_wave": SLUG if unique_active else None,
            "overworld_count": OVERWORLD_COUNT,
            "nether_count": NETHER_COUNT,
            "unique_name_count": UNIQUE_NAME_COUNT,
            "catalog_vein_count": len(document["removed_overworld_large_veins"]),
            "feature": FEATURE_TYPE,
            "step": STEP,
            "coltan_amount": COLTAN_AMOUNT,
            "coltan_range": COLTAN_RANGE,
            "rocksalt_material": "sylvite",
        },
    )
    _copy_empty_nbt()
    write_art()
    if unique_active:
        issue_active()
    return {
        "slug": SLUG,
        "overworld": OVERWORLD_COUNT,
        "nether": NETHER_COUNT,
        "veins": len(document["removed_overworld_large_veins"]),
        "unique_active": unique_active,
    }


def check() -> list[str]:
    errors: list[str] = []
    if not CATALOG.is_file():
        return ["missing small_ores.json"]
    actual = census.load_json(CATALOG)
    if actual.get("overworld_count") != OVERWORLD_COUNT:
        errors.append("overworld small-ore count drifted")
    if actual.get("nether_count") != NETHER_COUNT:
        errors.append("nether small-ore count drifted")
    if actual.get("unique_name_count") != UNIQUE_NAME_COUNT:
        errors.append("unique small-ore name count drifted")
    rocksalt = next(
        (row for row in actual.get("entries") or [] if row.get("gt6_name") == "ore.small.rocksalt"),
        None,
    )
    if rocksalt is None or rocksalt.get("material") != "sylvite":
        errors.append("ore.small.rocksalt must map to sylvite")
    names = {row["gt6_name"] for row in actual.get("entries") or []}
    if "ore.small.nikolite" in names or "ore.small.ancientdebris" in names:
        errors.append("mod-gated or hidden small ores must stay out")
    if any("custom" in name for name in names):
        errors.append("custom small-ore slots must stay out")
    for relative in (
        "worldgen/configured_feature/small_ores.json",
        "worldgen/placed_feature/small_ores.json",
        "neoforge/biome_modifier/add_small_ores.json",
        "neoforge/biome_modifier/add_nether_small_ores.json",
        "neoforge/biome_modifier/remove_remaining_overworld_large_veins.json",
    ):
        if not (DATA / relative).is_file():
            errors.append(f"missing {relative}")
    add_overworld = (DATA / "neoforge" / "biome_modifier" / "add_small_ores.json").read_text(
        encoding="utf-8"
    ) if (DATA / "neoforge" / "biome_modifier" / "add_small_ores.json").is_file() else ""
    if FEATURE_TYPE not in add_overworld or STEP not in add_overworld:
        errors.append("overworld small ores must hang on fluid_springs")
    if not REMOVE_FIVE.is_file():
        errors.append("closed stone-layer 5-vein remover missing")
    else:
        five = census.load_json(REMOVE_FIVE)
        if five.get("features") != list(FIVE_REMOVED_VEINS):
            errors.append("do not edit remove_overworld_large_veins.json")
    if REMOVE_REMAINING.is_file():
        remaining = census.load_json(REMOVE_REMAINING)
        expected_features = [f"cruciblecraft:{vein}" for vein in catalog_vein_ids()]
        if remaining.get("features") != expected_features:
            errors.append("remaining large-vein remover drifted from ore_veins.json")
        if remaining.get("biomes") != "#minecraft:is_overworld":
            errors.append("remaining large-vein remover must stay overworld")
    java = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "worldgen"
        / "SmallOreFeature.java"
    )
    if not java.is_file():
        errors.append("missing SmallOreFeature.java")
    else:
        text = java.read_text(encoding="utf-8")
        if "placeCount" not in text or "gt6ChunkRandom" not in text:
            errors.append("SmallOreFeature must keep GT6 amount and WD.random")
        if "GtItemScatterFeature" in text:
            errors.append("must not re-register catalog scatter")
        if "programmed_circuit" in text:
            errors.append("must not stand in programmed_circuit")
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
        if "must not reuse T20 large-vein ellipsoids" not in text:
            errors.append("GameTest must reject T20 ellipsoid stand-in")
        if "gt_item_scatter" not in text:
            errors.append("GameTest must keep catalog scatter unregistered")
        if "sylvite" not in text:
            errors.append("GameTest must assert rocksalt→sylvite")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing small-ore structure/empty.nbt")
    if ADD_CATALOG.is_file():
        catalog_add = census.load_json(ADD_CATALOG)
        if "cruciblecraft:small_ores" in json.dumps(catalog_add):
            errors.append("do not fold independent small ores into add_worldgen_catalog")
    names_blob = json.dumps(actual)
    if "GtItemScatterFeature" in names_blob or "programmed_circuit" in names_blob:
        errors.append("catalog must not mention scatter or stand-in parts")
    errors.extend(check_art())
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
            print("worldgen/gt-small-ores is current")
            return 0
        if args.write:
            print(json.dumps(write(unique_active=unique_active), indent=2))
            return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"gt small ores failed: {error}", file=sys.stderr)
        return 1
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
