#!/usr/bin/env python3
"""Paths and status constants for the bath/mte Bath MTE exact-relation wave."""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import builder_cli
from tools import census_common as census
from tools import owner_partition_common as owner
from tools import owner_runtime_common as owner_runtime
from tools import storage_common as storage
from tools import block_object_common as block_object

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

OWNER = "portfolio:track_a/bath_mte_bath_mte"
OPENING_EXECUTION_GAP = 2697
OPENING_AUTHORED_ENTRIES = 4865
OPENING_EAGER_ROWS = 16659
OPENING_LAZY_ROWS = 3466
OPENING_CACHE_CEILING_ROWS = 174
T14_CLOSING_BASIS = "compact_hybrid_13_publication_groups"
EMPTY_TREE_SHA256 = hashlib.sha256().hexdigest()
HARNESS_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "BathMteHarnessTest.java"
)
JAVA_LOCKED_EQUIVALENCE_FIELDS = ("shadow_order", "source_mte_meta")
COMPLETION_DELTA = 803
RECLASSIFICATION_DELTA = 0
CLOSING_EXECUTION_GAP = 1894
EXPECTED_FAMILY_COUNT = 803
EXPECTED_RELATION_COUNT = 1517
EXPECTED_EXACT_FAMILIES = 172
EXPECTED_EXACT_MULTI_FAMILIES = 631
EXPECTED_MTE_METAS = 118
EXPECTED_FLUID_OVERLAY = 30
DEFERRED_RECYCLING_COUNT = 1817
ALL_BATH_FAMILIES = 1348
ALL_BATH_RELATIONS = 49411
POST_BATH_MTE_BATH_FAMILIES = 545
POST_BATH_MTE_BATH_RELATIONS = 47894
MIN_PRODUCTION_FAMILIES = 300
HOST = "cruciblecraft:bath"
TARGET_MAP = "cruciblecraft:bath"
SOURCE_MAP = "gt.recipe.bath"
BATH_MAP_INDEX = 4
PUBLICATION_GROUP = "cruciblecraft:bath/mte"
PUBLICATION_GROUPS = (PUBLICATION_GROUP,)
PRODUCTION_FAMILY_COUNT = EXPECTED_FAMILY_COUNT
PRODUCTION_RELATION_COUNT = EXPECTED_RELATION_COUNT
OWNER_TRACK = "recycling/evidence_needed"
BATH_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.bath.json"
HOST_CONFIG = {
    HOST: {
        "dump": BATH_DUMP,
        "group": PUBLICATION_GROUP,
        "target_map": TARGET_MAP,
    },
}
ORACLE_RELATIVE = storage.ORACLE_RELATIVE
ORACLE_ROOT = storage.ORACLE_ROOT
oracle_revision = storage.oracle_revision
oracle_dirty = storage.oracle_dirty
RUNTIME_PREFIX = "cruciblecraft:gt_mte/"
V1_LOAD_POLICY = TOOLS / "recipe_load_load_budget_policy.json"
V1_IDENTITY_LEDGER = TOOLS / "global_build_identity_ledger.json"
V1_IDENTITY_SCHEMA = TOOLS / "global_build_identity_ledger.schema.json"
V1_IDENTITY_CURRENTNESS = TOOLS / "global_build_identity_ledger.currentness.json"
V1_RUNTIME_MANIFEST = TOOLS / "compact_recipe_runtime_manifest.json"
V1_RUNTIME_SCHEMA = TOOLS / "compact_recipe_runtime_manifest.schema.json"
V1_RUNTIME_CURRENTNESS = TOOLS / "compact_recipe_runtime_manifest.currentness.json"
V1_CUTOVER_READINESS = TOOLS / "compact_recipe_manifest_cutover_readiness.json"
V1_COMPILE_READINESS = TOOLS / "unified_recipe_compile_readiness.json"
V1_SHADOW_READINESS = TOOLS / "unified_import_shadow_readiness.json"
V1_PRODUCTION_BASELINE = TOOLS / "recipe_wave_production_baseline.json"
V1_SHADOW_PARITY = TOOLS / "recipe_wave_shadow_parity.json"
BLOCK_OBJECT_READINESS = block_object.READINESS
STORAGE_LOCK_READINESS = storage.READINESS
SMELTER_STONE_READINESS = TOOLS / "smelter_stone_readiness.json"
T42_OWNER_READINESS = owner_runtime.READINESS
ASSEMBLER_WOOD_READINESS = TOOLS / "assembler_wood_readiness.json"
T22_5_FLUID_MAPPING = TOOLS / "machine_fluid_mapping.json"
RECIPE_FAMILIES = census.RECIPE_FAMILIES
MATERIAL_REGISTRATION_GATE_JAVA = (
    ROOT / "src/main/java/com/masson/cruciblecraft/material/MaterialRegistrationGate.java"
)
B0_FLUID_IDS = (
    "cruciblecraft:hemp_oil",
    "cruciblecraft:lin_oil",
    "cruciblecraft:molten_midasium",
    "cruciblecraft:nut_oil",
    "cruciblecraft:olive_oil",
    "cruciblecraft:seed_oil",
    "cruciblecraft:sunflower_oil",
)

