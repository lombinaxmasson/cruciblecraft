#!/usr/bin/env python3
"""Shared paths and catalog/production helpers for electrolyzer/compact Electrolyzer."""
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
from tools import centrifuge_common as centrifuge

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

HOST = "cruciblecraft:electrolyzer"
SOURCE_MAP = "gt.recipe.electrolyzer"
TARGET_MAP = "cruciblecraft:electrolyzer"
OWNER = "portfolio:track_a/electrolyzer_compact"
LEGACY_VANILLA_ITEMS: dict[tuple[str, int], str] = dict(centrifuge.LEGACY_VANILLA_ITEMS)
PLAYER_PATH_SUPPORT_FORMS: dict[str, list[str]] = {}

CATALOG_FAMILY_COUNT = 61
CATALOG_RELATION_COUNT = 151
CATALOG_SINGLETON_FAMILIES = 34
CATALOG_SINGLETON_RELATIONS = 34
CATALOG_MULTI_FAMILIES = 25
CATALOG_MULTI_RELATIONS = 82
CATALOG_COMBINATORIAL_FAMILIES = 2
CATALOG_COMBINATORIAL_RELATIONS = 35
EXPECTED_SELECTION_SHA256 = (
    "6bd160457fe8472997fb0781b465b4264c4a5b74b838d11fcc0e06e340033a5c"
)
EXPECTED_DISTRIBUTION = {
    1: 34,
    2: 7,
    3: 12,
    4: 2,
    5: 1,
    6: 2,
    7: 1,
    15: 1,
    20: 1,
}
CENTRIFUGE_COMPACT_REMAINING_ORDINARY_FAMILIES = 5610
EXPECTED_REMAINING_ORDINARY_FAMILIES = 5597
PUBLICATION_GROUP_COUNT = 3
SINGLETON_GROUP = "cruciblecraft:electrolyzer/singleton"
MULTI_GROUP = "cruciblecraft:electrolyzer/multi"
COMBINATORIAL_GROUP = "cruciblecraft:electrolyzer/combinatorial"
COMBINATORIAL_TEMPLATE_KEYS = (
    "gt.recipe.electrolyzer#0000",
    "gt.recipe.electrolyzer#0001",
)
FIXTURE_ONLY_LOSSY_ITEM_ALIASES = dict(centrifuge.FIXTURE_ONLY_LOSSY_ITEM_ALIASES)
LOSSY_ITEM_ALIASES = FIXTURE_ONLY_LOSSY_ITEM_ALIASES

