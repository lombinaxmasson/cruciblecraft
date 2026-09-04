#!/usr/bin/env python3
"""Shared paths and selection helpers for the smelter/stone Smelter stone bulk wave."""
from __future__ import annotations

import hashlib
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
from tools import owner_partition_common as owner
from tools import owner_runtime_common as owner_runtime

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

HOST = "cruciblecraft:smelter"
SOURCE_MAP = "gt.recipe.smelter"
TARGET_MAP = "cruciblecraft:smelter"
OWNER = "portfolio:track_a/smelter_stone"
SMELTER_MAP_INDEX = 7
STONE_GROUP = "cruciblecraft:smelter/stone"
PUBLICATION_GROUPS = (STONE_GROUP,)
PUBLICATION_GROUP_COUNT = 1
NOTEBLOCK_SOURCE = "minecraft:noteblock"
NOTEBLOCK_RUNTIME = "minecraft:note_block"
STONE_ITEM_PREFIX = "gregtech:gt.stone."
RUNTIME_PREFIX = "cruciblecraft:gt_stone/"
COHORT_A_OWNER = "identity_mapping/unmapped"
COHORT_B_OWNER = "recipe_wave/smelter"
ALLOWED_UNSUPPORTED = {(), ("unmapped",)}
ALLOWED_SECONDARY = frozenset({"residual_not_ready", "unmapped_operands"})
MISSING_RUNTIME_PREFIX = "item_inputs:gregtech:gt.stone."

CATALOG_FAMILY_COUNT = 407
CATALOG_RELATION_COUNT = 407
PRODUCTION_FAMILY_COUNT = 407
PRODUCTION_RELATION_COUNT = 407
COHORT_A_FAMILIES = 406
COHORT_B_FAMILIES = 1
STONE_IDENTITY_COUNT = 119
OPENING_EXECUTION_GAP = 3483
CLOSING_EXECUTION_GAP = 3076
COMPLETION_DELTA = 407
RECLASSIFICATION_DELTA = 0
T42_OWNER_GAP = 3483

EXPECTED_SELECTION_SHA256 = (
    "52aedf63dcc1196f2fd43fc6da97a22ad119b2bd9bd7a59954d99ff83c73211b"
)
EXPECTED_DISTRIBUTION = {1: 407}

STONE_ENGLISH = {
    "andesite": "Andesite",
    "basalt": "Basalt",
    "blueschist": "Blueschist",
    "diorite": "Diorite",
    "granite": "Granite",
    "granite_black": "Black Granite",
    "granite_red": "Red Granite",
    "greenschist": "Greenschist",
    "kimberlite": "Kimberlite",
    "komatiite": "Komatiite",
    "limestone": "Limestone",
    "marble": "Marble",
    "prismarine_dark": "Dark Prismarine",
    "prismarine_light": "Light Prismarine",
    "quartzite": "Quartzite",
    "shale": "Shale",
    "slate": "Slate",
}
STONE_CHINESE = {
    "andesite": "安山岩",
    "basalt": "玄武岩",
    "blueschist": "蓝片岩",
    "diorite": "闪长岩",
    "granite": "花岗岩",
    "granite_black": "黑花岗岩",
    "granite_red": "红花岗岩",
    "greenschist": "绿片岩",
    "kimberlite": "金伯利岩",
    "komatiite": "科马提岩",
    "limestone": "石灰石",
    "marble": "大理石",
    "prismarine_dark": "暗海晶石",
    "prismarine_light": "海晶石",
    "quartzite": "石英岩",
    "shale": "页岩",
    "slate": "板岩",
}
STONE_TEXTURES = {
    "andesite": "minecraft:block/andesite",
    "basalt": "minecraft:block/basalt_side",
    "blueschist": "minecraft:block/cobbled_deepslate",
    "diorite": "minecraft:block/diorite",
    "granite": "minecraft:block/granite",
    "granite_black": "minecraft:block/blackstone",
    "granite_red": "minecraft:block/granite",
    "greenschist": "minecraft:block/mossy_cobblestone",
    "kimberlite": "minecraft:block/tuff",
    "komatiite": "minecraft:block/deepslate",
    "limestone": "minecraft:block/sandstone",
    "marble": "minecraft:block/calcite",
    "prismarine_dark": "minecraft:block/dark_prismarine",
    "prismarine_light": "minecraft:block/prismarine",
    "quartzite": "minecraft:block/quartz_block_top",
    "shale": "minecraft:block/packed_mud",
    "slate": "minecraft:block/smooth_stone",
}

