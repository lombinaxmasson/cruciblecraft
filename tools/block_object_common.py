#!/usr/bin/env python3
"""Paths and status constants for the block/object block-object recipe wave."""
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
from tools import census_common as census
from tools import owner_partition_common as owner
from tools import owner_runtime_common as owner_runtime
from tools import smelter_stone_common as smelter_stone
from tools import storage_common as storage

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

OWNER = "portfolio:track_a/block_object"
OPENING_EXECUTION_GAP = 3076
MIN_PRODUCTION_FAMILIES = 300
OPENING_AUTHORED_ENTRIES = 4484
T14_HARD_AUTHORED = 6600
OWNER_TRACK = "object_expression/block"
ALLOWED_HOSTS = (
    "cruciblecraft:smelter",
    "cruciblecraft:drying",
)
HOST = "cruciblecraft:smelter+drying"
EXCLUDED_TEMPLATE_KEYS = {
    "gt.recipe.centrifuge#0085",
    "gt.recipe.centrifuge#0086",
    "gt.recipe.centrifuge#0087",
}
SMELTER_GROUP = "cruciblecraft:smelter/block"
DRYING_GROUP = "cruciblecraft:drying/block"
PUBLICATION_GROUPS = (SMELTER_GROUP, DRYING_GROUP)
HOST_CONFIG = {
    "cruciblecraft:smelter": {
        "dump": ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.smelter.json",
        "group": SMELTER_GROUP,
        "map_index": 7,
        "source_map": "gt.recipe.smelter",
        "target_map": "cruciblecraft:smelter",
    },
    "cruciblecraft:drying": {
        "dump": ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.drying.json",
        "group": DRYING_GROUP,
        "map_index": 5,
        "source_map": "gt.recipe.drying",
        "target_map": "cruciblecraft:drying",
    },
}
BATH_MAP_INDEX = 4
BATH_SOURCE_MAP = "gt.recipe.bath"
BATH_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.bath.json"
RUNTIME_PREFIX = "cruciblecraft:gt_block/"
ORACLE_RELATIVE = storage.ORACLE_RELATIVE
ORACLE_ROOT = storage.ORACLE_ROOT
oracle_revision = storage.oracle_revision
oracle_dirty = storage.oracle_dirty

