#!/usr/bin/env python3
"""Paths and status constants for the bath/remainder Bath remainder bounded-lock wave."""
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
from tools import bath_mte_common as bath_mte

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

OWNER = "portfolio:track_a/bath_remainder_bath_remainder"
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
LAYERED_PLAYER_PATH_STATUS = "BATH_REMAINDER_LAYERED_PLAYER_PATH"
EMPTY_TREE_SHA256 = hashlib.sha256().hexdigest()
HARNESS_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "BathRemainderHarnessTest.java"
)
JAVA_LOCKED_EQUIVALENCE_FIELDS = ("shadow_order", "source_row_sha256")
DEFERRED_RECYCLING_COUNT = 1817
ALL_BATH_FAMILIES = 1348
ALL_BATH_RELATIONS = 49411
BATH_MTE_CLOSED_FAMILIES = 803
BATH_MTE_CLOSED_RELATIONS = 1517
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
CANDIDATE_PUBLICATION_GROUP = "cruciblecraft:bath/remainder/candidate"
PUBLICATION_GROUP_EXACT = "cruciblecraft:bath/remainder/exact"
PUBLICATION_GROUP_EXACT_MULTI = "cruciblecraft:bath/remainder/exact_multi"
PUBLICATION_GROUPS = (PUBLICATION_GROUP_EXACT, PUBLICATION_GROUP_EXACT_MULTI)
BATH_MTE_PUBLICATION_GROUP = bath_mte.PUBLICATION_GROUP
BATH_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.bath.json"
ORACLE_RELATIVE = storage.ORACLE_RELATIVE
ORACLE_ROOT = storage.ORACLE_ROOT
oracle_revision = storage.oracle_revision
oracle_dirty = storage.oracle_dirty
V1_LOAD_POLICY = bath_mte.V1_LOAD_POLICY
V1_IDENTITY_LEDGER = bath_mte.V1_IDENTITY_LEDGER
V1_RUNTIME_MANIFEST = bath_mte.V1_RUNTIME_MANIFEST
BATH_MTE_READINESS = bath_mte.READINESS
BLOCK_OBJECT_READINESS = bath_mte.BLOCK_OBJECT_READINESS
STORAGE_LOCK_READINESS = bath_mte.STORAGE_LOCK_READINESS
SMELTER_STONE_READINESS = bath_mte.SMELTER_STONE_READINESS
T42_OWNER_READINESS = bath_mte.T42_OWNER_READINESS
ASSEMBLER_WOOD_READINESS = bath_mte.ASSEMBLER_WOOD_READINESS
BATH_MTE_CENSUS_DELTA = bath_mte.CENSUS_DELTA
BATH_MTE_CARD_TOPOLOGY = bath_mte.CARD_TOPOLOGY
BATH_MTE_PRODUCTION_LOCK = bath_mte.PRODUCTION_LOCK
T22_5_FLUID_MAPPING = bath_mte.T22_5_FLUID_MAPPING
BATH_MTE_FLUID_MAPPING = bath_mte.FLUID_MAPPING
BATH_MTE_MTE_CATALOG = bath_mte.MTE_CATALOG
RECIPE_FAMILIES = census.RECIPE_FAMILIES
MATERIAL_REGISTRATION_GATE_JAVA = bath_mte.MATERIAL_REGISTRATION_GATE_JAVA
LOAD_POLICY_V2 = bath_mte.LOAD_POLICY_V2
LOAD_POLICY_V2_SCHEMA = bath_mte.LOAD_POLICY_V2_SCHEMA
IDENTITY_LEDGER_V2 = bath_mte.IDENTITY_LEDGER_V2
IDENTITY_LEDGER_V2_SCHEMA = bath_mte.IDENTITY_LEDGER_V2_SCHEMA
IDENTITY_LEDGER_V2_CURRENTNESS = bath_mte.IDENTITY_LEDGER_V2_CURRENTNESS
RUNTIME_MANIFEST_V2 = bath_mte.RUNTIME_MANIFEST_V2
RUNTIME_MANIFEST_V2_SCHEMA = bath_mte.RUNTIME_MANIFEST_V2_SCHEMA
RUNTIME_MANIFEST_V2_CURRENTNESS = bath_mte.RUNTIME_MANIFEST_V2_CURRENTNESS
FORWARD_V2_READINESS = bath_mte.FORWARD_V2_READINESS
FORWARD_V2_READINESS_SCHEMA = bath_mte.FORWARD_V2_READINESS_SCHEMA
BATH_MTE_IDENTITY_DELTA = bath_mte.IDENTITY_DELTA
BATH_MTE_RUNTIME_DELTA = bath_mte.RUNTIME_DELTA