CANDIDATE_SELECTION = TOOLS / "smelter_stone_candidate_selection.json"
PRODUCTION_LOCK = TOOLS / "smelter_stone_production_lock.json"
WORK_SET = TOOLS / "smelter_stone_work_set.json"
STONE_CATALOG = TOOLS / "smelter_stone_stone_catalog.json"
BUNDLED_STONE_CATALOG = (
    ROOT / "src/main/resources/data/cruciblecraft/gt_stone_catalog.json"
)
SOURCE = TOOLS / "smelter_stone_source.json"
RECEIPT = TOOLS / "smelter_stone_source_receipt.json"
REVIEW = TOOLS / "smelter_stone_source_review.json"
SOURCE_PACK = TOOLS / "smelter_stone_source_pack_manifest.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "smelter_stone_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = TOOLS / "smelter_stone_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "smelter_stone_shard_manifest.json"
SCHEMA = TOOLS / "compact_recipe_family_source.schema.json"
GENERATED_ROOT = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/smelter/stone"
)
CATALOG_FIXTURE_ROOT = (
    ROOT
    / "src/test/resources/smelter_stone_catalog_fixture/data/cruciblecraft/recipe/smelter_stone_catalog/smelter"
)
OPERAND_RUNTIME_MAP = TOOLS / "smelter_stone_operand_runtime_map.json"
PLAYER_PATH = TOOLS / "smelter_stone_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "smelter_stone_layered_player_path.json"
PLAYER_PATH_GAPS = TOOLS / "smelter_stone_player_path_gaps.json"
EQUIVALENCE = TOOLS / "smelter_stone_equivalence.json"
REQUIRED_FORMS = TOOLS / "smelter_stone_required_forms.json"
PLAYER_PATH_SUPPORT = TOOLS / "smelter_stone_player_path_support.json"
RECIPE_FAMILIES = census.RECIPE_FAMILIES
T35_CENSUS = census.CENSUS
T42_OWNER_READINESS = owner_runtime.READINESS
T42_OWNER_GAP_PARTITION = owner_runtime.GAP_PARTITION
T42_OWNER_LOCK = owner_runtime.DISPOSITION_LOCK
T42_BLOCKER = owner.BLOCKER_OVERLAY
T42_SNAPSHOT = owner.FAMILY_OPERAND_SNAPSHOT
T42_REACHABILITY = owner.REACHABILITY_BASELINE
POLICY = TOOLS / "smelter_stone_materialization_policy.json"
DECISION = TOOLS / "smelter_stone_materialization_decision.json"
MEASUREMENTS = TOOLS / "smelter_stone_materialization_measurements.json"
PUBLICATION_DELTA = TOOLS / "smelter_stone_publication_delta.json"
CENSUS_DELTA = TOOLS / "smelter_stone_census_delta.json"
SMELTER_STONE_CARD_TOPOLOGY = TOOLS / "smelter_stone_card_topology.json"
READINESS = TOOLS / "smelter_stone_readiness.json"
LOAD_PROJECTION_INPUT = TOOLS / "smelter_stone_load_projection_input.json"
LOAD_PROJECTION = TOOLS / "smelter_stone_load_projection.json"
GAME_TEST_ROOT = ROOT / "src/main/java/com/masson/cruciblecraft/gametest"
GAME_TEST_JAVA = GAME_TEST_ROOT / "SmelterStoneGameTests.java"
GAME_TEST_RECEIPT = TOOLS / "smelter_stone_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "smelter_stone_gametest.log"
GAME_TEST_NAMESPACE = "cruciblecraft_wave_smelter_stone"
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=smelter/stone --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=smelter/stone"
GAME_TEST_RECEIPT_SCHEMA = 3
GAME_TEST_METHOD_RE = re.compile(
    r"public static void (\w+)\s*\(\s*GameTestHelper"
)
GAME_TEST_RUN_LOG_CANDIDATES = (
    ROOT / "run-smelter/stone-recipes" / "logs" / "latest.log",
    ROOT / "run-smelter/stone-recipes" / "logs" / "debug.log",
)
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src/recipe_support_generated/resources/data/cruciblecraft/recipe/player_path_support/smelter_stone"
)
SMELTER_DUMP = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.smelter.json"
)
ROW_CLASSIFICATION = TOOLS / "machine_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "chemical_axis_template_denominator.json"
T21_REACHABILITY = TOOLS / "chemical_axis_operand_reachability.json"
MATERIAL_REGISTRATION_GATE_JSON = (
    ROOT / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MATERIAL_REGISTRATION_GATE_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/material/MaterialRegistrationGate.java"
)
PUBLICATION_POLICY_DATAPACK = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)
PUBLICATION_POLICY_DATAPACK_FILE = (
    PUBLICATION_POLICY_DATAPACK / "smelter_stone_catalog.json"
)
PUBLICATION_POLICY_SCHEMA = (
    ROOT
    / "src/main/resources/data/cruciblecraft/schema/compact_publication_policy.schema.json"
)
CACHE_CEILING = 24
HYBRID_CUTOFF = 0
T14_OPENING_LOAD_SOURCE = "tools/assembler_wood_census_delta.json#recipe_load.closing"
ASSEMBLER_WOOD_CENSUS_DELTA = TOOLS / "assembler_wood_census_delta.json"
ASSEMBLER_WOOD_READINESS = TOOLS / "assembler_wood_readiness.json"
T42_CARD_TOPOLOGY = TOOLS / "owner_card_topology.json"
EXPECTED_REMAINING_ORDINARY_FAMILIES = CLOSING_EXECUTION_GAP

