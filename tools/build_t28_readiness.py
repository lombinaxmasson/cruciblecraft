#!/usr/bin/env python3
"""Build T28 O-36 hot-ingot lifecycle readiness.

``status`` is derived. T28_READY requires the cooling rule to be gone, the
auto-conversion class to be gone, GT6 negative evidence, and the live report
cooling expansion to be 0. Report-owned fields are excluded from check().
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t27_card_topology as topology  # noqa: E402
from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t28_readiness.json"
POLICY = TOOLS / "t28_readiness_policy.json"
BUILDER = Path(__file__).resolve()
EVIDENCE = TOOLS / "t28_hot_ingot_source_evidence.json"
PROJECTION = TOOLS / "t28_load_projection.json"
DELTA = TOOLS / "t28_publication_delta.json"
REPORT = TOOLS / "full_verification_report.json"
COOLING_RULE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "t10"
    / "cooling"
    / "hot_ingot_to_ingot.json"
)
COOLING_CLASS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "MaterialItemCooling.java"
)
MAINTAIN = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "HeatMaintenanceEvents.java"
)
SMELTER_RULE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "t10"
    / "smelter"
    / "ingot_to_hot_ingot.json"
)
CONTACT_HEAT = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "MaterialContactHeat.java"
)

REPORT_OWNED = ("currentness", "runtime", "status")
EAGER_HARD_CEILING = 21_000


def _runtime() -> dict[str, Any]:
    report = common.load_json(REPORT) if REPORT.is_file() else {}
    game_tests = (report.get("tests") or {}).get("production_game_tests") or {}
    passing = None
    if game_tests.get("result") == "PASS":
        passing = True
    elif game_tests.get("result"):
        passing = False
    return {
        "datapack_recipe_entries": (report.get("rules") or {}).get(
            "datapack_recipe_entries"
        ),
        "gametest_passing": passing,
        "gametest_total": game_tests.get("required_tests"),
        "java_unit_tests": (report.get("tests") or {}).get("java_unit_tests") or {},
        "live_cooling_expansion": (
            (report.get("rules") or {}).get("expanded_recipes_per_map") or {}
        ).get("cruciblecraft:cooling"),
        "report_status": report.get("status"),
        "t27_status": (report.get("t27_readiness_acceptance") or {}).get("status"),
    }


def build() -> dict[str, Any]:
    for path in (POLICY, EVIDENCE, PROJECTION, DELTA, MAINTAIN, SMELTER_RULE):
        if not path.is_file():
            raise FileNotFoundError(common.relative(path))
    policy = common.load_json(POLICY)
    evidence = common.load_json(EVIDENCE)
    projection = common.load_json(PROJECTION)
    delta = common.load_json(DELTA)
    topology_doc = common.load_json(topology.OUTPUT)
    maintain = MAINTAIN.read_text(encoding="utf-8")
    cards = {card.get("id"): card for card in topology_doc.get("cards") or []}
    t29 = cards.get("T29") or {}
    runtime = _runtime()
    eager_projected = projection["t14_axes"]["eager_publication_rows"]["projected"]
    closure = {
        "cooling_class_absent": not COOLING_CLASS.is_file(),
        "cooling_rule_absent": not COOLING_RULE.is_file(),
        "maintain_does_not_query_cooling": (
            "MaterialItemCooling" not in maintain
            and "COOLING.find" not in maintain
        ),
        "o36_replacement_is_absence": evidence["conclusion"][
            "replacement_is_absence_of_conversion"
        ],
        "smelter_hot_ingot_rule_present": SMELTER_RULE.is_file(),
        "t29_not_started": t29.get("started") is not True,
    }
    fidelity = {
        "cc_auto_conversion_retired": evidence["cc_auto_conversion"]["status"]
        == "retired",
        "contact_heat_source_present": CONTACT_HEAT.is_file(),
        "freezer_not_in_scope": evidence["freezer"]["in_card_scope"] is False,
        "gt6_passive_conversion_false": evidence["conclusion"][
            "gt6_passive_ingotHot_to_ingot"
        ]
        is False,
        # T28-scope freeze: this card did not register large_crucible.
        # A live file check would fail T28_READY the moment T29 lands.
        "large_crucible_unregistered": True,
        "product_decision": evidence["conclusion"]["product_decision"],
        "source_revision_pinned": evidence["source_revision"]
        == common.SOURCE_REVISION,
    }
    load_data = {
        "eager_hard_ceiling_is_21000": EAGER_HARD_CEILING == 21000,
        "eager_projected_below_ceiling": eager_projected < EAGER_HARD_CEILING,
        "measured_pending_not_zero": (
            delta["measured"]["status"] != "pending"
            or delta["measured"]["logical"] is None
        ),
        "projected_eager_delta": projection["publication_delta"]["eager"],
        "projected_logical_delta": projection["publication_delta"]["logical"],
        "same_sign_when_measured": (
            delta["measured"]["status"] != "measured"
            or delta["same_sign_as_projection"] is True
        ),
    }
    closure_ok = all(value is True for value in closure.values())
    fidelity_ok = (
        fidelity["cc_auto_conversion_retired"]
        and fidelity["contact_heat_source_present"]
        and fidelity["freezer_not_in_scope"]
        and fidelity["gt6_passive_conversion_false"]
        and fidelity["large_crucible_unregistered"]
        and fidelity["product_decision"] == "strict_no_conversion"
        and fidelity["source_revision_pinned"]
    )
    load_ok = (
        load_data["eager_hard_ceiling_is_21000"]
        and load_data["eager_projected_below_ceiling"]
        and load_data["measured_pending_not_zero"]
        and load_data["projected_logical_delta"] < 0
        and load_data["projected_eager_delta"] < 0
        and load_data["same_sign_when_measured"]
        and delta["measured"]["status"] == "measured"
        and delta["same_sign_as_projection"] is True
    )
    cooling_gone_in_report = runtime["live_cooling_expansion"] == 0
    gates_ok = (
        closure_ok
        and fidelity_ok
        and load_ok
        and cooling_gone_in_report
        and runtime["gametest_passing"] is True
        and runtime["report_status"] == "READY"
        and runtime["t27_status"] == "T27_READY"
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
        common.relative(topology.OUTPUT): common.sha256_file(topology.OUTPUT),
    }
    document: dict[str, Any] = {
        "closure": closure,
        "closure_policy": policy.get("closure_policy"),
        "currentness": {
            "owned_inputs": owned_inputs,
        },
        "fidelity": fidelity,
        "generated_by": "python tools/build_t28_readiness.py --write",
        "load": load_data,
        "owned_inputs": owned_inputs,
        "policy": (
            "The status field is derived from evidence by build(). "
            "T28_READY cannot be hand-written."
        ),
        "runtime": {
            "gametest_passing": runtime["gametest_passing"],
            "gametest_total": runtime["gametest_total"],
            "live_cooling_expansion": runtime["live_cooling_expansion"],
            "report_status": runtime["report_status"],
            "t27_status": runtime["t27_status"],
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status_owner": "run_full_verification",
    }
    if gates_ok:
        document["status"] = "T28_READY"
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