HISTORY_FREEZE = TOOLS / "bath_mte_history_freeze_manifest.json"
LOAD_POLICY_V2 = TOOLS / "recipe_load_load_budget_policy.v2.json"
LOAD_POLICY_V2_SCHEMA = TOOLS / "recipe_load_load_budget_policy.v2.schema.json"
IDENTITY_LEDGER_V2 = TOOLS / "global_build_identity_ledger.v2.json"
IDENTITY_LEDGER_V2_SCHEMA = TOOLS / "global_build_identity_ledger.v2.schema.json"
IDENTITY_LEDGER_V2_CURRENTNESS = TOOLS / "global_build_identity_ledger.v2.currentness.json"
RUNTIME_MANIFEST_V2 = TOOLS / "compact_recipe_runtime_manifest.v2.json"
RUNTIME_MANIFEST_V2_SCHEMA = TOOLS / "compact_recipe_runtime_manifest.v2.schema.json"
RUNTIME_MANIFEST_V2_CURRENTNESS = TOOLS / "compact_recipe_runtime_manifest.v2.currentness.json"
FORWARD_V2_READINESS = TOOLS / "forward_recipe_authority_v2_readiness.json"
FORWARD_V2_READINESS_SCHEMA = TOOLS / "forward_recipe_authority_v2_readiness.schema.json"
FORWARD_V2_READINESS_CURRENTNESS = (
    TOOLS / "forward_recipe_authority_v2_readiness.currentness.json"
)
IDENTITY_DELTA = TOOLS / "bath_mte_identity_ledger_delta.json"
RUNTIME_DELTA = TOOLS / "bath_mte_runtime_manifest_delta.json"