T35_FOUNDATION = dict(owner.T35_FOUNDATION)
OPENING_DISPOSITION = "planned"
OPENING_CLOSURE = "incomplete"
OPENING_FIDELITY = "source_backed"
OPENING_LOAD = "pending"
CLOSING_DISPOSITION = "implemented"
CLOSING_CLOSURE = "closed"
CLOSING_LOAD = "measured"
T14_COUNTABLE = centrifuge.T14_COUNTABLE
T14_PENDING = centrifuge.T14_PENDING
STRATEGY_AXES = centrifuge.STRATEGY_AXES
MAX_INTERVAL_AXES = centrifuge.MAX_INTERVAL_AXES
PENDING_LOAD_VERDICT = centrifuge.PENDING_LOAD_VERDICT
PUBLICATION_GROUP_KEYS = {STONE_GROUP: "stone"}


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


def selection_payload(family_ids: list[str]) -> bytes:
    return ("".join(f"{family_id}\n" for family_id in family_ids)).encode("utf-8")


def selection_sha256(family_ids: list[str]) -> str:
    return hashlib.sha256(selection_payload(family_ids)).hexdigest()


def expanded_count_distribution(expanded_counts: list[int]) -> dict[str, int]:
    counts = Counter(expanded_counts)
    return {str(size): counts[size] for size in sorted(counts)}


def dump_present() -> bool:
    return SMELTER_DUMP.is_file()


def canonical_family_id(template_key: str) -> str:
    return f"portfolio:track_a/cruciblecraft:smelter/{template_key}"


def family_number(template_key: str) -> int:
    return int(str(template_key).rsplit("#", 1)[-1])


def is_stone_item(item_id: str | None) -> bool:
    return str(item_id or "").startswith(STONE_ITEM_PREFIX)


def source_item_slug(item_id: str) -> str:
    text = str(item_id)
    if not text.startswith(STONE_ITEM_PREFIX):
        raise ValueError(f"not a GT stone identity: {item_id}")
    return text[len(STONE_ITEM_PREFIX) :].replace(".", "_")


def parse_stone_identity(item_id: str) -> dict[str, Any]:
    slug = source_item_slug(item_id)
    slab_match = re.fullmatch(r"(.+)_slab_(\d+)", slug)
    if slab_match:
        stone = slab_match.group(1)
        return {
            "kind": "slab",
            "slab_variant": int(slab_match.group(2)),
            "stone": stone,
            "source_item": item_id,
        }
    return {
        "kind": "full",
        "slab_variant": None,
        "stone": slug,
        "source_item": item_id,
    }


