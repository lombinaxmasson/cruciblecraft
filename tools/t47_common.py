#!/usr/bin/env python3
"""Paths and status constants for the T47 Bath remainder bounded-lock wave."""
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
from tools import t35_common as t35
from tools import t42_common as t42
from tools import t42_owner_common as t42_owner
from tools import t44_common as t44
from tools import t45_common as t45
from tools import t46_common as t46

ROOT = t35.ROOT
TOOLS = t35.TOOLS
SOURCE_REVISION = t35.SOURCE_REVISION

OWNER = "portfolio:track_a/t47_bath_remainder"
OPENING_EXECUTION_GAP = 1894
OPENING_AUTHORED_ENTRIES = 5706
OPENING_EAGER_ROWS = 14
OPENING_LAZY_ROWS = 2758
OPENING_CACHE_CEILING_ROWS = 222
CACHE_CEILING = 128
HYBRID_CUTOFF = 16
OPENING_SYNC_BYTES = 273899
OPENING_LOOKUP_CANDIDATES_P95 = 10
OPENING_LOOKUP_P95_NS = 74437
OPENING_SERVER_RELOAD_MS = 30
OPENING_CLIENT_RELOAD_MS = 31
T14_CLOSING_BASIS = "compact_production_15_publication_groups"
COMPLETION_DELTA = 395
RECLASSIFICATION_DELTA = 0
CLOSING_EXECUTION_GAP = 1499
EXPECTED_FAMILY_COUNT = 395
EXPECTED_RELATION_COUNT = 13708
EXPECTED_IDENTITY_COUNT = 283
EXPECTED_FLUID_COUNT = 3
LAYERED_PLAYER_PATH_STATUS = "T47_LAYERED_PLAYER_PATH"
EMPTY_TREE_SHA256 = hashlib.sha256().hexdigest()
HARNESS_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CompactT47BathRemainderHarnessTest.java"
)
JAVA_LOCKED_EQUIVALENCE_FIELDS = ("shadow_order", "source_row_sha256")
DEFERRED_RECYCLING_COUNT = 1817
ALL_BATH_FAMILIES = 1348
ALL_BATH_RELATIONS = 49411
T46_CLOSED_FAMILIES = 803
T46_CLOSED_RELATIONS = 1517
CANDIDATE_FAMILY_COUNT = 545
CANDIDATE_RELATION_COUNT = 47894
CANDIDATE_EXACT_FAMILIES = 236
CANDIDATE_EXACT_MULTI_FAMILIES = 309
CANDIDATE_EXACT_MULTI_RELATIONS = 47658
MIN_PRODUCTION_FAMILIES = 300
HOST = "cruciblecraft:bath"
TARGET_MAP = "cruciblecraft:bath"
SOURCE_MAP = "gt.recipe.bath"
BATH_MAP_INDEX = 4
CANDIDATE_PUBLICATION_GROUP = "cruciblecraft:t47_bath_remainder_candidate"
PUBLICATION_GROUP_EXACT = "cruciblecraft:t47_bath_exact"
PUBLICATION_GROUP_EXACT_MULTI = "cruciblecraft:t47_bath_exact_multi"
PUBLICATION_GROUPS = (PUBLICATION_GROUP_EXACT, PUBLICATION_GROUP_EXACT_MULTI)
T46_PUBLICATION_GROUP = t46.PUBLICATION_GROUP
BATH_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.bath.json"
ORACLE_RELATIVE = t44.ORACLE_RELATIVE
ORACLE_ROOT = t44.ORACLE_ROOT
oracle_revision = t44.oracle_revision
oracle_dirty = t44.oracle_dirty
V1_LOAD_POLICY = t46.V1_LOAD_POLICY
V1_IDENTITY_LEDGER = t46.V1_IDENTITY_LEDGER
V1_RUNTIME_MANIFEST = t46.V1_RUNTIME_MANIFEST
T46_READINESS = t46.READINESS
T45_READINESS = t46.T45_READINESS
T44_READINESS = t46.T44_READINESS
T43_READINESS = t46.T43_READINESS
T42_OWNER_READINESS = t46.T42_OWNER_READINESS
T41_READINESS = t46.T41_READINESS
T46_CENSUS_DELTA = t46.CENSUS_DELTA
T46_CARD_TOPOLOGY = t46.CARD_TOPOLOGY
T46_PRODUCTION_LOCK = t46.PRODUCTION_LOCK
T22_5_FLUID_MAPPING = t46.T22_5_FLUID_MAPPING
T46_FLUID_MAPPING = t46.FLUID_MAPPING
T46_MTE_CATALOG = t46.MTE_CATALOG
RECIPE_FAMILIES = t35.RECIPE_FAMILIES
MATERIAL_REGISTRATION_GATE_JAVA = t46.MATERIAL_REGISTRATION_GATE_JAVA
LOAD_POLICY_V2 = t46.LOAD_POLICY_V2
LOAD_POLICY_V2_SCHEMA = t46.LOAD_POLICY_V2_SCHEMA
IDENTITY_LEDGER_V2 = t46.IDENTITY_LEDGER_V2
IDENTITY_LEDGER_V2_SCHEMA = t46.IDENTITY_LEDGER_V2_SCHEMA
IDENTITY_LEDGER_V2_CURRENTNESS = t46.IDENTITY_LEDGER_V2_CURRENTNESS
RUNTIME_MANIFEST_V2 = t46.RUNTIME_MANIFEST_V2
RUNTIME_MANIFEST_V2_SCHEMA = t46.RUNTIME_MANIFEST_V2_SCHEMA
RUNTIME_MANIFEST_V2_CURRENTNESS = t46.RUNTIME_MANIFEST_V2_CURRENTNESS
FORWARD_V2_READINESS = t46.FORWARD_V2_READINESS
FORWARD_V2_READINESS_SCHEMA = t46.FORWARD_V2_READINESS_SCHEMA
T46_IDENTITY_DELTA = t46.IDENTITY_DELTA
T46_RUNTIME_DELTA = t46.RUNTIME_DELTA

