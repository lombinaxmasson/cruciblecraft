#!/usr/bin/env python3
"""Build T29 large-crucible readiness.

``status`` is derived. T29_READY requires the controller to be registered
without colliding with the single-block crucible, structure 27, capacity
432, the ten T29 GameTests, T23 positions 27, and a measured 0/0/0 GT
publication delta. Report-owned fields are excluded from check().
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t29_readiness.json"
POLICY = TOOLS / "t29_readiness_policy.json"
BUILDER = Path(__file__).resolve()
EVIDENCE = TOOLS / "t29_crucible_source_evidence.json"
PROJECTION = TOOLS / "t29_load_projection.json"
DELTA = TOOLS / "t29_publication_delta.json"
T23_POLICY = TOOLS / "t23_multiblock_policy.json"
T23_PLUGINS = TOOLS / "t23_plugin_whitelist.json"
DATAPACK_PLUGINS = (
    ROOT / "src/main/resources/data/cruciblecraft/multiblock_plugins.json"
)
JAVA_PLUGINS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModMultiblockPlugins.java"
)
STRUCTURE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/multiblock_structures/large_crucible.json"
)
BLOCK = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/block/LargeCrucibleBlock.java"
)
SINGLE_BLOCK = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/blockentity/CrucibleBlockEntity.java"
)
PROCESS_CORE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/machine/component/CrucibleProcessCore.java"
)
GAMETEST = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
)
REPORT = TOOLS / "full_verification_report.json"
TOPOLOGY = TOOLS / "t27_card_topology.json"

REPORT_OWNED = ("currentness", "runtime", "status")
EAGER_HARD_CEILING = 21_000
EXPECTED_GAME_TESTS = (
    "t29LargeCrucibleFormation",
    "t29LargeCrucibleTeardown",
    "t29LargeCrucibleOutputJam",
    "t29LargeCruciblePowerLoss",
    "t29LargeCrucibleReload",
    "t29LargeCrucibleSaveQuarantine",
    "t29LargeCrucibleCapacity432",
    "t29LargeCrucibleSteelmaking",
    "t29LargeCrucibleMoldCast",
    "t29LargeCrucibleLayerPorts",
)
PLUGIN_SET = {
    "cruciblecraft:thermal_steelmaking_host",
    "cruciblecraft:heat_energy_input",
    "cruciblecraft:shared_port_supply",
}


def _runtime() -> dict[str, Any]:
    report = common.load_json(REPORT) if REPORT.is_file() else {}
    game_tests = (report.get("tests") or {}).get("production_game_tests") or {}
    passing = None
    if game_tests.get("result") == "PASS":
        passing = True
    elif game_tests.get("result"):
        passing = False
    java = (report.get("tests") or {}).get("java_unit_tests") or {}
    java_pass = None
    if java.get("result") == "PASS":
        java_pass = True
    elif java.get("result"):
        java_pass = False
    return {
        "gametest_passing": passing,
        "gametest_total": game_tests.get("required_tests"),
        "java_unit_tests_passing": java_pass,
        "report_status": report.get("status"),
        "t28_status": (report.get("t28_readiness_acceptance") or {}).get("status"),
        "rc_number": (report.get("phase5_portfolio") or {}).get("rc_number"),
    }


def _crucible_positions() -> int:
    for row in common.load_json(T23_POLICY).get("kinds") or []:
        if row.get("canonical_id") == "crucible":
            return int(row["schema_expressibility"]["positions"])
    raise ValueError("T23 policy is missing the crucible kind")


def _plugin_mirrors() -> dict[str, Any]:
    tools_ids = {row["id"] for row in common.load_json(T23_PLUGINS).get("plugins") or []}
    datapack_ids = {
        row["id"] for row in common.load_json(DATAPACK_PLUGINS).get("plugins") or []
    }
    java_text = JAVA_PLUGINS.read_text(encoding="utf-8") if JAVA_PLUGINS.is_file() else ""
    java_has_host = "thermal_steelmaking_host" in java_text
    consumers = {}
    for row in common.load_json(DATAPACK_PLUGINS).get("plugins") or []:
        consumers[row["id"]] = set(row.get("consumers") or [])
    return {
        "mirrors_agree": tools_ids == datapack_ids,
        "java_declares_host": java_has_host,
        "host_present": "cruciblecraft:thermal_steelmaking_host" in tools_ids,
        "not_processing_host": "large_crucible"
        not in consumers.get("cruciblecraft:processing_host", set()),
        "large_crucible_consumes_host": "large_crucible"
        in consumers.get("cruciblecraft:thermal_steelmaking_host", set()),
        "heat_consumer": "large_crucible"
        in consumers.get("cruciblecraft:heat_energy_input", set()),
        "shared_consumer": "large_crucible"
        in consumers.get("cruciblecraft:shared_port_supply", set()),
    }


def _scan_game_tests() -> int:
    text = GAMETEST.read_text(encoding="utf-8") if GAMETEST.is_file() else ""
    return sum(1 for name in EXPECTED_GAME_TESTS if f"void {name}(" in text)


def _capacity_constants() -> dict[str, bool]:
    core = PROCESS_CORE.read_text(encoding="utf-8") if PROCESS_CORE.is_file() else ""
    single = SINGLE_BLOCK.read_text(encoding="utf-8") if SINGLE_BLOCK.is_file() else ""
    return {
        "large_432": "LARGE_MAX_INGOTS = 432" in core,
        "single_8": "SINGLE_BLOCK_MAX_INGOTS = 8" in core
        and "MAX_INGOTS = CrucibleProcessCore.SINGLE_BLOCK_MAX_INGOTS" in single,
    }


def _structure_ok() -> dict[str, Any]:
    if not STRUCTURE.is_file():
        return {"present": False, "positions": 0, "controller_id": ""}
    document = common.load_json(STRUCTURE)
    controller = ""
    for value in (document.get("palette") or {}).values():
        if value.get("type") == "controller":
            controller = value.get("block") or ""
    return {
        "present": True,
        "positions": len(document.get("structure") or []),
        "controller_id": controller,
    }


def _replacement_text() -> str:
    topology = common.load_json(TOPOLOGY)
    for card in topology.get("cards") or []:
        if card.get("id") == "T29":
            conditions = card.get("replacement_conditions") or []
            return conditions[0] if conditions else ""
    kinds = common.load_json(TOOLS / "t27_portfolio" / "multiblock_kinds.json")
    for row in kinds.get("records") or []:
        if row.get("canonical_id") == "crucible":
            return str(row.get("replacement_condition") or "")
    return ""


def build() -> dict[str, Any]:
    for path in (
        POLICY,
        EVIDENCE,
        PROJECTION,
        DELTA,
        T23_POLICY,
        T23_PLUGINS,
        DATAPACK_PLUGINS,
        JAVA_PLUGINS,
    ):
        if not path.is_file():
            raise FileNotFoundError(common.relative(path))
    policy = common.load_json(POLICY)
    evidence = common.load_json(EVIDENCE)
    projection = common.load_json(PROJECTION)
    delta = common.load_json(DELTA)
    runtime = _runtime()
    plugins = _plugin_mirrors()
    structure = _structure_ok()
    capacity = _capacity_constants()
    game_tests = _scan_game_tests()
    t23_positions = _crucible_positions()
    replacement = _replacement_text()
    closure = {
        "large_crucible_registered": BLOCK.is_file()
        and structure["controller_id"] == "cruciblecraft:large_crucible",
        "single_block_crucible_retained": SINGLE_BLOCK.is_file(),
        "structure_positions_27": structure["positions"] == 27,
        "capacity_432": capacity["large_432"],
        "single_capacity_8": capacity["single_8"],
        "lifecycle_gametests_present": game_tests == len(EXPECTED_GAME_TESTS),
        "replacement_condition_text": bool(replacement),
        "replacement_satisfied": BLOCK.is_file() and SINGLE_BLOCK.is_file(),
    }
    fidelity = {
        "source_revision_pinned": evidence["source"]["revision"]
        == common.SOURCE_REVISION,
        "structure_27_source_backed": evidence["structure"]["positions"] == 27
        and projection["fidelity_layering"]["structure_27"] == "SOURCE_BACKED",
        "capacity_432_source_backed": projection["fidelity_layering"]["capacity_432"]
        == "SOURCE_BACKED",
        "layer_ports_source_backed": projection["fidelity_layering"]["layer_ports"]
        == "SOURCE_BACKED",
        "heat_scale_design_policy": projection["fidelity_layering"]["heat_scale"]
        == "DESIGN_POLICY",
        "steelmaking_source_derived": projection["fidelity_layering"]["steelmaking"]
        == "SOURCE_DERIVED",
        "t23_positions_27": t23_positions == 27,
        "plugin_mirrors_agree": plugins["mirrors_agree"]
        and plugins["java_declares_host"]
        and plugins["host_present"]
        and plugins["not_processing_host"]
        and plugins["large_crucible_consumes_host"]
        and plugins["heat_consumer"]
        and plugins["shared_consumer"],
        "not_processing_host": projection["fidelity_layering"]["not_a_processing_host"]
        is True,
    }
    load_data = {
        "eager_hard_ceiling_is_21000": EAGER_HARD_CEILING == 21000,
        "gt_delta_zero": delta["projected"] == {"eager": 0, "lazy": 0, "logical": 0},
        "measured_zero": delta["measured"]["status"] == "measured"
        and delta["measured"]["logical"] == 0
        and delta["measured"]["eager"] == 0
        and delta["measured"]["lazy"] == 0,
        "same_sign_when_measured": delta["same_sign_as_projection"] is True,
        "vanilla_controller_accounted": projection["vanilla_crafting"]["controller"]
        == 1
        and projection["vanilla_crafting"]["gt_eager"] is False,
        "gametest_projected_131": projection["tests"]["gametest"]["projected"] == 131,
    }
    closure_ok = all(value is True for value in closure.values())
    fidelity_ok = all(value is True for value in fidelity.values())
    load_ok = all(value is True for value in load_data.values())
    gates_ok = (
        closure_ok
        and fidelity_ok
        and load_ok
        and runtime["gametest_passing"] is True
        and runtime["gametest_total"] >= 131
        and runtime["java_unit_tests_passing"] is True
        and runtime["report_status"] == "READY"
        and runtime["t28_status"] == "T28_READY"
        and runtime["rc_number"] in (None, "")
        and len((policy.get("closure_policy") or {}).get("pending") or []) == 0
        and (policy.get("closure_policy") or {}).get("final_closure_attempted")
        is True
    )
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        common.relative(EVIDENCE): common.sha256_file(EVIDENCE),
        common.relative(PROJECTION): common.sha256_file(PROJECTION),
        common.relative(DELTA): common.sha256_file(DELTA),
    }
    document: dict[str, Any] = {
        "closure": closure,
        "closure_policy": policy.get("closure_policy"),
        "currentness": {"owned_inputs": owned_inputs},
        "fidelity": fidelity,
        "generated_by": "python tools/build_t29_readiness.py --write",
        "load": load_data,
        "owned_inputs": owned_inputs,
        "plugin_set": sorted(PLUGIN_SET),
        "policy": (
            "The status field is derived from evidence by build(). "
            "T29_READY cannot be hand-written."
        ),
        "replacement_condition": replacement,
        "runtime": {
            "gametest_passing": runtime["gametest_passing"],
            "gametest_total": runtime["gametest_total"],
            "java_unit_tests_passing": runtime["java_unit_tests_passing"],
            "rc_number": runtime["rc_number"],
            "report_status": runtime["report_status"],
            "t28_status": runtime["t28_status"],
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status_owner": "run_full_verification",
        "t29_gametests_found": game_tests,
    }
    if gates_ok:
        document["status"] = "T29_READY"
    return document


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = build()
    actual = json.loads(OUTPUT.read_text(encoding="utf-8"))
    for key in REPORT_OWNED:
        expected.pop(key, None)
        actual.pop(key, None)
    if common.stable_json(expected) != common.stable_json(actual):
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"status={document.get('status')}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
