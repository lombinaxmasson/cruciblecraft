#!/usr/bin/env python3
"""Shared paths and catalog/production helpers for assembler/wood Assembler bulk singletons."""
from __future__ import annotations

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

from tools import builder_cli
from tools import gametest_receipt_roots
from tools import census_common as census
from tools import centrifuge_common as centrifuge
from tools import electrolyzer_common as electrolyzer
from tools.build_assembler_source import VANILLA_RUNTIME_ALIASES as ASSEMBLER_COMPACT_VANILLA_ALIASES

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

HOST = "cruciblecraft:assembler"
SOURCE_MAP = "gt.recipe.assembler"
TARGET_MAP = "cruciblecraft:assembler"
OWNER = "portfolio:track_a/assembler_wood"
ASSEMBLER_MAP_INDEX = 9
ASSEMBLER_COMPACT_CLOSED_FAMILY_NUMBERS = range(2, 52)
ASSEMBLER_COMPACT_CLOSED_TEMPLATE_KEYS = tuple(
    f"gt.recipe.assembler#{index:04d}" for index in ASSEMBLER_COMPACT_CLOSED_FAMILY_NUMBERS
)

PLANKS_GROUP = "cruciblecraft:assembler/wood/planks"
FIREPROOF_GROUP = "cruciblecraft:assembler/wood/fireproof"
PLANKS2_GROUP = "cruciblecraft:assembler/wood/planks2"
PUBLICATION_GROUPS = (PLANKS_GROUP, FIREPROOF_GROUP, PLANKS2_GROUP)
PUBLICATION_GROUP_COUNT = 3
COMBINATORIAL_TEMPLATE_KEYS = (
    "gt.recipe.assembler#0000",
    "gt.recipe.assembler#0001",
)

GT_PLANKS = "gregtech:gt.block.planks"
GT_PLANKS_FIREPROOF = "gregtech:gt.block.planks.fireproof"
GT_PLANKS2 = "gregtech:gt.block.planks2"
GT_PLANKS2_FIREPROOF = "gregtech:gt.block.planks2.fireproof"
CIRCUIT_ITEM = "gregapi:gt.integrated_circuit"
PROGRAMMED_CIRCUIT = "cruciblecraft:programmed_circuit"
CIRCUIT_CONFIG = "cruciblecraft:circuit_config"
FIREPROOF_COMPONENT = "cruciblecraft:fireproof"
GT_WOOD_PREFIX = "cruciblecraft:gt_wood/"
GT_WOOD_SLUGS = (
    "blue_mahoe_planks",
    "blue_spruce_planks",
    "cinnamon_planks",
    "coconut_planks",
    "compressed_wood_planks",
    "crate",
    "dead_planks",
    "frozen_planks",
    "hazel_planks",
    "maple_planks",
    "mossy_planks",
    "rainbowood_planks",
    "rotten_planks",
    "rubberwood_planks",
    "willow_planks",
    "wood_planks",
)
COMBINATORIAL_CATALOG_GROUP = "catalog:combinatorial"
SINGLETON_CATALOG_GROUP = "catalog:singleton"

VANILLA_RUNTIME_ALIASES: dict[str, str] = dict(ASSEMBLER_COMPACT_VANILLA_ALIASES)
LEGACY_VANILLA_PLANKS: dict[tuple[str, int], str] = {
    ("minecraft:planks", 0): "minecraft:oak_planks",
    ("minecraft:planks", 1): "minecraft:spruce_planks",
    ("minecraft:planks", 2): "minecraft:birch_planks",
    ("minecraft:planks", 3): "minecraft:jungle_planks",
    ("minecraft:planks", 4): "minecraft:acacia_planks",
    ("minecraft:planks", 5): "minecraft:dark_oak_planks",
}
VANILLA_PLANK_DISPLAY: dict[str, str] = {
    "oak planks": "minecraft:oak_planks",
    "oak wood planks": "minecraft:oak_planks",
    "spruce planks": "minecraft:spruce_planks",
    "spruce wood planks": "minecraft:spruce_planks",
    "birch planks": "minecraft:birch_planks",
    "birch wood planks": "minecraft:birch_planks",
    "jungle planks": "minecraft:jungle_planks",
    "jungle wood planks": "minecraft:jungle_planks",
    "acacia planks": "minecraft:acacia_planks",
    "acacia wood planks": "minecraft:acacia_planks",
    "dark oak planks": "minecraft:dark_oak_planks",
    "dark oak wood planks": "minecraft:dark_oak_planks",
    "mangrove planks": "minecraft:mangrove_planks",
    "cherry planks": "minecraft:cherry_planks",
    "bamboo planks": "minecraft:bamboo_planks",
    "crimson planks": "minecraft:crimson_planks",
    "warped planks": "minecraft:warped_planks",
}