WORK_SET = TOOLS / "t47_work_set.json"
SOURCE_PACK = TOOLS / "t47_bath_source_pack_manifest.json"
SOURCE = TOOLS / "t47_bath_source.json"
RECEIPT = TOOLS / "t47_bath_source_receipt.json"
REVIEW = TOOLS / "t47_bath_source_review.json"
CANDIDATE_SELECTION = TOOLS / "t47_bath_candidate_selection.json"
IDENTITY_CATALOG = TOOLS / "bath_remainder_identity_catalog.json"
FLUID_MAPPING = TOOLS / "bath_remainder_fluid_mapping.json"
RECYCLING_DISPOSITION = TOOLS / "t47_bath_recycling_disposition.json"
OPERAND_RUNTIME_MAP = TOOLS / "t47_operand_runtime_map.json"
REQUIRED_FORMS = TOOLS / "t47_required_forms.json"
COMPILE_SPEC = TOOLS / "t47_recipe_compile_spec.json"
PRODUCTION_LOCK = TOOLS / "t47_production_lock.json"
IDENTITY_DELTA = TOOLS / "t47_identity_ledger_delta.json"
RUNTIME_DELTA = TOOLS / "t47_runtime_manifest_delta.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "t47_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = TOOLS / "t47_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "t47_shard_manifest.json"
PLAYER_PATH = TOOLS / "t47_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "t47_layered_player_path.json"
PLAYER_PATH_SUPPORT = TOOLS / "t47_player_path_support.json"
EQUIVALENCE = TOOLS / "t47_equivalence.json"
COMPILE_REPORT = TOOLS / "t47_compile_report.json"
POLICY = TOOLS / "t47_materialization_policy.json"
MEASUREMENTS = TOOLS / "t47_materialization_measurements.json"
INTEGRATED_MEASUREMENTS = TOOLS / "t47_integrated_measurements.json"
DECISION = TOOLS / "t47_materialization_decision.json"
PUBLICATION_DELTA = TOOLS / "t47_publication_delta.json"
LOAD_PROJECTION = TOOLS / "t47_load_projection.json"
LOAD_PROJECTION_INPUT = TOOLS / "t47_load_projection_input.json"
GAME_TEST_RECEIPT = TOOLS / "t47_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "t47_gametest.log"
CENSUS_DELTA = TOOLS / "t47_census_delta.json"
CARD_TOPOLOGY = TOOLS / "t47_card_topology.json"
READINESS = TOOLS / "t47_readiness.json"
SCHEMA = t46.SCHEMA
PUBLICATION_POLICY_SCHEMA = t46.PUBLICATION_POLICY_SCHEMA
GENERATED_ROOT = (
    ROOT / "src/t47_recipe_generated/resources/data/cruciblecraft/recipe/t47"
)
PUBLICATION_POLICY_DATAPACK = (
    ROOT
    / "src/t47_recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)
PUBLICATION_POLICY_DATAPACK_FILES = {
    PUBLICATION_GROUP_EXACT: PUBLICATION_POLICY_DATAPACK / "t47_bath_exact.json",
    PUBLICATION_GROUP_EXACT_MULTI: PUBLICATION_POLICY_DATAPACK / "t47_bath_exact_multi.json",
}
PUBLICATION_GROUP_KEYS = {
    PUBLICATION_GROUP_EXACT: "exact",
    PUBLICATION_GROUP_EXACT_MULTI: "exact_multi",
}
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src/t47_support_generated/resources/data/cruciblecraft/t47_player_path_support"
)
LOCKED_SUPPORT_RECIPE_ROOT = (
    ROOT
    / "src/t47_support_generated/resources/data/cruciblecraft/recipe/t47_player_path_support"
)
BUNDLED_IDENTITY_CATALOG = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_remainder_identity_catalog.json"
)
BUNDLED_FLUID_MAPPING = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_remainder_fluid_mapping.json"
)
PLAYER_PATH_REAL_KIND = "layered_b1"
SCATTER_FEATURE = "cruciblecraft:gt_block_object_scatter"
ITEM_SCATTER_FEATURE = "cruciblecraft:bath_remainder_scatter"
ITEM_SCATTER_TAG = "cruciblecraft:bath_remainder_items"
SCATTER_CONFIGURED = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/configured_feature"
    / "bath_remainder_scatter.json"
)
SCATTER_PLACED = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/placed_feature"
    / "bath_remainder_scatter.json"
)
SCATTER_BIOME_MODIFIER = (
    ROOT
    / "src/main/resources/data/cruciblecraft/neoforge/biome_modifier"
    / "add_bath_remainder_scatter.json"
)
SCATTER_CATALOG = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog"
    / "bath_remainder_scatter.json"
)
SCATTER_ITEM_TAG = (
    ROOT
    / "src/main/resources/data/cruciblecraft/tags/item"
    / "bath_remainder_items.json"
)
GAME_TEST_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/BathRemainderGameTests.java"
)
GAME_TEST_NAMESPACE = "cruciblecraft_wave_bath_remainder"
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=bath/remainder --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=bath/remainder"
GAME_TEST_METHOD_RE = re.compile(r"public static void (\w+)\s*\(\s*GameTestHelper")
ROW_CLASSIFICATION = t46.ROW_CLASSIFICATION
TEMPLATE_DENOMINATOR = t46.TEMPLATE_DENOMINATOR
T21_REACHABILITY = t46.T21_REACHABILITY
T42_OWNER_LOCK = t42_owner.DISPOSITION_LOCK
T42_OWNER_OVERLAY = t42_owner.TRACK_OVERLAY
T42_BLOCKER = t42.BLOCKER_OVERLAY
ORDINARY_CLASS = "ordinary_optional"
T14_COUNTABLE = t45.T14_COUNTABLE
T14_PENDING = t45.T14_PENDING
STRATEGY_AXES = t45.STRATEGY_AXES
MAX_INTERVAL_AXES = t45.MAX_INTERVAL_AXES
PENDING_LOAD_VERDICT = t45.PENDING_LOAD_VERDICT
GAME_TEST_RECEIPT_SCHEMA = 3
T35_FOUNDATION = dict(t42.T35_FOUNDATION)
T14_HARD_AUTHORED = 6600
T14_OPENING_LOAD_SOURCE = "tools/t46_readiness.json#t47_opening.t14_closing"
OPENING_DISPOSITION = "planned"
CLOSING_DISPOSITION = "implemented"
OPENING_CLOSURE = "pending"
CLOSING_CLOSURE = "closed"
OPENING_FIDELITY = "recorded"
OPENING_LOAD = "pending"
CLOSING_LOAD = "measured"
EXPECTED_REMAINING_ORDINARY_FAMILIES = CLOSING_EXECUTION_GAP


