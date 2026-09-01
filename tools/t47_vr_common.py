#!/usr/bin/env python3
"""Shared T47-VR repair constants, freeze facts, and later-R probes."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import t35_common as t35
from tools import t46_common as t46
from tools import t47_common as t47

ROOT = t35.ROOT
TOOLS = t35.TOOLS
SOURCE_REVISION = t35.SOURCE_REVISION

FREEZE = TOOLS / "t47_vr_pre_repair_freeze.json"
REPAIR_READINESS = TOOLS / "t47_vr_repair_readiness.json"
CLOSEOUT_SEAL = TOOLS / "closeout_seal.py"
CLOSEOUT_SEAL_SCHEMA = TOOLS / "closeout_seal.schema.json"
PROFILES = TOOLS / "verification_profiles.json"
VERIFY = TOOLS / "verify.py"
DEBT = TOOLS / "known_issues" / "verification-debt.json"
T48_PLAN = ROOT / "docs" / "history" / "card-plans" / "active" / "T48详细计划.md"
WAVE_SLICE_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CompactRecipeRuntimeWaveSlice.java"
)
T47_HARNESS_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CompactRecipeFamilyT47IntegratedMeasurementHarness.java"
)

FROZEN_COUNTS = {
    "t47_production_families": 395,
    "t47_production_relations": 13708,
    "t47_identities": 283,
    "t47_remaining_ordinary_gap": 1499,
    "t47_reclassification_r": 0,
    "t46_complete_families": 803,
    "t46_remaining_gap_opening": 1894,
    "composed_runtime_groups": 15,
    "t47_gametest_passed": 10,
    "t47_gametest_required": 10,
}

EXPECTED_T47_LOCK_SHA256 = (
    "884643b2ba569e4eb96c9fcfe63caeebc61be6bb2df3334f85d3cf13490614db"
)
EXPECTED_T46_LOCK_SHA256 = (
    "454a49b25ff329dbb053b9195841fe9bb51dcbc311fd293f990b354218a762f4"
)
OPEN_DEBT_IDS = ("T32-VD-002", "T32-VD-004")

IMMUTABLE_HASH_PATHS = (
    "tools/t46_production_lock.json",
    "tools/t47_production_lock.json",
)

SNAPSHOT_HASH_PATHS = (
    "tools/t46_production_lock.json",
    "tools/t46_census_delta.json",
    "tools/t46_card_topology.json",
    "tools/t46_readiness.json",
    "tools/t46_gametest_receipt.json",
    "tools/t46_runtime_dependency_manifest.json",
    "tools/t47_production_lock.json",
    "tools/t47_census_delta.json",
    "tools/t47_card_topology.json",
    "tools/t47_readiness.json",
    "tools/t47_gametest_receipt.json",
    "tools/t47_runtime_dependency_manifest.json",
    "tools/global_build_identity_ledger.v2.json",
    "tools/compact_recipe_runtime_manifest.v2.json",
    "tools/verification_profiles.json",
    "docs/current/recipe-wave-workflow.md",
)

SEALED_CARD_IDS = (
    "T38",
    "T39",
    "T40",
    "T41",
    "T43",
    "T44",
    "T45",
    "T46",
    "T47",
)

LIVE_PIN_KEYS = ("identity_ledger_v2", "runtime_manifest_v2")


def relative(path: Path) -> str:
    return t35.relative(path)


def load_json(path: Path) -> Any:
    return t35.load_json(path)


def sha256_file(path: Path) -> str:
    return t35.sha256_file(path)


def parse_write_check(description: str, argv: list[str] | None):
    return t46.parse_write_check(description, argv)


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return t46.check_document(path, document)


def live_counts() -> dict[str, int]:
    t47_lock = t47.load_production_lock()
    production = t47_lock.get("production") or {}
    catalog = load_json(t47.IDENTITY_CATALOG) if t47.IDENTITY_CATALOG.is_file() else {}
    t47_census = load_json(t47.CENSUS_DELTA) if t47.CENSUS_DELTA.is_file() else {}
    t46_census = load_json(t46.CENSUS_DELTA) if t46.CENSUS_DELTA.is_file() else {}
    runtime = (
        load_json(t47.RUNTIME_MANIFEST_V2) if t47.RUNTIME_MANIFEST_V2.is_file() else {}
    )
    receipt = load_json(t47.GAME_TEST_RECEIPT) if t47.GAME_TEST_RECEIPT.is_file() else {}
    remaining = t47_census.get("remaining_ordinary") or {}
    return {
        "t47_production_families": int(production.get("family_count") or 0),
        "t47_production_relations": int(production.get("relation_count") or 0),
        "t47_identities": int(catalog.get("identity_count") or 0),
        "t47_remaining_ordinary_gap": int(
            t47_census.get("remaining_recipe_gap")
            or remaining.get("remaining_ordinary_families")
            or 0
        ),
        "t47_reclassification_r": int(t47_census.get("reclassification_delta") or 0),
        "t46_complete_families": int(t46_census.get("complete_family_count") or 0),
        "t46_remaining_gap_opening": int(t46_census.get("remaining_recipe_gap") or 0),
        "composed_runtime_groups": int(
            runtime.get("group_count")
            or (runtime.get("composition") or {}).get("composed_group_count")
            or 0
        ),
        "t47_gametest_passed": int(receipt.get("passed") or 0),
        "t47_gametest_required": int(receipt.get("required_tests") or 0),
    }


def live_lock_hashes() -> dict[str, str]:
    return {
        "tools/t46_production_lock.json": t46.production_lock_sha256(),
        "tools/t47_production_lock.json": t47.production_lock_sha256(),
    }


def snapshot_hashes() -> dict[str, str | None]:
    hashes: dict[str, str | None] = {}
    for relative_path in SNAPSHOT_HASH_PATHS:
        path = ROOT / relative_path
        hashes[relative_path] = sha256_file(path) if path.is_file() else None
    return hashes


def open_debt_ids() -> list[str]:
    if not DEBT.is_file():
        return []
    document = load_json(DEBT)
    return [
        str(row.get("id") or "")
        for row in document.get("issues") or []
        if row.get("status") == "open"
    ]


def t48_not_issued() -> bool:
    if T48_PLAN.is_file():
        return False
    if any(TOOLS.glob("t48_production_lock.json")):
        return False
    if not t47.READINESS.is_file() or not t47.CARD_TOPOLOGY.is_file():
        return False
    readiness = load_json(t47.READINESS)
    topology = load_json(t47.CARD_TOPOLOGY)
    opening = readiness.get("t48_opening") or {}
    return (
        opening.get("preassigned_host") is False
        and opening.get("preassigned_family_ids") is False
        and opening.get("next_issue_id") == "T48"
        and topology.get("next_issue_id") == "T48"
        and topology.get("unique_active_card") is None
        and topology.get("unique_active_card") != "T47-VR"
    )


def open_debt_not_disguised() -> bool:
    if not DEBT.is_file():
        return False
    document = load_json(DEBT)
    by_id = {
        str(row.get("id") or ""): row
        for row in document.get("issues") or []
    }
    for debt_id in OPEN_DEBT_IDS:
        row = by_id.get(debt_id) or {}
        if row.get("status") != "open":
            return False
        if str(row.get("status") or "").lower() in {"pass", "skip", "ready"}:
            return False
    return True


def file_present(*paths: Path) -> bool:
    return all(path.is_file() for path in paths)


def r0_freeze_current() -> bool:
    if not FREEZE.is_file():
        return False
    freeze = load_json(FREEZE)
    if freeze.get("frozen_counts") != FROZEN_COUNTS:
        return False
    if live_counts() != FROZEN_COUNTS:
        return False
    immutable = freeze.get("immutable_lock_hashes") or {}
    live = live_lock_hashes()
    return (
        immutable.get("tools/t46_production_lock.json")
        == live["tools/t46_production_lock.json"]
        == EXPECTED_T46_LOCK_SHA256
        and immutable.get("tools/t47_production_lock.json")
        == live["tools/t47_production_lock.json"]
        == EXPECTED_T47_LOCK_SHA256
    )


def r1_closeout_seal() -> bool:
    if not file_present(CLOSEOUT_SEAL, CLOSEOUT_SEAL_SCHEMA):
        return False
    from tools import closeout_seal

    if not all(closeout_seal.seal_path(card_id).is_file() for card_id in ("T46", "T47")):
        return False
    if not all(closeout_seal.seal_path(card_id).is_file() for card_id in SEALED_CARD_IDS):
        return False
    t46_seal = closeout_seal.load_seal("T46")
    t47_seal = closeout_seal.load_seal("T47")
    return (
        int(t46_seal.get("complete_family_count") or 0) == 803
        and int(t47_seal.get("complete_family_count") or 0) == 395
        and t46_seal.get("gametest_status") == "PASS"
        and t47_seal.get("gametest_status") == "PASS"
        and not closeout_seal.check_all()
    )


def r2_receipt_binding() -> bool:
    t46_bound = t46.bound_gametest_artifacts()
    t47_bound = t47.bound_gametest_artifacts()
    if any(key in t46_bound for key in LIVE_PIN_KEYS):
        return False
    if any(key in t47_bound for key in LIVE_PIN_KEYS):
        return False
    if not t46.GAME_TEST_RECEIPT.is_file() or not t47.GAME_TEST_RECEIPT.is_file():
        return False
    t46_receipt = load_json(t46.GAME_TEST_RECEIPT)
    t47_receipt = load_json(t47.GAME_TEST_RECEIPT)
    t46_keys = t46_receipt.get("bound_artifacts") or {}
    t47_keys = t47_receipt.get("bound_artifacts") or {}
    if any(key in t46_keys for key in LIVE_PIN_KEYS):
        return False
    if any(key in t47_keys for key in LIVE_PIN_KEYS):
        return False
    required = (
        "gametest_java",
        "production_lock",
        "runtime_dependency_manifest",
        "publication_group_manifest",
        "shard_manifest",
    )
    return (
        all(t46_keys.get(key) for key in required)
        and all(t47_keys.get(key) for key in required)
        and t46_keys.get("t46_generated_recipes")
        and t47_keys.get("t47_generated_recipes")
        and not t46.gametest_receipt_errors()
        and not t47.gametest_receipt_errors()
    )


def r3_census_topology() -> bool:
    census_text = (TOOLS / "build_t46_census_delta.py").read_text(encoding="utf-8")
    topology_text = (TOOLS / "build_t47_card_topology.py").read_text(encoding="utf-8")
    if "closeout_seal" not in census_text:
        return False
    if "closeout_seal" not in topology_text:
        return False
    if not t46.CENSUS_DELTA.is_file() or not t47.CARD_TOPOLOGY.is_file():
        return False
    t46_census = load_json(t46.CENSUS_DELTA)
    topology = load_json(t47.CARD_TOPOLOGY)
    return (
        int(t46_census.get("complete_family_count") or 0) == 803
        and topology.get("unique_active_card") is None
        and topology.get("unique_active_card") != "T47-VR"
        and topology.get("next_issue_id") == "T48"
        and bool(topology.get("t47_complete"))
    )


def r4_profiles() -> bool:
    if not PROFILES.is_file():
        return False
    profiles = load_json(PROFILES)
    encoded = json.dumps(profiles)
    census = (profiles.get("profiles") or {}).get("census") or {}
    recipes = (profiles.get("profiles") or {}).get("recipes") or {}
    closeout = (profiles.get("profiles") or {}).get("closeout-seals") or {}
    verify_text = VERIFY.read_text(encoding="utf-8")
    return (
        census.get("gradle_tasks") == []
        and "test" in (recipes.get("gradle_tasks") or [])
        and closeout.get("gradle_tasks") == []
        and "closeout-seals" in encoded
        and 'CLOSEOUT_PROFILES = ("recipes", "census", "closeout-seals")' in verify_text
    )


def r5_harness() -> bool:
    if not WAVE_SLICE_JAVA.is_file() or not T47_HARNESS_JAVA.is_file():
        return False
    harness = T47_HARNESS_JAVA.read_text(encoding="utf-8")
    return (
        "t47_bath_exact" in harness
        and "t47_bath_exact_multi" in harness
        and ">= 13" in harness
    )


def r6_cutover() -> bool:
    archive = (
        ROOT
        / "docs"
        / "history"
        / "stage-archives"
        / "CrucibleCraft-阶段档案-T47-VR.md"
    )
    work_log = ROOT / "docs" / "history" / "work-logs" / "T47-VR-工作日志.md"
    closed = (
        ROOT
        / "docs"
        / "history"
        / "card-plans"
        / "closed"
        / "T47-VR详细计划.md"
    )
    return archive.is_file() and work_log.is_file() and closed.is_file()
