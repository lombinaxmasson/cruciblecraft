#!/usr/bin/env python3
"""Shared electrolyzer/compact-VR repair constants, freeze facts, and later-R probes."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import census_common as census
from tools import roaster_common as roaster
from tools import centrifuge_common as centrifuge
from tools import electrolyzer_common as electrolyzer

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

FREEZE = TOOLS / "verification_runtime_pre_repair_freeze.json"
REPAIR_READINESS = TOOLS / "verification_runtime_repair_readiness.json"
MATERIAL_FORM_AUTHORITY = TOOLS / "material_form_authority.json"
MATERIAL_FORM_AUTHORITY_SCHEMA = TOOLS / "material_form_authority.schema.json"
ATOMIC_IO = TOOLS / "atomic_io.py"
AUTHORITY_MANIFEST = TOOLS / "authority_manifest.json"
CURRENTNESS = TOOLS / "currentness.py"
REBIND_CURRENTNESS = TOOLS / "rebind_currentness.py"
VERIFICATION_DAG = TOOLS / "verification_dependency_dag.json"
VERIFICATION_DAG_SCHEMA = TOOLS / "verification_dependency_dag.schema.json"
RUN_VERIFICATION_DAG = TOOLS / "run_verification_dag.py"
BUILDER_CLI = TOOLS / "builder_cli.py"
CROSS_STACK_CLOSURE = TOOLS / "verification_runtime_material_cross_stack_closure.json"
CENTRIFUGE_COMPACT_SUPPORT_CONTRACT = TOOLS / "centrifuge_support_contract.json"
DEBT = TOOLS / "known_issues" / "verification-debt.json"
PROFILES = TOOLS / "verification_profiles.json"
BUILDER_POLICY = TOOLS / "verification_builder_policy.json"
ASSEMBLER_WOOD_PLAN = ROOT / "docs" / "history" / "card-plans" / "active" / "assembler/wood详细计划.md"

FROZEN_COUNTS = {
    "electrolyzer_production_families": 13,
    "electrolyzer_production_relations": 22,
    "electrolyzer_remaining_ordinary_gap": 5597,
    "centrifuge_locked_support_routes": 34,
    "roaster_support_authored": 19,
    "roaster_support_eager": 15,
    "factual_ore_materials": 137,
    "registered_ore_materials": 147,
    "roaster_acquisition_ore_delta": 10,
    "chemical_semantic_vein_ledger": 8,
}

EXPECTED_ELECTROLYZER_COMPACT_LOCK_SHA256 = (
    "26cca29698c34039cc8aa19ff8e584a0ec7a521165ec39e8dcece03cf75488e5"
)
EXPECTED_CENTRIFUGE_COMPACT_LOCK_SHA256 = (
    "b9034b2950a41b8905ef6668de793d04136432070cd238e577754148d7aa8600"
)
OPEN_DEBT_IDS = ("T32-VD-002", "T32-VD-004")

IMMUTABLE_HASH_PATHS = (
    "tools/centrifuge_production_lock.json",
    "tools/electrolyzer_production_lock.json",
)

SNAPSHOT_HASH_PATHS = (
    "tools/census_readiness.json",
    "tools/census.json",
    "tools/census_recipe_families.json",
    "tools/census_runtime_registry.json",
    "tools/census_card_topology.json",
    "tools/roaster_readiness.json",
    "tools/roaster_gametest_receipt.json",
    "tools/centrifuge_production_lock.json",
    "tools/centrifuge_readiness.json",
    "tools/centrifuge_gametest_receipt.json",
    "tools/centrifuge_runtime_dependency_manifest.json",
    "tools/electrolyzer_production_lock.json",
    "tools/electrolyzer_readiness.json",
    "tools/electrolyzer_gametest_receipt.json",
    "tools/electrolyzer_runtime_dependency_manifest.json",
    "src/main/resources/data/cruciblecraft/material_registration_gate.json",
    "src/main/java/com/masson/cruciblecraft/material/MaterialRegistrationGate.java",
    "tools/verification_profiles.json",
    "tools/verification_builder_policy.json",
    "tools/known_issues/verification-debt.json",
)

CURRENTNESS_SIDECARS = (
    "tools/material_registration_gate.currentness.json",
    "tools/census_recipe_families.currentness.json",
    "tools/census_runtime_registry.currentness.json",
    "tools/census.currentness.json",
    "tools/census_card_topology.currentness.json",
    "tools/census_readiness.currentness.json",
    "tools/centrifuge_census_delta.currentness.json",
    "tools/centrifuge_card_topology.currentness.json",
    "tools/centrifuge_readiness.currentness.json",
    "tools/electrolyzer_census_delta.currentness.json",
    "tools/electrolyzer_card_topology.currentness.json",
    "tools/electrolyzer_readiness.currentness.json",
)

_ORE_CHAIN: dict[str, Any] | None = None
_GATE: dict[str, Any] | None = None


def relative(path: Path) -> str:
    return census.relative(path)


def load_json(path: Path) -> Any:
    return census.load_json(path)


def sha256_file(path: Path) -> str:
    return census.sha256_file(path)


def parse_write_check(description: str, argv: list[str] | None):
    return electrolyzer.parse_write_check(description, argv)


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return electrolyzer.check_document(path, document)


def gate_document() -> dict[str, Any]:
    global _GATE
    if _GATE is None:
        _GATE = load_json(electrolyzer.MATERIAL_REGISTRATION_GATE_JSON)
    return _GATE


def ore_chain_document() -> dict[str, Any]:
    global _ORE_CHAIN
    if _ORE_CHAIN is None:
        _ORE_CHAIN = load_json(roaster.ORE_CHAIN_JSON)
    return _ORE_CHAIN


def live_counts() -> dict[str, int]:
    gate = gate_document()
    registered_ore = sum(
        1
        for forms in (gate.get("materials") or {}).values()
        if isinstance(forms, list) and "ore" in forms
    )
    roaster_support = roaster.support_recipe_ledger()
    ore_chain = ore_chain_document()
    vein_ledger = (ore_chain.get("coverage_ledger") or {}).get(
        "worldgen_ore_materials"
    ) or []
    centrifuge_lock = centrifuge.load_production_lock()
    return {
        "electrolyzer_production_families": electrolyzer.production_family_count(),
        "electrolyzer_production_relations": electrolyzer.production_relation_count(),
        "electrolyzer_remaining_ordinary_gap": int(
            (load_json(electrolyzer.READINESS).get("evidence") or {}).get(
                "remaining_recipe_gap"
            )
            or 0
        ),
        "centrifuge_locked_support_routes": int(
            (centrifuge_lock.get("support") or {}).get("route_count") or 0
        ),
        "roaster_support_authored": int(roaster_support["authored"]),
        "roaster_support_eager": int(roaster_support["eager"]),
        "factual_ore_materials": int(
            (gate.get("counts") or {}).get("ore_source_materials") or 0
        ),
        "registered_ore_materials": registered_ore,
        "roaster_acquisition_ore_delta": len(
            roaster_support.get("new_ore_block_materials") or []
        ),
        "chemical_semantic_vein_ledger": len(vein_ledger),
    }


def live_lock_hashes() -> dict[str, str]:
    return {
        "tools/centrifuge_production_lock.json": centrifuge.production_lock_sha256(),
        "tools/electrolyzer_production_lock.json": electrolyzer.production_lock_sha256(),
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


def assembler_wood_not_issued() -> bool:
    readiness = load_json(electrolyzer.READINESS)
    opening = readiness.get("assembler_wood_opening") or {}
    return (
        not ASSEMBLER_WOOD_PLAN.is_file()
        and opening.get("preassigned_host") is False
        and opening.get("preassigned_family_ids") is False
        and readiness.get("evidence", {}).get("unique_active_card") is None
        and opening.get("next_issue_id") == "assembler/wood"
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
        immutable.get("tools/centrifuge_production_lock.json") == live["tools/centrifuge_production_lock.json"]
        == EXPECTED_CENTRIFUGE_COMPACT_LOCK_SHA256
        and immutable.get("tools/electrolyzer_production_lock.json")
        == live["tools/electrolyzer_production_lock.json"]
        == EXPECTED_ELECTROLYZER_COMPACT_LOCK_SHA256
    )


def r1_material_authority() -> bool:
    if not file_present(
        MATERIAL_FORM_AUTHORITY,
        MATERIAL_FORM_AUTHORITY_SCHEMA,
        CROSS_STACK_CLOSURE,
    ):
        return False
    authority = load_json(MATERIAL_FORM_AUTHORITY)
    if not (authority.get("sources") or []):
        return False
    java_text = electrolyzer.MATERIAL_REGISTRATION_GATE_JAVA.read_text(encoding="utf-8")
    if "java_overlay_sections" not in java_text:
        return False
    recipes = (TOOLS / "build_centrifuge_centrifuge_recipes.py").read_text(encoding="utf-8")
    if "GATE_OUT.write_text" in recipes:
        return False
    return int(gate_document().get("schema_version") or 0) == 2


def r2_atomic_io() -> bool:
    if not file_present(ATOMIC_IO, AUTHORITY_MANIFEST):
        return False
    t27 = (TOOLS / "json_ledger.py").read_text(encoding="utf-8")
    t35_text = (TOOLS / "census_common.py").read_text(encoding="utf-8")
    runner = (TOOLS / "run_python_tests.py").read_text(encoding="utf-8")
    return (
        "from tools import atomic_io" in t27
        or "import tools.atomic_io" in t27
        or "atomic_io.write_bytes" in t27
        or "atomic_io.write_stable" in t27
        or "atomic_io.write_bytes_atomic" in t27
    ) and (
        "atomic_io" in t35_text
    ) and (
        "authority" in runner.lower() and "guard" in runner.lower()
    )


def r3_currentness() -> bool:
    if not file_present(CURRENTNESS, REBIND_CURRENTNESS):
        return False
    return all((ROOT / path).is_file() for path in CURRENTNESS_SIDECARS)


def r4_dag_receipts() -> bool:
    if not file_present(
        VERIFICATION_DAG,
        VERIFICATION_DAG_SCHEMA,
        RUN_VERIFICATION_DAG,
        CENTRIFUGE_COMPACT_SUPPORT_CONTRACT,
    ):
        return False
    electrolyzer_receipt = load_json(electrolyzer.GAME_TEST_RECEIPT)
    return (
        "behavior_root_sha256" in electrolyzer_receipt
        and "currentness_root_sha256" in electrolyzer_receipt
    )


def r5_runner_cli() -> bool:
    if not file_present(BUILDER_CLI):
        return False
    verify_text = (TOOLS / "verify.py").read_text(encoding="utf-8")
    component = (TOOLS / "build_component_rules.py").read_text(encoding="utf-8")
    profiles = load_json(PROFILES)
    encoded = json.dumps(profiles)
    recipes = (profiles.get("profiles") or {}).get("recipes") or {}
    return (
        "--report-all" in verify_text
        and "--json" in verify_text
        and "currentness" in verify_text
        and "card-closeout" in encoded
        and "card-diagnostic-electrolyzer/compact" in encoded
        and isinstance(recipes.get("gametest"), dict)
        and "parser.error(" in component
        and "args.check" in component
        and "args.write" in component
    )


def r6_cutover() -> bool:
    archive = (
        ROOT
        / "docs"
        / "history"
        / "stage-archives"
        / "CrucibleCraft-阶段档案-electrolyzer/compact-VR.md"
    )
    work_log = ROOT / "docs" / "history" / "work-logs" / "electrolyzer/compact-VR-工作日志.md"
    closed = (
        ROOT
        / "docs"
        / "history"
        / "card-plans"
        / "closed"
        / "electrolyzer/compact-VR详细计划.md"
    )
    return archive.is_file() and work_log.is_file() and closed.is_file()