CATALOG_FAMILY_COUNT = 294
CATALOG_RELATION_COUNT = 1531
CATALOG_SINGLETON_FAMILIES = 292
CATALOG_SINGLETON_RELATIONS = 292
CATALOG_COMBINATORIAL_FAMILIES = 2
CATALOG_COMBINATORIAL_RELATIONS = 1239
EXPECTED_SELECTION_SHA256 = (
    "e36fe2d88257660075f03d4a89d7d2a68ec8dfe0cbc9dcb2927a049a3988d3d2"
)
EXPECTED_PRODUCTION_SELECTION_SHA256 = (
    "cb15dd7468ca56979f26f4686c6799173d4c556cd204473549b2d35c5e94e548"
)
EXPECTED_DISTRIBUTION = {1: 292, 619: 1, 620: 1}
ELECTROLYZER_COMPACT_REMAINING_ORDINARY_FAMILIES = 5597
EXPECTED_REMAINING_ORDINARY_FAMILIES = 5305
PRODUCTION_FAMILY_COUNT = 292
PRODUCTION_RELATION_COUNT = 292

CANDIDATE_SELECTION = TOOLS / "assembler_wood_candidate_selection.json"
PRODUCTION_SELECTION = CANDIDATE_SELECTION
PRODUCTION_LOCK = TOOLS / "assembler_wood_production_lock.json"
PRODUCTION_LOCK_SCHEMA = TOOLS / "assembler_wood_production_lock.schema.json"
OPERAND_DISPOSITION = TOOLS / "assembler_wood_operand_disposition.json"
WORK_SET = TOOLS / "assembler_wood_work_set.json"
SOURCE = TOOLS / "assembler_wood_source.json"
RECEIPT = TOOLS / "assembler_wood_source_receipt.json"
REVIEW = TOOLS / "assembler_wood_source_review.json"
SOURCE_PACK = TOOLS / "assembler_wood_source_pack_manifest.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "assembler_wood_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = TOOLS / "assembler_wood_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "assembler_wood_shard_manifest.json"
SCHEMA = TOOLS / "compact_recipe_family_source.schema.json"
GENERATED_ROOT = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "assembler"
    / "wood"
)
CATALOG_FIXTURE_ROOT = (
    ROOT
    / "src"
    / "test"
    / "resources"
    / "assembler_wood_catalog_fixture"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "assembler_wood_catalog"
    / "assembler"
)
OPERAND_RUNTIME_MAP = TOOLS / "assembler_wood_operand_runtime_map.json"
PLAYER_PATH = TOOLS / "assembler_wood_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "assembler_wood_layered_player_path.json"
PLAYER_PATH_GAPS = TOOLS / "assembler_wood_player_path_gaps.json"
EQUIVALENCE = TOOLS / "assembler_wood_equivalence.json"
REQUIRED_FORMS = TOOLS / "assembler_wood_required_forms.json"
PLAYER_PATH_SUPPORT = TOOLS / "assembler_wood_player_path_support.json"
RECIPE_FAMILIES = census.RECIPE_FAMILIES
T35_CENSUS = census.CENSUS
ELECTROLYZER_COMPACT_CENSUS_DELTA = electrolyzer.CENSUS_DELTA
ELECTROLYZER_COMPACT_READINESS = electrolyzer.READINESS
ELECTROLYZER_COMPACT_CARD_TOPOLOGY = electrolyzer.ELECTROLYZER_COMPACT_CARD_TOPOLOGY
POLICY = TOOLS / "assembler_wood_materialization_policy.json"
DECISION = TOOLS / "assembler_wood_materialization_decision.json"
MEASUREMENTS = TOOLS / "assembler_wood_materialization_measurements.json"
PUBLICATION_DELTA = TOOLS / "assembler_wood_publication_delta.json"
CENSUS_DELTA = TOOLS / "assembler_wood_census_delta.json"
ASSEMBLER_WOOD_CARD_TOPOLOGY = TOOLS / "assembler_wood_card_topology.json"
READINESS = TOOLS / "assembler_wood_readiness.json"
LOAD_PROJECTION_INPUT = TOOLS / "assembler_wood_load_projection_input.json"
LOAD_PROJECTION = TOOLS / "assembler_wood_load_projection.json"
GAME_TEST_ROOT = ROOT / "src/main/java/com/masson/cruciblecraft/gametest"
GAME_TEST_JAVA = GAME_TEST_ROOT / "AssemblerWoodGameTests.java"
GAME_TEST_RECEIPT = TOOLS / "assembler_wood_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "assembler_wood_gametest.log"
GAME_TEST_NAMESPACE = "cruciblecraft_wave_assembler_wood"
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=assembler/wood --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=assembler/wood"
GAME_TEST_RECEIPT_SCHEMA = 3
GAME_TEST_METHOD_RE = re.compile(
    r"public static void (\w+)\s*\(\s*GameTestHelper"
)
GAME_TEST_RUN_LOG_CANDIDATES = (
    ROOT / "run-assembler/wood-recipes" / "logs" / "latest.log",
    ROOT / "run-assembler/wood-recipes" / "logs" / "debug.log",
)
MATERIAL_REGISTRATION_GATE_JAVA = centrifuge.MATERIAL_REGISTRATION_GATE_JAVA
MATERIAL_REGISTRATION_GATE_JSON = centrifuge.MATERIAL_REGISTRATION_GATE_JSON
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src"
    / "assembler_wood_support_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "assembler_wood_player_path_support"
)
ASSEMBLER_DUMP = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.assembler.json"
)
ROW_CLASSIFICATION = TOOLS / "machine_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "chemical_axis_template_denominator.json"
T14_OPENING_LOAD_SOURCE = "tools/electrolyzer_readiness.json#assembler_wood_opening.recipe_load_closing"
T35_FOUNDATION = dict(electrolyzer.T35_FOUNDATION)
OPENING_DISPOSITION = "planned"
OPENING_CLOSURE = "incomplete"
OPENING_FIDELITY = "source_backed"
OPENING_LOAD = "pending"
CLOSING_DISPOSITION = "implemented"
CLOSING_CLOSURE = "closed"
CLOSING_LOAD = "measured"
T14_COUNTABLE = electrolyzer.T14_COUNTABLE
T14_PENDING = electrolyzer.T14_PENDING
STRATEGY_AXES = electrolyzer.STRATEGY_AXES
MAX_INTERVAL_AXES = electrolyzer.MAX_INTERVAL_AXES
PENDING_LOAD_VERDICT = electrolyzer.PENDING_LOAD_VERDICT
PUBLICATION_GROUP_KEYS = {
    PLANKS_GROUP: "planks",
    FIREPROOF_GROUP: "fireproof",
    PLANKS2_GROUP: "planks2",
}
PRIOR_COMPACT_ROOTS = (
    ROOT / "src/assembler_compact_recipe_generated/resources",
    ROOT / "src/roaster_recipe_generated/resources",
    ROOT / "src/centrifuge_recipe_generated/resources",
    ROOT / "src/electrolyzer_recipe_generated/resources",
)
ASSEMBLER_COMPACT_SUPPORT_ROOT = ROOT / "src/main/resources/data/cruciblecraft/recipe"
ROASTER_COMPACT_SUPPORT_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/recipe/roaster_player_path_recovery"
)
CENTRIFUGE_COMPACT_SUPPORT_ROOT = (
    ROOT
    / "src"
    / "centrifuge_support_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "centrifuge_player_path_support"
)
ELECTROLYZER_COMPACT_SUPPORT_ROOT = electrolyzer.LOCKED_SUPPORT_ROOT