WORK_SET = TOOLS / "bath_remainder_work_set.json"
SOURCE_PACK = TOOLS / "bath_remainder_source_pack_manifest.json"
SOURCE = TOOLS / "bath_remainder_source.json"
RECEIPT = TOOLS / "bath_remainder_source_receipt.json"
REVIEW = TOOLS / "bath_remainder_source_review.json"
CANDIDATE_SELECTION = TOOLS / "bath_remainder_candidate_selection.json"
IDENTITY_CATALOG = TOOLS / "bath_remainder_identity_catalog.json"
FLUID_MAPPING = TOOLS / "bath_remainder_fluid_mapping.json"
RECYCLING_DISPOSITION = TOOLS / "bath_remainder_recycling_disposition.json"
OPERAND_RUNTIME_MAP = TOOLS / "bath_remainder_operand_runtime_map.json"
REQUIRED_FORMS = TOOLS / "bath_remainder_required_forms.json"
COMPILE_SPEC = TOOLS / "bath_remainder_recipe_compile_spec.json"
PRODUCTION_LOCK = TOOLS / "bath_remainder_production_lock.json"
IDENTITY_DELTA = TOOLS / "bath_remainder_identity_ledger_delta.json"
RUNTIME_DELTA = TOOLS / "bath_remainder_runtime_manifest_delta.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "bath_remainder_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = TOOLS / "bath_remainder_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "bath_remainder_shard_manifest.json"
PLAYER_PATH = TOOLS / "bath_remainder_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "bath_remainder_layered_player_path.json"
PLAYER_PATH_SUPPORT = TOOLS / "bath_remainder_player_path_support.json"
EQUIVALENCE = TOOLS / "bath_remainder_equivalence.json"
COMPILE_REPORT = TOOLS / "bath_remainder_compile_report.json"
POLICY = TOOLS / "bath_remainder_materialization_policy.json"
MEASUREMENTS = TOOLS / "bath_remainder_materialization_measurements.json"
INTEGRATED_MEASUREMENTS = TOOLS / "bath_remainder_integrated_measurements.json"
DECISION = TOOLS / "bath_remainder_materialization_decision.json"
PUBLICATION_DELTA = TOOLS / "bath_remainder_publication_delta.json"
LOAD_PROJECTION = TOOLS / "bath_remainder_load_projection.json"
LOAD_PROJECTION_INPUT = TOOLS / "bath_remainder_load_projection_input.json"
GAME_TEST_RECEIPT = TOOLS / "bath_remainder_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "bath_remainder_gametest.log"
CENSUS_DELTA = TOOLS / "bath_remainder_census_delta.json"
CARD_TOPOLOGY = TOOLS / "bath_remainder_card_topology.json"
READINESS = TOOLS / "bath_remainder_readiness.json"
SCHEMA = bath_mte.SCHEMA
PUBLICATION_POLICY_SCHEMA = bath_mte.PUBLICATION_POLICY_SCHEMA
GENERATED_ROOT = (
    ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/remainder"
)
PUBLICATION_POLICY_DATAPACK = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)
PUBLICATION_POLICY_DATAPACK_FILES = {
    PUBLICATION_GROUP_EXACT: PUBLICATION_POLICY_DATAPACK / "bath_remainder_exact.json",
    PUBLICATION_GROUP_EXACT_MULTI: PUBLICATION_POLICY_DATAPACK / "bath_remainder_exact_multi.json",
}
PUBLICATION_GROUP_KEYS = {
    PUBLICATION_GROUP_EXACT: "exact",
    PUBLICATION_GROUP_EXACT_MULTI: "exact_multi",
}
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/player_path_support/bath_remainder"
)
LOCKED_SUPPORT_RECIPE_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/recipe/player_path_support/bath_remainder"
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
ROW_CLASSIFICATION = bath_mte.ROW_CLASSIFICATION
TEMPLATE_DENOMINATOR = bath_mte.TEMPLATE_DENOMINATOR
T21_REACHABILITY = bath_mte.T21_REACHABILITY
T42_OWNER_LOCK = owner_runtime.DISPOSITION_LOCK
T42_OWNER_OVERLAY = owner_runtime.TRACK_OVERLAY
T42_BLOCKER = owner.BLOCKER_OVERLAY
ORDINARY_CLASS = "ordinary_optional"
T14_COUNTABLE = block_object.T14_COUNTABLE
T14_PENDING = block_object.T14_PENDING
STRATEGY_AXES = block_object.STRATEGY_AXES
MAX_INTERVAL_AXES = block_object.MAX_INTERVAL_AXES
PENDING_LOAD_VERDICT = block_object.PENDING_LOAD_VERDICT
GAME_TEST_RECEIPT_SCHEMA = 3
T35_FOUNDATION = dict(owner.T35_FOUNDATION)
T14_HARD_AUTHORED = 6600
T14_OPENING_LOAD_SOURCE = "tools/bath_mte_readiness.json#bath_remainder_opening.t14_closing"
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