def relative(path: Path) -> str:
    try:
        return t35.relative(path)
    except ValueError:
        return path.as_posix()


def load_json(path: Path) -> Any:
    return t35.load_json(path)


def parse_managed(description: str, argv: list[str] | None = None):
    return builder_cli.parse_managed(description, argv)


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return t35.check_generated_document(path, document)


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
            t35.write_stable(output, build())
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


def assert_runtime_id(value: Any, *, consume: bool) -> str:
    runtime = str(value or "")
    if not runtime.startswith("minecraft:") and not runtime.startswith("cruciblecraft:"):
        raise ValueError(f"T47 runtime id must be minecraft: or cruciblecraft:: {runtime}")
    if consume and runtime.startswith(("gregtech:", "gregapi:")):
        raise ValueError(f"T47 consume runtime leaked a source id: {runtime}")
    return runtime


def t46_closed_family_ids() -> set[str]:
    return set(t46.production_family_ids())


def overlay_bath_remainder_families() -> list[dict[str, Any]]:
    overlay = load_json(T42_OWNER_OVERLAY)
    blocker = load_json(T42_BLOCKER)
    if overlay.get("source_revision") != SOURCE_REVISION:
        raise ValueError("T42 owner overlay source_revision drifted")
    closed = t46_closed_family_ids()
    if len(closed) != T46_CLOSED_FAMILIES:
        raise ValueError(f"T46 closed family count drifted: {len(closed)}")
    by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    selected: list[dict[str, Any]] = []
    for row in overlay.get("families") or []:
        if str(row.get("host") or "") != HOST:
            continue
        family_id = str(row.get("family_id") or "")
        if family_id in closed:
            continue
        kinds = list((by_id.get(family_id) or {}).get("unique_kinds") or [])
        if kinds == ["mte"] and str(row.get("current_owner") or "") == t46.OWNER_TRACK:
            raise ValueError(f"{family_id}: T46 MTE family leaked into T47 remainder")
        selected.append(row)
    selected.sort(key=lambda row: str(row["family_id"]))
    if len(selected) != CANDIDATE_FAMILY_COUNT:
        raise ValueError(
            f"T47 remainder families {len(selected)} != {CANDIDATE_FAMILY_COUNT}"
        )
    return selected