WORK_SET = TOOLS / "bath_mte_work_set.json"
SOURCE_PACK = TOOLS / "bath_mte_source_pack_manifest.json"
SOURCE = TOOLS / "bath_mte_source.json"
RECEIPT = TOOLS / "bath_mte_source_receipt.json"
REVIEW = TOOLS / "bath_mte_source_review.json"
CANDIDATE_SELECTION = TOOLS / "bath_mte_candidate_selection.json"
MTE_CATALOG = TOOLS / "bath_mte_identity_catalog.json"
BUNDLED_MTE_CATALOG = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_mte_identity_catalog.json"
)
FLUID_MAPPING = TOOLS / "bath_mte_fluid_mapping.json"
RECYCLING_DISPOSITION = TOOLS / "bath_mte_recycling_disposition.json"
OPERAND_RUNTIME_MAP = TOOLS / "bath_mte_operand_runtime_map.json"
REQUIRED_FORMS = TOOLS / "bath_mte_required_forms.json"
COMPILE_SPEC = TOOLS / "bath_mte_recipe_compile_spec.json"
PRODUCTION_LOCK = TOOLS / "bath_mte_production_lock.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "bath_mte_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = TOOLS / "bath_mte_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "bath_mte_shard_manifest.json"
PLAYER_PATH = TOOLS / "bath_mte_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "bath_mte_layered_player_path.json"
PLAYER_PATH_SUPPORT = TOOLS / "bath_mte_player_path_support.json"
EQUIVALENCE = TOOLS / "bath_mte_equivalence.json"
COMPILE_REPORT = TOOLS / "bath_mte_compile_report.json"
ANALYZE_REPORT = TOOLS / "bath_mte_analyze_report.json"
POLICY = TOOLS / "bath_mte_materialization_policy.json"
MEASUREMENTS = TOOLS / "bath_mte_materialization_measurements.json"
DECISION = TOOLS / "bath_mte_materialization_decision.json"
PUBLICATION_DELTA = TOOLS / "bath_mte_publication_delta.json"
LOAD_PROJECTION = TOOLS / "bath_mte_load_projection.json"
LOAD_PROJECTION_INPUT = TOOLS / "bath_mte_load_projection_input.json"
GAME_TEST_RECEIPT = TOOLS / "bath_mte_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "bath_mte_gametest.log"
CENSUS_DELTA = TOOLS / "bath_mte_census_delta.json"
CARD_TOPOLOGY = TOOLS / "bath_mte_card_topology.json"
READINESS = TOOLS / "bath_mte_readiness.json"
SCHEMA = TOOLS / "compact_recipe_family_source.schema.json"
PUBLICATION_POLICY_SCHEMA = (
    ROOT
    / "src/main/resources/data/cruciblecraft/schema/compact_publication_policy.schema.json"
)
GENERATED_ROOT = (
    ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/mte"
)
PUBLICATION_POLICY_DATAPACK = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)
PUBLICATION_POLICY_DATAPACK_FILES = {
    PUBLICATION_GROUP: PUBLICATION_POLICY_DATAPACK / "bath_mte.json",
}
CATALOG_FIXTURE_ROOT = ROOT / "src/test/resources/bath_mte_catalog_fixture"
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/player_path_support/bath_mte"
)
LOCKED_SUPPORT_RECIPE_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/recipe/player_path_support/bath_mte"
)
SCATTER_CONFIGURED = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/configured_feature"
    / "bath_mte_scatter.json"
)
SCATTER_PLACED = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/placed_feature"
    / "bath_mte_scatter.json"
)
SCATTER_BIOME_MODIFIER = (
    ROOT
    / "src/main/resources/data/cruciblecraft/neoforge/biome_modifier"
    / "add_bath_mte_scatter.json"
)
SCATTER_CATALOG = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog"
    / "bath_mte_scatter.json"
)
SCATTER_ITEM_TAG = (
    ROOT
    / "src/main/resources/data/cruciblecraft/tags/item"
    / "bath_mte_items.json"
)
BUNDLED_FLUID_MAPPING = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_mte_fluid_mapping.json"
)
INTEGRATED_MEASUREMENTS = TOOLS / "bath_mte_integrated_measurements.json"
PLAYER_PATH_REAL_KIND = "layered_b1"
INTEGRATED_MEASUREMENT_SCENARIO = "integrated"
GAME_TEST_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/BathMteGameTests.java"
)
GAME_TEST_NAMESPACE = "cruciblecraft_wave_bath_mte"
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=bath/mte --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=bath/mte"
GAME_TEST_METHOD_RE = re.compile(r"public static void (\w+)\s*\(\s*GameTestHelper")
ROW_CLASSIFICATION = TOOLS / "machine_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "chemical_axis_template_denominator.json"
T21_REACHABILITY = TOOLS / "operand_reachability.json"
T42_OWNER_LOCK = owner_runtime.DISPOSITION_LOCK
T42_OWNER_OVERLAY = owner_runtime.TRACK_OVERLAY
T42_BLOCKER = owner.BLOCKER_OVERLAY
T42_SNAPSHOT = owner.FAMILY_OPERAND_SNAPSHOT
ORDINARY_CLASS = "ordinary_optional"
T14_COUNTABLE = block_object.T14_COUNTABLE
T14_PENDING = block_object.T14_PENDING
STRATEGY_AXES = block_object.STRATEGY_AXES
MAX_INTERVAL_AXES = block_object.MAX_INTERVAL_AXES
PENDING_LOAD_VERDICT = block_object.PENDING_LOAD_VERDICT
CACHE_CEILING = 24
HYBRID_CUTOFF = 0
GAME_TEST_RECEIPT_SCHEMA = 3
T35_FOUNDATION = dict(owner.T35_FOUNDATION)
PUBLICATION_GROUP_KEYS = {PUBLICATION_GROUP: "bath_mte"}

