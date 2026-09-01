#!/usr/bin/env python3
"""Paths and status constants for the T49 Bath recycling-correction remainder wave."""
from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import t35_common as t35
from tools import t46_common as t46
from tools import t47_common as t47
from tools import t48_common as t48

ROOT = t35.ROOT
TOOLS = t35.TOOLS
SOURCE_REVISION = t35.SOURCE_REVISION

OWNER = "portfolio:track_a/t49_bath_recycling_correction"
OPENING_EXECUTION_GAP = 1354
OPENING_AUTHORED_ENTRIES = 6263
OPENING_EAGER_ROWS = 14
OPENING_LAZY_ROWS = 50557
OPENING_CACHE_CEILING_ROWS = 781
OPENING_SYNC_BYTES = 5009445
OPENING_LOOKUP_CANDIDATES_P95 = 10
OPENING_LOOKUP_P95_NS = 93625
OPENING_SERVER_RELOAD_MS = 347
OPENING_CLIENT_RELOAD_MS = 357
OPENING_ALLOCATION_BYTES = 686579392
CACHE_CEILING = 128
HYBRID_CUTOFF = 16
COMPLETION_DELTA = 5
RECLASSIFICATION_DELTA = 0
CLOSING_EXECUTION_GAP = 1349
EXPECTED_FAMILY_COUNT = 5
EXPECTED_RELATION_COUNT = 95
EXPECTED_IDENTITY_COUNT = 0
EXPECTED_REMAINING_ORDINARY_FAMILIES = CLOSING_EXECUTION_GAP
T14_CLOSING_BASIS = "compact_production_19_publication_groups"
DEFERRED_RECYCLING_COUNT = 1817
CANDIDATE_FAMILY_COUNT = 5
CANDIDATE_RELATION_COUNT = 95
CANDIDATE_EXACT_FAMILIES = 0
CANDIDATE_EXACT_MULTI_FAMILIES = 5
EXCEPTION_KIND = "bath_host_remainder_recycling_correction"
CANDIDATE_FAMILY_CEILING = 5
CANDIDATE_RELATION_CEILING = 95
MIN_PRODUCTION_FAMILIES = 1
PADDING_HOSTS_FORBIDDEN = ("Mixer", "Smelter", "Centrifuge production")
CORRECTION_FAMILY_COUNT = 13
HOST = "cruciblecraft:bath"
TARGET_MAP = "cruciblecraft:bath"
SOURCE_MAP = "gt.recipe.bath"
CANDIDATE_PUBLICATION_GROUP = "cruciblecraft:t49_bath_remainder_candidate"
PUBLICATION_GROUP_EXACT_MULTI = "cruciblecraft:bath/tiny_purified/exact_multi"
PUBLICATION_GROUPS = (PUBLICATION_GROUP_EXACT_MULTI,)
T46_PUBLICATION_GROUP = t46.PUBLICATION_GROUP
T47_PUBLICATION_GROUP_EXACT = t47.PUBLICATION_GROUP_EXACT
T47_PUBLICATION_GROUP_EXACT_MULTI = t47.PUBLICATION_GROUP_EXACT_MULTI
T48_PUBLICATION_GROUP_EXACT = t48.PUBLICATION_GROUP_EXACT
T48_PUBLICATION_GROUP_EXACT_MULTI = t48.PUBLICATION_GROUP_EXACT_MULTI
T48_PUBLICATION_GROUP_TOOL_HEAD = t48.PUBLICATION_GROUP_TOOL_HEAD
FORBIDDEN_PUBLICATION_GROUPS = {
    T46_PUBLICATION_GROUP,
    T47_PUBLICATION_GROUP_EXACT,
    T47_PUBLICATION_GROUP_EXACT_MULTI,
    T48_PUBLICATION_GROUP_EXACT,
    T48_PUBLICATION_GROUP_EXACT_MULTI,
    T48_PUBLICATION_GROUP_TOOL_HEAD,
}
LOCK_FAMILY_IDS = (
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0072",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0098",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0107",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0110",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0188",
)
LAYERED_PLAYER_PATH_STATUS = "T49_LAYERED_PLAYER_PATH"
EMPTY_TREE_SHA256 = hashlib.sha256().hexdigest()
HARNESS_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CompactT49BathRemainderHarnessTest.java"
)
JAVA_LOCKED_EQUIVALENCE_FIELDS = ("shadow_order", "source_row_sha256")
PLAYER_PATH_REAL_KIND = "layered_b1"
GAME_TEST_RECEIPT_SCHEMA = 3

