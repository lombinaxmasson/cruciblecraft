#!/usr/bin/env python3
"""Shared paths and frozen work-set helpers for centrifuge/compact Centrifuge R0+."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import gametest_receipt_roots
from tools import census_common as census

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

HOST = "cruciblecraft:centrifuge"
SOURCE_MAP = "gt.recipe.centrifuge"
TARGET_MAP = "cruciblecraft:centrifuge"
# 1.12 dye metas that still appear in the frozen GT6 dump / compact source.
LEGACY_VANILLA_ITEMS: dict[tuple[str, int], str] = {
    ("minecraft:dye", 15): "minecraft:bone_meal",
}
OWNER = "portfolio:track_a/centrifuge_compact"
# Locked support uses already-registered forms; broad recovery-only overlays are
# not admitted to the production material gate.
PLAYER_PATH_SUPPORT_FORMS: dict[str, list[str]] = {}
CATALOG_FAMILY_COUNT = 157
CATALOG_RELATION_COUNT = 250
CATALOG_SINGLETON_FAMILIES = 123
CATALOG_SINGLETON_RELATIONS = 123
CATALOG_MULTI_FAMILIES = 34
CATALOG_MULTI_RELATIONS = 127
# Backward-compatible names are catalog-only. Production builders must read the
# immutable production lock through the helpers below.
FAMILY_COUNT = CATALOG_FAMILY_COUNT
SOURCE_ROWS = CATALOG_RELATION_COUNT
SINGLETON_FAMILIES = CATALOG_SINGLETON_FAMILIES
SINGLETON_RELATIONS = CATALOG_SINGLETON_RELATIONS
MULTI_FAMILIES = CATALOG_MULTI_FAMILIES
MULTI_RELATIONS = CATALOG_MULTI_RELATIONS
PUBLICATION_GROUP_COUNT = 2
SINGLETON_GROUP = "cruciblecraft:centrifuge/singleton"
MULTI_GROUP = "cruciblecraft:centrifuge/multi"
EXPECTED_SELECTION_SHA256 = (
    "3e125fd647b88b0b93e2840f1cf0681c7196a5e91a8b16a321fa9e0b1b4f2c19"
)
EXPECTED_DISTRIBUTION = {
    1: 123,
    2: 16,
    3: 8,
    4: 1,
    5: 3,
    6: 2,
    7: 1,
    8: 1,
    11: 1,
    14: 1,
}
ROASTER_COMPACT_REMAINING_ORDINARY_FAMILIES = 5639
# The lock closes 22 ordinary families and formally reclassifies seven
# stateful fuel-rod families to the post-1.x nuclear owner.
EXPECTED_REMAINING_ORDINARY_FAMILIES = 5610
WITHDRAWN_CATALOG_CLOSING_REMAINING = 5482
# Hosted/state GT6 items collapsed onto vanilla/CC ids. Player path fail-closes
# these unless an equivalence proof is recorded. Dye meta 15 → bone_meal is a
# 1.12/1.21 id translation, not a hosted-ore collapse.
FIXTURE_ONLY_LOSSY_ITEM_ALIASES: dict[tuple[str, int], str] = {
    ("gregtech:gt.block.sands", 0): "minecraft:soul_sand",
    ("gregtech:gt.block.sands", 1): "minecraft:basalt",
    ("gregtech:gt.block.sands", 2): "minecraft:granite",
    ("gregtech:gt.multiitem.food", 1051): "minecraft:turtle_egg",
    ("gregtech:gt.multiitem.food", 1074): "minecraft:white_dye",
    ("gregtech:gt.multiitem.food", 1073): "minecraft:yellow_dye",
    ("gregtech:gt.multiitem.food", 12050): "cruciblecraft:rubber/plate",
    ("gregtech:gt.multiitem.food", 12098): "minecraft:slime_block",
    ("gregtech:gt.multiitem.food", 30001): "minecraft:honeycomb",
    ("gregtech:gt.multiitem.food", 30002): "cruciblecraft:wax_magic/dust",
    ("gregtech:gt.multiitem.food", 30004): "minecraft:chorus_fruit",
    ("gregtech:gt.multiitem.food", 30006): "minecraft:cocoa_beans",
    ("gregtech:gt.multiitem.food", 30008): "minecraft:brown_mushroom",
    ("gregtech:gt.multiitem.food", 30009): "minecraft:sand",
    ("gregtech:gt.multiitem.food", 30101): "minecraft:honey_block",
    ("gregtech:gt.multiitem.food", 30104): "cruciblecraft:wax_amnesic/dust",
    ("gregtech:gt.multiitem.food", 30105): "minecraft:gunpowder",
    ("gregtech:gt.multiitem.food", 30202): "minecraft:feather",
    ("gregtech:gt.meta.ore.broken.andesite", 9851): "cruciblecraft:andesite/rock",
    ("gregtech:gt.meta.ore.broken.basalt", 9851): "cruciblecraft:basalt/rock",
    ("gregtech:gt.meta.ore.broken.blackgranite", 9851): "cruciblecraft:granite_black/rock",
    ("gregtech:gt.meta.ore.broken.blueschist", 9851): "cruciblecraft:blueschist/rock",
    ("gregtech:gt.meta.ore.broken.diorite", 9851): "cruciblecraft:diorite/rock",
    ("gregtech:gt.meta.ore.broken.granite", 9851): "cruciblecraft:granite/rock",
    ("gregtech:gt.meta.ore.broken.greenschist", 9851): "cruciblecraft:greenschist/rock",
    ("gregtech:gt.meta.ore.broken.kimberlite", 9851): "cruciblecraft:kimberlite/rock",
    ("gregtech:gt.meta.ore.broken.komatiite", 9851): "cruciblecraft:komatiite/rock",
    ("gregtech:gt.meta.ore.broken.limestone", 9851): "cruciblecraft:limestone/rock",
    ("gregtech:gt.meta.ore.broken.marble", 9851): "cruciblecraft:marble/rock",
    ("gregtech:gt.meta.ore.broken.prismarine.dark", 9851): "cruciblecraft:prismarine_dark/rock",
    ("gregtech:gt.meta.ore.broken.prismarine.light", 9851): "cruciblecraft:prismarine/rock",
    ("gregtech:gt.meta.ore.broken.quartzite", 9851): "cruciblecraft:quartzite/rock",
    ("gregtech:gt.meta.ore.broken.redgranite", 9851): "cruciblecraft:granite_red/rock",
    ("gregtech:gt.meta.ore.broken.shale", 9851): "cruciblecraft:shale/rock",
    ("gregtech:gt.meta.ore.broken.slate", 9851): "cruciblecraft:slate/rock",
    ("gregtech:gt.meta.ore.normal.default", 9851): "minecraft:stone",
    ("gregtech:gt.meta.ore.normal.endstone", 9851): "minecraft:end_stone",
    ("gregtech:gt.meta.ore.normal.gravel", 9851): "minecraft:gravel",
    ("gregtech:gt.meta.ore.normal.mud", 9851): "minecraft:mud",
    ("gregtech:gt.meta.ore.normal.netherrack", 9851): "minecraft:netherrack",
    ("gregtech:gt.meta.ore.normal.redsand", 9851): "minecraft:red_sand",
    ("gregtech:gt.meta.ore.normal.sand", 9851): "minecraft:suspicious_sand",
    ("gregtech:gt.meta.ore.normal.sandstone", 9851): "minecraft:sandstone",
    ("gregtech:gt.multitileentity", 9441): "cruciblecraft:naquadah_enriched/rod",
    ("gregtech:gt.meta.scrapGt", 400): "cruciblecraft:zirconium/dust",
    ("gregtech:gt.multitileentity", 9319): "cruciblecraft:cyanite/rod",
    ("gregtech:gt.multitileentity", 9329): "cruciblecraft:yellorium/rod",
    ("gregtech:gt.multitileentity", 9339): "cruciblecraft:blutonium/rod",
    ("gregtech:gt.multitileentity", 9349): "cruciblecraft:ludicrite/rod",
    ("gregtech:gt.multitileentity", 9360): "cruciblecraft:naquadah_enriched/ingot",
    ("gregtech:gt.multitileentity", 9361): "cruciblecraft:naquadria/rod",
}
# Classification compatibility only. Production generation never consumes
# these aliases; the withdrawn catalog fixture may use them to exercise codec,
# router, and load capacity without asserting semantic equivalence.
LOSSY_ITEM_ALIASES = FIXTURE_ONLY_LOSSY_ITEM_ALIASES
CANDIDATE_SELECTION = TOOLS / "centrifuge_candidate_selection.json"
# Compatibility pointer for callers while the old builder name is retained.
PRODUCTION_SELECTION = CANDIDATE_SELECTION
PRODUCTION_LOCK = TOOLS / "centrifuge_production_lock.json"
PRODUCTION_LOCK_SCHEMA = TOOLS / "centrifuge_production_lock.schema.json"
OPERAND_DISPOSITION = TOOLS / "centrifuge_operand_disposition.json"

WORK_SET = TOOLS / "centrifuge_work_set.json"
SOURCE = TOOLS / "centrifuge_source.json"
RECEIPT = TOOLS / "centrifuge_source_receipt.json"
REVIEW = TOOLS / "centrifuge_source_review.json"
SOURCE_PACK = TOOLS / "centrifuge_source_pack_manifest.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "centrifuge_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = TOOLS / "centrifuge_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "centrifuge_shard_manifest.json"
SCHEMA = TOOLS / "compact_recipe_family_source.schema.json"
BUILDER = TOOLS / "build_centrifuge_source.py"
GENERATED_ROOT = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "centrifuge"
    / "compact"
)
CATALOG_FIXTURE_ROOT = (
    ROOT
    / "src"
    / "test"
    / "resources"
    / "centrifuge_catalog_fixture"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "centrifuge_catalog"
    / "centrifuge"
)
OPERAND_RUNTIME_MAP = TOOLS / "centrifuge_operand_runtime_map.json"
PLAYER_PATH = TOOLS / "centrifuge_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "centrifuge_layered_player_path.json"
EQUIVALENCE = TOOLS / "centrifuge_equivalence.json"
REQUIRED_FORMS = TOOLS / "centrifuge_required_forms.json"
RECIPE_FAMILIES = census.RECIPE_FAMILIES
T35_CENSUS = census.CENSUS
ROASTER_COMPACT_CENSUS_DELTA = TOOLS / "roaster_census_delta.json"
ROASTER_COMPACT_READINESS = TOOLS / "roaster_readiness.json"
ROASTER_COMPACT_CARD_TOPOLOGY = TOOLS / "roaster_card_topology.json"
POLICY = TOOLS / "centrifuge_materialization_policy.json"
DECISION = TOOLS / "centrifuge_materialization_decision.json"
MEASUREMENTS = TOOLS / "centrifuge_materialization_measurements.json"
PUBLICATION_DELTA = TOOLS / "centrifuge_publication_delta.json"
CENSUS_DELTA = TOOLS / "centrifuge_census_delta.json"
CENTRIFUGE_COMPACT_CARD_TOPOLOGY = TOOLS / "centrifuge_card_topology.json"
READINESS = TOOLS / "centrifuge_readiness.json"
REPAIR_READINESS = TOOLS / "centrifuge_repair_readiness.json"
LOAD_PROJECTION_INPUT = TOOLS / "centrifuge_load_projection_input.json"
LOAD_PROJECTION = TOOLS / "centrifuge_load_projection.json"
CARD_TOPOLOGY = ROASTER_COMPACT_CARD_TOPOLOGY
GAME_TEST_ROOT = ROOT / "src/main/java/com/masson/cruciblecraft/gametest"
GAME_TEST_JAVA = GAME_TEST_ROOT / "CentrifugeCompactGameTests.java"
GAME_TEST_RECEIPT = TOOLS / "centrifuge_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "centrifuge_gametest.log"
GAME_TEST_NAMESPACE = "cruciblecraft_wave_centrifuge_compact"
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=centrifuge/compact --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=centrifuge/compact"
GAME_TEST_RECEIPT_SCHEMA = 3
GAME_TEST_METHOD_RE = re.compile(
    r"public static void (\w+)\s*\(\s*GameTestHelper"
)
GAME_TEST_RUN_LOG_CANDIDATES = (
    ROOT / "run-centrifuge/compact-recipes" / "logs" / "latest.log",
    ROOT / "run-centrifuge/compact-recipes" / "logs" / "debug.log",
)
MATERIAL_REGISTRATION_GATE_JAVA = (
    ROOT / "src/main/java/com/masson/cruciblecraft/material/MaterialRegistrationGate.java"
)
MATERIAL_REGISTRATION_GATE_JSON = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
LEGACY_PLAYER_PATH_RECOVERY_ROOT = (
    ROOT
    / "src/main/resources/data/cruciblecraft/recipe/player_path_recovery"
)
WITHDRAWN_RECOVERY_ROOT = (
    ROOT
    / "src"
    / "test"
    / "resources"
    / "player_path_recovery_fixture"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "player_path_recovery"
)
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src"
    / "centrifuge_support_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "centrifuge_player_path_support"
)
# Compatibility name for the withdrawn broad-recovery builder only.
PLAYER_PATH_RECOVERY_ROOT = WITHDRAWN_RECOVERY_ROOT
CENTRIFUGE_DUMP = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.centrifuge.json"
)
ROW_CLASSIFICATION = TOOLS / "machine_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "chemical_axis_template_denominator.json"
T14_OPENING_LOAD_SOURCE = "tools/roaster_census_delta.json#recipe_load.closing"
T35_FOUNDATION = {
    "machine_tree_identities": 765,
    "exclusion_source_sites": 763,
    "exclusion_expanded_rows": 1701,
    "recipe_rows_accounted": 78682,
    "recipe_families": 5718,
}
OPENING_DISPOSITION = "planned"
OPENING_CLOSURE = "incomplete"
OPENING_FIDELITY = "source_backed"
OPENING_LOAD = "pending"
CLOSING_DISPOSITION = "implemented"
CLOSING_CLOSURE = "closed"
CLOSING_LOAD = "measured"
T14_COUNTABLE = (
    "datapack_authored_entries",
    "eager_publication_rows",
    "lazy_logical_rows",
)
T14_PENDING = (
    "lazy_cache_ceiling_rows",
    "sync_bytes",
    "server_reload_ms",
    "server_index_ms",
    "client_reload_ms",
    "client_index_ms",
    "retained_memory_bytes",
    "allocation_bytes",
    "lookup_p95_ns",
    "lookup_candidate_count",
)
STRATEGY_AXES = (
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
)
MAX_INTERVAL_AXES = (
    "lookup_p95_ns",
    "lookup_candidate_count",
)
PENDING_LOAD_VERDICT = "BLOCKED_PENDING_MEASUREMENT"
PUBLICATION_GROUP_KEYS = {
    SINGLETON_GROUP: "singleton",
    MULTI_GROUP: "multi",
}


def relative(path: Path) -> str:
    try:
        return census.relative(path)
    except ValueError:
        return path.as_posix()


def load_json(path: Path) -> Any:
    return census.load_json(path)


def parse_write_check(description: str, argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    return args


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return census.check_generated_document(path, document)


def load_production_lock(path: Path | None = None) -> dict[str, Any]:
    lock_path = path or PRODUCTION_LOCK
    document = load_json(lock_path)
    if document.get("schema_version") != 1:
        raise ValueError("centrifuge/compact production lock schema_version must be 1")
    if document.get("status") != "CENTRIFUGE_COMPACT_PRODUCTION_LOCKED":
        raise ValueError("centrifuge/compact production lock is not locked")
    if document.get("source_revision") != SOURCE_REVISION:
        raise ValueError("centrifuge/compact production lock source_revision drifted")
    catalog = document.get("catalog_fixture") or {}
    if (
        int(catalog.get("families") or 0) != CATALOG_FAMILY_COUNT
        or int(catalog.get("relations") or 0) != CATALOG_RELATION_COUNT
        or catalog.get("selection_sha256") != EXPECTED_SELECTION_SHA256
    ):
        raise ValueError("centrifuge/compact production lock catalog fixture contract drifted")
    production = document.get("production") or {}
    families = production.get("families") or []
    if not isinstance(families, list) or not families:
        raise ValueError("centrifuge/compact production lock has no production families")
    family_ids = [str(row.get("family_id") or "") for row in families]
    template_keys = [str(row.get("template_key") or "") for row in families]
    if any(not value for value in family_ids + template_keys):
        raise ValueError("centrifuge/compact production lock has an empty family identity")
    if len(set(family_ids)) != len(family_ids):
        raise ValueError("centrifuge/compact production lock has duplicate family ids")
    if len(set(template_keys)) != len(template_keys):
        raise ValueError("centrifuge/compact production lock has duplicate template keys")
    if family_ids != list(production.get("family_ids") or []):
        raise ValueError("centrifuge/compact production lock family_ids drifted from families")
    if template_keys != list(production.get("template_keys") or []):
        raise ValueError("centrifuge/compact production lock template_keys drifted from families")
    if int(production.get("family_count") or 0) != len(families):
        raise ValueError("centrifuge/compact production lock family_count drifted")
    relations = sum(int(row.get("expanded_count") or 0) for row in families)
    if int(production.get("relation_count") or 0) != relations:
        raise ValueError("centrifuge/compact production lock relation_count drifted")
    digest = selection_sha256(family_ids)
    if production.get("selection_sha256") != digest:
        raise ValueError("centrifuge/compact production lock selection_sha256 drifted")
    support = document.get("support") or {}
    route_keys = [str(value) for value in support.get("route_keys") or []]
    if route_keys != sorted(set(route_keys)):
        raise ValueError("centrifuge/compact production lock support route_keys are not unique/sorted")
    if int(support.get("route_count") or 0) != len(route_keys):
        raise ValueError("centrifuge/compact production lock support route_count drifted")
    deferred = document.get("phase_deferred") or []
    deferred_ids = [str(row.get("family_id") or "") for row in deferred]
    if len(deferred_ids) != len(set(deferred_ids)):
        raise ValueError("centrifuge/compact production lock phase_deferred has duplicates")
    if set(deferred_ids) & set(family_ids):
        raise ValueError("centrifuge/compact production and phase-deferred families overlap")
    return document


def production_lock_sha256() -> str:
    load_production_lock()
    return census.sha256_file(PRODUCTION_LOCK)


def production_families() -> list[dict[str, Any]]:
    return list((load_production_lock().get("production") or {}).get("families") or [])


def production_family_ids() -> list[str]:
    return [str(row["family_id"]) for row in production_families()]


def production_template_keys() -> list[str]:
    return [str(row["template_key"]) for row in production_families()]


def production_family_count() -> int:
    return len(production_families())


def production_relation_count() -> int:
    return sum(int(row["expanded_count"]) for row in production_families())


def production_group_counts() -> dict[str, dict[str, int]]:
    counts = {
        SINGLETON_GROUP: {"families": 0, "relations": 0},
        MULTI_GROUP: {"families": 0, "relations": 0},
    }
    for row in production_families():
        group = str(row["publication_group"])
        if group not in counts:
            raise ValueError(f"unexpected centrifuge/compact production group {group}")
        counts[group]["families"] += 1
        counts[group]["relations"] += int(row["expanded_count"])
    return counts


def production_group_map() -> dict[str, str]:
    return {
        str(row["family_id"]): str(row["publication_group"])
        for row in production_families()
    }


def phase_deferred_family_ids() -> list[str]:
    return [
        str(row["family_id"])
        for row in (load_production_lock().get("phase_deferred") or [])
    ]


def dump_present() -> bool:
    return CENTRIFUGE_DUMP.is_file()


def is_lossy_item_alias(item_id: str | None, meta: Any) -> bool:
    if not isinstance(item_id, str):
        return False
    try:
        meta_key = int(meta) if meta is not None else None
    except (TypeError, ValueError):
        return False
    if meta_key is None:
        return False
    return (item_id, meta_key) in LOSSY_ITEM_ALIASES


def canonical_family_id(template_key: str) -> str:
    return f"portfolio:track_a/cruciblecraft:centrifuge/{template_key}"


def family_fidelity_blockers(relations: list[dict[str, Any]]) -> list[str]:
    """Return source-fidelity blockers that keep a family out of production.

    Molten/chemical id translations stay eligible. Hosted/state item collapses
    and pahoehoe-lava flattening do not.
    """
    blockers: list[str] = []
    seen: set[str] = set()
    for relation in relations:
        for side in (
            "item_inputs",
            "item_outputs",
            "fluid_inputs",
            "fluid_outputs",
        ):
            for operand in relation.get(side) or []:
                if not isinstance(operand, dict):
                    continue
                source = operand.get("source") or {}
                fluid = source.get("fluid") or source.get("id") or ""
                if isinstance(fluid, str) and "pahoehoe" in fluid.lower():
                    key = f"pahoehoe_lava_collapse:{fluid}"
                    if key not in seen:
                        seen.add(key)
                        blockers.append(key)
                if source_operand_is_unproven_lossy_alias(operand):
                    name = (
                        source.get("item")
                        or source.get("fluid")
                        or operand.get("runtime_id")
                        or side
                    )
                    key = f"unproven_lossy_alias:{side}:{name}"
                    if key not in seen:
                        seen.add(key)
                        blockers.append(key)
    return blockers


def source_operand_is_unproven_lossy_alias(operand: dict[str, Any]) -> bool:
    """Return True when a frozen source consume is a hosted/state collapse.

    Molten/fluid id translations stay eligible for T21. Oil Sand, Black Sand,
    resin/comb/goo/egg, and depleted fuel-rod aliases do not.
    """
    source = operand.get("source") or {}
    if is_lossy_item_alias(source.get("item"), source.get("meta")):
        return True
    if operand.get("mapping") != "source_derived_alias":
        return False
    item_id = source.get("item")
    return isinstance(item_id, str) and item_id.startswith(
        ("gregtech:gt.meta.ore.", "gregtech:gt.block.sands",
         "gregtech:gt.multiitem.food", "gregtech:gt.multitileentity")
    )


def t14_opening_load(census_delta: dict[str, Any] | None = None) -> dict[str, Any]:
    document = census_delta if census_delta is not None else load_json(ROASTER_COMPACT_CENSUS_DELTA)
    closing = ((document.get("recipe_load_load") or {}).get("closing") or {})
    if not isinstance(closing, dict) or not closing:
        raise ValueError(f"missing {T14_OPENING_LOAD_SOURCE}")
    return dict(closing)


def publication_group_for_expanded_count(expanded_count: int) -> str:
    if expanded_count == 1:
        return SINGLETON_GROUP
    if expanded_count > 1:
        return MULTI_GROUP
    raise ValueError(f"expanded_count must be positive, got {expanded_count}")


def selection_payload(family_ids: list[str]) -> bytes:
    return ("".join(f"{family_id}\n" for family_id in family_ids)).encode("utf-8")


def selection_sha256(family_ids: list[str]) -> str:
    return hashlib.sha256(selection_payload(family_ids)).hexdigest()


def expanded_count_distribution(expanded_counts: list[int]) -> dict[str, int]:
    counts = Counter(expanded_counts)
    return {str(size): counts[size] for size in sorted(counts)}


def select_centrifuge_families(
    families_doc: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    families_doc = families_doc if families_doc is not None else load_json(RECIPE_FAMILIES)
    if families_doc.get("source_revision") != SOURCE_REVISION:
        raise ValueError("t35_recipe_families source_revision drifted")
    selected: list[dict[str, Any]] = []
    for row in families_doc.get("families") or []:
        if not isinstance(row, dict):
            continue
        if row.get("classification") != "ordinary_optional":
            continue
        if row.get("cc_host_map") != HOST:
            continue
        if row.get("membership_kind") != "semantic_template":
            continue
        selected.append(row)
    selected.sort(key=lambda row: str(row.get("template_key") or ""))
    return selected


def generated_family_files(scope: str = "production") -> list[Path]:
    root = GENERATED_ROOT if scope == "production" else CATALOG_FIXTURE_ROOT
    if scope not in {"production", "catalog"}:
        raise ValueError(f"unknown centrifuge/compact generated scope: {scope}")
    if not root.is_dir():
        return []
    return sorted(
        path
        for path in root.glob("gt_recipe_centrifuge_*.json")
        if path.is_file()
    )


def _file_sha256(path: Path) -> str | None:
    return census.sha256_file(path) if path.is_file() else None


def _tree_sha256(root: Path) -> str | None:
    if not root.is_dir():
        return None
    digest = hashlib.sha256()
    for path in sorted(child for child in root.rglob("*") if child.is_file()):
        relative_path = path.relative_to(root).as_posix()
        digest.update(relative_path.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def bound_gametest_artifacts() -> dict[str, str | None]:
    from tools import semantic_projection as _projection

    return {
        "gametest_java": _file_sha256(GAME_TEST_JAVA),
        "material_registration_gate_java": _file_sha256(MATERIAL_REGISTRATION_GATE_JAVA),
        "material_registration_gate_json": _projection.gate_semantic_root_sha256(),
        "production_lock": _file_sha256(PRODUCTION_LOCK),
        "centrifuge_generated_recipes": _tree_sha256(GENERATED_ROOT),
        "centrifuge_locked_support": _tree_sha256(LOCKED_SUPPORT_ROOT),
        "runtime_dependency_manifest": _file_sha256(RUNTIME_DEPENDENCY_MANIFEST),
        "publication_group_manifest": _file_sha256(PUBLICATION_GROUP_MANIFEST),
        "shard_manifest": _file_sha256(SHARD_MANIFEST),
    }


def read_gametest_log(path: Path) -> str:
    raw = path.read_bytes()
    if raw.startswith(b"\xff\xfe") or raw.startswith(b"\xfe\xff"):
        return raw.decode("utf-16")
    if raw.startswith(b"\xef\xbb\xbf"):
        return raw.decode("utf-8-sig")
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError:
        return raw.decode("utf-16")


def log_fingerprint(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def normalize_gametest_log_text(text: str) -> str:
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    if text and not text.endswith("\n"):
        text += "\n"
    return text


def commit_gametest_log(source: Path) -> str:
    text = normalize_gametest_log_text(read_gametest_log(source))
    GAME_TEST_EVIDENCE_LOG.parent.mkdir(parents=True, exist_ok=True)
    GAME_TEST_EVIDENCE_LOG.write_text(text, encoding="utf-8", newline="\n")
    return text


def discovered_centrifuge_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted({match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)})


GAME_TEST_REQUIRED = len(discovered_centrifuge_gametest_ids())
GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"


def player_gametest_source_present() -> bool:
    if not GAME_TEST_JAVA.is_file():
        return False
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    markers = ("cruciblecraft_wave_centrifuge_compact", "CentrifugeCompactGameTests", "-PwaveRecipes=centrifuge/compact")
    return any(marker in text for marker in markers)


def parse_gametest_log(text: str) -> dict[str, Any]:
    required = GAME_TEST_REQUIRED
    match = re.search(r"All (\d+) required tests passed", text)
    if match:
        passed = int(match.group(1))
        return {
            "failed": 0,
            "passed": passed,
            "required_tests": passed,
            "status": "PASS" if passed == required else "FAIL",
        }
    failed_match = re.search(r"(\d+) required tests? failed", text)
    complete_match = re.search(r"(\d+)\s+GAME TESTS COMPLETE", text)
    failed = int(failed_match.group(1)) if failed_match else (
        GAME_TEST_REQUIRED if "FAILED" in text else GAME_TEST_REQUIRED
    )
    total = int(complete_match.group(1)) if complete_match else GAME_TEST_REQUIRED
    passed = max(0, total - failed)
    return {
        "failed": failed,
        "passed": passed,
        "required_tests": required,
        "status": "FAIL",
    }


def gametest_receipt_document(parsed: dict[str, Any], log_path: Path) -> dict[str, Any]:
    test_ids = discovered_centrifuge_gametest_ids()
    log_text = normalize_gametest_log_text(read_gametest_log(log_path))
    document = {
        "bound_artifacts": bound_gametest_artifacts(),
        "command": GAME_TEST_COMMAND,
        "failed": int(parsed.get("failed") or 0),
        "java_sha256": census.sha256_file(GAME_TEST_JAVA) if GAME_TEST_JAVA.is_file() else None,
        "java_source": relative(GAME_TEST_JAVA),
        "log_fingerprint": log_fingerprint(log_text),
        "log_path": relative(GAME_TEST_EVIDENCE_LOG),
        "namespace": GAME_TEST_NAMESPACE,
        "note": (
            "Source markers are not a pass. This receipt is the isolated "
            "-PwaveRecipes=centrifuge/compact result bound to MaterialRegistrationGate, the centrifuge/compact "
            "recipe resource trees, and the committed UTF-8 evidence log. "
            "The gitignored run-centrifuge/compact-recipes directory is not evidence. "
            "Injecting recipe inputs still does not prove a player path."
        ),
        "pass_marker": GAME_TEST_PASS_MARKER,
        "passed": int(parsed.get("passed") or 0),
        "required_tests": int(parsed.get("required_tests") or GAME_TEST_REQUIRED),
        "schema_version": GAME_TEST_RECEIPT_SCHEMA,
        "skip_is_not_pass": True,
        "status": parsed.get("status") or "FAIL",
        "test_ids": test_ids,
    }
    return gametest_receipt_roots.attach_receipt_roots(
        document,
        bound=document["bound_artifacts"],
        test_ids=test_ids,
        parsed=parsed,
        command=GAME_TEST_COMMAND,
        log_path=relative(GAME_TEST_EVIDENCE_LOG),
        lock_sha256=production_lock_sha256() if PRODUCTION_LOCK.is_file() else None,
        equivalence_path=EQUIVALENCE,
        log_fingerprint_value=document["log_fingerprint"],
    )


def gametest_receipt_errors(document: dict[str, Any] | None = None) -> list[str]:
    if document is None and not GAME_TEST_RECEIPT.is_file():
        return ["missing GameTest receipt: tools/centrifuge_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_centrifuge_gametest_ids()
    java_hash = census.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("centrifuge/compact GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("centrifuge/compact GameTest receipt command is missing -PwaveRecipes=centrifuge/compact")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("centrifuge/compact GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("centrifuge/compact GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(
            f"centrifuge/compact GameTest receipt passed count is not {GAME_TEST_REQUIRED}"
        )
    if receipt.get("failed") != 0:
        errors.append("centrifuge/compact GameTest receipt records failures")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("centrifuge/compact GameTest receipt pass marker drifted")
    if list(receipt.get("test_ids") or []) != test_ids:
        errors.append("centrifuge/compact GameTest receipt test_ids do not match Java @GameTest methods")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"centrifuge/compact GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("java_sha256") != java_hash:
        errors.append("centrifuge/compact GameTest receipt java_sha256 does not match current source")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("centrifuge/compact GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("centrifuge/compact GameTest receipt schema_version must be 3")
    if not player_gametest_source_present():
        errors.append("centrifuge/compact GameTest Java source markers are missing")
    bound = receipt.get("bound_artifacts") or {}
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "centrifuge/compact", bound, bound_gametest_artifacts()
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "centrifuge/compact GameTest receipt log_path must be the committed evidence file "
            "tools/centrifuge_gametest.log"
        )
    log_path = GAME_TEST_EVIDENCE_LOG
    if not log_path.is_file():
        errors.append(
            "centrifuge/compact GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(read_gametest_log(log_path))
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "centrifuge/compact GameTest receipt log_fingerprint does not match the committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("centrifuge/compact committed GameTest evidence log is not PASS")
    return errors


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "centrifuge/compact",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def load_centrifuge_frozen_family_ids(
    work_set: dict[str, Any] | None = None,
) -> list[str]:
    """Load only the issued centrifuge/compact family ids from the frozen work set."""
    work_set = work_set if work_set is not None else load_json(WORK_SET)
    if work_set.get("source_revision") != SOURCE_REVISION:
        raise ValueError("centrifuge/compact work set source_revision drifted")
    if work_set.get("host") != HOST:
        raise ValueError(f"centrifuge/compact work set host must be {HOST}")
    family_ids = [str(row["family_id"]) for row in work_set.get("families") or []]
    if len(family_ids) != FAMILY_COUNT:
        raise ValueError(
            f"centrifuge/compact work set family_ids must be {FAMILY_COUNT}, got {len(family_ids)}"
        )
    if len(set(family_ids)) != FAMILY_COUNT:
        raise ValueError("centrifuge/compact work set has duplicate family_ids")
    return family_ids


def work_set_group_map(
    work_set: dict[str, Any] | None = None,
) -> dict[str, str]:
    work_set = work_set if work_set is not None else load_json(WORK_SET)
    return {
        str(row["family_id"]): str(row["publication_group"])
        for row in work_set.get("families") or []
    }


def support_recipe_ledger() -> dict[str, Any]:
    recovery_files = sorted(
        path for path in LOCKED_SUPPORT_ROOT.glob("*.json") if path.is_file()
    )
    gt_recovery: list[str] = []
    crafting: list[str] = []
    for path in recovery_files:
        document = load_json(path)
        recipe_type = str(document.get("type") or "")
        relative_path = relative(path)
        if recipe_type == "minecraft:crafting_shapeless":
            crafting.append(relative_path)
        elif recipe_type == "cruciblecraft:gt_recipe":
            gt_recovery.append(relative_path)
        else:
            raise ValueError(
                f"unexpected centrifuge/compact recovery recipe type {recipe_type!r} in {relative_path}"
            )
    return {
        "gt_recovery": gt_recovery,
        "crafting": crafting,
        "gt_recovery_count": len(gt_recovery),
        "crafting_count": len(crafting),
        "authored": len(gt_recovery) + len(crafting),
        "eager": len(gt_recovery),
    }


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    group_winners = {
        key: nested.get("group_winners", {}).get(key)
        for key in ("singleton", "multi")
    }
    card_winner = nested.get("card_aggregate_winner")
    blocked = (
        decision.get("status") == "CENTRIFUGE_COMPACT_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or card_winner in {None, ""}
        or any(winner in {None, ""} for winner in group_winners.values())
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_centrifuge_recipe_load_benchmark as benchmark

            benchmark.validate_artifact(decision)
            recomputable = True
        except (ValueError, KeyError, OSError, json.JSONDecodeError, FileNotFoundError):
            blocked = True
            group_winners = {"singleton": None, "multi": None}
            card_winner = None
    return {
        "blocked": blocked,
        "group_winners": group_winners,
        "card_aggregate_winner": None if blocked else str(card_winner),
        "status": nested.get("status") or decision.get("status"),
        "recomputable": recomputable,
    }


def partition_for_winner(
    winner: str | None,
    group: str = "card",
) -> dict[str, int | None]:
    policy = load_json(POLICY)
    contract = policy["publication_group_contract"]
    group_key = {
        "singleton": "singleton",
        "multi": "multi",
        "card": "card_aggregate",
    }.get(group)
    if group_key is None:
        return {
            "eager_publication_rows": None,
            "lazy_logical_rows": None,
            "lazy_cache_ceiling_rows": None,
        }
    partitions = contract[group_key]["partitions"]
    if winner not in partitions:
        return {
            "eager_publication_rows": None,
            "lazy_logical_rows": None,
            "lazy_cache_ceiling_rows": None,
        }
    eager, lazy, cache = partitions[winner]
    return {
        "eager_publication_rows": int(eager),
        "lazy_logical_rows": int(lazy),
        "lazy_cache_ceiling_rows": int(cache),
    }


def hybrid_eager_stable_ids(group: str) -> set[str]:
    policy = load_json(POLICY)
    boundary = policy["publication_group_contract"][group]["hybrid_boundary"]
    return {str(value) for value in (boundary.get("eager_stable_ids") or [])}
