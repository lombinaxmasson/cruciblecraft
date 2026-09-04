#!/usr/bin/env python3
"""Shared bath/remainder-VR repair constants, freeze facts, and later-R probes."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import census_common as census
from tools import bath_mte_common as bath_mte
from tools import bath_remainder_common as bath_remainder

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

FREEZE = TOOLS / "bath_remainder_vr_pre_repair_freeze.json"
REPAIR_READINESS = TOOLS / "bath_remainder_vr_repair_readiness.json"
CLOSEOUT_SEAL = TOOLS / "closeout_seal.py"
CLOSEOUT_SEAL_SCHEMA = TOOLS / "closeout_seal.schema.json"
PROFILES = TOOLS / "verification_profiles.json"
VERIFY = TOOLS / "verify.py"
DEBT = TOOLS / "known_issues" / "verification-debt.json"
BATH_IDENTITY_PLAN = ROOT / "docs" / "history" / "card-plans" / "active" / "bath/identity详细计划.md"
WAVE_SLICE_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CompactRecipeRuntimeWaveSlice.java"
)
BATH_REMAINDER_HARNESS_JAVA = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "BathRemainderIntegratedMeasurementHarness.java"
)

FROZEN_COUNTS = {
    "bath_remainder_production_families": 395,
    "bath_remainder_production_relations": 13708,
    "bath_remainder_identities": 283,
    "bath_remainder_remaining_ordinary_gap": 1499,
    "bath_remainder_reclassification_r": 0,
    "bath_mte_complete_families": 803,
    "bath_mte_remaining_gap_opening": 1894,
    "composed_runtime_groups": 15,
    "bath_remainder_gametest_passed": 10,
    "bath_remainder_gametest_required": 10,
}

EXPECTED_BATH_REMAINDER_LOCK_SHA256 = (
    "884643b2ba569e4eb96c9fcfe63caeebc61be6bb2df3334f85d3cf13490614db"
)
EXPECTED_BATH_MTE_LOCK_SHA256 = (
    "454a49b25ff329dbb053b9195841fe9bb51dcbc311fd293f990b354218a762f4"
)
OPEN_DEBT_IDS = ("T32-VD-002", "T32-VD-004")

IMMUTABLE_HASH_PATHS = (
    "tools/bath_mte_production_lock.json",
    "tools/bath_remainder_production_lock.json",
)

SNAPSHOT_HASH_PATHS = (
    "tools/bath_mte_production_lock.json",
    "tools/bath_mte_census_delta.json",
    "tools/bath_mte_card_topology.json",
    "tools/bath_mte_readiness.json",
    "tools/bath_mte_gametest_receipt.json",
    "tools/bath_mte_runtime_dependency_manifest.json",
    "tools/bath_remainder_production_lock.json",
    "tools/bath_remainder_census_delta.json",
    "tools/bath_remainder_card_topology.json",
    "tools/bath_remainder_readiness.json",
    "tools/bath_remainder_gametest_receipt.json",
    "tools/bath_remainder_runtime_dependency_manifest.json",
    "tools/global_build_identity_ledger.v2.json",
    "tools/compact_recipe_runtime_manifest.v2.json",
    "tools/verification_profiles.json",
    "docs/current/recipe-wave-workflow.md",
)

SEALED_CARD_IDS = (
    "roaster/compact",
    "centrifuge/compact",
    "electrolyzer/compact",
    "assembler/wood",
    "smelter/stone",
    "storage/lock",
    "block/object",
    "bath/mte",
    "bath/remainder",
)

LIVE_PIN_KEYS = ("identity_ledger_v2", "runtime_manifest_v2")


def relative(path: Path) -> str:
    return census.relative(path)


def load_json(path: Path) -> Any:
    return census.load_json(path)


def sha256_file(path: Path) -> str:
    return census.sha256_file(path)


def parse_write_check(description: str, argv: list[str] | None):
    return bath_mte.parse_write_check(description, argv)


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return bath_mte.check_document(path, document)


def live_counts() -> dict[str, int]:
    bath_remainder_lock = bath_remainder.load_production_lock()
    production = bath_remainder_lock.get("production") or {}
    catalog = load_json(bath_remainder.IDENTITY_CATALOG) if bath_remainder.IDENTITY_CATALOG.is_file() else {}
    bath_remainder_census = load_json(bath_remainder.CENSUS_DELTA) if bath_remainder.CENSUS_DELTA.is_file() else {}
    bath_mte_census = load_json(bath_mte.CENSUS_DELTA) if bath_mte.CENSUS_DELTA.is_file() else {}
    runtime = (
        load_json(bath_remainder.RUNTIME_MANIFEST_V2) if bath_remainder.RUNTIME_MANIFEST_V2.is_file() else {}
    )
    receipt = load_json(bath_remainder.GAME_TEST_RECEIPT) if bath_remainder.GAME_TEST_RECEIPT.is_file() else {}
    remaining = bath_remainder_census.get("remaining_ordinary") or {}
    return {
        "bath_remainder_production_families": int(production.get("family_count") or 0),
        "bath_remainder_production_relations": int(production.get("relation_count") or 0),
        "bath_remainder_identities": int(catalog.get("identity_count") or 0),
        "bath_remainder_remaining_ordinary_gap": int(
            bath_remainder_census.get("remaining_recipe_gap")
            or remaining.get("remaining_ordinary_families")
            or 0
        ),
        "bath_remainder_reclassification_r": int(bath_remainder_census.get("reclassification_delta") or 0),
        "bath_mte_complete_families": int(bath_mte_census.get("complete_family_count") or 0),
        "bath_mte_remaining_gap_opening": int(bath_mte_census.get("remaining_recipe_gap") or 0),
        "composed_runtime_groups": int(
            runtime.get("group_count")
            or (runtime.get("composition") or {}).get("composed_group_count")
            or 0
        ),
        "bath_remainder_gametest_passed": int(receipt.get("passed") or 0),
        "bath_remainder_gametest_required": int(receipt.get("required_tests") or 0),
    }


def live_lock_hashes() -> dict[str, str]:
    return {
        "tools/bath_mte_production_lock.json": bath_mte.production_lock_sha256(),
        "tools/bath_remainder_production_lock.json": bath_remainder.production_lock_sha256(),
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


def bath_identity_not_issued() -> bool:
    if BATH_IDENTITY_PLAN.is_file():
        return False
    if any(TOOLS.glob("bath_identity_production_lock.json")):
        return False
    if not bath_remainder.READINESS.is_file() or not bath_remainder.CARD_TOPOLOGY.is_file():
        return False
    readiness = load_json(bath_remainder.READINESS)
    topology = load_json(bath_remainder.CARD_TOPOLOGY)
    opening = readiness.get("bath_identity_opening") or {}
    return (
        opening.get("preassigned_host") is False
        and opening.get("preassigned_family_ids") is False
        and opening.get("next_issue_id") == "bath/identity"
        and topology.get("next_issue_id") == "bath/identity"
        and topology.get("unique_active_card") is None
        and topology.get("unique_active_card") != "bath/remainder-VR"
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
        immutable.get("tools/bath_mte_production_lock.json")
        == live["tools/bath_mte_production_lock.json"]
        == EXPECTED_BATH_MTE_LOCK_SHA256
        and immutable.get("tools/bath_remainder_production_lock.json")
        == live["tools/bath_remainder_production_lock.json"]
        == EXPECTED_BATH_REMAINDER_LOCK_SHA256
    )


def r1_closeout_seal() -> bool:
    if not file_present(CLOSEOUT_SEAL, CLOSEOUT_SEAL_SCHEMA):
        return False
    from tools import closeout_seal

    if not all(closeout_seal.seal_path(card_id).is_file() for card_id in ("bath/mte", "bath/remainder")):
        return False
    if not all(closeout_seal.seal_path(card_id).is_file() for card_id in SEALED_CARD_IDS):
        return False
    bath_mte_seal = closeout_seal.load_seal("bath/mte")
    bath_remainder_seal = closeout_seal.load_seal("bath/remainder")
    return (
        int(bath_mte_seal.get("complete_family_count") or 0) == 803
        and int(bath_remainder_seal.get("complete_family_count") or 0) == 395
        and bath_mte_seal.get("gametest_status") == "PASS"
        and bath_remainder_seal.get("gametest_status") == "PASS"
        and not closeout_seal.check_all()
    )


def r2_receipt_binding() -> bool:
    bath_mte_bound = bath_mte.bound_gametest_artifacts()
    bath_remainder_bound = bath_remainder.bound_gametest_artifacts()
    if any(key in bath_mte_bound for key in LIVE_PIN_KEYS):
        return False
    if any(key in bath_remainder_bound for key in LIVE_PIN_KEYS):
        return False
    if not bath_mte.GAME_TEST_RECEIPT.is_file() or not bath_remainder.GAME_TEST_RECEIPT.is_file():
        return False
    bath_mte_receipt = load_json(bath_mte.GAME_TEST_RECEIPT)
    bath_remainder_receipt = load_json(bath_remainder.GAME_TEST_RECEIPT)
    bath_mte_keys = bath_mte_receipt.get("bound_artifacts") or {}
    bath_remainder_keys = bath_remainder_receipt.get("bound_artifacts") or {}
    if any(key in bath_mte_keys for key in LIVE_PIN_KEYS):
        return False
    if any(key in bath_remainder_keys for key in LIVE_PIN_KEYS):
        return False
    required = (
        "gametest_java",
        "production_lock",
        "runtime_dependency_manifest",
        "publication_group_manifest",
        "shard_manifest",
    )
    return (
        all(bath_mte_keys.get(key) for key in required)
        and all(bath_remainder_keys.get(key) for key in required)
        and bath_mte_keys.get("bath_mte_generated_recipes")
        and bath_remainder_keys.get("bath_remainder_generated_recipes")
        and not bath_mte.gametest_receipt_errors()
        and not bath_remainder.gametest_receipt_errors()
    )


def r3_census_topology() -> bool:
    census_text = (TOOLS / "build_bath_mte_census_delta.py").read_text(encoding="utf-8")
    topology_text = (TOOLS / "build_bath_remainder_card_topology.py").read_text(encoding="utf-8")
    if "closeout_seal" not in census_text:
        return False
    if "closeout_seal" not in topology_text:
        return False
    if not bath_mte.CENSUS_DELTA.is_file() or not bath_remainder.CARD_TOPOLOGY.is_file():
        return False
    bath_mte_census = load_json(bath_mte.CENSUS_DELTA)
    topology = load_json(bath_remainder.CARD_TOPOLOGY)
    return (
        int(bath_mte_census.get("complete_family_count") or 0) == 803
        and topology.get("unique_active_card") is None
        and topology.get("unique_active_card") != "bath/remainder-VR"
        and topology.get("next_issue_id") == "bath/identity"
        and bool(topology.get("bath_remainder_complete"))
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
    if not WAVE_SLICE_JAVA.is_file() or not BATH_REMAINDER_HARNESS_JAVA.is_file():
        return False
    harness = BATH_REMAINDER_HARNESS_JAVA.read_text(encoding="utf-8")
    return (
        "bath_remainder_exact" in harness
        and "bath_remainder_exact_multi" in harness
        and ">= 13" in harness
    )


def r6_cutover() -> bool:
    archive = (
        ROOT
        / "docs"
        / "history"
        / "stage-archives"
        / "CrucibleCraft-阶段档案-bath/remainder-VR.md"
    )
    work_log = ROOT / "docs" / "history" / "work-logs" / "bath/remainder-VR-工作日志.md"
    closed = (
        ROOT
        / "docs"
        / "history"
        / "card-plans"
        / "closed"
        / "bath/remainder-VR详细计划.md"
    )
    return archive.is_file() and work_log.is_file() and closed.is_file()