def registry_path(item_id: str, meta: int) -> str:
    return f"gt_stone/{source_item_slug(item_id)}_m{int(meta)}"


def runtime_id(item_id: str, meta: Any) -> str:
    if str(item_id) in {NOTEBLOCK_SOURCE, NOTEBLOCK_RUNTIME}:
        return NOTEBLOCK_RUNTIME
    if not is_stone_item(item_id):
        raise ValueError(f"cannot project runtime id for {item_id}")
    if meta is None or meta == "*":
        raise ValueError(f"stone runtime id requires integer meta: {item_id}")
    return f"cruciblecraft:{registry_path(item_id, int(meta))}"


def assert_runtime_id(value: Any, *, consume: bool) -> str:
    runtime = str(value or "")
    if not runtime.startswith("minecraft:") and not runtime.startswith("cruciblecraft:"):
        raise ValueError(f"smelter/stone runtime id must be minecraft: or cruciblecraft:: {runtime}")
    if consume and runtime.startswith("gregtech:"):
        raise ValueError(f"smelter/stone consume runtime leaked a source id: {runtime}")
    return runtime


def _empty(value: Any) -> bool:
    return not value


def is_cohort_a(lock_row: dict[str, Any], blocker_row: dict[str, Any]) -> bool:
    if lock_row.get("host") != HOST:
        return False
    if lock_row.get("current_owner") != COHORT_A_OWNER:
        return False
    missing = list(blocker_row.get("missing_runtime_ids") or [])
    if not missing:
        return False
    if not all(str(item).startswith(MISSING_RUNTIME_PREFIX) for item in missing):
        return False
    if (
        blocker_row.get("missing_forms")
        or blocker_row.get("missing_fluids")
        or blocker_row.get("b0_unreachable_inputs")
        or blocker_row.get("unique_objects")
        or blocker_row.get("lossy_aliases")
    ):
        return False
    semantics = tuple(blocker_row.get("unsupported_semantics") or [])
    if semantics not in ALLOWED_UNSUPPORTED:
        return False
    secondary = set(blocker_row.get("secondary_blockers") or [])
    return secondary.issubset(ALLOWED_SECONDARY)


def is_cohort_b(lock_row: dict[str, Any]) -> bool:
    return (
        lock_row.get("host") == HOST
        and lock_row.get("current_owner") == COHORT_B_OWNER
    )