V1_LOAD_POLICY = t48.V1_LOAD_POLICY
LOAD_POLICY_V2 = t48.LOAD_POLICY_V2
FORWARD_V2_READINESS = t48.FORWARD_V2_READINESS
MATERIAL_REGISTRATION_GATE_JAVA = t48.MATERIAL_REGISTRATION_GATE_JAVA
RECIPE_FAMILIES = t48.RECIPE_FAMILIES
T42_BLOCKER = t48.T42_BLOCKER
T42_OWNER_LOCK = t48.T42_OWNER_LOCK
ORDINARY_CLASS = t48.ORDINARY_CLASS
T14_HARD_AUTHORED = t48.T14_HARD_AUTHORED
T14_OPENING_LOAD_SOURCE = "tools/t48_readiness.json#t48_opening.t14_closing"
OPENING_DISPOSITION = "planned"
CLOSING_DISPOSITION = "implemented"
OPENING_CLOSURE = "pending"
CLOSING_CLOSURE = "closed"
OPENING_FIDELITY = "recorded"
OPENING_LOAD = "pending"
CLOSING_LOAD = "measured"

SCHEMA = t48.SCHEMA
PUBLICATION_POLICY_SCHEMA = t48.PUBLICATION_POLICY_SCHEMA
T14_COUNTABLE = t48.T14_COUNTABLE
T14_PENDING = t48.T14_PENDING
STRATEGY_AXES = t48.STRATEGY_AXES
MAX_INTERVAL_AXES = t48.MAX_INTERVAL_AXES
PENDING_LOAD_VERDICT = t48.PENDING_LOAD_VERDICT
T35_FOUNDATION = dict(t48.T35_FOUNDATION)
T22_5_FLUID_MAPPING = t48.T22_5_FLUID_MAPPING
T46_FLUID_MAPPING = t48.T46_FLUID_MAPPING
T47_FLUID_MAPPING = t48.T47_FLUID_MAPPING
T48_FLUID_MAPPING = t48.FLUID_MAPPING
T46_READINESS = t48.T46_READINESS
T47_READINESS = t48.T47_READINESS
T45_READINESS = t48.T45_READINESS
T44_READINESS = t48.T44_READINESS
T43_READINESS = t48.T43_READINESS
T42_OWNER_READINESS = t48.T42_OWNER_READINESS
T41_READINESS = t48.T41_READINESS
T48_READINESS = t48.READINESS
T48_CENSUS_DELTA = t48.CENSUS_DELTA
T48_CARD_TOPOLOGY = t48.CARD_TOPOLOGY
T48_PRODUCTION_LOCK = t48.PRODUCTION_LOCK
T48_CLOSEOUT_SEAL = t48.CLOSEOUT_SEAL
T48_SOURCE = t48.SOURCE
T48_RECEIPT = t48.RECEIPT
T48_CANDIDATE_SELECTION = t48.CANDIDATE_SELECTION
T47_VR_REPAIR_READINESS = t48.T47_VR_REPAIR_READINESS
EXPECTED_REMAINING_BATH_ORDINARY = 0
T14_CLOSING_GROUP_COUNT = 19