def assert_t46_opening_current() -> dict[str, Any]:
    readiness = load_json(T46_READINESS)
    if readiness.get("status") != "T46_READY":
        raise ValueError("T47 requires T46_READY")
    if readiness.get("failed_gates"):
        raise ValueError("T47 requires T46 failed_gates=[]")
    evidence = readiness.get("evidence") or {}
    if evidence.get("remaining_recipe_gap") != OPENING_EXECUTION_GAP:
        raise ValueError("T47 opening gap is not T46 remaining_recipe_gap 1894")
    load = evidence.get("load") or {}
    if load.get("authored_closing") != OPENING_AUTHORED_ENTRIES:
        raise ValueError("T47 opening authored is not T46 authored_closing 5706")
    opening = readiness.get("t47_opening") or {}
    t14 = opening.get("t14_closing") or {}
    if t14.get("eager_publication_rows") != OPENING_EAGER_ROWS:
        raise ValueError("T47 opening eager is not T46 13-group remeasurement 14")
    if t14.get("lazy_logical_rows") != OPENING_LAZY_ROWS:
        raise ValueError("T47 opening lazy is not T46 13-group remeasurement 2758")
    if t14.get("lazy_cache_ceiling_rows") != OPENING_CACHE_CEILING_ROWS:
        raise ValueError("T47 opening cache is not T46 13-group remeasurement 222")
    census = load_json(T46_CENSUS_DELTA)
    if census.get("status") != "T46_CENSUS_DELTA_READY":
        raise ValueError("T47 requires T46_CENSUS_DELTA_READY")
    topology = load_json(T46_CARD_TOPOLOGY)
    if topology.get("next_issue_id") != "T47":
        raise ValueError("T47 requires T46 topology next_issue_id=T47")
    if topology.get("preassigned_host") or topology.get("preassigned_family_ids"):
        raise ValueError("T47 must not inherit a T46-preassigned host or family ids")
    forward = load_json(FORWARD_V2_READINESS)
    if forward.get("status") != "FORWARD_RECIPE_AUTHORITY_V2_READY":
        raise ValueError("T47 requires FORWARD_RECIPE_AUTHORITY_V2_READY")
    return {
        "authored_entries": OPENING_AUTHORED_ENTRIES,
        "eager_rows": OPENING_EAGER_ROWS,
        "execution_gap": OPENING_EXECUTION_GAP,
        "lazy_rows": OPENING_LAZY_ROWS,
        "cache_ceiling_rows": OPENING_CACHE_CEILING_ROWS,
        "t14_closing": t14,
        "t46_production_lock_sha256": t46.production_lock_sha256(),
        "v2_load_policy_sha256": t35.sha256_file(LOAD_POLICY_V2),
    }