FROZEN_AUTHORITY_FILES = (
    V1_LOAD_POLICY,
    V1_IDENTITY_LEDGER,
    V1_IDENTITY_SCHEMA,
    V1_IDENTITY_CURRENTNESS,
    V1_RUNTIME_MANIFEST,
    V1_RUNTIME_SCHEMA,
    V1_RUNTIME_CURRENTNESS,
    V1_CUTOVER_READINESS,
    V1_COMPILE_READINESS,
    V1_SHADOW_READINESS,
    V1_PRODUCTION_BASELINE,
    V1_SHADOW_PARITY,
    BLOCK_OBJECT_READINESS,
    T22_5_FLUID_MAPPING,
)
FROZEN_GENERATED_ROOTS = (
    ROOT / "src/assembler_compact_recipe_generated",
    ROOT / "src/roaster_recipe_generated",
    ROOT / "src/centrifuge_recipe_generated",
    ROOT / "src/electrolyzer_recipe_generated",
    ROOT / "src/assembler_wood_recipe_generated",
    ROOT / "src/smelter_stone_recipe_generated",
    ROOT / "src/block_object_recipe_generated",
    ROOT / "src/compact_recipe_policy_generated",
)
FROZEN_CARD_GLOBS = (
    "tools/assembler_compact_*.json",
    "tools/roaster_*.json",
    "tools/centrifuge_*.json",
    "tools/electrolyzer_*.json",
    "tools/assembler_wood_*.json",
    "tools/owner_*.json",
    "tools/owner_runtime_*.json",
    "tools/smelter_stone_*.json",
    "tools/storage_*.json",
    "tools/block_object_*.json",
)


def relative(path: Path) -> str:
    try:
        return census.relative(path)
    except ValueError:
        return path.as_posix()


def load_json(path: Path) -> Any:
    return census.load_json(path)


def parse_managed(description: str, argv: list[str] | None = None):
    return builder_cli.parse_managed(description, argv)


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


def selection_sha256(family_ids: list[str]) -> str:
    payload = "".join(f"{family_id}\n" for family_id in family_ids).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def tree_sha256(root: Path) -> str:
    hasher = hashlib.sha256()
    if not root.exists():
        return hasher.hexdigest()
    if root.is_file():
        hasher.update(root.read_bytes())
        return hasher.hexdigest()
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        rel = path.relative_to(root).as_posix().encode("utf-8")
        hasher.update(rel)
        hasher.update(path.read_bytes())
    return hasher.hexdigest()


def _tree_sha256(root: Path) -> str | None:
    if not root.exists():
        return None
    return tree_sha256(root)


def production_stable_ids() -> list[str]:
    return list((load_production_lock().get("production") or {}).get("stable_ids") or [])


def assert_runtime_id(value: Any, *, consume: bool) -> str:
    runtime = str(value or "")
    if not runtime.startswith("minecraft:") and not runtime.startswith("cruciblecraft:"):
        raise ValueError(f"bath/mte runtime id must be minecraft: or cruciblecraft:: {runtime}")
    if consume and runtime.startswith(("gregtech:", "gregapi:")):
        raise ValueError(f"bath/mte consume runtime leaked a source id: {runtime}")
    return runtime


def mte_registry_path(source_item: str, meta: int) -> str:
    from tools import catalog_modern_ids as modern

    return modern.registry_path_for(source_item, meta)


def mte_runtime_id(source_item: str, meta: int) -> str:
    return f"cruciblecraft:{mte_registry_path(source_item, meta)}"


def overlay_bath_mte_families() -> list[dict[str, Any]]:
    overlay = load_json(T42_OWNER_OVERLAY)
    blocker = load_json(T42_BLOCKER)
    if overlay.get("source_revision") != SOURCE_REVISION:
        raise ValueError("owner overlay source_revision drifted")
    by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    selected: list[dict[str, Any]] = []
    for row in overlay.get("families") or []:
        if str(row.get("host") or "") != HOST:
            continue
        if str(row.get("current_owner") or "") != OWNER_TRACK:
            continue
        family_id = str(row.get("family_id") or "")
        kinds = list((by_id.get(family_id) or {}).get("unique_kinds") or [])
        if kinds != ["mte"]:
            continue
        selected.append(row)
    selected.sort(key=lambda row: str(row["family_id"]))
    return selected