WAVE_DIR = TOOLS / "waves" / "bath" / "tiny-purified"
WORK_SET = WAVE_DIR / "t49_work_set.json"
SOURCE_PACK = WAVE_DIR / "t49_bath_source_pack_manifest.json"
SOURCE = WAVE_DIR / "t49_bath_source.json"
RECEIPT = WAVE_DIR / "t49_bath_source_receipt.json"
REVIEW = WAVE_DIR / "t49_bath_source_review.json"
CANDIDATE_SELECTION = WAVE_DIR / "t49_bath_candidate_selection.json"
IDENTITY_CATALOG = WAVE_DIR / "t49_identity_catalog.json"
FLUID_MAPPING = WAVE_DIR / "t49_bath_fluid_mapping.json"
RECYCLING_DISPOSITION = WAVE_DIR / "t49_bath_recycling_disposition.json"
RECYCLING_CORRECTION = WAVE_DIR / "t49_recycling_candidate_correction.json"
OPERAND_RUNTIME_MAP = WAVE_DIR / "t49_operand_runtime_map.json"
REQUIRED_FORMS = WAVE_DIR / "t49_required_forms.json"
COMPILE_SPEC = WAVE_DIR / "t49_recipe_compile_spec.json"
PRODUCTION_LOCK = WAVE_DIR / "t49_production_lock.json"
IDENTITY_DELTA = WAVE_DIR / "t49_identity_ledger_delta.json"
RUNTIME_DELTA = WAVE_DIR / "t49_runtime_manifest_delta.json"
RUNTIME_DEPENDENCY_MANIFEST = WAVE_DIR / "t49_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = WAVE_DIR / "t49_publication_group_manifest.json"
SHARD_MANIFEST = WAVE_DIR / "t49_shard_manifest.json"
PLAYER_PATH = WAVE_DIR / "t49_player_path.json"
LAYERED_PLAYER_PATH = WAVE_DIR / "t49_layered_player_path.json"
PLAYER_PATH_SUPPORT = WAVE_DIR / "t49_player_path_support.json"
EQUIVALENCE = WAVE_DIR / "t49_equivalence.json"
COMPILE_REPORT = WAVE_DIR / "t49_compile_report.json"
POLICY = WAVE_DIR / "t49_materialization_policy.json"
MEASUREMENTS = WAVE_DIR / "t49_materialization_measurements.json"
INTEGRATED_MEASUREMENTS = WAVE_DIR / "t49_integrated_measurements.json"
DECISION = WAVE_DIR / "t49_materialization_decision.json"
PUBLICATION_DELTA = WAVE_DIR / "t49_publication_delta.json"
LOAD_PROJECTION = WAVE_DIR / "t49_load_projection.json"
LOAD_PROJECTION_INPUT = WAVE_DIR / "t49_load_projection_input.json"
GAME_TEST_RECEIPT = WAVE_DIR / "t49_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = WAVE_DIR / "t49_gametest.log"
CENSUS_DELTA = WAVE_DIR / "t49_census_delta.json"
CARD_TOPOLOGY = WAVE_DIR / "t49_card_topology.json"
READINESS = WAVE_DIR / "t49_readiness.json"
CLOSEOUT_SEAL = TOOLS / "t49_closeout_seal.json"

GENERATED_ROOT = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/tiny_purified"
)
PUBLICATION_POLICY_DATAPACK = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)
PUBLICATION_POLICY_DATAPACK_FILES = {
    PUBLICATION_GROUP_EXACT_MULTI: PUBLICATION_POLICY_DATAPACK
    / "bath_tiny_purified_exact_multi.json",
}
PUBLICATION_GROUP_KEYS = {PUBLICATION_GROUP_EXACT_MULTI: "exact_multi"}
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/player_path_support/bath_tiny_purified"
)
LOCKED_SUPPORT_RECIPE_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/recipe/player_path_support/bath_tiny_purified"
)
ITEM_SCATTER_FEATURE = "cruciblecraft:bath_tiny_washed_scatter"
ITEM_SCATTER_TAG = "cruciblecraft:bath_tiny_washed_items"
SCATTER_FEATURE = ITEM_SCATTER_FEATURE
SCATTER_CONFIGURED = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/configured_feature"
    / "bath_tiny_washed_scatter.json"
)
SCATTER_PLACED = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/placed_feature"
    / "bath_tiny_washed_scatter.json"
)
SCATTER_BIOME_MODIFIER = (
    ROOT
    / "src/main/resources/data/cruciblecraft/neoforge/biome_modifier"
    / "add_bath_tiny_washed_scatter.json"
)
SCATTER_CATALOG = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog"
    / "bath_tiny_washed_scatter.json"
)
SCATTER_ITEM_TAG = (
    ROOT
    / "src/main/resources/data/cruciblecraft/tags/item"
    / "bath_tiny_washed_items.json"
)
GAME_TEST_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/BathTinyPurifiedGameTests.java"
)
GAME_TEST_NAMESPACE = "cruciblecraft_wave_bath_tiny_purified"
GAME_TEST_COMMAND = (
    ".\\gradlew.bat runGameTestServer -PwaveRecipes=bath/tiny-purified --no-daemon"
)
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=bath/tiny-purified"
GAME_TEST_METHOD_RE = re.compile(r"public static void (\w+)\s*\(\s*GameTestHelper")