def relative(path: Path) -> str:
    try:
        return census.relative(path)
    except ValueError:
        return path.as_posix()


def load_json(path: Path) -> Any:
    return census.load_json(path)


def parse_managed(description: str, argv: list[str] | None = None):
    return builder_cli.parse_managed(description, argv)


def parse_write_check(description: str, argv: list[str] | None = None):
    import argparse

    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    return args


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return census.check_generated_document(path, document)


def handle_rebind(args: Any, output: Path) -> bool:
    if not getattr(args, "rebind_currentness_only", False):
        return False
    from tools import currentness

    currentness.rebind_sidecar(output)
    print(f"rebound currentness sidecar for {relative(output)}")
    return True


def run_managed(
    description: str,
    output: Path,
    *,
    build,
    write=None,
    check=None,
    argv: list[str] | None = None,
) -> int:
    args = parse_managed(description, argv)
    if handle_rebind(args, output):
        return 0
    document = None
    if args.write:
        if write is not None:
            write()
        else:
            census.write_stable(output, build())
        print(f"Wrote {relative(output)}")
        return 0
    errors = check() if check is not None else check_document(output, build())
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"{relative(output)} is current")
    return 0


def canonical_family_id(template_key: str) -> str:
    return f"portfolio:track_a/cruciblecraft:assembler/{template_key}"


def family_number(template_key: str) -> int:
    return int(str(template_key).rsplit("#", 1)[-1])


def selection_payload(family_ids: list[str]) -> bytes:
    return ("".join(f"{family_id}\n" for family_id in family_ids)).encode("utf-8")


def selection_sha256(family_ids: list[str]) -> str:
    return hashlib.sha256(selection_payload(family_ids)).hexdigest()


def expanded_count_distribution(expanded_counts: list[int]) -> dict[str, int]:
    counts = Counter(expanded_counts)
    return {str(size): counts[size] for size in sorted(counts)}