def assert_runtime_id(value: Any, *, consume: bool) -> str:
    runtime = str(value or "")
    if not runtime.startswith("minecraft:") and not runtime.startswith("cruciblecraft:"):
        raise ValueError(f"bath/remainder runtime id must be minecraft: or cruciblecraft:: {runtime}")
    if consume and runtime.startswith(("gregtech:", "gregapi:")):
        raise ValueError(f"bath/remainder consume runtime leaked a source id: {runtime}")
    return runtime


def bath_mte_closed_family_ids() -> set[str]:
    return set(bath_mte.production_family_ids())


def overlay_bath_remainder_families() -> list[dict[str, Any]]:
    overlay = load_json(T42_OWNER_OVERLAY)
    blocker = load_json(T42_BLOCKER)
    if overlay.get("source_revision") != SOURCE_REVISION:
        raise ValueError("owner overlay source_revision drifted")
    closed = bath_mte_closed_family_ids()
    if len(closed) != BATH_MTE_CLOSED_FAMILIES:
        raise ValueError(f"bath/mte closed family count drifted: {len(closed)}")
    by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    selected: list[dict[str, Any]] = []
    for row in overlay.get("families") or []:
        if str(row.get("host") or "") != HOST:
            continue
        family_id = str(row.get("family_id") or "")
        if family_id in closed:
            continue
        kinds = list((by_id.get(family_id) or {}).get("unique_kinds") or [])
        if kinds == ["mte"] and str(row.get("current_owner") or "") == bath_mte.OWNER_TRACK:
            raise ValueError(f"{family_id}: bath/mte MTE family leaked into bath/remainder remainder")
        selected.append(row)
    selected.sort(key=lambda row: str(row["family_id"]))
    if len(selected) != CANDIDATE_FAMILY_COUNT:
        raise ValueError(
            f"bath/remainder remainder families {len(selected)} != {CANDIDATE_FAMILY_COUNT}"
        )
    return selected