relative = t48.relative
load_json = t48.load_json
parse_managed = t48.parse_managed
parse_write_check = t48.parse_write_check
check_document = t48.check_document
handle_rebind = t48.handle_rebind
run_managed = t48.run_managed
selection_sha256 = t48.selection_sha256
tree_sha256 = t48.tree_sha256
assert_runtime_id = t48.assert_runtime_id
membership_root_sha256 = t48.membership_root_sha256
read_gametest_log = t48.read_gametest_log
normalize_gametest_log_text = t48.normalize_gametest_log_text


def size_exception() -> dict[str, Any]:
    return {
        "candidate_family_ceiling": CANDIDATE_FAMILY_CEILING,
        "candidate_relation_ceiling": CANDIDATE_RELATION_CEILING,
        "correction_family_count": CORRECTION_FAMILY_COUNT,
        "exception_kind": EXCEPTION_KIND,
        "minimum_N": MIN_PRODUCTION_FAMILIES,
        "padding_hosts_forbidden": list(PADDING_HOSTS_FORBIDDEN),
    }


def prior_locked_family_ids() -> set[str]:
    return (
        set(t46.production_family_ids())
        | set(t47.production_family_ids())
        | set(t48.production_family_ids())
    )


def assert_t48_opening_current() -> dict[str, Any]:
    readiness = load_json(T48_READINESS)
    if readiness.get("status") != "T48_READY":
        raise ValueError("T49 requires T48_READY")
    if readiness.get("failed_gates"):
        raise ValueError("T49 requires T48 failed_gates=[]")
    evidence = readiness.get("evidence") or {}
    if evidence.get("remaining_recipe_gap") != OPENING_EXECUTION_GAP:
        raise ValueError("T49 opening gap is not T48 remaining_recipe_gap 1354")
    load = evidence.get("load") or {}
    if load.get("authored_closing") != OPENING_AUTHORED_ENTRIES:
        raise ValueError("T49 opening authored is not T48 authored_closing 6263")
    opening = readiness.get("t49_opening") or readiness.get("t48_opening") or {}
    t14 = opening.get("t14_closing") or {}
    if t14.get("eager_publication_rows") != OPENING_EAGER_ROWS:
        raise ValueError("T49 opening eager is not T48 18-group remeasurement 14")
    if t14.get("lazy_logical_rows") != OPENING_LAZY_ROWS:
        raise ValueError("T49 opening lazy is not T48 18-group remeasurement 50557")
    if t14.get("lazy_cache_ceiling_rows") != OPENING_CACHE_CEILING_ROWS:
        raise ValueError("T49 opening cache is not T48 18-group remeasurement 781")
    if t14.get("sync_bytes") != OPENING_SYNC_BYTES:
        raise ValueError("T49 opening sync_bytes drifted from T48 t14_closing")
    if t14.get("lookup_candidate_count") != OPENING_LOOKUP_CANDIDATES_P95:
        raise ValueError("T49 opening lookup candidates p95 drifted")
    if t14.get("lookup_p95_ns") != OPENING_LOOKUP_P95_NS:
        raise ValueError("T49 opening lookup p95 ns drifted")
    if t14.get("server_reload_ms") != OPENING_SERVER_RELOAD_MS:
        raise ValueError("T49 opening server reload ms drifted")
    if t14.get("client_reload_ms") != OPENING_CLIENT_RELOAD_MS:
        raise ValueError("T49 opening client reload ms drifted")
    if t14.get("allocation_bytes") != OPENING_ALLOCATION_BYTES:
        raise ValueError("T49 opening allocation bytes drifted")
    census = load_json(T48_CENSUS_DELTA)
    if census.get("status") != "T48_CENSUS_DELTA_READY":
        raise ValueError("T49 requires T48_CENSUS_DELTA_READY")
    topology = load_json(T48_CARD_TOPOLOGY)
    if topology.get("next_issue_id") != "T49":
        raise ValueError("T49 requires T48 topology next_issue_id=T49")
    if topology.get("preassigned_host") or topology.get("preassigned_family_ids"):
        raise ValueError("T49 must not inherit a T48-preassigned host or family ids")
    vr = load_json(T47_VR_REPAIR_READINESS)
    if vr.get("status") != "T47_VR_READY":
        raise ValueError("T49 requires T47_VR_READY")
    forward = load_json(FORWARD_V2_READINESS)
    if forward.get("status") != "FORWARD_RECIPE_AUTHORITY_V2_READY":
        raise ValueError("T49 requires FORWARD_RECIPE_AUTHORITY_V2_READY")
    seal = load_json(T48_CLOSEOUT_SEAL)
    if seal.get("remaining_recipe_gap") != OPENING_EXECUTION_GAP:
        raise ValueError("T49 requires T48 closeout seal remaining gap 1354")
    return {
        "allocation_bytes": OPENING_ALLOCATION_BYTES,
        "authored_entries": OPENING_AUTHORED_ENTRIES,
        "client_reload_ms": OPENING_CLIENT_RELOAD_MS,
        "eager_rows": OPENING_EAGER_ROWS,
        "execution_gap": OPENING_EXECUTION_GAP,
        "lazy_rows": OPENING_LAZY_ROWS,
        "cache_ceiling_rows": OPENING_CACHE_CEILING_ROWS,
        "lookup_candidates_p95": OPENING_LOOKUP_CANDIDATES_P95,
        "lookup_p95_ns": OPENING_LOOKUP_P95_NS,
        "server_reload_ms": OPENING_SERVER_RELOAD_MS,
        "sync_bytes": OPENING_SYNC_BYTES,
        "t14_closing": t14,
        "t48_production_lock_sha256": t48.production_lock_sha256(),
        "v2_load_policy_sha256": t35.sha256_file(LOAD_POLICY_V2),
    }