def opening_from_t46() -> dict[str, Any]:
    return assert_t46_opening_current()


def t14_opening_load() -> dict[str, Any]:
    opening = assert_t46_opening_current()
    return dict(opening["t14_closing"])


def dump_present() -> bool:
    return BATH_DUMP.is_file()


def load_production_lock() -> dict[str, Any]:
    if not PRODUCTION_LOCK.is_file():
        raise FileNotFoundError("T47 production lock is missing")
    document = load_json(PRODUCTION_LOCK)
    if document.get("status") != "T47_PRODUCTION_LOCKED":
        raise ValueError("T47 production lock is not locked")
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
    found = sorted(
        {
            str(row.get("publication_group") or "")
            for row in production_families()
            if row.get("publication_group")
        }
    )
    return tuple(found)


def generated_family_files() -> list[Path]:
    if not GENERATED_ROOT.is_dir():
        return []
    return sorted(
        path
        for path in GENERATED_ROOT.rglob("gt_recipe_*.json")
        if path.is_file()
    )


def membership_root_sha256(family_ids: list[str], stable_ids: list[str]) -> str:
    payload = "".join(f"{family_id}\n" for family_id in family_ids)
    payload += "".join(f"{stable_id}\n" for stable_id in stable_ids)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


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
    if not LOCKED_SUPPORT_RECIPE_ROOT.is_dir() or not support_recipe_files():
        return EMPTY_TREE_SHA256
    return tree_sha256(LOCKED_SUPPORT_RECIPE_ROOT)


def locked_support_tree_current() -> bool:
    digest = locked_support_tree_sha256()
    return digest != EMPTY_TREE_SHA256 and bool(support_recipe_files())