def select_remaining_assembler_families(
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
        template_key = str(row.get("template_key") or "")
        if not template_key.startswith(f"{SOURCE_MAP}#"):
            continue
        if family_number(template_key) in ASSEMBLER_COMPACT_CLOSED_FAMILY_NUMBERS:
            continue
        selected.append(row)
    selected.sort(key=lambda row: str(row.get("template_key") or ""))
    return selected


def dump_present() -> bool:
    return ASSEMBLER_DUMP.is_file()


def family_fidelity_blockers(relations: list[dict[str, Any]]) -> list[str]:
    return centrifuge.family_fidelity_blockers(relations)


def source_operand_is_unproven_lossy_alias(operand: dict[str, Any]) -> bool:
    return centrifuge.source_operand_is_unproven_lossy_alias(operand)


def t14_opening_load(readiness: dict[str, Any] | None = None) -> dict[str, Any]:
    document = readiness if readiness is not None else load_json(ELECTROLYZER_COMPACT_READINESS)
    closing = ((document.get("assembler_wood_opening") or {}).get("recipe_load_closing") or {})
    if not isinstance(closing, dict) or not closing:
        raise ValueError(f"missing {T14_OPENING_LOAD_SOURCE}")
    return dict(closing)


def consume_plank_class(item_id: str | None) -> str | None:
    item = str(item_id or "")
    if not item:
        return None
    if item in {GT_PLANKS_FIREPROOF, GT_PLANKS2_FIREPROOF} or (
        "fireproof" in item and "plank" in item
    ):
        return FIREPROOF_GROUP
    if item == GT_PLANKS2 or ("planks2" in item and "fireproof" not in item):
        return PLANKS2_GROUP
    if item == GT_PLANKS:
        return PLANKS_GROUP
    if item == "minecraft:planks" or (
        item.startswith("minecraft:") and "plank" in item
    ):
        return PLANKS2_GROUP
    return None


def publication_group_for_relation(relation: dict[str, Any]) -> str:
    classes: list[str] = []
    inputs = relation.get("item_inputs") or []
    counts = relation.get("item_input_counts") or []
    actions = relation.get("item_input_actions") or []
    for operand, count, action in zip(inputs, counts, actions):
        kind = str((action or {}).get("kind") or "").lower()
        if kind not in {"consume", "CONSUME"} or int(count or 0) <= 0:
            continue
        source = (operand or {}).get("source") or {}
        classified = consume_plank_class(source.get("item"))
        if classified:
            classes.append(classified)
    if not classes:
        return PLANKS2_GROUP
    return Counter(classes).most_common(1)[0][0]


def publication_group_for_relations(relations: list[dict[str, Any]]) -> str:
    if not relations:
        raise ValueError("cannot classify an empty assembler/wood family")
    counts = Counter(publication_group_for_relation(row) for row in relations)
    return counts.most_common(1)[0][0]


def display_slug(display_name: str) -> str:
    text = str(display_name or "").strip().lower()
    text = re.sub(r"\s*\(fireproof\)\s*", " ", text)
    text = re.sub(r"[^a-z0-9]+", "_", text).strip("_")
    if not text:
        raise ValueError(f"cannot slugify display name {display_name!r}")
    return text


def is_fireproof_plank(item_id: str | None, display_name: str | None = None) -> bool:
    item = str(item_id or "")
    if "fireproof" in item:
        return True
    lowered = str(display_name or "").lower()
    return "fireproof" in lowered


def outputs_are_oak_products(outputs: list[dict[str, Any]]) -> bool:
    if not outputs:
        return False
    oakish = {
        "minecraft:chest",
        "minecraft:crafting_table",
        "minecraft:bookshelf",
        "minecraft:jukebox",
        "minecraft:note_block",
        "minecraft:noteblock",
        "minecraft:oak_button",
        "minecraft:wooden_button",
        "minecraft:oak_door",
        "minecraft:wooden_door",
        "minecraft:oak_trapdoor",
        "minecraft:trapdoor",
        "minecraft:oak_pressure_plate",
        "minecraft:wooden_pressure_plate",
    }
    for operand in outputs:
        source = (operand or {}).get("source") or {}
        item_id = str(
            operand.get("runtime_id")
            or operand.get("alias")
            or source.get("item")
            or ""
        )
        mapped = VANILLA_RUNTIME_ALIASES.get(item_id, item_id)
        if mapped not in oakish and not mapped.startswith("minecraft:oak_"):
            return False
    return True


def map_plank_runtime(
    *,
    item_id: str,
    meta: Any,
    display_name: str,
    outputs: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    fireproof = is_fireproof_plank(item_id, display_name)
    if isinstance(meta, int) and (item_id, meta) in LEGACY_VANILLA_PLANKS:
        runtime_id = LEGACY_VANILLA_PLANKS[(item_id, meta)]
        return {
            "class": "SOURCE_DERIVED",
            "component": FIREPROOF_COMPONENT if fireproof else None,
            "display_name": display_name,
            "fireproof": fireproof,
            "reason": (
                "1.7 minecraft:planks meta is a proven vanilla species alias."
            ),
            "runtime_id": runtime_id,
            "slug": display_slug(display_name),
        }
    stripped = re.sub(
        r"\s*\(fireproof\)\s*",
        "",
        str(display_name or ""),
        flags=re.IGNORECASE,
    ).strip().lower()
    vanilla = VANILLA_PLANK_DISPLAY.get(stripped)
    if vanilla is not None and not fireproof:
        reason = (
            "GT plank displayName matches a vanilla species; mapped without "
            "folding unmatched woods onto leftover vanilla twins."
        )
        klass = "SOURCE_DERIVED"
    elif vanilla is not None and fireproof:
        reason = (
            "Fireproof variant keeps the matched vanilla species and adds "
            f"{FIREPROOF_COMPONENT}=1 rather than collapsing onto a leftover twin."
        )
        klass = "SOURCE_DERIVED"
    else:
        slug = display_slug(display_name)
        vanilla = f"{GT_WOOD_PREFIX}{slug}"
        reason = (
            "No vanilla species match; SOURCE_BACKED to a deterministic "
            f"cruciblecraft:gt_wood/{slug} item."
        )
        klass = "SOURCE_BACKED"
    return {
        "class": klass,
        "component": FIREPROOF_COMPONENT if fireproof else None,
        "display_name": display_name,
        "fireproof": fireproof,
        "reason": reason,
        "runtime_id": vanilla,
        "slug": display_slug(display_name),
    }


def runtime_item_identity(item_id: str, *, fireproof: bool = False) -> str:
    if fireproof:
        return f"item:{item_id}+fireproof=1"
    return f"item:{item_id}"


def ingredient_identity(ingredient: dict[str, Any]) -> str:
    item_id = str(ingredient.get("item") or ingredient.get("items") or "")
    components = ingredient.get("components") or {}
    fireproof = bool(components.get(FIREPROOF_COMPONENT))
    return runtime_item_identity(item_id, fireproof=fireproof)


def assert_runtime_id(item_id: str | None, *, consume: bool = False) -> str:
    if not isinstance(item_id, str) or not item_id:
        if consume:
            raise ValueError("needs_current_expression: empty runtime_id")
        raise ValueError("missing runtime id")
    if not item_id.startswith(("minecraft:", "cruciblecraft:")):
        raise ValueError(
            "needs_current_expression: runtime_id must be minecraft: or "
            f"cruciblecraft:, got {item_id}"
        )
    return item_id


def load_production_lock(path: Path | None = None) -> dict[str, Any]:
    lock_path = path or PRODUCTION_LOCK
    document = load_json(lock_path)
    if document.get("schema_version") != 1:
        raise ValueError("assembler/wood production lock schema_version must be 1")
    if document.get("status") != "ASSEMBLER_WOOD_PRODUCTION_LOCKED":
        raise ValueError("assembler/wood production lock is not locked")
    if document.get("source_revision") != SOURCE_REVISION:
        raise ValueError("assembler/wood production lock source_revision drifted")
    catalog = document.get("catalog_fixture") or {}
    if (
        int(catalog.get("families") or 0) != CATALOG_FAMILY_COUNT
        or int(catalog.get("relations") or 0) != CATALOG_RELATION_COUNT
        or catalog.get("selection_sha256") != EXPECTED_SELECTION_SHA256
    ):
        raise ValueError("assembler/wood production lock catalog fixture contract drifted")
    production = document.get("production") or {}
    families = production.get("families") or []
    if not isinstance(families, list) or len(families) != PRODUCTION_FAMILY_COUNT:
        raise ValueError(
            f"assembler/wood production lock must contain {PRODUCTION_FAMILY_COUNT} families"
        )
    family_ids = [str(row.get("family_id") or "") for row in families]
    template_keys = [str(row.get("template_key") or "") for row in families]
    if any(not value for value in family_ids + template_keys):
        raise ValueError("assembler/wood production lock has an empty family identity")
    if len(set(family_ids)) != len(family_ids):
        raise ValueError("assembler/wood production lock has duplicate family ids")
    if len(set(template_keys)) != len(template_keys):
        raise ValueError("assembler/wood production lock has duplicate template keys")
    if family_ids != list(production.get("family_ids") or []):
        raise ValueError("assembler/wood production lock family_ids drifted from families")
    if template_keys != list(production.get("template_keys") or []):
        raise ValueError("assembler/wood production lock template_keys drifted from families")
    if int(production.get("family_count") or 0) != len(families):
        raise ValueError("assembler/wood production lock family_count drifted")
    relations = sum(int(row.get("expanded_count") or 0) for row in families)
    if int(production.get("relation_count") or 0) != relations:
        raise ValueError("assembler/wood production lock relation_count drifted")
    if relations != PRODUCTION_RELATION_COUNT:
        raise ValueError("assembler/wood production lock relation_count is not 292")
    digest = selection_sha256(family_ids)
    if production.get("selection_sha256") != digest:
        raise ValueError("assembler/wood production lock selection_sha256 drifted")
    if digest != EXPECTED_PRODUCTION_SELECTION_SHA256:
        raise ValueError("assembler/wood production lock selection_sha256 drifted from 292/292 contract")
    support = document.get("support") or {}
    route_keys = [str(value) for value in support.get("route_keys") or []]
    if route_keys != sorted(set(route_keys)):
        raise ValueError("assembler/wood production lock support route_keys are not unique/sorted")
    if int(support.get("route_count") or 0) != len(route_keys):
        raise ValueError("assembler/wood production lock support route_count drifted")
    deferred = document.get("phase_deferred") or []
    deferred_ids = [str(row.get("family_id") or "") for row in deferred]
    if len(deferred_ids) != len(set(deferred_ids)):
        raise ValueError("assembler/wood production lock phase_deferred has duplicates")
    if set(deferred_ids) & set(family_ids):
        raise ValueError("assembler/wood production and phase-deferred families overlap")
    if len(deferred) != 2:
        raise ValueError("assembler/wood production lock must defer exactly #0000/#0001")
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
        PLANKS_GROUP: {"families": 0, "relations": 0},
        FIREPROOF_GROUP: {"families": 0, "relations": 0},
        PLANKS2_GROUP: {"families": 0, "relations": 0},
    }
    for row in production_families():
        group = str(row["publication_group"])
        if group not in counts:
            raise ValueError(f"unexpected assembler/wood production group {group}")
        counts[group]["families"] += 1
        counts[group]["relations"] += int(row["expanded_count"])
    return {
        group: value
        for group, value in counts.items()
        if value["families"] or value["relations"]
    }


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


def generated_family_files(scope: str = "production") -> list[Path]:
    root = GENERATED_ROOT if scope == "production" else CATALOG_FIXTURE_ROOT
    if scope not in {"production", "catalog"}:
        raise ValueError(f"unknown assembler/wood generated scope: {scope}")
    if not root.is_dir():
        return []
    return sorted(
        path
        for path in root.glob("gt_recipe_assembler_*.json")
        if path.is_file()
    )


_RELATION_CORE_KEYS = (
    "item_inputs",
    "item_input_counts",
    "item_input_actions",
    "item_outputs",
    "fluid_inputs",
    "fluid_outputs",
    "output_chances",
    "duration",
    "eut",
    "special_value",
    "can_be_buffered",
)


def _relation_core(relation: dict[str, Any]) -> str:
    return json.dumps(
        {key: relation.get(key) for key in _RELATION_CORE_KEYS},
        sort_keys=True,
        separators=(",", ":"),
    )


def assembler_compact_expressed_assembler_wood_family_ids() -> list[str]:
    """assembler/wood leftover-vanilla rows whose consume/output/duration/eut already
    live on assembler/compact #0002-#0051. These cannot independently close the 292 lock.
    """
    from tools import assembler_compact_common as assembler

    assembler_compact_cores = {
        _relation_core(relation)
        for path in assembler.generated_family_files()
        for relation in (json.loads(path.read_text(encoding="utf-8")).get("relations") or [])
    }
    if not assembler_compact_cores:
        return []
    by_template = {
        str(row["template_key"]): str(row["family_id"])
        for row in production_families()
    }
    expressed: list[str] = []
    for path in generated_family_files():
        document = json.loads(path.read_text(encoding="utf-8"))
        template = str(document.get("family_id") or "")
        family_id = by_template.get(template)
        if family_id is None:
            continue
        if any(
            _relation_core(relation) in assembler_compact_cores
            for relation in (document.get("relations") or [])
        ):
            expressed.append(family_id)
    return expressed


def independent_live_family_count() -> int:
    return production_family_count() - len(assembler_compact_expressed_assembler_wood_family_ids())


GAME_TEST_LIVE_RELATIONS_RE = re.compile(
    r"ASSEMBLER_WOOD_LOCKED_RELATIONS\s*=\s*(\d+)"
)


def gametest_locked_live_count() -> int | None:
    if not GAME_TEST_JAVA.is_file():
        return None
    match = GAME_TEST_LIVE_RELATIONS_RE.search(
        GAME_TEST_JAVA.read_text(encoding="utf-8")
    )
    return int(match.group(1)) if match else None


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
        "assembler_wood_generated_recipes": _tree_sha256(GENERATED_ROOT),
        "assembler_wood_locked_support": _tree_sha256(LOCKED_SUPPORT_ROOT),
        "runtime_dependency_manifest": _file_sha256(RUNTIME_DEPENDENCY_MANIFEST),
        "publication_group_manifest": _file_sha256(PUBLICATION_GROUP_MANIFEST),
        "shard_manifest": _file_sha256(SHARD_MANIFEST),
    }