def t14_opening_load() -> dict[str, Any]:
    return dict(assert_t48_opening_current()["t14_closing"])


def load_production_lock() -> dict[str, Any]:
    if not PRODUCTION_LOCK.is_file():
        raise FileNotFoundError("T49 production lock is missing")
    document = load_json(PRODUCTION_LOCK)
    if document.get("status") != "T49_PRODUCTION_LOCKED":
        raise ValueError("T49 production lock is not locked")
    return document


def production_lock_sha256() -> str:
    return t35.sha256_file(PRODUCTION_LOCK)


def production_family_count() -> int:
    return int(load_production_lock()["production"]["family_count"])


def production_relation_count() -> int:
    return int(load_production_lock()["production"]["relation_count"])


def production_families() -> list[dict[str, Any]]:
    return list((load_production_lock().get("production") or {}).get("families") or [])


def production_family_ids() -> list[str]:
    return [str(row["family_id"]) for row in production_families()]


def production_stable_ids() -> list[str]:
    return list((load_production_lock().get("production") or {}).get("stable_ids") or [])


def production_publication_groups() -> tuple[str, ...]:
    groups = (load_production_lock().get("production") or {}).get("publication_groups") or []
    if groups:
        return tuple(str(value) for value in groups)
    return PUBLICATION_GROUPS


def generated_family_files() -> list[Path]:
    if not GENERATED_ROOT.is_dir():
        return []
    return sorted(
        path
        for path in GENERATED_ROOT.rglob("gt_recipe_*.json")
        if path.is_file()
    )


def _file_sha256(path: Path) -> str | None:
    if not path.is_file():
        return None
    return t35.sha256_file(path)


def discovered_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted(
        {match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)}
    )


def support_recipe_files() -> list[Path]:
    if not LOCKED_SUPPORT_RECIPE_ROOT.is_dir():
        return []
    return sorted(
        path
        for path in LOCKED_SUPPORT_RECIPE_ROOT.glob("*.json")
        if path.is_file()
    )


def locked_support_tree_sha256() -> str:
    if not scatter_datapack_present():
        return EMPTY_TREE_SHA256
    hasher = hashlib.sha256()
    for path in (
        SCATTER_CONFIGURED,
        SCATTER_PLACED,
        SCATTER_BIOME_MODIFIER,
        SCATTER_CATALOG,
        SCATTER_ITEM_TAG,
    ):
        hasher.update(path.read_bytes())
    if LOCKED_SUPPORT_RECIPE_ROOT.is_dir():
        hasher.update(tree_sha256(LOCKED_SUPPORT_RECIPE_ROOT).encode("utf-8"))
    return hasher.hexdigest()


def locked_support_tree_current() -> bool:
    return scatter_datapack_present()


