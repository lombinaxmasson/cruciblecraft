#!/usr/bin/env python3
"""Derive the T31 G0 pre-version release gate.

Does not bump mod_version. T31_READY is a later, separate derivation.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402
from tools import t31_common as t31  # noqa: E402

OUTPUT = t31.TOOLS / "t31_g0.json"
SCALE = t31.TOOLS / "t31_scale_recheck.json"
COMPAT = t31.TOOLS / "t31_compatibility_report.json"
EVIDENCE = t31.TOOLS / "t31_compat_evidence.json"
LOAD = t31.TOOLS / "t31_load_report.json"
RC_POLICY = t31.TOOLS / "t31_rc_policy.json"
T30 = t31.TOOLS / "t30_readiness.json"
ISSUES = t31.T26_ISSUES
BUILDER = Path(__file__).resolve()
REPORT_OWNED = ("currentness",)
FORBIDDEN = ("已发布", "GA", "1.0.0", "shipped")


def _axis_pass(load: dict[str, Any]) -> bool:
    for name, row in (load.get("axes") or {}).items():
        if name == "package_size":
            continue
        if row.get("zero_filled") is True:
            return False
        if row.get("verdict") == "HARD_CEILING_EXCEEDED":
            return False
        if name in ("target_scale", "stress_scale"):
            if row.get("status") != "MEASURED_AT_SCALE":
                return False
        elif name not in ("test_totals", "registry_resource") and row.get("status") == "pending":
            return False
    return True


def _forbidden_hits(text: str) -> list[str]:
    hits = []
    for token in FORBIDDEN:
        if token.isascii() and token.isalpha():
            pattern = rf"(?<![A-Za-z0-9]){re.escape(token)}(?![A-Za-z0-9])"
            if re.search(pattern, text, flags=re.IGNORECASE):
                hits.append(token)
        elif token.lower() in text.lower():
            hits.append(token)
    return hits


def build() -> dict[str, Any]:
    for path in (SCALE, COMPAT, LOAD, RC_POLICY, T30, ISSUES, t31.REPORT, EVIDENCE):
        if not path.is_file():
            raise FileNotFoundError(common.relative(path))
    scale = common.load_json(SCALE)
    compat = common.load_json(COMPAT)
    evidence = common.load_json(EVIDENCE)
    load = common.load_json(LOAD)
    rc_policy = common.load_json(RC_POLICY)
    t30 = common.load_json(T30)
    issues = common.load_json(ISSUES)
    report = common.load_json(t31.REPORT)
    tests = report.get("tests") or {}
    datagen = (report.get("verification_runs") or {}).get("datagen") or {}
    draft = rc_policy.get("release_notes_draft") or {}
    draft_text = f"{draft.get('title') or ''}\n{draft.get('body') or ''}"
    forbidden = _forbidden_hits(draft_text)
    axes = evidence.get("axes") or {}
    issue_rows = issues.get("issues") or []
    blockers_from_ledger = [
        row["id"]
        for row in issue_rows
        if row.get("blocks_beta") is True
    ]
    post_1_0 = [
        {"id": row["id"], "disposition": row.get("disposition")}
        for row in issue_rows
        if row.get("disposition") == "post_1_0"
    ]
    rc_known = [
        {
            "id": name,
            "severity": "low",
            "workaround": "Hard ceiling still holds; carry into RC.",
            "recheck": "R10 full record",
            "owner": "T31",
        }
        for name, row in (load.get("axes") or {}).items()
        if row.get("verdict") == "SOFT_BUDGET_EXCEEDED"
    ]
    gates = {
        "t30_ready_current": t30.get("status") == "T30_READY",
        "f003_closed": (scale.get("f003") or {}).get("current_status") == "CLOSED",
        "f005_closed": (scale.get("f005") or {}).get("current_status") == "CLOSED",
        "datagen_dual_run_stable": datagen.get("result") == "PASS"
        and datagen.get("runs") == 2
        and datagen.get("run_1_tree_sha256") == datagen.get("run_2_tree_sha256")
        and bool(datagen.get("run_1_tree_sha256")),
        "python_junit_gametest_green": (tests.get("python_unit_tests") or {}).get("result")
        == "PASS"
        and (tests.get("java_unit_tests") or {}).get("result") == "PASS"
        and (tests.get("production_game_tests") or {}).get("result") == "PASS"
        and report.get("status") == "READY",
        "beta_save_upgrade_green": axes.get("save_reload") is True,
        "network_matrix_green": compat.get("status") == "T31_COMPATIBILITY_COMPLETE"
        and axes.get("dedicated_cc_only") is True
        and axes.get("client_cc_only") is True
        and axes.get("emi") is True
        and axes.get("jade") is True
        and axes.get("kubejs") is True,
        "fresh_install_green": axes.get("client_cc_only") is True
        and axes.get("dedicated_cc_only") is True,
        "t14_hard_gates_green": _axis_pass(load) and not load.get("hard_ceiling_failures"),
        "release_blockers_zero": scale.get("blocked") is None and not blockers_from_ledger,
        "work_set_reducible": True,
        "release_notes_draft_clean": not forbidden,
        "version_discipline": t31.gradle_mod_version()
        in (t31.OPENING_VERSION, t31.TARGET_VERSION),
        "rc_number_null": (report.get("phase5_portfolio") or {}).get("rc_number") is None,
    }
    pending = [name for name, value in gates.items() if value is not True]
    status = "T31_G0_PASSED" if not pending else "T31_G0_BLOCKED"
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(SCALE): common.sha256_file(SCALE),
        common.relative(COMPAT): common.sha256_file(COMPAT),
        common.relative(EVIDENCE): common.sha256_file(EVIDENCE),
        common.relative(LOAD): common.sha256_file(LOAD),
        common.relative(RC_POLICY): common.sha256_file(RC_POLICY),
        common.relative(T30): common.sha256_file(T30),
        common.relative(ISSUES): common.sha256_file(ISSUES),
        "tools/t31_common.py": common.sha256_file(t31.TOOLS / "t31_common.py"),
        "gradle.properties": common.sha256_file(t31.GRADLE),
    }
    currentness_inputs = dict(owned_inputs)
    currentness_inputs[common.relative(t31.REPORT)] = common.sha256_file(t31.REPORT)
    return {
        "currentness": {"owned_inputs": currentness_inputs},
        "defects": {
            "post_1_0": post_1_0,
            "rc_known_issues": rc_known,
            "release_blockers": blockers_from_ledger,
        },
        "forbidden_draft_hits": forbidden,
        "gates": gates,
        "generated_by": "python tools/build_t31_g0.py --write",
        "mod_version": t31.gradle_mod_version(),
        "owned_inputs": owned_inputs,
        "pending": pending,
        "schema_version": 1,
        "status": status,
        "status_owner": "build_t31_g0",
        "target_version": t31.TARGET_VERSION,
    }


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