def b0_identities() -> set[str]:
    identities_found = set(t46.b0_identities())
    if t46.LAYERED_PLAYER_PATH.is_file():
        layered = load_json(t46.LAYERED_PLAYER_PATH)
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
    fluid_routes = [row for row in routes if str(row.get("kind")) == "bath_gt_recipe"]
    item_routes = [row for row in routes if str(row.get("kind")) == "worldgen_drop"]
    if len(recipes) != len(fluid_routes) or not item_routes:
        return False
    for row in routes:
        inputs = [str(value) for value in (row.get("input_identities") or [])]
        if any(value.endswith(":iron_ingot") for value in inputs):
            return False
        if str(row.get("kind")) == "worldgen_drop" and inputs:
            return False
        if str(row.get("kind")) == "bath_gt_recipe":
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
    if not PRODUCTION_LOCK.is_file():
        return False
    return (
        document.get("status") == "T47_EQUIVALENCE"
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
    if document.get("status") != "T47_INTEGRATED_MEASUREMENT_READY":
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
        raise FileNotFoundError("T47 materialization policy is missing")
    contract = load_json(POLICY)["publication_group_contract"][group_key]
    eager, lazy, cache = contract["partitions"][winner]
    return {
        "eager_publication_rows": int(eager),
        "lazy_logical_rows": int(lazy),
        "lazy_cache_ceiling_rows": int(cache),
    }


def hybrid_eager_stable_ids(group_key: str) -> list[str]:
    if not POLICY.is_file():
        raise FileNotFoundError("T47 materialization policy is missing")
    boundary = load_json(POLICY)["publication_group_contract"][group_key][
        "hybrid_boundary"
    ]
    return list(boundary.get("eager_stable_ids") or [])


def parse_write_check(description: str, argv: list[str] | None = None):
    return t46.parse_write_check(description, argv)


def integrated_production_row() -> dict[str, Any] | None:
    """T37-T47 production mix: historical group policies plus T47 winners.

    CompactRecipeFamilyT47IntegratedMeasurementHarness records this as
    candidate ``hybrid`` (productionPolicies), not uniform on_demand.
    """
    if not integrated_load_measured():
        return None
    if production_strategy()["blocked"]:
        return None
    document = load_json(INTEGRATED_MEASUREMENTS)
    for row in document.get("candidates") or []:
        if (
            isinstance(row, dict)
            and row.get("candidate") == "hybrid"
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
        decision.get("status") == "T47_MATERIALIZATION_DECISION_BLOCKED"
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


def bound_gametest_artifacts() -> dict[str, str | None]:
    from tools import semantic_projection as _projection

    return {
        "gametest_java": _file_sha256(GAME_TEST_JAVA),
        "material_registration_gate_java": _file_sha256(MATERIAL_REGISTRATION_GATE_JAVA),
        "material_registration_gate_json": _projection.gate_semantic_root_sha256(),
        "production_lock": _file_sha256(PRODUCTION_LOCK),
        "t47_generated_recipes": tree_sha256(GENERATED_ROOT) if GENERATED_ROOT.exists() else None,
        "t47_locked_support": locked_support_tree_sha256()
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
        for marker in ("cruciblecraft_wave_bath_remainder", "BathRemainderGameTests", "-PwaveRecipes=bath/remainder")
    )


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "T47",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def authored_family_path(family: dict[str, Any]) -> Path:
    template = str(family.get("template_key") or family.get("source_template") or "")
    filename = template.replace(".", "_").replace("#", "_") + ".json"
    return GENERATED_ROOT / "bath" / filename


GAME_TEST_REQUIRED = 0
GAME_TEST_PASS_MARKER = "All 0 required tests passed :)"


def refresh_gametest_required() -> int:
    global GAME_TEST_REQUIRED, GAME_TEST_PASS_MARKER
    GAME_TEST_REQUIRED = len(discovered_gametest_ids())
    GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"
    return GAME_TEST_REQUIRED


def read_gametest_log(path: Path) -> str:
    from tools import t43_common as t43

    return t43.read_gametest_log(path)


def normalize_gametest_log_text(text: str) -> str:
    from tools import t43_common as t43

    return t43.normalize_gametest_log_text(text)


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
            "-PwaveRecipes=bath/remainder result bound to the T47 production lock, recipe "
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
        return ["missing GameTest receipt: tools/t47_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_gametest_ids()
    java_hash = t35.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("T47 GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("T47 GameTest receipt command is missing -PwaveRecipes=bath/remainder")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("T47 GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("T47 GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(f"T47 GameTest receipt passed count is not {GAME_TEST_REQUIRED}")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("T47 GameTest receipt pass_marker drifted")
    if receipt.get("java_sha256") != java_hash:
        errors.append("T47 GameTest Java sha256 drifted")
    if sorted(receipt.get("test_ids") or []) != test_ids:
        errors.append("T47 GameTest receipt test_ids drifted")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"T47 GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("failed") != 0:
        errors.append("T47 GameTest receipt records failures")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("T47 GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("T47 GameTest receipt schema_version must be 3")
    bound = receipt.get("bound_artifacts") or {}
    live_bound = bound_gametest_artifacts()
    support_hash = bound.get("t47_locked_support")
    live_support = live_bound.get("t47_locked_support")
    if support_hash in (None, EMPTY_TREE_SHA256):
        errors.append(
            "T47 GameTest t47_locked_support is bound to an empty tree; "
            "real recipes are under recipe/t47_player_path_support/"
        )
    if live_support in (None, EMPTY_TREE_SHA256):
        errors.append(
            "T47 live t47_locked_support tree is empty; "
            "bind GameTest to recipe/t47_player_path_support/"
        )
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "T47",
            bound,
            live_bound,
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "T47 GameTest receipt log_path must be the committed evidence file "
            "tools/t47_gametest.log"
        )
    if not GAME_TEST_EVIDENCE_LOG.is_file():
        errors.append(
            "T47 GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(
            read_gametest_log(GAME_TEST_EVIDENCE_LOG)
        )
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "T47 GameTest receipt log_fingerprint does not match the "
                "committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("T47 committed GameTest evidence log is not PASS")
    return errors


def v2_policy_hash() -> str:
    if not LOAD_POLICY_V2.is_file():
        raise FileNotFoundError("T47 forward-v2 load policy is missing")
    return t35.sha256_file(LOAD_POLICY_V2)


refresh_gametest_required()