def assert_bath_mte_opening_current() -> dict[str, Any]:
    readiness = load_json(BATH_MTE_READINESS)
    if readiness.get("status") != "BATH_MTE_READY":
        raise ValueError("bath/remainder requires BATH_MTE_READY")
    if readiness.get("failed_gates"):
        raise ValueError("bath/remainder requires bath/mte failed_gates=[]")
    evidence = readiness.get("evidence") or {}
    if evidence.get("remaining_recipe_gap") != OPENING_EXECUTION_GAP:
        raise ValueError("bath/remainder opening gap is not bath/mte remaining_recipe_gap 1894")
    load = evidence.get("load") or {}
    if load.get("authored_closing") != OPENING_AUTHORED_ENTRIES:
        raise ValueError("bath/remainder opening authored is not bath/mte authored_closing 5706")
    opening = readiness.get("bath_remainder_opening") or {}
    t14 = opening.get("recipe_load_closing") or {}
    if t14.get("eager_publication_rows") != OPENING_EAGER_ROWS:
        raise ValueError("bath/remainder opening eager is not bath/mte 13-group remeasurement 14")
    if t14.get("lazy_logical_rows") != OPENING_LAZY_ROWS:
        raise ValueError("bath/remainder opening lazy is not bath/mte 13-group remeasurement 2758")
    if t14.get("lazy_cache_ceiling_rows") != OPENING_CACHE_CEILING_ROWS:
        raise ValueError("bath/remainder opening cache is not bath/mte 13-group remeasurement 222")
    census = load_json(BATH_MTE_CENSUS_DELTA)
    if census.get("status") != "BATH_MTE_CENSUS_DELTA_READY":
        raise ValueError("bath/remainder requires BATH_MTE_CENSUS_DELTA_READY")
    topology = load_json(BATH_MTE_CARD_TOPOLOGY)
    if topology.get("next_issue_id") != "bath/remainder":
        raise ValueError("bath/remainder requires bath/mte topology next_issue_id=bath/remainder")
    if topology.get("preassigned_host") or topology.get("preassigned_family_ids"):
        raise ValueError("bath/remainder must not inherit a bath/mte-preassigned host or family ids")
    forward = load_json(FORWARD_V2_READINESS)
    if forward.get("status") != "FORWARD_RECIPE_AUTHORITY_V2_READY":
        raise ValueError("bath/remainder requires FORWARD_RECIPE_AUTHORITY_V2_READY")
    return {
        "authored_entries": OPENING_AUTHORED_ENTRIES,
        "eager_rows": OPENING_EAGER_ROWS,
        "execution_gap": OPENING_EXECUTION_GAP,
        "lazy_rows": OPENING_LAZY_ROWS,
        "cache_ceiling_rows": OPENING_CACHE_CEILING_ROWS,
        "recipe_load_closing": t14,
        "bath_mte_production_lock_sha256": bath_mte.production_lock_sha256(),
        "v2_load_policy_sha256": census.sha256_file(LOAD_POLICY_V2),
    }


def opening_from_t46() -> dict[str, Any]:
    return assert_bath_mte_opening_current()


def t14_opening_load() -> dict[str, Any]:
    opening = assert_bath_mte_opening_current()
    return dict(opening["recipe_load_closing"])


def dump_present() -> bool:
    return BATH_DUMP.is_file()


def load_production_lock() -> dict[str, Any]:
    if not PRODUCTION_LOCK.is_file():
        raise FileNotFoundError("bath/remainder production lock is missing")
    document = load_json(PRODUCTION_LOCK)
    if document.get("status") != "BATH_REMAINDER_PRODUCTION_LOCKED":
        raise ValueError("bath/remainder production lock is not locked")
    return document


def production_lock_sha256() -> str:
    return census.sha256_file(PRODUCTION_LOCK)


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
    return census.sha256_file(path)


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
    identities_found = set(bath_mte.b0_identities())
    if bath_mte.LAYERED_PLAYER_PATH.is_file():
        layered = load_json(bath_mte.LAYERED_PLAYER_PATH)
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
        document.get("status") == "BATH_REMAINDER_EQUIVALENCE"
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
    if document.get("status") != "BATH_REMAINDER_INTEGRATED_MEASUREMENT_READY":
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
        raise FileNotFoundError("bath/remainder materialization policy is missing")
    contract = load_json(POLICY)["publication_group_contract"][group_key]
    eager, lazy, cache = contract["partitions"][winner]
    return {
        "eager_publication_rows": int(eager),
        "lazy_logical_rows": int(lazy),
        "lazy_cache_ceiling_rows": int(cache),
    }