def read_gametest_log(path: Path) -> str:
    return electrolyzer.read_gametest_log(path)


def log_fingerprint(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def normalize_gametest_log_text(text: str) -> str:
    return electrolyzer.normalize_gametest_log_text(text)


def commit_gametest_log(source: Path) -> str:
    text = normalize_gametest_log_text(read_gametest_log(source))
    GAME_TEST_EVIDENCE_LOG.parent.mkdir(parents=True, exist_ok=True)
    GAME_TEST_EVIDENCE_LOG.write_text(text, encoding="utf-8", newline="\n")
    return text


def discovered_assembler_wood_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted(
        {match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)}
    )


GAME_TEST_REQUIRED = len(discovered_assembler_wood_gametest_ids())
GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"


def player_gametest_source_present() -> bool:
    if not GAME_TEST_JAVA.is_file():
        return False
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    markers = ("cruciblecraft_wave_assembler_wood", "AssemblerWoodGameTests", "-PwaveRecipes=assembler/wood")
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
    failed = int(failed_match.group(1)) if failed_match else GAME_TEST_REQUIRED
    total = int(complete_match.group(1)) if complete_match else GAME_TEST_REQUIRED
    passed = max(0, total - failed)
    return {
        "failed": failed,
        "passed": passed,
        "required_tests": required,
        "status": "FAIL",
    }