WORK_SET = TOOLS / "block_object_work_set.json"
SOURCE_PACK = TOOLS / "block_object_source_pack_manifest.json"
SOURCE = TOOLS / "block_object_source.json"
RECEIPT = TOOLS / "block_object_source_receipt.json"
REVIEW = TOOLS / "block_object_source_review.json"
BATH_AUDIT = TOOLS / "block_object_bath_mte_audit.json"
OPERAND_RUNTIME_MAP = TOOLS / "block_object_operand_runtime_map.json"
CANDIDATE_SELECTION = TOOLS / "block_object_candidate_selection.json"
OBJECT_EVIDENCE = TOOLS / "block_object_object_expression_evidence.json"
PRODUCTION_LOCK = TOOLS / "block_object_production_lock.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "block_object_runtime_dependency_manifest.json"
COMPILE_SPEC = TOOLS / "block_object_recipe_compile_spec.json"
BLOCK_CATALOG = TOOLS / "block_object_catalog.json"
BUNDLED_BLOCK_CATALOG = (
    ROOT / "src/main/resources/data/cruciblecraft/gt_block_object_catalog.json"
)
PUBLICATION_GROUP_MANIFEST = TOOLS / "block_object_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "block_object_shard_manifest.json"
PLAYER_PATH = TOOLS / "block_object_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "block_object_layered_player_path.json"
PLAYER_PATH_SUPPORT = TOOLS / "block_object_player_path_support.json"
REQUIRED_FORMS = TOOLS / "block_object_required_forms.json"
EQUIVALENCE = TOOLS / "block_object_equivalence.json"
COMPILE_REPORT = TOOLS / "block_object_compile_report.json"
ANALYZE_REPORT = TOOLS / "block_object_analyze_report.json"
POLICY = TOOLS / "block_object_materialization_policy.json"
MEASUREMENTS = TOOLS / "block_object_materialization_measurements.json"
DECISION = TOOLS / "block_object_materialization_decision.json"
PUBLICATION_DELTA = TOOLS / "block_object_publication_delta.json"
LOAD_PROJECTION = TOOLS / "block_object_load_projection.json"
LOAD_PROJECTION_INPUT = TOOLS / "block_object_load_projection_input.json"
GAME_TEST_RECEIPT = TOOLS / "block_object_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "block_object_gametest.log"
CENSUS_DELTA = TOOLS / "block_object_census_delta.json"
CARD_TOPOLOGY = TOOLS / "block_object_card_topology.json"
READINESS = TOOLS / "block_object_readiness.json"
SCHEMA = TOOLS / "compact_recipe_family_source.schema.json"
IR_SCHEMA = TOOLS / "recipe_bulk_ir.schema.json"
PUBLICATION_POLICY_SCHEMA = (
    ROOT
    / "src/main/resources/data/cruciblecraft/schema/compact_publication_policy.schema.json"
)
GENERATED_ROOT = (
    ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe"
)
PUBLICATION_POLICY_DATAPACK = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)
CATALOG_FIXTURE_ROOT = (
    ROOT / "src/test/resources/block_object_compiler_fixture/data/cruciblecraft/recipe/block_object_catalog"
)
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/recipe/player_path_support/block_object"
)
GAME_TEST_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/BlockObjectGameTests.java"
)
GAME_TEST_NAMESPACE = "cruciblecraft_wave_block_object"
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=block/object --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=block/object"
GAME_TEST_METHOD_RE = re.compile(r"public static void (\w+)\s*\(\s*GameTestHelper")
ROW_CLASSIFICATION = TOOLS / "machine_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "chemical_axis_template_denominator.json"
T21_REACHABILITY = TOOLS / "chemical_axis_operand_reachability.json"
T42_OWNER_LOCK = owner_runtime.DISPOSITION_LOCK
T42_OWNER_OVERLAY = owner_runtime.TRACK_OVERLAY
T42_BLOCKER = owner.BLOCKER_OVERLAY
T42_SNAPSHOT = owner.FAMILY_OPERAND_SNAPSHOT
RECIPE_FAMILIES = census.RECIPE_FAMILIES
STORAGE_LOCK_READINESS = storage.READINESS
STORAGE_LOCK_CARD_TOPOLOGY = storage.CARD_TOPOLOGY
SMELTER_STONE_READINESS = smelter_stone.READINESS
T42_OWNER_READINESS = owner_runtime.READINESS
ASSEMBLER_WOOD_READINESS = TOOLS / "assembler_wood_readiness.json"
CACHE_CEILING = 24
HYBRID_CUTOFF = 0
GAME_TEST_RECEIPT_SCHEMA = 3
CLOSING_EXECUTION_GAP = 2697
COMPLETION_DELTA = 379
RECLASSIFICATION_DELTA = 0
EXPECTED_REMAINING_ORDINARY_FAMILIES = CLOSING_EXECUTION_GAP
T14_OPENING_LOAD_SOURCE = "tools/storage_census_delta.json#recipe_load.closing"
MATERIAL_REGISTRATION_GATE_JSON = (
    ROOT / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MATERIAL_REGISTRATION_GATE_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/material/MaterialRegistrationGate.java"
)
PUBLICATION_POLICY_DATAPACK_FILES = {
    SMELTER_GROUP: PUBLICATION_POLICY_DATAPACK / "smelter_block_catalog.json",
    DRYING_GROUP: PUBLICATION_POLICY_DATAPACK / "drying_block_catalog.json",
}
PUBLICATION_GROUP_KEYS = {
    SMELTER_GROUP: "smelter",
    DRYING_GROUP: "drying",
}
T14_COUNTABLE = smelter_stone.T14_COUNTABLE
T14_PENDING = smelter_stone.T14_PENDING
STRATEGY_AXES = smelter_stone.STRATEGY_AXES
MAX_INTERVAL_AXES = smelter_stone.MAX_INTERVAL_AXES
PENDING_LOAD_VERDICT = smelter_stone.PENDING_LOAD_VERDICT
T35_FOUNDATION = dict(owner.T35_FOUNDATION)
OPENING_DISPOSITION = "planned"
OPENING_CLOSURE = "incomplete"
OPENING_FIDELITY = "source_backed"
OPENING_LOAD = "pending"
CLOSING_DISPOSITION = "implemented"
CLOSING_CLOSURE = "closed"
CLOSING_LOAD = "measured"
ORDINARY_CLASS = "ordinary_optional"
SCATTER_FEATURE = "cruciblecraft:gt_block_object_scatter"

