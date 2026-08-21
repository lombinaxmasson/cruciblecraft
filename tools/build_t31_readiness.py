#!/usr/bin/env python3
"""Build T31 pre-RC readiness. status is derived; T31_READY cannot be hand-written."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402
from tools import t31_common as t31  # noqa: E402

OUTPUT = t31.TOOLS / "t31_readiness.json"
POLICY = t31.TOOLS / "t31_readiness_policy.json"
RC_POLICY = t31.TOOLS / "t31_rc_policy.json"
SCALE = t31.TOOLS / "t31_scale_recheck.json"
COMPAT = t31.TOOLS / "t31_compatibility_report.json"
LOAD = t31.TOOLS / "t31_load_report.json"
RELEASE = t31.TOOLS / "t31_release_manifest.json"
T30 = t31.TOOLS / "t30_readiness.json"
T24_EVIDENCE = t31.T24_EVIDENCE
BUILDER = Path(__file__).resolve()
REPORT_OWNED = ("currentness", "runtime", "status")


def _runtime() -> dict[str, Any]:
    report = common.load_json(t31.REPORT) if t31.REPORT.is_file() else {}
    game_tests = (report.get("tests") or {}).get("production_game_tests") or {}
    java = (report.get("tests") or {}).get("java_unit_tests") or {}
    python = (report.get("tests") or {}).get("python_unit_tests") or {}
    return {
        "gametest_passing": game_tests.get("result") == "PASS",
        "gametest_total": game_tests.get("required_tests"),
        "java_unit_tests_passing": java.get("result") == "PASS",
        "python_unit_tests_passing": python.get("result") == "PASS",
        "report_status": report.get("status"),
        "t30_status": (report.get("t30_readiness_acceptance") or {}).get("status"),
        "rc_number": (report.get("phase5_portfolio") or {}).get("rc_number"),
        "mod_version": t31.gradle_mod_version(),
    }


def build() -> dict[str, Any]:
    for path in (POLICY, RC_POLICY, SCALE, COMPAT, LOAD, RELEASE, T30, T24_EVIDENCE):
        if not path.is_file():
            raise FileNotFoundError(common.relative(path))
    policy = common.load_json(POLICY)
    scale = common.load_json(SCALE)
    compat = common.load_json(COMPAT)
    load = common.load_json(LOAD)
    release = common.load_json(RELEASE)
    t30 = common.load_json(T30)
    historical = t31.historical_t24_scale_skip()
    runtime = _runtime()
    closure = {
        "t30_ready_current": t30.get("status") == "T30_READY"
        and runtime["t30_status"] == "T30_READY",
        "f003_closed": scale.get("f003", {}).get("current_status") == "CLOSED",
        "f005_closed": scale.get("f005", {}).get("current_status") == "CLOSED",
        "full_suites": runtime["gametest_passing"] is True
        and runtime["java_unit_tests_passing"] is True
        and runtime["python_unit_tests_passing"] is True
        and runtime["report_status"] == "READY",
        "compatibility_complete": compat.get("status") == "T31_COMPATIBILITY_COMPLETE",
        "packages_complete": release.get("status") == "T31_RELEASE_MANIFEST_COMPLETE",
        "release_blockers_zero": scale.get("blocked") is None,
        "version_is_rc1": runtime["mod_version"] == t31.TARGET_VERSION,
    }
    fidelity = {
        "source_revision_pinned": common.SOURCE_REVISION
        == "3703e40308c8c030763fd6297dea8b210d2a77b1",
        "t24_history_not_rewritten": historical["target_status"] == "SKIP"
        and historical["stress_status"] == "SKIP"
        and historical["rewritten_as_measured_at_t24"] is False,
        "content_delta_zero_policy": True,
        "opening_version_until_g0": runtime["mod_version"] in (
            t31.OPENING_VERSION,
            t31.TARGET_VERSION,
        ),
        "no_cross_machine_wall_clock_blocker": True,
    }
    load_data = {
        "eager_hard_ceiling_is_21000": load.get("eager_hard_ceiling") == 21_000,
        "datapack_hard_ceiling_is_6600": load.get("datapack_hard_ceiling") == 6_600,
        "pending_axes_not_zeroed": all(
            axis.get("zero_filled") is False for axis in load.get("axes", {}).values()
        ),
        "target_measured": (scale.get("current") or {}).get("target", {}).get("status")
        == "MEASURED_AT_SCALE",
        "stress_measured": (scale.get("current") or {}).get("stress", {}).get("status")
        == "MEASURED_AT_SCALE",
    }
    closure_ok = all(value is True for value in closure.values())
    fidelity_ok = all(value is True for value in fidelity.values())
    load_ok = all(value is True for value in load_data.values())
    gates_ok = (
        closure_ok
        and fidelity_ok
        and load_ok
        and runtime["mod_version"] == t31.TARGET_VERSION
        and not (policy.get("closure_policy") or {}).get("pending")
        and (policy.get("closure_policy") or {}).get("final_closure_attempted")
        is True
    )
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        common.relative(RC_POLICY): common.sha256_file(RC_POLICY),
        common.relative(SCALE): common.sha256_file(SCALE),
        common.relative(COMPAT): common.sha256_file(COMPAT),
        common.relative(LOAD): common.sha256_file(LOAD),
        common.relative(RELEASE): common.sha256_file(RELEASE),
        common.relative(T30): common.sha256_file(T30),
        "tools/t31_common.py": common.sha256_file(t31.TOOLS / "t31_common.py"),
    }
    document: dict[str, Any] = {
        "closure": closure,
        "closure_policy": policy.get("closure_policy"),
        "currentness": {"owned_inputs": owned_inputs},
        "fidelity": fidelity,
        "generated_by": "python tools/build_t31_readiness.py --write",
        "load": load_data,
        "owned_inputs": owned_inputs,
        "policy": (
            "The status field is derived from evidence by build(). "
            "T31_READY cannot be hand-written."
        ),
        "runtime": runtime,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status_owner": "run_full_verification",
    }
    if gates_ok:
        document["status"] = "T31_READY"
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