def load_production_lock() -> dict[str, Any]:
    if not PRODUCTION_LOCK.is_file():
        raise FileNotFoundError("bath/mte production lock is missing")
    document = load_json(PRODUCTION_LOCK)
    if document.get("status") != "BATH_MTE_PRODUCTION_LOCKED":
        raise ValueError("bath/mte production lock is not locked")
    return document


def production_lock_sha256() -> str:
    return census.sha256_file(PRODUCTION_LOCK)


def generated_family_files() -> list[Path]:
    if not GENERATED_ROOT.is_dir():
        return []
    return sorted(
        path
        for path in GENERATED_ROOT.rglob("gt_recipe_*.json")
        if path.is_file()
    )


def dump_present() -> bool:
    return BATH_DUMP.is_file()


def discovered_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted(
        {match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)}
    )


def opening_from_t45() -> dict[str, Any]:
    document = load_json(BLOCK_OBJECT_READINESS)
    evidence = document.get("evidence") or {}
    if evidence.get("remaining_recipe_gap") != OPENING_EXECUTION_GAP:
        raise ValueError("bath/mte opening gap is not block/object remaining_recipe_gap 2697")
    load = evidence.get("load") or {}
    if load.get("authored_closing") != OPENING_AUTHORED_ENTRIES:
        raise ValueError("bath/mte opening authored is not block/object authored_closing 4865")
    return {
        "authored_entries": OPENING_AUTHORED_ENTRIES,
        "eager_rows": OPENING_EAGER_ROWS,
        "execution_gap": OPENING_EXECUTION_GAP,
        "lazy_rows": OPENING_LAZY_ROWS,
        "cache_ceiling_rows": OPENING_CACHE_CEILING_ROWS,
    }


