#!/usr/bin/env python3
"""Build T30 hopper-family readiness.

``status`` is derived. T30_READY requires 60 source rows, 120 hopper-family
identities, steel_dust_funnel, 121 vanilla recipes, the T30 GameTests, a
measured 0/0/0 GT publication delta, and rc_number still null. Report-owned
fields are excluded from check().
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
OUTPUT = TOOLS / "t30_readiness.json"
POLICY = TOOLS / "t30_readiness_policy.json"
BUILDER = Path(__file__).resolve()
EVIDENCE = TOOLS / "t30_hopper_source_evidence.json"
PROJECTION = TOOLS / "t30_load_projection.json"
DELTA = TOOLS / "t30_publication_delta.json"
CONTRACT = TOOLS / "phase5_1_pre_rc_logistics_contract.json"
ART = TOOLS / "t30_art_asset_provenance.json"
CATALOG = ROOT / "src/main/resources/data/cruciblecraft/hopper_variants.json"
HOPPER_BLOCK = (
    ROOT / "src/main/java/com/masson/cruciblecraft/content/block/HopperBlock.java"
)
FUNNEL_BLOCK = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/block/DustFunnelBlock.java"
)
HOPPER_BE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/blockentity/HopperBlockEntity.java"
)
FUNNEL_BE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/blockentity/DustFunnelBlockEntity.java"
)
GAMETEST = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
)
RECIPES = ROOT / "src/generated/resources/data/cruciblecraft/recipe/hoppers"
REPORT = TOOLS / "full_verification_report.json"
EAGER_HARD_CEILING = 21_000
DATAPACK_HARD_CEILING = 6_600
EXPECTED_GAME_TESTS = (
    "hopperSlotSentinelsPullAndPushChests",
    "hopperRedstonePausesAndItemEntityPickupResumes",
    "hopperModeExactAndToolCycles",
    "hopperSaveReloadBreakAndItemPipe",
    "queueHopperFifoSlotLimitsAndRedstone",
    "dustFunnelConvertsModesAndPersists",
)
REPORT_OWNED = ("currentness", "runtime", "status")


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
        "t29_status": (report.get("t29_readiness_acceptance") or {}).get("status"),
        "rc_number": (report.get("phase5_portfolio") or {}).get("rc_number"),
    }


def _source_game_test_count() -> int:
    if not GAMETEST.is_file():
        return 0
    return GAMETEST.read_text(encoding="utf-8").count("@GameTest(")


def _scan_game_tests() -> int:
    text = GAMETEST.read_text(encoding="utf-8") if GAMETEST.is_file() else ""
    return sum(1 for name in EXPECTED_GAME_TESTS if f"void {name}(" in text)


def _recipe_count() -> int:
    if not RECIPES.is_dir():
        return 0
    return sum(1 for path in RECIPES.glob("*.json") if path.is_file())


def _bare_ids_absent() -> bool:
    catalog = common.load_json(CATALOG) if CATALOG.is_file() else {}
    rows = catalog.get("variants") or []
    names = {
        str(row.get("material") or "").split(":")[-1]
        for row in rows
    }
    return "hopper" not in names and "queue_hopper" not in names


def build() -> dict[str, Any]:
    for path in (POLICY, EVIDENCE, PROJECTION, DELTA, CONTRACT, ART, CATALOG):
        if not path.is_file():
            raise FileNotFoundError(common.relative(path))
    policy = common.load_json(POLICY)
    evidence = common.load_json(EVIDENCE)
    projection = common.load_json(PROJECTION)
    delta = common.load_json(DELTA)
    contract = common.load_json(CONTRACT)
    art = common.load_json(ART)
    runtime = _runtime()
    game_tests = _scan_game_tests()
    recipes = _recipe_count()
    closure = {
        "source_rows_60": evidence["counts"]["rows"] == 60,
        "hopper_identities_120": projection["registration"]["hopper"] == 60
        and projection["registration"]["queue_hopper"] == 60,
        "dust_funnel_1": projection["registration"]["dust_funnel"] == 1
        and FUNNEL_BLOCK.is_file()
        and FUNNEL_BE.is_file(),
        "hopper_host_present": HOPPER_BLOCK.is_file() and HOPPER_BE.is_file(),
        "vanilla_recipes_121": recipes == 121,
        "no_bare_hopper_ids": _bare_ids_absent(),
        "lifecycle_gametests_present": game_tests == len(EXPECTED_GAME_TESTS),
        "replacement_satisfied": contract["catalog"]["total_new_blocks"] == 121,
    }
    fidelity = {
        "source_revision_pinned": evidence.get("source_revision")
        == common.SOURCE_REVISION,
        "slots_source_backed": evidence["counts"]["rows"] == 60
        and evidence["counts"]["unresolved"] == 0,
        "recipes_source_derived": projection["vanilla_crafting"]["gt_eager"] is False
        and "plateCurved" in projection["vanilla_crafting"]["note"],
        "dust_three_form_bounded": contract["dust_forms"]
        == ["dust", "small_dust", "tiny_dust"],
        "art_derived": art.get("status") == "T30_ART_ASSET_PROVENANCE"
        and art.get("classification") == "ART_DERIVED",
        "no_plate_curved_runtime": "plateCurved_runtime"
        in contract["post_1_0_exclusions"],
        "no_block_dust": "blockDust" in contract["post_1_0_exclusions"],
        "rc_number_null": contract["opening"]["rc_number"] is None,
    }
    load_data = {
        "eager_hard_ceiling_is_21000": EAGER_HARD_CEILING == 21_000,
        "datapack_hard_ceiling_is_6600": DATAPACK_HARD_CEILING == 6_600,
        "gt_delta_zero": delta["projected"] == {"eager": 0, "lazy": 0, "logical": 0},
        "measured_zero": delta["measured"]["status"] == "measured"
        and delta["measured"]["logical"] == 0
        and delta["measured"]["eager"] == 0
        and delta["measured"]["lazy"] == 0,
        "same_sign_when_measured": delta["same_sign_as_projection"] is True,
        "vanilla_hopper_accounted": projection["vanilla_crafting"]["total"] == 121
        and projection["vanilla_crafting"]["gt_eager"] is False,
        "pending_axes_not_zeroed": all(
            axis.get("status") != "pending" or axis.get("delta") is None
            for axis in projection["t14_axes"].values()
        ),
    }
    closure_ok = all(value is True for value in closure.values())
    fidelity_ok = all(value is True for value in fidelity.values())
    load_ok = all(value is True for value in load_data.values())
    gates_ok = (
        closure_ok
        and fidelity_ok
        and load_ok
        and runtime["gametest_passing"] is True
        and runtime["gametest_total"] == _source_game_test_count()
        and runtime["java_unit_tests_passing"] is True
        and runtime["report_status"] == "READY"
        and runtime["t29_status"] == "T29_READY"
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
        common.relative(CONTRACT): common.sha256_file(CONTRACT),
        common.relative(ART): common.sha256_file(ART),
    }
    document: dict[str, Any] = {
        "closure": closure,
        "closure_policy": policy.get("closure_policy"),
        "currentness": {"owned_inputs": owned_inputs},
        "fidelity": fidelity,
        "generated_by": "python tools/build_t30_readiness.py --write",
        "load": load_data,
        "owned_inputs": owned_inputs,
        "policy": (
            "The status field is derived from evidence by build(). "
            "T30_READY cannot be hand-written."
        ),
        "runtime": {
            "gametest_passing": runtime["gametest_passing"],
            "gametest_total": runtime["gametest_total"],
            "java_unit_tests_passing": runtime["java_unit_tests_passing"],
            "rc_number": runtime["rc_number"],
            "report_status": runtime["report_status"],
            "t29_status": runtime["t29_status"],
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status_owner": "run_full_verification",
        "t30_gametests_found": game_tests,
        "vanilla_recipe_count": recipes,
    }
    if gates_ok:
        document["status"] = "T30_READY"
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
