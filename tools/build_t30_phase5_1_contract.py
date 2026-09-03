#!/usr/bin/env python3
"""Build the Phase 5.1 pre-RC logistics contract.

This is an append-only work-set revision. It does not rewrite T27–T29
historical artifacts, start RC numbering, or close F003/F005.
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
from tools import t35_common as t35  # noqa: E402

TOOLS = common.TOOLS
POLICY = TOOLS / "t30_hopper_source_policy.json"
T29_READINESS = TOOLS / "t29_readiness.json"
REPORT = TOOLS / "full_verification_report.json"
HISTORICAL_PORTFOLIO = TOOLS / "phase5_portfolio_contract.json"
OUTPUT = TOOLS / "phase5_1_pre_rc_logistics_contract.json"
BUILDER = Path(__file__).resolve()


def _require(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _policy() -> dict[str, Any]:
    document = common.load_json(POLICY)
    if document.get("status") != "T30_HOPPER_SOURCE_POLICY":
        raise ValueError("T30 hopper source policy header drifted")
    return document


def _verify_opening(policy: dict[str, Any]) -> dict[str, Any]:
    report = common.load_json(REPORT)
    acceptance = report.get("t29_readiness_acceptance") or {}
    expected = policy["opening"]
    if acceptance.get("status") != expected["t29_status"]:
        raise ValueError(
            "Phase 5.1 opening requires "
            f"{expected['t29_status']}, found {acceptance.get('status')}"
        )
    if report.get("status") != expected["report_status"]:
        raise ValueError(
            f"full verification status is {report.get('status')}, "
            f"not {expected['report_status']}"
        )
    if report.get("rc_number") not in (None, expected["rc_number"]):
        raise ValueError("rc_number must remain null until T30_READY")
    runtime = acceptance.get("runtime") or {}
    if runtime.get("rc_number") not in (None, expected["rc_number"]):
        raise ValueError("T29 acceptance rc_number must remain null")
    historical = common.load_json(HISTORICAL_PORTFOLIO)
    if historical.get("status") != "PHASE5_PORTFOLIO_CONTRACT":
        raise ValueError("historical Phase 5 portfolio contract drifted")
    t29 = common.load_json(T29_READINESS)
    if t29.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("T29 readiness source revision drifted")
    return {
        "historical_portfolio_status": historical["status"],
        "rc_number": None,
        "report_status": report["status"],
        "t29_status": acceptance["status"],
    }


def build() -> dict[str, Any]:
    for path in (POLICY, T29_READINESS, REPORT, HISTORICAL_PORTFOLIO):
        _require(path)
    policy = _policy()
    opening = _verify_opening(policy)
    execution = policy["execution"]
    catalog = policy["catalog"]
    expected = policy["expected"]
    row_count = int(expected["row_count"])
    return {
        "catalog": {
            "bare_ids_forbidden": list(catalog["bare_ids_forbidden"]),
            "dust_funnel_id": catalog["dust_funnel_id"],
            "id_pattern": catalog["id_pattern"],
            "kinds": list(catalog["kinds"]),
            "source_rows": row_count,
            "total_new_blocks": row_count * len(catalog["kinds"]) + 1,
        },
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(POLICY): common.sha256_file(POLICY),
                common.relative(T29_READINESS): common.sha256_file(T29_READINESS),
                common.relative(HISTORICAL_PORTFOLIO): common.sha256_file(
                    HISTORICAL_PORTFOLIO
                ),
            }
        },
        "dust_forms": list(policy["dust_forms"]),
        "dust_units": dict(policy["dust_units"]),
        "exit_conditions": {
            "closure": (
                "60 source rows, 120 hopper-family identities, one steel_dust_funnel, "
                "registration/resources/acquisition equal, player paths and archive tests green"
            ),
            "fidelity": (
                "slots and hopper/queue behavior SOURCE_BACKED; recipes SOURCE_DERIVED; "
                "dust three-form bounded adaptation; plateCurved/blockDust/div72 not claimed"
            ),
            "load": (
                "each independent axis has base/delta/measured; pending is not filled with 0; "
                "T14 hard ceilings are not exceeded"
            ),
        },
        "generated_by": "python tools/build_t30_phase5_1_contract.py --write",
        "historical_artifacts": {
            "do_not_rewrite": list(execution["historical_artifacts_do_not_rewrite"]),
            "note": (
                "T27–T29 Hopper=post_1_0 is a historical snapshot. Phase 5.1 appends "
                "a new pre-RC work set; it does not rewrite those artifacts as if T30 "
                "had already been planned."
            ),
        },
        "legacy_machine_policy": dict(policy["legacy_machine_policy"]),
        "opening": opening,
        "owners": dict(policy["owners"]),
        "post_1_0_exclusions": list(policy["post_1_0_exclusions"]),
        "rc_policy": {
            "f003_f005_remain_rc_recheck": True,
            "mod_version_remains": execution["mod_version_remains"],
            "rc_number": None,
            "rc_numbering": execution["rc_numbering"],
        },
        "recipe_adaptation": dict(policy["recipe_adaptation"]),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "PHASE5_1_PRE_RC_LOGISTICS_CONTRACT",
        "work_set": {
            "current_active_t": execution["current_active_t"],
            "maximum_active_t_cards": execution["maximum_active_t_cards"],
            "next_t": execution["next_t"],
            "t31_depends_on": execution["t31_depends_on"],
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return t35.check_compact(OUTPUT, build(), encode=common.stable_json)


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
                f"active={document['work_set']['current_active_t']} "
                f"rc={document['rc_policy']['rc_numbering']}"
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