def t14_opening_load() -> dict[str, Any]:
    census = load_json(block_object.CENSUS_DELTA)
    t14 = census.get("recipe_load_load") or {}
    closing = dict(t14.get("closing") or {})
    if not closing:
        closing = {
            str(row["axis"]): row.get("closing")
            for row in t14.get("axes") or []
            if isinstance(row, dict) and row.get("axis")
        }
    if not closing:
        raise ValueError("missing tools/block_object_census_delta.json#recipe_load.closing")
    return closing


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    raw_winners = nested.get("group_winners") or {}
    winner = (
        raw_winners.get("bath_mte")
        or raw_winners.get(PUBLICATION_GROUP)
        or nested.get("card_aggregate_winner")
        or decision.get("winner")
    )
    blocked = (
        decision.get("status") == "BATH_MTE_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or winner in {None, "", "BLOCKED"}
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_bath_mte_recipe_load_benchmark as benchmark

            benchmark.validate_artifact(decision)
            recomputable = True
        except Exception:
            recomputable = False
            blocked = True
    return {
        "blocked": blocked,
        "card_aggregate_winner": None if blocked else winner,
        "group_winners": {
            "bath_mte": None if blocked else winner,
            PUBLICATION_GROUP: None if blocked else winner,
        },
        "recomputable": recomputable,
    }


def partition_for_winner(winner: str, group_key: str) -> dict[str, int]:
    if winner == "immediate":
        return {
            "eager_publication_rows": EXPECTED_RELATION_COUNT,
            "lazy_logical_rows": 0,
            "lazy_cache_ceiling_rows": 0,
        }
    if winner == "on_demand":
        return {
            "eager_publication_rows": 0,
            "lazy_logical_rows": EXPECTED_RELATION_COUNT,
            "lazy_cache_ceiling_rows": CACHE_CEILING,
        }
    if winner == "hybrid":
        return {
            "eager_publication_rows": HYBRID_CUTOFF,
            "lazy_logical_rows": EXPECTED_RELATION_COUNT - HYBRID_CUTOFF,
            "lazy_cache_ceiling_rows": CACHE_CEILING,
        }
    raise ValueError(f"unknown bath/mte winner {winner} for {group_key}")


def b0_identities() -> set[str]:
    from tools import bath_mte_identities as identities

    identities_found = set(block_object.b0_identities())
    if block_object.LAYERED_PLAYER_PATH.is_file():
        layered = load_json(block_object.LAYERED_PLAYER_PATH)
        identities_found.update((layered.get("b1") or {}).get("added_identities") or [])
        identities_found.update((layered.get("b2") or {}).get("added_identities") or [])
    identities_found.update(identities.VANILLA_OVERWORLD_HARVESTABLES)
    return identities_found


def support_recipe_files() -> list[Path]:
    if not LOCKED_SUPPORT_RECIPE_ROOT.is_dir():
        return []
    return sorted(
        path
        for path in LOCKED_SUPPORT_RECIPE_ROOT.glob("*.json")
        if path.is_file()
    )


def scatter_datapack_present() -> bool:
    return all(
        path.is_file()
        for path in (
            SCATTER_CONFIGURED,
            SCATTER_PLACED,
            SCATTER_BIOME_MODIFIER,
            SCATTER_CATALOG,
            SCATTER_ITEM_TAG,
        )
    )


def player_path_real() -> bool:
    if not locked_support_tree_current():
        return False
    if not PLAYER_PATH_SUPPORT.is_file() or not scatter_datapack_present():
        return False
    support = load_json(PLAYER_PATH_SUPPORT)
    if support.get("kind") != PLAYER_PATH_REAL_KIND:
        return False
    routes = list(support.get("routes") or [])
    if not routes:
        return False
    recipes = support_recipe_files()
    fluid_routes = [row for row in routes if str(row.get("kind")) == "mixer_gt_recipe"]
    item_routes = [row for row in routes if str(row.get("kind")) == "worldgen_drop"]
    if len(recipes) != len(fluid_routes) or not item_routes:
        return False
    for row in routes:
        inputs = [str(value) for value in (row.get("input_identities") or [])]
        if any(value.endswith(":iron_ingot") for value in inputs):
            return False
        if str(row.get("kind")) == "worldgen_drop" and inputs:
            return False
        if str(row.get("kind")) == "mixer_gt_recipe":
            if not any(value.startswith("item:") for value in inputs):
                return False
            if inputs == ["fluid:minecraft:water"]:
                return False
        if str(row.get("kind")) in {"b1_declaration", "b1_support", "declared_support"}:
            return False
    return True


def equivalence_fields_locked() -> bool:
    if not EQUIVALENCE.is_file():
        return False
    document = load_json(EQUIVALENCE)
    fields = document.get("locked_fields") or []
    rows = document.get("relations") or []
    return (
        document.get("status") == "BATH_MTE_EQUIVALENCE"
        and document.get("mutation_sensitive") is True
        and "stable_id" in fields
        and "fluid_inputs" in fields
        and "item_inputs" in fields
        and "shadow_order" in fields
        and "source_mte_meta" in fields
        and len(rows) == production_relation_count()
    )


def java_locked_fields_asserted() -> bool:
    if not HARNESS_JAVA.is_file():
        return False
    text = HARNESS_JAVA.read_text(encoding="utf-8")
    return all(field in text for field in JAVA_LOCKED_EQUIVALENCE_FIELDS)


def locked_support_tree_sha256() -> str:
    if not LOCKED_SUPPORT_RECIPE_ROOT.is_dir() or not support_recipe_files():
        return EMPTY_TREE_SHA256
    return tree_sha256(LOCKED_SUPPORT_RECIPE_ROOT)


def locked_support_tree_current() -> bool:
    digest = locked_support_tree_sha256()
    return digest != EMPTY_TREE_SHA256 and bool(support_recipe_files())


def integrated_load_measured() -> bool:
    if not INTEGRATED_MEASUREMENTS.is_file():
        return False
    document = load_json(INTEGRATED_MEASUREMENTS)
    if document.get("status") != "BATH_MTE_INTEGRATED_MEASUREMENT_READY":
        return False
    if int(document.get("group_count") or 0) != 13:
        return False
    candidates = document.get("candidates") or []
    return any(
        row.get("candidate") == "hybrid"
        and row.get("status") == "PASS"
        and int(row.get("eager_publication_rows") or -1) >= 0
        and int(row.get("lazy_logical_rows") or 0) > 0
        for row in candidates
        if isinstance(row, dict)
    )


def integrated_winner_row() -> dict[str, Any] | None:
    if not integrated_load_measured():
        return None
    strategy = production_strategy()
    if strategy["blocked"]:
        return None
    winner = strategy.get("card_aggregate_winner")
    document = load_json(INTEGRATED_MEASUREMENTS)
    for row in document.get("candidates") or []:
        if isinstance(row, dict) and row.get("candidate") == winner:
            return row
    return None


def parse_write_check(description: str, argv: list[str] | None = None):
    import argparse

    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    return args


def production_family_count() -> int:
    return int(load_production_lock()["production"]["family_count"])


def production_relation_count() -> int:
    return int(load_production_lock()["production"]["relation_count"])


def production_families() -> list[dict[str, Any]]:
    return list((load_production_lock().get("production") or {}).get("families") or [])


def production_family_ids() -> list[str]:
    return [str(row["family_id"]) for row in production_families()]


def membership_root_sha256(family_ids: list[str], stable_ids: list[str]) -> str:
    payload = "".join(f"{family_id}\n" for family_id in family_ids)
    payload += "".join(f"{stable_id}\n" for stable_id in stable_ids)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _file_sha256(path: Path) -> str | None:
    if not path.is_file():
        return None
    return census.sha256_file(path)


def bound_gametest_artifacts() -> dict[str, str | None]:
    from tools import semantic_projection as _projection

    return {
        "gametest_java": _file_sha256(GAME_TEST_JAVA),
        "material_registration_gate_java": _file_sha256(MATERIAL_REGISTRATION_GATE_JAVA),
        "material_registration_gate_json": _projection.gate_semantic_root_sha256(),
        "production_lock": _file_sha256(PRODUCTION_LOCK),
        "bath_mte_generated_recipes": tree_sha256(GENERATED_ROOT) if GENERATED_ROOT.exists() else None,
        "bath_mte_locked_support": locked_support_tree_sha256()
        if LOCKED_SUPPORT_RECIPE_ROOT.exists()
        else None,
        "runtime_dependency_manifest": _file_sha256(RUNTIME_DEPENDENCY_MANIFEST),
        "publication_group_manifest": _file_sha256(PUBLICATION_GROUP_MANIFEST),
        "shard_manifest": _file_sha256(SHARD_MANIFEST),
    }


def player_gametest_source_present() -> bool:
    if not GAME_TEST_JAVA.is_file():
        return False
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return any(
        marker in text
        for marker in ("cruciblecraft_wave_bath_mte", "BathMteGameTests", "-PwaveRecipes=bath/mte")
    )


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "bath/mte",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def authored_family_path(family: dict[str, Any]) -> Path:
    template = str(family.get("template_key") or family.get("source_template") or "")
    filename = template.replace(".", "_").replace("#", "_") + ".json"
    return GENERATED_ROOT / "bath" / filename


def hybrid_eager_stable_ids(group_key: str = "bath_mte") -> list[str]:
    if not POLICY.is_file():
        return []
    policy = load_json(POLICY)
    contract = (policy.get("publication_group_contract") or {}).get(group_key) or {}
    return list(
        ((contract.get("hybrid_boundary") or {}).get("eager_stable_ids")) or []
    )


GAME_TEST_REQUIRED = 0
GAME_TEST_PASS_MARKER = "All 0 required tests passed :)"
T14_HARD_AUTHORED = 6600
EXPECTED_REMAINING_ORDINARY_FAMILIES = CLOSING_EXECUTION_GAP
T14_OPENING_LOAD_SOURCE = "tools/block_object_census_delta.json#recipe_load.closing"
OPENING_DISPOSITION = "planned"
CLOSING_DISPOSITION = "implemented"
OPENING_CLOSURE = "pending"
CLOSING_CLOSURE = "closed"
OPENING_FIDELITY = "recorded"
OPENING_LOAD = "pending"
CLOSING_LOAD = "measured"
BUNDLED_FLUID_MAPPING = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_mte_fluid_mapping.json"
)
BLOCK_OBJECT_CENSUS_DELTA = block_object.CENSUS_DELTA
BLOCK_OBJECT_CARD_TOPOLOGY = block_object.CARD_TOPOLOGY