CANDIDATE_SELECTION = TOOLS / "electrolyzer_candidate_selection.json"
PRODUCTION_SELECTION = CANDIDATE_SELECTION
PRODUCTION_LOCK = TOOLS / "electrolyzer_production_lock.json"
PRODUCTION_LOCK_SCHEMA = TOOLS / "electrolyzer_production_lock.schema.json"
OPERAND_DISPOSITION = TOOLS / "electrolyzer_operand_disposition.json"
WORK_SET = TOOLS / "electrolyzer_work_set.json"
SOURCE = TOOLS / "electrolyzer_source.json"
RECEIPT = TOOLS / "electrolyzer_source_receipt.json"
REVIEW = TOOLS / "electrolyzer_source_review.json"
SOURCE_PACK = TOOLS / "electrolyzer_source_pack_manifest.json"
RUNTIME_DEPENDENCY_MANIFEST = TOOLS / "electrolyzer_runtime_dependency_manifest.json"
PUBLICATION_GROUP_MANIFEST = TOOLS / "electrolyzer_publication_group_manifest.json"
SHARD_MANIFEST = TOOLS / "electrolyzer_shard_manifest.json"
SCHEMA = TOOLS / "compact_recipe_family_source.schema.json"
BUILDER = TOOLS / "build_electrolyzer_source.py"
GENERATED_ROOT = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "electrolyzer"
    / "compact"
)
CATALOG_FIXTURE_ROOT = (
    ROOT
    / "src"
    / "test"
    / "resources"
    / "electrolyzer_catalog_fixture"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "electrolyzer_catalog"
    / "electrolyzer"
)
OPERAND_RUNTIME_MAP = TOOLS / "electrolyzer_operand_runtime_map.json"
PLAYER_PATH = TOOLS / "electrolyzer_player_path.json"
LAYERED_PLAYER_PATH = TOOLS / "electrolyzer_layered_player_path.json"
EQUIVALENCE = TOOLS / "electrolyzer_equivalence.json"
REQUIRED_FORMS = TOOLS / "electrolyzer_required_forms.json"
PLAYER_PATH_SUPPORT = TOOLS / "electrolyzer_player_path_support.json"
RECIPE_FAMILIES = census.RECIPE_FAMILIES
T35_CENSUS = census.CENSUS
CENTRIFUGE_COMPACT_CENSUS_DELTA = TOOLS / "centrifuge_census_delta.json"
CENTRIFUGE_COMPACT_READINESS = TOOLS / "centrifuge_readiness.json"
CENTRIFUGE_COMPACT_CARD_TOPOLOGY = TOOLS / "centrifuge_card_topology.json"
POLICY = TOOLS / "electrolyzer_materialization_policy.json"
DECISION = TOOLS / "electrolyzer_materialization_decision.json"
MEASUREMENTS = TOOLS / "electrolyzer_materialization_measurements.json"
PUBLICATION_DELTA = TOOLS / "electrolyzer_publication_delta.json"
CENSUS_DELTA = TOOLS / "electrolyzer_census_delta.json"
ELECTROLYZER_COMPACT_CARD_TOPOLOGY = TOOLS / "electrolyzer_card_topology.json"
READINESS = TOOLS / "electrolyzer_readiness.json"
LOAD_PROJECTION_INPUT = TOOLS / "electrolyzer_load_projection_input.json"
LOAD_PROJECTION = TOOLS / "electrolyzer_load_projection.json"
GAME_TEST_ROOT = ROOT / "src/test/java/com/masson/cruciblecraft/gametest"
GAME_TEST_JAVA = GAME_TEST_ROOT / "ElectrolyzerCompactGameTests.java"
GAME_TEST_RECEIPT = TOOLS / "electrolyzer_gametest_receipt.json"
GAME_TEST_EVIDENCE_LOG = TOOLS / "electrolyzer_gametest.log"
GAME_TEST_NAMESPACE = "cruciblecraft_wave_electrolyzer_compact"
GAME_TEST_COMMAND = ".\\gradlew.bat runGameTestServer -PwaveRecipes=electrolyzer/compact --no-daemon"
GAME_TEST_COMMAND_MARKER = "-PwaveRecipes=electrolyzer/compact"
GAME_TEST_RECEIPT_SCHEMA = 3
GAME_TEST_METHOD_RE = re.compile(
    r"public static void (\w+)\s*\(\s*GameTestHelper"
)
GAME_TEST_RUN_LOG_CANDIDATES = (
    ROOT / "run-electrolyzer/compact-recipes" / "logs" / "latest.log",
    ROOT / "run-electrolyzer/compact-recipes" / "logs" / "debug.log",
)
MATERIAL_REGISTRATION_GATE_JAVA = centrifuge.MATERIAL_REGISTRATION_GATE_JAVA
MATERIAL_REGISTRATION_GATE_JSON = centrifuge.MATERIAL_REGISTRATION_GATE_JSON
LOCKED_SUPPORT_ROOT = (
    ROOT
    / "src"
    / "electrolyzer_support_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "electrolyzer_player_path_support"
)
ELECTROLYZER_DUMP = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.electrolyzer.json"
)
ROW_CLASSIFICATION = TOOLS / "machine_row_classification.json"
TEMPLATE_DENOMINATOR = TOOLS / "chemical_axis_template_denominator.json"
T14_OPENING_LOAD_SOURCE = "tools/centrifuge_readiness.json#electrolyzer_opening.recipe_load_closing"
T35_FOUNDATION = dict(centrifuge.T35_FOUNDATION)
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
PUBLICATION_GROUP_KEYS = {
    SINGLETON_GROUP: "singleton",
    MULTI_GROUP: "multi",
    COMBINATORIAL_GROUP: "combinatorial",
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
        raise ValueError("electrolyzer/compact production lock schema_version must be 1")
    if document.get("status") != "ELECTROLYZER_COMPACT_PRODUCTION_LOCKED":
        raise ValueError("electrolyzer/compact production lock is not locked")
    if document.get("source_revision") != SOURCE_REVISION:
        raise ValueError("electrolyzer/compact production lock source_revision drifted")
    catalog = document.get("catalog_fixture") or {}
    if (
        int(catalog.get("families") or 0) != CATALOG_FAMILY_COUNT
        or int(catalog.get("relations") or 0) != CATALOG_RELATION_COUNT
        or catalog.get("selection_sha256") != EXPECTED_SELECTION_SHA256
    ):
        raise ValueError("electrolyzer/compact production lock catalog fixture contract drifted")
    production = document.get("production") or {}
    families = production.get("families") or []
    if not isinstance(families, list) or not families:
        raise ValueError("electrolyzer/compact production lock has no production families")
    family_ids = [str(row.get("family_id") or "") for row in families]
    template_keys = [str(row.get("template_key") or "") for row in families]
    if any(not value for value in family_ids + template_keys):
        raise ValueError("electrolyzer/compact production lock has an empty family identity")
    if len(set(family_ids)) != len(family_ids):
        raise ValueError("electrolyzer/compact production lock has duplicate family ids")
    if len(set(template_keys)) != len(template_keys):
        raise ValueError("electrolyzer/compact production lock has duplicate template keys")
    if family_ids != list(production.get("family_ids") or []):
        raise ValueError("electrolyzer/compact production lock family_ids drifted from families")
    if template_keys != list(production.get("template_keys") or []):
        raise ValueError("electrolyzer/compact production lock template_keys drifted from families")
    if int(production.get("family_count") or 0) != len(families):
        raise ValueError("electrolyzer/compact production lock family_count drifted")
    relations = sum(int(row.get("expanded_count") or 0) for row in families)
    if int(production.get("relation_count") or 0) != relations:
        raise ValueError("electrolyzer/compact production lock relation_count drifted")
    digest = selection_sha256(family_ids)
    if production.get("selection_sha256") != digest:
        raise ValueError("electrolyzer/compact production lock selection_sha256 drifted")
    support = document.get("support") or {}
    route_keys = [str(value) for value in support.get("route_keys") or []]
    if route_keys != sorted(set(route_keys)):
        raise ValueError("electrolyzer/compact production lock support route_keys are not unique/sorted")
    if int(support.get("route_count") or 0) != len(route_keys):
        raise ValueError("electrolyzer/compact production lock support route_count drifted")
    deferred = document.get("phase_deferred") or []
    deferred_ids = [str(row.get("family_id") or "") for row in deferred]
    if len(deferred_ids) != len(set(deferred_ids)):
        raise ValueError("electrolyzer/compact production lock phase_deferred has duplicates")
    if set(deferred_ids) & set(family_ids):
        raise ValueError("electrolyzer/compact production and phase-deferred families overlap")
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
        COMBINATORIAL_GROUP: {"families": 0, "relations": 0},
    }
    for row in production_families():
        group = str(row["publication_group"])
        if group not in counts:
            raise ValueError(f"unexpected electrolyzer/compact production group {group}")
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