def select_smelter_stone_families(
    *,
    owner_lock: dict[str, Any] | None = None,
    blocker: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    owner_lock = owner_lock if owner_lock is not None else load_json(T42_OWNER_LOCK)
    blocker = blocker if blocker is not None else load_json(T42_BLOCKER)
    if owner_lock.get("source_revision") != SOURCE_REVISION:
        raise ValueError("owner lock source_revision drifted")
    if blocker.get("status") != "T42_BLOCKER_OVERLAY":
        raise ValueError("owner blocker overlay status drifted")
    by_id = {
        str(row.get("family_id") or ""): row
        for row in blocker.get("families") or []
        if isinstance(row, dict)
    }
    selected: list[dict[str, Any]] = []
    for row in owner_lock.get("families") or []:
        if not isinstance(row, dict):
            continue
        family_id = str(row.get("family_id") or "")
        blocker_row = by_id.get(family_id) or {}
        cohort = None
        if is_cohort_b(row):
            cohort = "B"
        elif is_cohort_a(row, blocker_row):
            cohort = "A"
        if cohort is None:
            continue
        selected.append(
            {
                "cohort": cohort,
                "current_owner": row.get("current_owner"),
                "expanded_count": int(row.get("relation_count") or 0),
                "family_id": family_id,
                "host": HOST,
                "missing_runtime_ids": list(blocker_row.get("missing_runtime_ids") or []),
                "owner_state": row.get("owner_state"),
                "primary_bucket": row.get("primary_bucket"),
                "template_key": str(row.get("template_key") or ""),
            }
        )
    selected.sort(key=lambda row: str(row["family_id"]))
    family_ids = [str(row["family_id"]) for row in selected]
    if len(selected) != CATALOG_FAMILY_COUNT:
        raise ValueError(
            f"smelter/stone work set must contain {CATALOG_FAMILY_COUNT} families, got {len(selected)}"
        )
    cohort_a = [row for row in selected if row["cohort"] == "A"]
    cohort_b = [row for row in selected if row["cohort"] == "B"]
    if len(cohort_a) != COHORT_A_FAMILIES:
        raise ValueError(f"smelter/stone cohort A must be {COHORT_A_FAMILIES}, got {len(cohort_a)}")
    if len(cohort_b) != COHORT_B_FAMILIES:
        raise ValueError(f"smelter/stone cohort B must be {COHORT_B_FAMILIES}, got {len(cohort_b)}")
    if any(int(row["expanded_count"]) != 1 for row in selected):
        raise ValueError("smelter/stone work set contains a non-singleton family")
    digest = selection_sha256(family_ids)
    if digest != EXPECTED_SELECTION_SHA256:
        raise ValueError(
            "smelter/stone selection_sha256 drifted: "
            f"computed {digest}, expected {EXPECTED_SELECTION_SHA256}"
        )
    if family_ids != sorted(family_ids):
        raise ValueError("smelter/stone family ids are not sorted")
    return selected


def load_production_lock(path: Path | None = None) -> dict[str, Any]:
    document = load_json(path or PRODUCTION_LOCK)
    production = document.get("production") or {}
    families = list(production.get("families") or [])
    family_ids = [str(row["family_id"]) for row in families]
    if len(families) != PRODUCTION_FAMILY_COUNT:
        raise ValueError("smelter/stone production lock family_count drifted")
    digest = selection_sha256(family_ids)
    if production.get("selection_sha256") != digest:
        raise ValueError("smelter/stone production lock selection_sha256 drifted")
    if digest != EXPECTED_SELECTION_SHA256:
        raise ValueError("smelter/stone production lock selection_sha256 drifted from 407 contract")
    if int(production.get("relation_count") or 0) != PRODUCTION_RELATION_COUNT:
        raise ValueError("smelter/stone production lock relation_count is not 407")
    return document


def production_lock_sha256() -> str:
    load_production_lock()
    return census.sha256_file(PRODUCTION_LOCK)


def production_families() -> list[dict[str, Any]]:
    return list((load_production_lock().get("production") or {}).get("families") or [])


def production_family_ids() -> list[str]:
    return [str(row["family_id"]) for row in production_families()]


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
        "smelter_stone_generated_recipes": _tree_sha256(GENERATED_ROOT),
        "smelter_stone_locked_support": _tree_sha256(LOCKED_SUPPORT_ROOT),
        "runtime_dependency_manifest": _file_sha256(RUNTIME_DEPENDENCY_MANIFEST),
        "publication_group_manifest": _file_sha256(PUBLICATION_GROUP_MANIFEST),
        "shard_manifest": _file_sha256(SHARD_MANIFEST),
    }


def discovered_smelter_stone_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted(
        {match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)}
    )


def player_gametest_source_present() -> bool:
    if not GAME_TEST_JAVA.is_file():
        return False
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return any(
        marker in text
        for marker in ("cruciblecraft_wave_smelter_stone", "SmelterStoneGameTests", "-PwaveRecipes=smelter/stone")
    )


def production_family_count() -> int:
    return PRODUCTION_FAMILY_COUNT


def production_relation_count() -> int:
    return PRODUCTION_RELATION_COUNT


def generated_family_files(scope: str = "production") -> list[Path]:
    root = GENERATED_ROOT if scope == "production" else CATALOG_FIXTURE_ROOT
    if scope not in {"production", "catalog"}:
        raise ValueError(f"unknown smelter/stone generated scope: {scope}")
    if not root.is_dir():
        return []
    return sorted(
        path
        for path in root.glob("gt_recipe_smelter_*.json")
        if path.is_file()
    )


def ingredient_identity(ingredient: dict[str, Any]) -> str:
    item_id = str(ingredient.get("item") or ingredient.get("items") or "")
    return f"item:{item_id}"