def gametest_receipt_document(parsed: dict[str, Any], log_path: Path) -> dict[str, Any]:
    test_ids = discovered_assembler_wood_gametest_ids()
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
            "-PwaveRecipes=assembler/wood result bound to MaterialRegistrationGate, the assembler/wood "
            "recipe resource trees, and the committed UTF-8 evidence log. "
            "The gitignored run-assembler/wood-recipes directory is not evidence. "
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
        return ["missing GameTest receipt: tools/assembler_wood_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_assembler_wood_gametest_ids()
    java_hash = census.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("assembler/wood GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("assembler/wood GameTest receipt command is missing -PwaveRecipes=assembler/wood")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("assembler/wood GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("assembler/wood GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(
            f"assembler/wood GameTest receipt passed count is not {GAME_TEST_REQUIRED}"
        )
    if receipt.get("failed") != 0:
        errors.append("assembler/wood GameTest receipt records failures")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("assembler/wood GameTest receipt pass marker drifted")
    if list(receipt.get("test_ids") or []) != test_ids:
        errors.append("assembler/wood GameTest receipt test_ids do not match Java @GameTest methods")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"assembler/wood GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("java_sha256") != java_hash:
        errors.append("assembler/wood GameTest receipt java_sha256 does not match current source")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("assembler/wood GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("assembler/wood GameTest receipt schema_version must be 3")
    if not player_gametest_source_present():
        errors.append("assembler/wood GameTest Java source markers are missing")
    bound = receipt.get("bound_artifacts") or {}
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "assembler/wood", bound, bound_gametest_artifacts()
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "assembler/wood GameTest receipt log_path must be the committed evidence file "
            "tools/assembler_wood_gametest.log"
        )
    log_path = GAME_TEST_EVIDENCE_LOG
    if not log_path.is_file():
        errors.append(
            "assembler/wood GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(read_gametest_log(log_path))
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "assembler/wood GameTest receipt log_fingerprint does not match the committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("assembler/wood committed GameTest evidence log is not PASS")
    return errors


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "assembler/wood",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def load_assembler_wood_catalog_family_ids(
    work_set: dict[str, Any] | None = None,
) -> list[str]:
    work_set = work_set if work_set is not None else load_json(WORK_SET)
    if work_set.get("source_revision") != SOURCE_REVISION:
        raise ValueError("assembler/wood work set source_revision drifted")
    if work_set.get("host") != HOST:
        raise ValueError(f"assembler/wood work set host must be {HOST}")
    family_ids = [str(row["family_id"]) for row in work_set.get("families") or []]
    if len(family_ids) != CATALOG_FAMILY_COUNT:
        raise ValueError(
            f"assembler/wood catalog family_ids must be {CATALOG_FAMILY_COUNT}, "
            f"got {len(family_ids)}"
        )
    if len(set(family_ids)) != CATALOG_FAMILY_COUNT:
        raise ValueError("assembler/wood work set has duplicate family_ids")
    return family_ids


def work_set_group_map(
    work_set: dict[str, Any] | None = None,
) -> dict[str, str | None]:
    work_set = work_set if work_set is not None else load_json(WORK_SET)
    return {
        str(row["family_id"]): row.get("publication_group")
        for row in work_set.get("families") or []
    }


def support_recipe_ledger() -> dict[str, Any]:
    if not LOCKED_SUPPORT_ROOT.is_dir():
        return {
            "gt_recovery": [],
            "crafting": [],
            "gt_recovery_count": 0,
            "crafting_count": 0,
            "authored": 0,
            "eager": 0,
        }
    files = sorted(
        path for path in LOCKED_SUPPORT_ROOT.glob("*.json") if path.is_file()
    )
    gt_recipes: list[str] = []
    crafting: list[str] = []
    for path in files:
        document = load_json(path)
        recipe_type = str(document.get("type") or "")
        relative_path = relative(path)
        if recipe_type == "minecraft:crafting_shapeless":
            crafting.append(relative_path)
        elif recipe_type == "cruciblecraft:gt_recipe":
            gt_recipes.append(relative_path)
        else:
            raise ValueError(
                f"unexpected assembler/wood support recipe type {recipe_type!r} in {relative_path}"
            )
    return {
        "gt_recovery": gt_recipes,
        "crafting": crafting,
        "gt_recovery_count": len(gt_recipes),
        "crafting_count": len(crafting),
        "authored": len(gt_recipes) + len(crafting),
        "eager": len(gt_recipes),
    }


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    owned_groups = tuple(production_group_counts()) if PRODUCTION_LOCK.is_file() else PUBLICATION_GROUPS
    group_winners = {
        PUBLICATION_GROUP_KEYS[group]: nested.get("group_winners", {}).get(
            PUBLICATION_GROUP_KEYS[group]
        )
        for group in owned_groups
        if group in PUBLICATION_GROUP_KEYS
    }
    card_winner = nested.get("card_aggregate_winner")
    blocked = (
        decision.get("status") == "ASSEMBLER_WOOD_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or card_winner in {None, ""}
        or any(winner in {None, ""} for winner in group_winners.values())
        or not group_winners
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_assembler_wood_recipe_load_benchmark as benchmark

            benchmark.validate_artifact(decision)
            recomputable = True
        except (ValueError, KeyError, OSError, json.JSONDecodeError, FileNotFoundError):
            blocked = True
            group_winners = {key: None for key in group_winners}
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
    empty = {
        "eager_publication_rows": None,
        "lazy_logical_rows": None,
        "lazy_cache_ceiling_rows": None,
    }
    if not POLICY.is_file() or winner is None:
        return empty
    policy = load_json(POLICY)
    contract = policy["publication_group_contract"]
    group_key = {
        "planks": "planks",
        "fireproof": "fireproof",
        "planks2": "planks2",
        "card": "card_aggregate",
        PLANKS_GROUP: "planks",
        FIREPROOF_GROUP: "fireproof",
        PLANKS2_GROUP: "planks2",
    }.get(group)
    if group_key is None or group_key not in contract:
        return empty
    partitions = contract[group_key]["partitions"]
    if winner not in partitions:
        return empty
    eager, lazy, cache = partitions[winner]
    return {
        "eager_publication_rows": int(eager),
        "lazy_logical_rows": int(lazy),
        "lazy_cache_ceiling_rows": int(cache),
    }


def hybrid_eager_stable_ids(group: str) -> set[str]:
    policy = load_json(POLICY)
    key = PUBLICATION_GROUP_KEYS.get(group, group)
    boundary = policy["publication_group_contract"][key]["hybrid_boundary"]
    return {str(value) for value in (boundary.get("eager_stable_ids") or [])}


def compact_generated_identities(roots: tuple[Path, ...] | None = None) -> set[str]:
    identities: set[str] = set()
    for root in roots or PRIOR_COMPACT_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*.json"):
            if not path.is_file():
                continue
            try:
                document = load_json(path)
            except (OSError, json.JSONDecodeError):
                continue
            for relation in document.get("relations") or []:
                if not isinstance(relation, dict):
                    continue
                for ingredient in relation.get("item_inputs") or []:
                    if isinstance(ingredient, dict):
                        item_id = ingredient.get("item") or ingredient.get("items")
                        if isinstance(item_id, str) and item_id:
                            identities.add(f"item:{item_id}")
                for stack in relation.get("item_outputs") or []:
                    if isinstance(stack, dict) and stack.get("id"):
                        identities.add(f"item:{stack['id']}")
                for stack in list(relation.get("fluid_inputs") or []) + list(
                    relation.get("fluid_outputs") or []
                ):
                    if isinstance(stack, dict) and stack.get("id"):
                        identities.add(f"fluid:{stack['id']}")
    return identities