def dump_present() -> bool:
    return ELECTROLYZER_DUMP.is_file()


def is_lossy_item_alias(item_id: str | None, meta: Any) -> bool:
    return centrifuge.is_lossy_item_alias(item_id, meta)


def canonical_family_id(template_key: str) -> str:
    return f"portfolio:track_a/cruciblecraft:electrolyzer/{template_key}"


def family_fidelity_blockers(relations: list[dict[str, Any]]) -> list[str]:
    return centrifuge.family_fidelity_blockers(relations)


def source_operand_is_unproven_lossy_alias(operand: dict[str, Any]) -> bool:
    return centrifuge.source_operand_is_unproven_lossy_alias(operand)


def t14_opening_load(readiness: dict[str, Any] | None = None) -> dict[str, Any]:
    document = readiness if readiness is not None else load_json(CENTRIFUGE_COMPACT_READINESS)
    closing = ((document.get("electrolyzer_opening") or {}).get("recipe_load_closing") or {})
    if not isinstance(closing, dict) or not closing:
        raise ValueError(f"missing {T14_OPENING_LOAD_SOURCE}")
    return dict(closing)


def publication_group_for_expanded_count(expanded_count: int) -> str:
    if expanded_count == 1:
        return SINGLETON_GROUP
    if 2 <= expanded_count <= 7:
        return MULTI_GROUP
    if expanded_count >= 8:
        return COMBINATORIAL_GROUP
    raise ValueError(f"expanded_count must be positive, got {expanded_count}")


def publication_group_for_template(
    template_key: str,
    expanded_count: int,
) -> str:
    if template_key in COMBINATORIAL_TEMPLATE_KEYS:
        return COMBINATORIAL_GROUP
    return publication_group_for_expanded_count(expanded_count)