def t14_opening_load() -> dict[str, Any]:
    census = load_json(ASSEMBLER_WOOD_CENSUS_DELTA)
    closing = (census.get("recipe_load_load") or {}).get("closing") or {}
    if not closing:
        raise ValueError("missing tools/assembler_wood_census_delta.json#recipe_load.closing")
    return dict(closing)


GAME_TEST_REQUIRED = 0
GAME_TEST_PASS_MARKER = "All 0 required tests passed :)"


def refresh_gametest_required() -> int:
    global GAME_TEST_REQUIRED, GAME_TEST_PASS_MARKER
    GAME_TEST_REQUIRED = len(discovered_smelter_stone_gametest_ids())
    GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"
    return GAME_TEST_REQUIRED


def read_gametest_log(path: Path) -> str:
    return centrifuge.read_gametest_log(path)


def normalize_gametest_log_text(text: str) -> str:
    return centrifuge.normalize_gametest_log_text(text)


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
    test_ids = discovered_smelter_stone_gametest_ids()
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
            "-PwaveRecipes=smelter/stone result bound to MaterialRegistrationGate, the smelter/stone "
            "recipe resource trees, and the committed UTF-8 evidence log. "
            "The gitignored run-smelter/stone-recipes directory is not evidence. "
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
        return ["missing GameTest receipt: tools/smelter_stone_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_smelter_stone_gametest_ids()
    java_hash = census.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("smelter/stone GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("smelter/stone GameTest receipt command is missing -PwaveRecipes=smelter/stone")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("smelter/stone GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("smelter/stone GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(f"smelter/stone GameTest receipt passed count is not {GAME_TEST_REQUIRED}")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("smelter/stone GameTest receipt pass_marker drifted")
    if receipt.get("java_sha256") != java_hash:
        errors.append("smelter/stone GameTest Java sha256 drifted")
    if sorted(receipt.get("test_ids") or []) != test_ids:
        errors.append("smelter/stone GameTest receipt test_ids drifted")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"smelter/stone GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("failed") != 0:
        errors.append("smelter/stone GameTest receipt records failures")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("smelter/stone GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("smelter/stone GameTest receipt schema_version must be 3")
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "smelter/stone",
            receipt.get("bound_artifacts") or {},
            bound_gametest_artifacts(),
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "smelter/stone GameTest receipt log_path must be the committed evidence file "
            "tools/smelter_stone_gametest.log"
        )
    if not GAME_TEST_EVIDENCE_LOG.is_file():
        errors.append(
            "smelter/stone GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(
            read_gametest_log(GAME_TEST_EVIDENCE_LOG)
        )
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "smelter/stone GameTest receipt log_fingerprint does not match the "
                "committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("smelter/stone committed GameTest evidence log is not PASS")
    return errors


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "smelter/stone",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def production_template_keys() -> list[str]:
    return [str(row["template_key"]) for row in production_families()]


def partition_for_winner(
    winner: str | None,
    group_key: str = "stone",
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


def hybrid_eager_stable_ids(group_key: str = "stone") -> list[str]:
    if not POLICY.is_file():
        return []
    policy = load_json(POLICY)
    contract = (policy.get("publication_group_contract") or {}).get(group_key) or {}
    return list(
        ((contract.get("hybrid_boundary") or {}).get("eager_stable_ids")) or []
    )


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    group_winners = {
        "stone": (nested.get("group_winners") or {}).get("stone")
        or (nested.get("group_winners") or {}).get(STONE_GROUP)
    }
    card_winner = nested.get("card_aggregate_winner") or (
        (decision.get("production") or {}).get("winner")
    ) or decision.get("winner")
    blocked = (
        decision.get("status") == "SMELTER_STONE_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or card_winner in {None, "", "BLOCKED"}
        or group_winners["stone"] in {None, ""}
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_smelter_stone_recipe_load_benchmark as benchmark

            benchmark.validate_artifact(decision)
            recomputable = True
        except Exception:
            recomputable = False
            blocked = True
    winner = None if blocked else card_winner
    return {
        "blocked": blocked,
        "card_aggregate_winner": winner,
        "group_winners": {
            "stone": None if blocked else group_winners["stone"] or winner,
            STONE_GROUP: None if blocked else group_winners["stone"] or winner,
        },
        "recomputable": recomputable,
    }