DYE_WOOL = (
    "white",
    "orange",
    "magenta",
    "light_blue",
    "yellow",
    "lime",
    "pink",
    "gray",
    "light_gray",
    "cyan",
    "purple",
    "blue",
    "brown",
    "green",
    "red",
    "black",
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


def publication_group_for(host: str) -> str:
    config = HOST_CONFIG.get(host)
    if config is None:
        raise ValueError(f"block/object host is not in production scope: {host}")
    return str(config["group"])


def object_kind(source_item: str) -> str:
    item = str(source_item)
    if ".slab." in item:
        return "slab"
    if "lilypad" in item:
        return "lily"
    if "spikes" in item:
        return "spike"
    if ".rail." in item:
        return "rail"
    if ".bars." in item:
        return "bars"
    if ".log." in item:
        return "log"
    if "bale" in item:
        return "bale"
    if "cfoam.fresh" in item:
        return "cfoam_fresh"
    if "cfoam" in item:
        return "cfoam"
    if "sands" in item:
        return "sands"
    return "solid"


def registry_path(source_item: str, meta: int) -> str:
    tail = str(source_item).removeprefix("gregtech:gt.block.")
    slug = tail.replace(".", "_")
    return f"gt_block/{slug}_m{meta}"


def runtime_id(source_item: str, meta: int) -> str:
    return f"cruciblecraft:{registry_path(source_item, meta)}"


def texture_for(source_item: str, meta: int) -> str:
    item = str(source_item)
    if "cfoam" in item:
        return f"minecraft:block/{DYE_WOOL[int(meta) % 16]}_wool"
    if "asphalt" in item:
        return "minecraft:block/black_concrete"
    if "concrete" in item:
        return "minecraft:block/light_gray_concrete"
    if "log" in item and "fireproof" in item:
        return "minecraft:block/stripped_oak_log"
    if "log" in item:
        return "minecraft:block/oak_log"
    if "bale.crop" in item:
        return "minecraft:block/hay_block_top"
    if "bale" in item:
        return "minecraft:block/hay_block_side"
    if "bars" in item:
        return "minecraft:block/iron_bars"
    if "rail" in item:
        return "minecraft:block/rail"
    if "spikes" in item:
        return "minecraft:block/iron_block"
    if "lilypad" in item:
        return "minecraft:block/lily_pad"
    return "minecraft:block/stone"


def english_name(source_item: str, meta: int) -> str:
    tail = str(source_item).removeprefix("gregtech:gt.block.").replace(".", " ")
    return f"GT {tail} m{meta}"


def chinese_name(source_item: str, meta: int) -> str:
    return f"GT方块 {source_item.split('.')[-1]} m{meta}"


def assert_runtime_id(value: Any, *, consume: bool) -> str:
    runtime = str(value or "")
    if not runtime.startswith("minecraft:") and not runtime.startswith("cruciblecraft:"):
        raise ValueError(f"block/object runtime id must be minecraft: or cruciblecraft:: {runtime}")
    if consume and runtime.startswith("gregtech:"):
        raise ValueError(f"block/object consume runtime leaked a source id: {runtime}")
    return runtime


def overlay_block_families() -> list[dict[str, Any]]:
    overlay = load_json(T42_OWNER_OVERLAY)
    if overlay.get("source_revision") != SOURCE_REVISION:
        raise ValueError("owner overlay source_revision drifted")
    lossy = lossy_wildcard_family_ids()
    selected: list[dict[str, Any]] = []
    for row in overlay.get("families") or []:
        if row.get("current_owner") != OWNER_TRACK:
            continue
        host = str(row.get("host") or "")
        template_key = str(row.get("template_key") or "")
        family_id = str(row.get("family_id") or "")
        if host not in ALLOWED_HOSTS:
            continue
        if template_key in EXCLUDED_TEMPLATE_KEYS:
            continue
        if family_id in lossy:
            continue
        if int(row.get("relation_count") or 0) != 1:
            continue
        selected.append(row)
    selected.sort(key=lambda row: str(row["family_id"]))
    return selected


def lossy_wildcard_family_ids() -> set[str]:
    """Families whose source block operand uses GT wildcard meta (lossy vs exact mapping)."""
    snapshot = load_json(T42_SNAPSHOT)
    items = list((snapshot.get("dictionaries") or {}).get("items") or [])
    overlay = load_json(T42_OWNER_OVERLAY)
    wanted = {
        str(row.get("family_id") or "")
        for row in overlay.get("families") or []
        if row.get("current_owner") == OWNER_TRACK
        and str(row.get("host") or "") in ALLOWED_HOSTS
    }
    lossy: set[str] = set()
    for family in snapshot.get("families") or []:
        family_id = str(family.get("family_id") or "")
        if family_id not in wanted:
            continue
        for relation in family.get("relations") or []:
            for operand in relation.get("operands") or []:
                item_index = operand[1]
                meta = operand[3]
                if item_index is None:
                    continue
                item = items[item_index]
                if str(item).startswith("gregtech:gt.block.") and not isinstance(meta, int):
                    lossy.add(family_id)
    return lossy


def load_production_lock() -> dict[str, Any]:
    if not PRODUCTION_LOCK.is_file():
        raise FileNotFoundError("block/object production lock is missing")
    document = load_json(PRODUCTION_LOCK)
    if document.get("status") != "BLOCK_OBJECT_PRODUCTION_LOCKED":
        raise ValueError("block/object production lock is not locked")
    return document


def production_family_count() -> int:
    return int(load_production_lock()["production"]["family_count"])


def production_relation_count() -> int:
    return int(load_production_lock()["production"]["relation_count"])


def production_lock_sha256() -> str:
    return census.sha256_file(PRODUCTION_LOCK)


def generated_family_files() -> list[Path]:
    files: list[Path] = []
    for prefix in ("smelter/block", "drying/block"):
        root = GENERATED_ROOT / prefix
        if not root.is_dir():
            continue
        files.extend(
            path for path in root.rglob("gt_recipe_*.json") if path.is_file()
        )
    return sorted(files)


def _tree_sha256(root: Path) -> str | None:
    if not root.exists():
        return None
    hasher = hashlib.sha256()
    if root.is_file():
        hasher.update(root.read_bytes())
        return hasher.hexdigest()
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        rel = path.relative_to(root).as_posix().encode("utf-8")
        hasher.update(rel)
        hasher.update(path.read_bytes())
    return hasher.hexdigest()


def ingredient_identity(ingredient: dict[str, Any]) -> str:
    item_id = str(ingredient.get("item") or ingredient.get("items") or "")
    return f"item:{item_id}"


def discovered_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted(
        {match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)}
    )


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    raw_winners = nested.get("group_winners") or {}
    smelter_winner = raw_winners.get("smelter") or raw_winners.get(SMELTER_GROUP)
    drying_winner = raw_winners.get("drying") or raw_winners.get(DRYING_GROUP)
    winners = {
        "smelter": smelter_winner,
        "drying": drying_winner,
        SMELTER_GROUP: smelter_winner,
        DRYING_GROUP: drying_winner,
    }
    card_winner = nested.get("card_aggregate_winner") or decision.get("winner")
    blocked = (
        decision.get("status") == "BLOCK_OBJECT_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or card_winner in {None, "", "BLOCKED"}
        or smelter_winner in {None, ""}
        or drying_winner in {None, ""}
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_block_object_recipe_load_benchmark as benchmark

            benchmark.validate_artifact(decision)
            recomputable = True
        except Exception:
            recomputable = False
            blocked = True
    return {
        "blocked": blocked,
        "card_aggregate_winner": None if blocked else card_winner,
        "group_winners": winners,
        "recomputable": recomputable,
    }


def dump_present() -> bool:
    return all(Path(config["dump"]).is_file() for config in HOST_CONFIG.values())


def t14_opening_load() -> dict[str, Any]:
    smelter_stone_census = load_json(smelter_stone.CENSUS_DELTA)
    closing = dict((smelter_stone_census.get("recipe_load_load") or {}).get("closing") or {})
    if not closing:
        raise ValueError("missing tools/smelter_stone_census_delta.json#recipe_load.closing")
    storage_census = load_json(storage.CENSUS_DELTA)
    authored = None
    for axis in (storage_census.get("recipe_load_load") or {}).get("axes") or []:
        if axis.get("axis") == "datapack_authored_entries":
            authored = axis.get("closing")
    if authored is None:
        raise ValueError("missing storage/lock authored closing")
    closing["datapack_authored_entries"] = authored
    return closing


def parse_write_check(description: str, argv: list[str] | None = None):
    import argparse

    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    return args


def production_families() -> list[dict[str, Any]]:
    return list((load_production_lock().get("production") or {}).get("families") or [])


def production_family_ids() -> list[str]:
    return [str(row["family_id"]) for row in production_families()]


def production_template_keys() -> list[str]:
    return [str(row["template_key"]) for row in production_families()]


from tools.recipe_bulk.membership import membership_root


def membership_root_sha256(family_ids: list[str], stable_ids: list[str]) -> str:
    return membership_root(family_ids, stable_ids)


def b0_identities() -> set[str]:
    t21 = load_json(T21_REACHABILITY)
    identities = set((t21.get("closure") or {}).get("reachable_identities") or [])
    baseline = load_json(owner.REACHABILITY_BASELINE)
    identities.update(
        value
        for value in baseline.get("added_identities") or []
        if str(value).startswith(("item:", "fluid:"))
    )
    if smelter_stone.LAYERED_PLAYER_PATH.is_file():
        layered = load_json(smelter_stone.LAYERED_PLAYER_PATH)
        identities.update((layered.get("b1") or {}).get("added_identities") or [])
        identities.update((layered.get("b2") or {}).get("added_identities") or [])
    return identities


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
        "block_object_generated_recipes": _tree_sha256(GENERATED_ROOT),
        "block_object_locked_support": _tree_sha256(LOCKED_SUPPORT_ROOT),
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
        for marker in ("cruciblecraft_wave_block_object", "BlockObjectGameTests", "-PwaveRecipes=block/object")
    )


