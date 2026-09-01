#!/usr/bin/env python3
"""Shared paths and frozen work-set helpers for T38 Roaster R0."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import gametest_receipt_roots
from tools import t35_common as t35
from tools import t36_common as t36

ROOT = t35.ROOT
TOOLS = t35.TOOLS
SOURCE_REVISION = t35.SOURCE_REVISION

HOST = "cruciblecraft:roaster"
SOURCE_MAP = "gt.recipe.roaster"
TARGET_MAP = "cruciblecraft:roaster"
OWNER = "portfolio:track_a/t38_roaster"
FAMILY_COUNT = 29
SOURCE_ROWS = 73

SOURCE = TOOLS / "t38_roaster_source.json"
RECEIPT = TOOLS / "t38_roaster_source_receipt.json"
REVIEW = TOOLS / "t38_roaster_source_review.json"
SCHEMA = TOOLS / "compact_recipe_family_source.schema.json"
BUILDER = TOOLS / "build_t38_roaster_source.py"
GENERATED_ROOT = (
    ROOT
    / "src"
    / "t38_recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "t38"
    / "roaster"
)
OPERAND_RUNTIME_MAP = TOOLS / "t38_operand_runtime_map.json"
PLAYER_PATH = TOOLS / "t38_player_path.json"
EQUIVALENCE = TOOLS / "t38_roaster_equivalence.json"
REQUIRED_FORMS = TOOLS / "t38_required_forms.json"
RECIPE_FAMILIES = t35.RECIPE_FAMILIES
T35_CENSUS = t35.CENSUS
CARD_TOPOLOGY = TOOLS / "t37_card_topology.json"
T37_CENSUS_DELTA = TOOLS / "t37_census_delta.json"
T37_READINESS = TOOLS / "t37_readiness.json"
POLICY = TOOLS / "t38_materialization_policy.json"
DECISION = TOOLS / "t38_materialization_decision.json"
MEASUREMENTS = TOOLS / "t38_materialization_measurements.json"
PUBLICATION_DELTA = TOOLS / "t38_publication_delta.json"
CENSUS_DELTA = TOOLS / "t38_census_delta.json"
T38_CARD_TOPOLOGY = TOOLS / "t38_card_topology.json"
READINESS = TOOLS / "t38_readiness.json"
LOAD_PROJECTION_INPUT = TOOLS / "t38_load_projection_input.json"
LOAD_PROJECTION = TOOLS / "t38_load_projection.json"
GAME_TEST_ROOT = ROOT / "src/main/java/com/masson/cruciblecraft/gametest"
GAME_TEST_JAVA = GAME_TEST_ROOT / "RoasterCompactGameTests.java"
GAME_TEST_RECEIPT = TOOLS / "t38_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "t38_gametest.log"
GAME_TEST_NAMESPACE = "cruciblecraft_wave_roaster_compact"
GAME_TEST_REQUIRED = 5
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=roaster/compact --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=roaster/compact"
GAME_TEST_PASS_MARKER = "All 5 required tests passed :)"
GAME_TEST_RECEIPT_SCHEMA = 3
GAME_TEST_METHOD_RE = re.compile(
    r"public static void (\w+)\s*\(\s*GameTestHelper"
)
# Gitignored run-dir logs. --write --from-log may read them; --check never does.
GAME_TEST_RUN_LOG_CANDIDATES = (
    ROOT / "run-t38-recipes" / "logs" / "latest.log",
    ROOT / "run-t38-recipes" / "logs" / "debug.log",
)
MATERIAL_REGISTRATION_GATE_JAVA = (
    ROOT / "src/main/java/com/masson/cruciblecraft/material/MaterialRegistrationGate.java"
)
MATERIAL_REGISTRATION_GATE_JSON = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
PLAYER_PATH_RECOVERY_ROOT = (
    ROOT
    / "src/main/resources/data/cruciblecraft/recipe/t38_player_path_recovery"
)
ORE_CHAIN_JSON = TOOLS / "gt6_ore_chain.json"
SOURCE_BACKED_ACQUISITION = TOOLS / "t38_source_backed_acquisition.json"
ALREADY_FACTUAL_OVERLAY_ORES = frozenset({"gold", "molybdenum"})
SUPPORT_GT_RECOVERY_COUNT = 5
SUPPORT_ORE_CHAIN_COUNT = 10
SUPPORT_CRAFTING_COUNT = 4
SUPPORT_AUTHORED_COUNT = (
    SUPPORT_GT_RECOVERY_COUNT + SUPPORT_ORE_CHAIN_COUNT + SUPPORT_CRAFTING_COUNT
)
SUPPORT_EAGER_COUNT = SUPPORT_GT_RECOVERY_COUNT + SUPPORT_ORE_CHAIN_COUNT
AUTHORED_DATAPACK_COUNT = FAMILY_COUNT + SUPPORT_AUTHORED_COUNT
T14_AUTHORED_CLOSING = 3616 + AUTHORED_DATAPACK_COUNT
T14_EAGER_OPENING = 16611
T14_EAGER_CLOSING = T14_EAGER_OPENING + SUPPORT_EAGER_COUNT
T14_LAZY_CLOSING = 2261 + SOURCE_ROWS
T37_REMAINING_ORDINARY_FAMILIES = 5668
EXPECTED_REMAINING_ORDINARY_FAMILIES = 5639
T35_FOUNDATION = {
    "t13_identities": 765,
    "exclusion_source_sites": 763,
    "exclusion_expanded_rows": 1701,
    "recipe_rows_accounted": 78682,
    "recipe_families": 5718,
}
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
OPENING_DISPOSITION = "planned"
OPENING_CLOSURE = "incomplete"
OPENING_FIDELITY = "source_backed"
OPENING_LOAD = "pending"
CLOSING_DISPOSITION = "implemented"
CLOSING_CLOSURE = "closed"
CLOSING_LOAD = "measured"
ROW_CLASSIFICATION = TOOLS / "t22_5_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "t21_template_denominator.json"
ROASTER_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.roaster.json"

# T38 begins from the T37 T14 closing measurement; T38's own load delta is
# deliberately outside this R0 source-freeze contract.
T14_OPENING_LOAD_SOURCE = "tools/t37_census_delta.json#t14_load.closing"


def relative(path: Path) -> str:
    try:
        return t35.relative(path)
    except ValueError:
        return path.as_posix()


def load_json(path: Path) -> Any:
    return t35.load_json(path)


def parse_write_check(description: str, argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    return args


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return t35.check_generated_document(path, document)


def t14_opening_load(census_delta: dict[str, Any] | None = None) -> dict[str, Any]:
    """Return the T37 closing measurement that opens the T38 load ledger."""
    document = census_delta if census_delta is not None else load_json(T37_CENSUS_DELTA)
    closing = ((document.get("t14_load") or {}).get("closing") or {})
    if not isinstance(closing, dict) or not closing:
        raise ValueError(f"missing {T14_OPENING_LOAD_SOURCE}")
    return dict(closing)


def load_t38_frozen_family_ids(
    topology: dict[str, Any] | None = None,
) -> list[str]:
    """Load only the issued T38 family ids from the T37 card topology."""
    topology = topology if topology is not None else load_json(CARD_TOPOLOGY)
    if topology.get("source_revision") != SOURCE_REVISION:
        raise ValueError("t37 card topology source_revision drifted")
    for row in topology.get("sequence") or []:
        if not isinstance(row, dict) or row.get("id") != "T38":
            continue
        if row.get("host") != HOST:
            raise ValueError(f"T38 topology host must be {HOST}")
        family_ids = [str(value) for value in (row.get("family_ids") or [])]
        if len(family_ids) != FAMILY_COUNT:
            raise ValueError(
                f"T38 topology family_ids must be {FAMILY_COUNT}, got {len(family_ids)}"
            )
        expected_numbers = [0, 1, *range(3, 30)]
        expected_keys = [f"{SOURCE_MAP}#{number:04d}" for number in expected_numbers]
        actual_keys = [family_id.rsplit("/", 1)[-1] for family_id in family_ids]
        if actual_keys != expected_keys:
            raise ValueError("T38 topology family_ids are not the frozen Roaster sequence")
        if any(key.endswith("#0002") for key in actual_keys):
            raise ValueError("T38 topology must not include gt.recipe.roaster#0002")
        if len(set(family_ids)) != FAMILY_COUNT:
            raise ValueError("T38 topology has duplicate family_ids")
        return family_ids
    raise ValueError("t37 card topology is missing T38")


def dump_present() -> bool:
    return ROASTER_DUMP.is_file()


def generated_family_files() -> list[Path]:
    if not GENERATED_ROOT.is_dir():
        return []
    return sorted(
        path
        for path in GENERATED_ROOT.glob("gt_recipe_roaster_*.json")
        if path.is_file()
    )


def _file_sha256(path: Path) -> str:
    return t35.sha256_file(path) if path.is_file() else None


def _tree_sha256(root: Path) -> str | None:
    if not root.is_dir():
        return None
    digest = hashlib.sha256()
    for path in sorted(child for child in root.rglob("*") if child.is_file()):
        relative = path.relative_to(root).as_posix()
        digest.update(relative.encode("utf-8"))
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
        "t38_generated_recipes": _tree_sha256(GENERATED_ROOT),
        "t38_player_path_recovery": _tree_sha256(PLAYER_PATH_RECOVERY_ROOT),
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


def support_recipe_ledger() -> dict[str, Any]:
    recovery_files = sorted(
        path for path in PLAYER_PATH_RECOVERY_ROOT.glob("*.json") if path.is_file()
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
                f"unexpected T38 recovery recipe type {recipe_type!r} in {relative_path}"
            )
    acquisition = load_json(SOURCE_BACKED_ACQUISITION)
    overlay_ores = {
        str(material)
        for material, forms in (acquisition.get("required_forms") or {}).items()
        if "ore" in (forms or [])
    }
    new_ores = sorted(overlay_ores - ALREADY_FACTUAL_OVERLAY_ORES)
    ore_chain = load_json(ORE_CHAIN_JSON)
    ore_chain_rows = [
        row
        for row in ore_chain.get("recipes") or []
        if row.get("family") == "crush_ore_block_to_crushed"
        and str(row.get("material") or "") in new_ores
    ]
    ore_chain_paths = sorted(
        str(row.get("path") or "")
        for row in ore_chain_rows
        if row.get("path")
    )
    return {
        "gt_recovery": gt_recovery,
        "ore_chain": ore_chain_paths,
        "crafting": crafting,
        "gt_recovery_count": len(gt_recovery),
        "ore_chain_count": len(ore_chain_paths),
        "crafting_count": len(crafting),
        "authored": len(gt_recovery) + len(ore_chain_paths) + len(crafting),
        "eager": len(gt_recovery) + len(ore_chain_paths),
        "new_ore_block_materials": new_ores,
    }


def player_gametest_source_present() -> bool:
    if not GAME_TEST_JAVA.is_file():
        return False
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    markers = ("cruciblecraft_wave_roaster_compact", "RoasterCompactGameTests", "-PwaveRecipes=roaster/compact")
    return any(marker in text for marker in markers)


def discovered_t38_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted({match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)})


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
    test_ids = discovered_t38_gametest_ids()
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
            "-PwaveRecipes=roaster/compact result bound to MaterialRegistrationGate, the T38 "
            "recipe resource trees, and the committed UTF-8 evidence log. "
            "The gitignored run-t38-recipes directory is not evidence. "
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
        lock_sha256=None,
        equivalence_path=EQUIVALENCE,
        log_fingerprint_value=document["log_fingerprint"],
    )


def gametest_receipt_errors(document: dict[str, Any] | None = None) -> list[str]:
    if document is None and not GAME_TEST_RECEIPT.is_file():
        return ["missing GameTest receipt: tools/t38_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_t38_gametest_ids()
    java_hash = t35.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("T38 GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("T38 GameTest receipt command is missing -PwaveRecipes=roaster/compact")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("T38 GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("T38 GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append("T38 GameTest receipt passed count is not 5")
    if receipt.get("failed") != 0:
        errors.append("T38 GameTest receipt records failures")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("T38 GameTest receipt pass marker drifted")
    if list(receipt.get("test_ids") or []) != test_ids:
        errors.append("T38 GameTest receipt test_ids do not match Java @GameTest methods")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append("T38 GameTest Java does not declare 5 @GameTest methods")
    if receipt.get("java_sha256") != java_hash:
        errors.append("T38 GameTest receipt java_sha256 does not match current source")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("T38 GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("T38 GameTest receipt schema_version must be 3")
    if not player_gametest_source_present():
        errors.append("T38 GameTest Java source markers are missing")
    bound = receipt.get("bound_artifacts") or {}
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "T38", bound, bound_gametest_artifacts()
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "T38 GameTest receipt log_path must be the committed evidence file "
            "tools/t38_gametest.log"
        )
    log_path = GAME_TEST_EVIDENCE_LOG
    if not log_path.is_file():
        errors.append(
            "T38 GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(read_gametest_log(log_path))
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "T38 GameTest receipt log_fingerprint does not match the committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("T38 committed GameTest evidence log is not PASS")
    return errors


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "T38",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def production_strategy(decision: dict[str, Any] | None = None) -> dict[str, Any]:
    decision = decision if decision is not None else (
        load_json(DECISION) if DECISION.is_file() else {}
    )
    nested = decision.get("decision") or {}
    winner = nested.get("production_winner")
    blocked = (
        decision.get("status") == "T38_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or winner in {None, ""}
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_t38_recipe_load_benchmark as benchmark
            benchmark.validate_artifact(decision)
            recomputable = True
        except (ValueError, KeyError, OSError, json.JSONDecodeError):
            blocked = True
            winner = None
    return {
        "blocked": blocked,
        "winner": None if blocked else str(winner),
        "status": nested.get("status") or decision.get("status"),
        "recomputable": recomputable,
    }


def partition_for_winner(winner: str | None) -> dict[str, int | None]:
    table = {
        "immediate": {
            "eager_publication_rows": 73,
            "lazy_logical_rows": 0,
            "lazy_cache_ceiling_rows": 0,
        },
        "on_demand": {
            "eager_publication_rows": 0,
            "lazy_logical_rows": 73,
            "lazy_cache_ceiling_rows": 16,
        },
        "hybrid": {
            "eager_publication_rows": 38,
            "lazy_logical_rows": 35,
            "lazy_cache_ceiling_rows": 16,
        },
    }
    if winner not in table:
        return {
            "eager_publication_rows": None,
            "lazy_logical_rows": None,
            "lazy_cache_ceiling_rows": None,
        }
    return table[winner]


# Keep the T36 import intentional: both stages must stay on T35's immutable
# source revision, but T38 does not require any historical host status to be live.
assert t36.SOURCE_REVISION == SOURCE_REVISION