def hybrid_eager_stable_ids(group_key: str) -> list[str]:
    if not POLICY.is_file():
        raise FileNotFoundError("bath/remainder materialization policy is missing")
    boundary = load_json(POLICY)["publication_group_contract"][group_key][
        "hybrid_boundary"
    ]
    return list(boundary.get("eager_stable_ids") or [])


def parse_write_check(description: str, argv: list[str] | None = None):
    return bath_mte.parse_write_check(description, argv)


def integrated_production_row() -> dict[str, Any] | None:
    """assembler/compact-bath/remainder production mix: historical group policies plus bath/remainder winners.

    BathRemainderIntegratedMeasurementHarness records this as
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
        decision.get("status") == "BATH_REMAINDER_MATERIALIZATION_DECISION_BLOCKED"
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
        "bath_remainder_generated_recipes": tree_sha256(GENERATED_ROOT) if GENERATED_ROOT.exists() else None,
        "bath_remainder_locked_support": locked_support_tree_sha256()
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
        "bath/remainder",
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
            "-PwaveRecipes=bath/remainder result bound to the bath/remainder production lock, recipe "
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
        return ["missing GameTest receipt: tools/bath_remainder_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_gametest_ids()
    java_hash = census.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("bath/remainder GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("bath/remainder GameTest receipt command is missing -PwaveRecipes=bath/remainder")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("bath/remainder GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("bath/remainder GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(f"bath/remainder GameTest receipt passed count is not {GAME_TEST_REQUIRED}")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("bath/remainder GameTest receipt pass_marker drifted")
    if receipt.get("java_sha256") != java_hash:
        errors.append("bath/remainder GameTest Java sha256 drifted")
    if sorted(receipt.get("test_ids") or []) != test_ids:
        errors.append("bath/remainder GameTest receipt test_ids drifted")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"bath/remainder GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("failed") != 0:
        errors.append("bath/remainder GameTest receipt records failures")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("bath/remainder GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("bath/remainder GameTest receipt schema_version must be 3")
    bound = receipt.get("bound_artifacts") or {}
    live_bound = bound_gametest_artifacts()
    support_hash = bound.get("bath_remainder_locked_support")
    live_support = live_bound.get("bath_remainder_locked_support")
    if support_hash in (None, EMPTY_TREE_SHA256):
        errors.append(
            "bath/remainder GameTest bath_remainder_locked_support is bound to an empty tree; "
            "real recipes are under recipe/bath_remainder_player_path_support/"
        )
    if live_support in (None, EMPTY_TREE_SHA256):
        errors.append(
            "bath/remainder live bath_remainder_locked_support tree is empty; "
            "bind GameTest to recipe/bath_remainder_player_path_support/"
        )
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "bath/remainder",
            bound,
            live_bound,
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "bath/remainder GameTest receipt log_path must be the committed evidence file "
            "tools/bath_remainder_gametest.log"
        )
    if not GAME_TEST_EVIDENCE_LOG.is_file():
        errors.append(
            "bath/remainder GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(
            read_gametest_log(GAME_TEST_EVIDENCE_LOG)
        )
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "bath/remainder GameTest receipt log_fingerprint does not match the "
                "committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("bath/remainder committed GameTest evidence log is not PASS")
    return errors


def v2_policy_hash() -> str:
    if not LOAD_POLICY_V2.is_file():
        raise FileNotFoundError("bath/remainder forward-v2 load policy is missing")
    return census.sha256_file(LOAD_POLICY_V2)


refresh_gametest_required()