def refresh_gametest_required() -> int:
    global GAME_TEST_REQUIRED, GAME_TEST_PASS_MARKER
    GAME_TEST_REQUIRED = len(discovered_gametest_ids())
    GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"
    return GAME_TEST_REQUIRED


def read_gametest_log(path: Path) -> str:
    from tools import smelter_stone_common as smelter_stone

    return smelter_stone.read_gametest_log(path)


def normalize_gametest_log_text(text: str) -> str:
    from tools import smelter_stone_common as smelter_stone

    return smelter_stone.normalize_gametest_log_text(text)


def log_fingerprint(text: str) -> str:
    return hashlib.sha256(normalize_gametest_log_text(text).encode("utf-8")).hexdigest()


def commit_gametest_log(source: Path) -> str:
    text = normalize_gametest_log_text(read_gametest_log(source))
    GAME_TEST_EVIDENCE_LOG.parent.mkdir(parents=True, exist_ok=True)
    GAME_TEST_EVIDENCE_LOG.write_text(text, encoding="utf-8", newline="\n")
    return text


def parse_gametest_log(text: str) -> dict[str, Any]:
    refresh_gametest_required()
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
    failed = int(failed_match.group(1)) if failed_match else required
    total = int(complete_match.group(1)) if complete_match else required
    passed = max(0, total - failed)
    return {
        "failed": failed,
        "passed": passed,
        "required_tests": required,
        "status": "FAIL",
    }