GAME_TEST_REQUIRED = 0
GAME_TEST_PASS_MARKER = "All 0 required tests passed :)"


def refresh_gametest_required() -> int:
    global GAME_TEST_REQUIRED, GAME_TEST_PASS_MARKER
    GAME_TEST_REQUIRED = len(discovered_gametest_ids())
    GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"
    return GAME_TEST_REQUIRED


def read_gametest_log(path: Path) -> str:
    return smelter_stone.read_gametest_log(path)


def normalize_gametest_log_text(text: str) -> str:
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
            "-PwaveRecipes=block/object result bound to MaterialRegistrationGate, the block/object "
            "recipe resource trees, and the committed UTF-8 evidence log. "
            "The gitignored run-block/object-recipes directory is not evidence. "
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
    from tools import gametest_receipt_roots

    refresh_gametest_required()
    if document is None and not GAME_TEST_RECEIPT.is_file():
        return ["missing GameTest receipt: tools/block_object_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_gametest_ids()
    java_hash = census.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("block/object GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("block/object GameTest receipt command is missing -PwaveRecipes=block/object")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("block/object GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("block/object GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(f"block/object GameTest receipt passed count is not {GAME_TEST_REQUIRED}")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("block/object GameTest receipt pass_marker drifted")
    if receipt.get("java_sha256") != java_hash:
        errors.append("block/object GameTest Java sha256 drifted")
    if sorted(receipt.get("test_ids") or []) != test_ids:
        errors.append("block/object GameTest receipt test_ids drifted")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"block/object GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("failed") != 0:
        errors.append("block/object GameTest receipt records failures")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("block/object GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("block/object GameTest receipt schema_version must be 3")
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "block/object",
            receipt.get("bound_artifacts") or {},
            bound_gametest_artifacts(),
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "block/object GameTest receipt log_path must be the committed evidence file "
            "tools/block_object_gametest.log"
        )
    if not GAME_TEST_EVIDENCE_LOG.is_file():
        errors.append(
            "block/object GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(
            read_gametest_log(GAME_TEST_EVIDENCE_LOG)
        )
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "block/object GameTest receipt log_fingerprint does not match the "
                "committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("block/object committed GameTest evidence log is not PASS")
    return errors


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "block/object",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def partition_for_winner(
    winner: str | None,
    group_key: str = "smelter",
) -> dict[str, int | None]:
    empty = {
        "eager_publication_rows": None,
        "lazy_logical_rows": None,
        "lazy_cache_ceiling_rows": None,
    }
    if winner in (None, "", "BLOCKED") or not POLICY.is_file():
        return empty
    policy = load_json(POLICY)
    contract_key = "card_aggregate" if group_key == "card" else group_key
    contract = (policy.get("publication_group_contract") or {}).get(contract_key) or {}
    partitions = contract.get("partitions") or {}
    row = partitions.get(winner)
    if not isinstance(row, list) or len(row) != 3:
        return empty
    return {
        "eager_publication_rows": int(row[0]),
        "lazy_logical_rows": int(row[1]),
        "lazy_cache_ceiling_rows": int(row[2]),
    }


def hybrid_eager_stable_ids(group_key: str = "smelter") -> list[str]:
    if not POLICY.is_file():
        return []
    policy = load_json(POLICY)
    contract = (policy.get("publication_group_contract") or {}).get(group_key) or {}
    return list(
        ((contract.get("hybrid_boundary") or {}).get("eager_stable_ids")) or []
    )


def authored_family_path(family: dict[str, Any]) -> Path:
    template = str(family.get("template_key") or family.get("source_template") or "")
    host = str(family.get("host") or "")
    folder = "smelter" if "smelter" in host or template.startswith("gt.recipe.smelter") else "drying"
    filename = template.replace(".", "_").replace("#", "_") + ".json"
    return GENERATED_ROOT / folder / filename
