#!/usr/bin/env python3
"""Build the staged T24 readiness artifact.

The ``status`` field is derived by ``build()`` from the evidence gates
below; it is never hand-written.  ``check()`` recomputes and compares
against the committed file, with a required-keys pre-check that reports
missing keys in human-readable form instead of KeyError cascades.

Evidence sources:
  - workload policy + manifest          : t24_workload_policy.json / t24_workload_manifest.json
  - bounded-count evidence              : t24_scale_bounds.json
  - finding ledger                      : t24_findings.json
  - publication ledger                  : src/main/resources/data/cruciblecraft/t23_publication_baseline.json
  - small-scenario GameTests            : scanned from CrucibleCraftGameTests.java
  - runtime GameTest evidence           : full_verification_report.json

Runtime evidence is only recorded by the verification session, so
``build()`` derives it from the committed full verification report;
report-owned fields (runtime / status / completed stages / currentness)
are excluded from ``check()`` staleness comparisons via REPORT_OWNED
and their correctness is enforced strictly by the T24 gate in
verify_full_verification_report.py.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t24_readiness.json"
POLICY = TOOLS / "t24_readiness_policy.json"
BUILDER = Path(__file__).resolve()

WORKLOAD_POLICY = TOOLS / "t24_workload_policy.json"
WORKLOAD_MANIFEST = TOOLS / "t24_workload_manifest.json"
SCALE_EVIDENCE = TOOLS / "t24_scale_evidence.json"
SCALE_BOUNDS = TOOLS / "t24_scale_bounds.json"
FINDINGS = TOOLS / "t24_findings.json"
PUBLICATION_BASELINE = (
    ROOT / "src/main/resources/data/cruciblecraft/t23_publication_baseline.json"
)
REPORT = TOOLS / "full_verification_report.json"
GAMETEST_SOURCE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
)

REQUIRED_SCENARIOS = ("small", "target", "stress")
SCENARIO_TESTS = (
    "t24SmallWorkloadBuildsToDeclaredIdentity",
    "t24SmallWorkloadRunsToDeterministicSummary",
    "t24SmallWorkloadConservesItemsFluidsEnergy",
    "t24SmallWorkloadRespectsDeclaredOperationCaps",
    "t24WorkloadMutationFailsStructureGate",
)
MUTATION_TEST = "t24WorkloadMutationFailsStructureGate"
# run_full_verification 拥有的字段：由报告推导，不参与产物过期比对
REPORT_OWNED = (
    "currentness",
    "runtime",
    "status",
    "completed_stages",
    "pending_stages",
)
HARD_CEILING = 21_000
FROZEN_LOGICAL = 18_882
FROZEN_EAGER = 16_657
FROZEN_LAZY = 2_225


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _load_if_exists(path: Path, default: Any = None) -> Any:
    """Load a JSON file if it exists; return *default* otherwise.
    Evidence files may not exist at skeleton-creation time; check()
    must never throw KeyError on a missing evidence file."""
    if path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def _load_closure() -> dict[str, Any]:
    manifest = _load_if_exists(WORKLOAD_MANIFEST) or {}
    scenarios = manifest.get("scenarios") or {}
    identity = manifest.get("workload_identity")
    partition_exact = all(
        (row.get("derived") or {}).get("pipes_partition_exact") is True
        for row in scenarios.values()
    )
    rebuildable = (
        manifest.get("status") == "MANIFEST_COMPLETE"
        and set(scenarios) == set(REQUIRED_SCENARIOS)
        and partition_exact
        and bool(identity)
    )
    text = (
        GAMETEST_SOURCE.read_text(encoding="utf-8")
        if GAMETEST_SOURCE.is_file()
        else ""
    )
    executed = sum(1 for name in SCENARIO_TESTS if name in text)
    return {
        "scenarios_defined": len(scenarios),
        "scenarios_rebuildable_from_empty": rebuildable,
        "workload_identity": identity,
        "gametest_scenarios_executed": executed,
        "gametest_scenarios_expected": len(SCENARIO_TESTS),
        "mutation_gate_present": MUTATION_TEST in text,
    }


def _load_fidelity() -> dict[str, Any]:
    policy = _load_if_exists(WORKLOAD_POLICY) or {}
    evidence = _load_if_exists(SCALE_EVIDENCE) or {}
    measured_at_scale = evidence.get("measured_at_scale") or {}
    synthetic_marked_as_measured = any(
        row.get("evidence_class") == "SYNTHETIC_BENCHMARK"
        for row in measured_at_scale.values()
    )
    skipped_metrics = [
        name
        for name, row in measured_at_scale.items()
        if row.get("status") == "SKIP"
    ]
    return {
        "distribution_source": "T20-T23 runtime sets",
        "synthetic_marked_as_measured": synthetic_marked_as_measured,
        "skipped_metrics": skipped_metrics,
        "design_policy_declared":
            policy.get("fidelity_class") == "DESIGN_POLICY",
    }


def _load_load() -> dict[str, Any]:
    bounds = _load_if_exists(SCALE_BOUNDS) or {}
    findings = _load_if_exists(FINDINGS) or {}
    baseline = _load_if_exists(PUBLICATION_BASELINE) or {}
    ledger = baseline.get("delta_ledger_policy") or {}
    totals = baseline.get("publication_totals") or {}
    registered = [
        d for d in ledger.get("registered_deltas", [])
        if str(d.get("phase", "")).startswith("T24")
    ]
    logical_delta = sum(d.get("logical", 0) for d in registered)
    eager_delta = sum(d.get("eager", 0) for d in registered)
    lazy_delta = sum(d.get("lazy", 0) for d in registered)
    current_logical = totals.get("logical_rows", FROZEN_LOGICAL)
    consistent = (
        totals.get("logical_rows") == FROZEN_LOGICAL + logical_delta
        and totals.get("eager_rows") == FROZEN_EAGER + eager_delta
        and totals.get("lazy_rows") == FROZEN_LAZY + lazy_delta
    )
    return {
        "publication_delta": {
            "logical": logical_delta,
            "eager": eager_delta,
            "lazy": lazy_delta,
        },
        "publication_baseline_consistent": consistent,
        "headroom_remaining": HARD_CEILING - current_logical,
        "bounded_counts_status": bounds.get("status"),
        "findings_blocking": (findings.get("counts") or {}).get(
            "blocking", -1
        ),
        "findings_status": findings.get("status"),
    }


def _load_runtime() -> dict[str, Any]:
    """Runtime evidence from the committed full verification report.

    ``gametest_passing`` remains null until the verification session
    records the GameTest run; it is never hand-written into this file.
    """
    report = _load_if_exists(REPORT) or {}
    game_tests = (report.get("tests") or {}).get(
        "production_game_tests"
    ) or {}
    passing = None
    if game_tests.get("result") == "PASS":
        passing = True
    elif game_tests.get("result"):
        passing = False
    return {
        "gametest_passing": passing,
        "gametest_total": game_tests.get("required_tests"),
    }


# ---------------------------------------------------------------------------
# build / check / write
# ---------------------------------------------------------------------------


def build() -> dict[str, Any]:
    closure = _load_closure()
    fidelity = _load_fidelity()
    load_data = _load_load()
    runtime = _load_runtime()

    policy = _load_if_exists(POLICY) or {}
    closure_policy = policy.get("closure_policy") or {}
    pending = closure_policy.get("pending", [])

    scenarios_complete = (
        closure["scenarios_defined"] == 3
        and closure["scenarios_rebuildable_from_empty"]
        and bool(closure["workload_identity"])
    )
    gametest_complete = (
        closure["gametest_scenarios_executed"]
        == closure["gametest_scenarios_expected"]
        and closure["mutation_gate_present"]
    )

    gates_ok = (
        len(pending) == 0
        and closure_policy.get("final_closure_attempted", False) is True
        and scenarios_complete
        and gametest_complete
        and fidelity["design_policy_declared"]
        and not fidelity["synthetic_marked_as_measured"]
        and load_data["publication_delta"] == {
            "logical": 0, "eager": 0, "lazy": 0
        }
        and load_data["publication_baseline_consistent"]
        and load_data["bounded_counts_status"] == "MEASURED"
        and load_data["findings_blocking"] == 0
        and load_data["findings_status"] == "FINDINGS_LEDGER_READY"
        and load_data["headroom_remaining"] >= 0
        and runtime["gametest_passing"] is True
        and (runtime["gametest_total"] or 0) > 0
    )

    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "run_full_verification",
        "policy": (
            "The status field is derived from evidence by build(). "
            "Any mismatch between the committed value and a fresh build "
            "is detected by check()."
        ),
        "completed_stages": [],
        "pending_stages": [],
        "closure": closure,
        "fidelity": fidelity,
        "load": load_data,
        "runtime": runtime,
        "closure_policy": closure_policy,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(POLICY): _sha256(POLICY),
            },
            "full_verification": {
                "evidence": "tools/full_verification_report.json",
                "status": "BOUND_TO_FULL_VERIFICATION_REPORT",
                "pending": [],
            },
        },
    }

    if gates_ok:
        document["status"] = "T24_READY"
        document["completed_stages"] = [
            "T24a",
            "T24b",
            "T24c",
        ]
        document["pending_stages"] = []

    return document


def _check_required_keys(
    on_disk: dict[str, Any],
    expected: dict[str, Any],
    errors: list[str],
) -> None:
    """Verify all required top-level and nested keys exist before
    field comparison — a missing key yields one human-readable error,
    never a KeyError cascade."""
    required_top = {
        "schema_version",
        "status_owner",
        "closure",
        "fidelity",
        "load",
        "runtime",
        "closure_policy",
        "currentness",
    }
    missing_top = required_top - set(on_disk.keys())
    if missing_top:
        errors.append(
            f"T24 readiness is missing top-level keys: {sorted(missing_top)}"
        )
    required_closure = {
        "scenarios_defined",
        "scenarios_rebuildable_from_empty",
        "workload_identity",
        "gametest_scenarios_executed",
        "gametest_scenarios_expected",
        "mutation_gate_present",
    }
    closure = on_disk.get("closure") or {}
    missing = required_closure - set(closure.keys())
    if missing:
        errors.append(
            f"T24 readiness closure is missing keys: {sorted(missing)}"
        )
    required_fidelity = {
        "distribution_source",
        "synthetic_marked_as_measured",
        "skipped_metrics",
        "design_policy_declared",
    }
    fidelity = on_disk.get("fidelity") or {}
    missing = required_fidelity - set(fidelity.keys())
    if missing:
        errors.append(
            f"T24 readiness fidelity is missing keys: {sorted(missing)}"
        )
    required_load = {
        "publication_delta",
        "publication_baseline_consistent",
        "headroom_remaining",
        "bounded_counts_status",
        "findings_blocking",
        "findings_status",
    }
    load_data = on_disk.get("load") or {}
    missing = required_load - set(load_data.keys())
    if missing:
        errors.append(
            f"T24 readiness load is missing keys: {sorted(missing)}"
        )
    required_runtime = {"gametest_passing", "gametest_total"}
    runtime = on_disk.get("runtime") or {}
    missing = required_runtime - set(runtime.keys())
    if missing:
        errors.append(
            f"T24 readiness runtime is missing keys: {sorted(missing)}"
        )


def check() -> list[str]:
    """Return staleness / integrity errors (empty = clean)."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors

    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status_owner") != "run_full_verification":
        errors.append("status_owner must be run_full_verification")

    expected = build()
    _check_required_keys(on_disk, expected, errors)
    if errors:
        return errors

    disk_stripped = {
        k: v for k, v in on_disk.items() if k not in REPORT_OWNED
    }
    expected_stripped = {
        k: v for k, v in expected.items() if k not in REPORT_OWNED
    }
    if _stable(disk_stripped) != _stable(expected_stripped):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")

    return errors


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T24 readiness failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "schema_version": document.get("schema_version"),
        "status_owner": document.get("status_owner"),
    }
    if "status" in document:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