def gametest_receipt_document(parsed: dict[str, Any], log_path: Path) -> dict[str, Any]:
    from tools import gametest_receipt_roots

    refresh_gametest_required()
    test_ids = discovered_gametest_ids()
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
            "-PwaveRecipes=bath/mte result bound to the bath/mte production lock, recipe "
            "and support trees, and the committed UTF-8 evidence log. "
            "Composed identity/runtime v2 is a close-time snapshot, not a live pin."
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
    from tools import gametest_receipt_roots

    refresh_gametest_required()
    if document is None and not GAME_TEST_RECEIPT.is_file():
        return ["missing GameTest receipt: tools/bath_mte_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_gametest_ids()
    java_hash = census.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("bath/mte GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("bath/mte GameTest receipt command is missing -PwaveRecipes=bath/mte")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("bath/mte GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("bath/mte GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(f"bath/mte GameTest receipt passed count is not {GAME_TEST_REQUIRED}")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("bath/mte GameTest receipt pass_marker drifted")
    if receipt.get("java_sha256") != java_hash:
        errors.append("bath/mte GameTest Java sha256 drifted")
    if sorted(receipt.get("test_ids") or []) != test_ids:
        errors.append("bath/mte GameTest receipt test_ids drifted")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"bath/mte GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("failed") != 0:
        errors.append("bath/mte GameTest receipt records failures")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("bath/mte GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("bath/mte GameTest receipt schema_version must be 3")
    bound = receipt.get("bound_artifacts") or {}
    live_bound = bound_gametest_artifacts()
    support_hash = bound.get("bath_mte_locked_support")
    live_support = live_bound.get("bath_mte_locked_support")
    if support_hash in (None, EMPTY_TREE_SHA256):
        errors.append(
            "bath/mte GameTest bath_mte_locked_support is bound to an empty tree; "
            "real recipes are under recipe/bath_mte_player_path_support/"
        )
    if live_support in (None, EMPTY_TREE_SHA256):
        errors.append(
            "bath/mte live bath_mte_locked_support tree is empty; "
            "bind GameTest to recipe/bath_mte_player_path_support/"
        )
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "bath/mte",
            bound,
            live_bound,
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "bath/mte GameTest receipt log_path must be the committed evidence file "
            "tools/bath_mte_gametest.log"
        )
    if not GAME_TEST_EVIDENCE_LOG.is_file():
        errors.append(
            "bath/mte GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(
            read_gametest_log(GAME_TEST_EVIDENCE_LOG)
        )
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "bath/mte GameTest receipt log_fingerprint does not match the "
                "committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("bath/mte committed GameTest evidence log is not PASS")
    return errors


def v2_policy_hash() -> str:
    if not LOAD_POLICY_V2.is_file():
        raise FileNotFoundError("bath/mte forward-v2 load policy is missing")
    return census.sha256_file(LOAD_POLICY_V2)


refresh_gametest_required()