def b0_identities() -> set[str]:
    identities_found = set(t48.b0_identities())
    if t48.LAYERED_PLAYER_PATH.is_file():
        layered = load_json(t48.LAYERED_PLAYER_PATH)
        identities_found.update((layered.get("b1") or {}).get("added_identities") or [])
        identities_found.update((layered.get("b2") or {}).get("added_identities") or [])
    return identities_found


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
    if not PLAYER_PATH_SUPPORT.is_file() or not scatter_datapack_present():
        return False
    support = load_json(PLAYER_PATH_SUPPORT)
    if support.get("kind") != PLAYER_PATH_REAL_KIND:
        return False
    routes = list(support.get("routes") or [])
    if not routes:
        return False
    recipes = support_recipe_files()
    bath_routes = [row for row in routes if str(row.get("kind")) == "bath_gt_recipe"]
    item_routes = [row for row in routes if str(row.get("kind")) == "worldgen_drop"]
    if len(recipes) != len(bath_routes) or not item_routes:
        return False
    for row in routes:
        inputs = [str(value) for value in (row.get("input_identities") or [])]
        if any(value.endswith(":iron_ingot") for value in inputs):
            return False
        if str(row.get("kind")) == "worldgen_drop" and inputs:
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
        document.get("status") == "T49_EQUIVALENCE"
        and document.get("mutation_sensitive") is True
        and "stable_id" in fields
        and "fluid_inputs" in fields
        and "item_inputs" in fields
        and "shadow_order" in fields
        and len(rows) == production_relation_count()
    )


def java_locked_fields_asserted() -> bool:
    if not HARNESS_JAVA.is_file():
        return False
    text = HARNESS_JAVA.read_text(encoding="utf-8")
    return all(field in text for field in JAVA_LOCKED_EQUIVALENCE_FIELDS)


def integrated_load_measured() -> bool:
    if not INTEGRATED_MEASUREMENTS.is_file():
        return False
    document = load_json(INTEGRATED_MEASUREMENTS)
    if document.get("status") != "T49_INTEGRATED_MEASUREMENT_READY":
        return False
    candidates = document.get("candidates") or []
    return any(
        row.get("candidate") in {"hybrid", "on_demand", "immediate"}
        and row.get("status") == "PASS"
        for row in candidates
        if isinstance(row, dict)
    )


def partition_for_winner(winner: str, group_key: str) -> dict[str, int]:
    if not POLICY.is_file():
        raise FileNotFoundError("T49 materialization policy is missing")
    contract = load_json(POLICY)["publication_group_contract"][group_key]
    eager, lazy, cache = contract["partitions"][winner]
    return {
        "eager_publication_rows": int(eager),
        "lazy_logical_rows": int(lazy),
        "lazy_cache_ceiling_rows": int(cache),
    }


def hybrid_eager_stable_ids(group_key: str) -> list[str]:
    if not POLICY.is_file():
        raise FileNotFoundError("T49 materialization policy is missing")
    boundary = load_json(POLICY)["publication_group_contract"][group_key][
        "hybrid_boundary"
    ]
    return list(boundary.get("eager_stable_ids") or [])


def integrated_production_row() -> dict[str, Any] | None:
    if not integrated_load_measured():
        return None
    if production_strategy()["blocked"]:
        return None
    document = load_json(INTEGRATED_MEASUREMENTS)
    for row in document.get("candidates") or []:
        if (
            isinstance(row, dict)
            and row.get("candidate") in {"hybrid", "on_demand", "immediate"}
            and row.get("status") == "PASS"
        ):
            if row.get("candidate") == production_strategy()["card_aggregate_winner"]:
                return row
    for row in document.get("candidates") or []:
        if (
            isinstance(row, dict)
            and row.get("candidate") == "on_demand"
            and row.get("status") == "PASS"
        ):
            return row
    return None


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    raw_winners = nested.get("group_winners") or {}
    winner = nested.get("card_aggregate_winner") or decision.get("winner")
    blocked = (
        decision.get("status") == "T49_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or winner in {None, "", "BLOCKED"}
        or not raw_winners
    )
    return {
        "blocked": blocked,
        "card_aggregate_winner": None if blocked else winner,
        "group_winners": {} if blocked else dict(raw_winners),
        "recomputable": not blocked,
    }