def selection_payload(family_ids: list[str]) -> bytes:
    return ("".join(f"{family_id}\n" for family_id in family_ids)).encode("utf-8")


def selection_sha256(family_ids: list[str]) -> str:
    return hashlib.sha256(selection_payload(family_ids)).hexdigest()


def expanded_count_distribution(expanded_counts: list[int]) -> dict[str, int]:
    counts = Counter(expanded_counts)
    return {str(size): counts[size] for size in sorted(counts)}


def select_electrolyzer_families(
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
        raise ValueError(f"unknown electrolyzer/compact generated scope: {scope}")
    if not root.is_dir():
        return []
    return sorted(
        path
        for path in root.glob("gt_recipe_electrolyzer_*.json")
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
        "electrolyzer_generated_recipes": _tree_sha256(GENERATED_ROOT),
        "electrolyzer_locked_support": _tree_sha256(LOCKED_SUPPORT_ROOT),
        "runtime_dependency_manifest": _file_sha256(RUNTIME_DEPENDENCY_MANIFEST),
        "publication_group_manifest": _file_sha256(PUBLICATION_GROUP_MANIFEST),
        "shard_manifest": _file_sha256(SHARD_MANIFEST),
    }


def read_gametest_log(path: Path) -> str:
    return centrifuge.read_gametest_log(path)


def log_fingerprint(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def normalize_gametest_log_text(text: str) -> str:
    return centrifuge.normalize_gametest_log_text(text)


def commit_gametest_log(source: Path) -> str:
    text = normalize_gametest_log_text(read_gametest_log(source))
    GAME_TEST_EVIDENCE_LOG.parent.mkdir(parents=True, exist_ok=True)
    GAME_TEST_EVIDENCE_LOG.write_text(text, encoding="utf-8", newline="\n")
    return text


def discovered_electrolyzer_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    return sorted(
        {match.group(1).casefold() for match in GAME_TEST_METHOD_RE.finditer(text)}
    )


GAME_TEST_REQUIRED = len(discovered_electrolyzer_gametest_ids())
GAME_TEST_PASS_MARKER = f"All {GAME_TEST_REQUIRED} required tests passed :)"


def player_gametest_source_present() -> bool:
    if not GAME_TEST_JAVA.is_file():
        return False
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    markers = ("cruciblecraft_wave_electrolyzer_compact", "ElectrolyzerCompactGameTests", "-PwaveRecipes=electrolyzer/compact")
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
    test_ids = discovered_electrolyzer_gametest_ids()
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
            "-PwaveRecipes=electrolyzer/compact result bound to MaterialRegistrationGate, the electrolyzer/compact "
            "recipe resource trees, and the committed UTF-8 evidence log. "
            "The gitignored run-electrolyzer/compact-recipes directory is not evidence. "
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
        return ["missing GameTest receipt: tools/electrolyzer_gametest_receipt.json"]
    if not GAME_TEST_JAVA.is_file():
        return [f"missing GameTest source: {relative(GAME_TEST_JAVA)}"]
    receipt = document if document is not None else load_json(GAME_TEST_RECEIPT)
    errors: list[str] = []
    test_ids = discovered_electrolyzer_gametest_ids()
    java_hash = census.sha256_file(GAME_TEST_JAVA)
    if receipt.get("status") != "PASS":
        errors.append("electrolyzer/compact GameTest receipt is not PASS")
    if GAME_TEST_COMMAND_MARKER not in str(receipt.get("command") or ""):
        errors.append("electrolyzer/compact GameTest receipt command is missing -PwaveRecipes=electrolyzer/compact")
    if receipt.get("namespace") != GAME_TEST_NAMESPACE:
        errors.append("electrolyzer/compact GameTest receipt namespace drifted")
    if receipt.get("required_tests") != GAME_TEST_REQUIRED:
        errors.append("electrolyzer/compact GameTest receipt required_tests drifted")
    if receipt.get("passed") != GAME_TEST_REQUIRED:
        errors.append(
            f"electrolyzer/compact GameTest receipt passed count is not {GAME_TEST_REQUIRED}"
        )
    if receipt.get("failed") != 0:
        errors.append("electrolyzer/compact GameTest receipt records failures")
    if receipt.get("pass_marker") != GAME_TEST_PASS_MARKER:
        errors.append("electrolyzer/compact GameTest receipt pass marker drifted")
    if list(receipt.get("test_ids") or []) != test_ids:
        errors.append("electrolyzer/compact GameTest receipt test_ids do not match Java @GameTest methods")
    if len(test_ids) != GAME_TEST_REQUIRED:
        errors.append(
            f"electrolyzer/compact GameTest Java does not declare {GAME_TEST_REQUIRED} @GameTest methods"
        )
    if receipt.get("java_sha256") != java_hash:
        errors.append("electrolyzer/compact GameTest receipt java_sha256 does not match current source")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("electrolyzer/compact GameTest receipt must record skip_is_not_pass")
    if receipt.get("schema_version") != GAME_TEST_RECEIPT_SCHEMA:
        errors.append("electrolyzer/compact GameTest receipt schema_version must be 3")
    if not player_gametest_source_present():
        errors.append("electrolyzer/compact GameTest Java source markers are missing")
    bound = receipt.get("bound_artifacts") or {}
    errors.extend(
        gametest_receipt_roots.bound_drift_errors(
            "electrolyzer/compact", bound, bound_gametest_artifacts()
        )
    )
    stored_log = str(receipt.get("log_path") or "")
    if stored_log != relative(GAME_TEST_EVIDENCE_LOG):
        errors.append(
            "electrolyzer/compact GameTest receipt log_path must be the committed evidence file "
            "tools/electrolyzer_gametest.log"
        )
    log_path = GAME_TEST_EVIDENCE_LOG
    if not log_path.is_file():
        errors.append(
            "electrolyzer/compact GameTest committed evidence log is missing; receipt cannot stay PASS"
        )
    else:
        evidence_text = normalize_gametest_log_text(read_gametest_log(log_path))
        if receipt.get("log_fingerprint") != log_fingerprint(evidence_text):
            errors.append(
                "electrolyzer/compact GameTest receipt log_fingerprint does not match the committed evidence log"
            )
        parsed = parse_gametest_log(evidence_text)
        if parsed.get("status") != "PASS":
            errors.append("electrolyzer/compact committed GameTest evidence log is not PASS")
    return errors


def player_gametest_present() -> bool:
    from tools import closeout_seal

    return closeout_seal.sealed_or_live_present(
        "electrolyzer/compact",
        lambda: player_gametest_source_present() and not gametest_receipt_errors(),
    )


def load_electrolyzer_catalog_family_ids(
    work_set: dict[str, Any] | None = None,
) -> list[str]:
    work_set = work_set if work_set is not None else load_json(WORK_SET)
    if work_set.get("source_revision") != SOURCE_REVISION:
        raise ValueError("electrolyzer/compact work set source_revision drifted")
    if work_set.get("host") != HOST:
        raise ValueError(f"electrolyzer/compact work set host must be {HOST}")
    family_ids = [str(row["family_id"]) for row in work_set.get("families") or []]
    if len(family_ids) != CATALOG_FAMILY_COUNT:
        raise ValueError(
            f"electrolyzer/compact catalog family_ids must be {CATALOG_FAMILY_COUNT}, "
            f"got {len(family_ids)}"
        )
    if len(set(family_ids)) != CATALOG_FAMILY_COUNT:
        raise ValueError("electrolyzer/compact work set has duplicate family_ids")
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
                f"unexpected electrolyzer/compact support recipe type {recipe_type!r} in {relative_path}"
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
    owned_groups = tuple(production_group_counts())
    group_winners = {
        PUBLICATION_GROUP_KEYS[group]: nested.get("group_winners", {}).get(
            PUBLICATION_GROUP_KEYS[group]
        )
        for group in owned_groups
    }
    card_winner = nested.get("card_aggregate_winner")
    blocked = (
        decision.get("status") == "ELECTROLYZER_COMPACT_MATERIALIZATION_DECISION_BLOCKED"
        or nested.get("status") == "PRODUCTION_WINNER_BLOCKED"
        or card_winner in {None, ""}
        or any(winner in {None, ""} for winner in group_winners.values())
        or not group_winners
    )
    recomputable = False
    if not blocked:
        try:
            from tools import build_electrolyzer_recipe_load_benchmark as benchmark

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
        "singleton": "singleton",
        "multi": "multi",
        "combinatorial": "combinatorial",
        "card": "card_aggregate",
        SINGLETON_GROUP: "singleton",
        MULTI_GROUP: "multi",
        COMBINATORIAL_GROUP: "combinatorial",
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