def authored_family_path(family: dict[str, Any]) -> Path:
    template = str(family.get("template_key") or family.get("source_template") or "")
    filename = template.replace(".", "_").replace("#", "_") + ".json"
    return GENERATED_ROOT / filename


def bound_gametest_artifacts() -> dict[str, str | None]:
    from tools import semantic_projection as _projection

    return {
        "gametest_java": _file_sha256(GAME_TEST_JAVA),
        "material_registration_gate_java": _file_sha256(MATERIAL_REGISTRATION_GATE_JAVA),
        "material_registration_gate_json": _projection.gate_semantic_root_sha256(),
        "production_lock": _file_sha256(PRODUCTION_LOCK),
        "t49_generated_recipes": tree_sha256(GENERATED_ROOT) if GENERATED_ROOT.exists() else None,
        "t49_locked_support": locked_support_tree_sha256()
        if scatter_datapack_present()
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
        for marker in ("cruciblecraft_wave_bath_tiny_purified", "BathTinyPurifiedGameTests", "-PwaveRecipes=bath/tiny-purified")
    )


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "T49",
        live=lambda: (
            player_gametest_source_present()
            and GAME_TEST_RECEIPT.is_file()
            and not gametest_receipt_errors()
        ),
    )


GAME_TEST_REQUIRED = 0
GAME_TEST_PASS_MARKER = "All 0 required tests passed :)"


def refresh_gametest_required() -> int:
    global GAME_TEST_REQUIRED, GAME_TEST_PASS_MARKER
    GAME_TEST_REQUIRED = len(discovered_gametest_ids())
    GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"
    return GAME_TEST_REQUIRED


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
        "java_sha256": t35.sha256_file(GAME_TEST_JAVA) if GAME_TEST_JAVA.is_file() else None,
        "java_source": relative(GAME_TEST_JAVA),
        "log_fingerprint": log_fingerprint(log_text),
        "log_path": relative(GAME_TEST_EVIDENCE_LOG),
        "namespace": GAME_TEST_NAMESPACE,
        "note": (
            "Source markers are not a pass. This receipt is the isolated "
            "-Pt49Recipes result bound to the T49 production lock, recipe "
            "tree, scatter support, and the committed UTF-8 evidence log. "
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


def _live_gametest_receipt_errors(document: dict[str, Any] | None = None) -> list[str]:
    from tools import gametest_receipt_roots

    refresh_gametest_required()
    if document is None and not GAME_TEST_RECEIPT.is_file():
        return ["missing GameTest receipt: tools/t49_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_gametest_ids()
    java_hash = t35.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("T49 GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("T49 GameTest receipt command is missing -Pt49Recipes")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("T49 GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("T49 GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(f"T49 GameTest receipt passed count is not {GAME_TEST_REQUIRED}")
    if receipt.get("java_sha256") != java_hash:
        errors.append("T49 GameTest Java sha256 drifted")
    if sorted(receipt.get("test_ids") or []) != test_ids:
        errors.append("T49 GameTest receipt test_ids drifted")
    if receipt.get("failed") != 0:
        errors.append("T49 GameTest receipt records failures")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("T49 GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("T49 GameTest receipt schema_version must be 3")
    bound = receipt.get("bound_artifacts") or {}
    live_bound = bound_gametest_artifacts()
    errors.extend(gametest_receipt_roots.bound_drift_errors("T49", bound, live_bound))
    if str(receipt.get("log_path") or "") != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append("T49 GameTest receipt log_path must be tools/t49_gametest.log")
    if not GAME_TEST_EVIDENCE_LOG.is_file():
        errors.append("T49 GameTest committed evidence log is missing")
    else:
        evidence_text = normalize_gametest_log_text(read_gametest_log(GAME_TEST_EVIDENCE_LOG))
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append("T49 GameTest receipt log_fingerprint drifted")
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("T49 committed GameTest evidence log is not PASS")
    return errors


def gametest_receipt_errors(document: dict[str, Any] | None = None) -> list[str]:
    if document is not None:
        return _live_gametest_receipt_errors(document)
    from tools import closeout_seal

    return closeout_seal.live_or_sealed_errors(
        "T49",
        "receipt",
        lambda: _live_gametest_receipt_errors(None),
    )


def v2_policy_hash() -> str:
    return t35.sha256_file(LOAD_POLICY_V2)


refresh_gametest_required()
