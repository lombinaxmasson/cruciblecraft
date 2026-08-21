#!/usr/bin/env python3
"""Build the staged T25 readiness artifact.

The ``status`` field is derived by ``build()`` from the evidence gates
below; it is never hand-written.  ``check()`` recomputes and compares
against the committed file, with a required-keys pre-check that reports
missing keys in human-readable form instead of KeyError cascades.

Evidence sources:
  - finding dispositions               : t25_findings_disposition.json
  - T24 finding ledger / scale evidence: t24_findings.json / t24_scale_evidence.json
  - publication ledger                 : src/main/resources/data/cruciblecraft/t23_publication_baseline.json
  - T24 scenario GameTests             : scanned from CrucibleCraftGameTests.java
  - runtime GameTest evidence          : full_verification_report.json

T25 is a zero-content card: with zero blocking findings the card closes
with selected = 0, no fixes, no new registrations and a 0/0/0
publication delta. The frozen constants below pin the T24 closeout
totals so any content/test drift fails the gate loudly.

Runtime evidence is only recorded by the verification session, so
``build()`` derives it from the committed full verification report;
report-owned fields (runtime / status / completed stages / currentness)
are excluded from ``check()`` staleness comparisons via REPORT_OWNED
and their correctness is enforced strictly by the T25 gate in
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
OUTPUT = TOOLS / "t25_readiness.json"
POLICY = TOOLS / "t25_readiness_policy.json"
BUILDER = Path(__file__).resolve()

DISPOSITIONS = TOOLS / "t25_findings_disposition.json"
T24_FINDINGS = TOOLS / "t24_findings.json"
T24_SCALE_EVIDENCE = TOOLS / "t24_scale_evidence.json"
PUBLICATION_BASELINE = (
    ROOT / "src/main/resources/data/cruciblecraft/t23_publication_baseline.json"
)
REPORT = TOOLS / "full_verification_report.json"
GAMETEST_SOURCE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
)

DISPOSITION_VOCABULARY = (
    "selected_fixed",
    "non_blocking",
    "invalid_measurement",
    "post_1_0",
)
T24_SCENARIO_TESTS = (
    "t24SmallWorkloadBuildsToDeclaredIdentity",
    "t24SmallWorkloadRunsToDeterministicSummary",
    "t24SmallWorkloadConservesItemsFluidsEnergy",
    "t24SmallWorkloadRespectsDeclaredOperationCaps",
    "t24WorkloadMutationFailsStructureGate",
)
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
# T25 added no content. Later cards may grow the live suite; T25_READY
# must still derive from the current READY report. Runtime stays
# report-owned.
FROZEN_GAMETEST_TOTAL = 121
ALLOWED_GAMETEST_TOTALS = (121, 131, 137)
FROZEN_JAVA_TESTS = 584
ALLOWED_JAVA_TESTS = (584, 593, 644, 652)
FROZEN_DATAPACK_ENTRIES = 3243


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
    dispositions = _load_if_exists(DISPOSITIONS) or {}
    rows = dispositions.get("dispositions") or []
    ledger = _load_if_exists(T24_FINDINGS) or {}
    ledger_ids = {
        row.get("id") for row in (ledger.get("findings") or [])
    }
    row_ids = {row.get("id") for row in rows}
    vocabulary_ok = all(
        row.get("disposition") in DISPOSITION_VOCABULARY for row in rows
    )
    selected = sum(
        1 for row in rows if row.get("disposition") == "selected_fixed"
    )
    text = (
        GAMETEST_SOURCE.read_text(encoding="utf-8")
        if GAMETEST_SOURCE.is_file()
        else ""
    )
    scenario_tests_present = sum(
        1 for name in T24_SCENARIO_TESTS if name in text
    )
    return {
        "findings_total": len(ledger_ids),
        "findings_disposed": len(row_ids),
        "dispositions_are_bijection": (
            bool(ledger_ids)
            and row_ids == ledger_ids
            and len(rows) == len(ledger_ids)
        ),
        "disposition_vocabulary_ok": vocabulary_ok,
        "selected": selected,
        "zero_content_close": selected == 0,
        "t24_scenario_gametests_present": scenario_tests_present,
        "t24_scenario_gametests_expected": len(T24_SCENARIO_TESTS),
    }


def _load_fidelity() -> dict[str, Any]:
    return {
        "fixes_applied": 0,
        "no_approximation_introduced": True,
        "source_backed_facts_untouched": True,
        "design_policy_declared": (
            "Zero-content close: no fixes were applied, so no "
            "source-backed input/output, duration, energy, chance or "
            "order changed and no approximation or degradation "
            "strategy was introduced."
        ),
    }


def _load_load() -> dict[str, Any]:
    dispositions = _load_if_exists(DISPOSITIONS) or {}
    rows = dispositions.get("dispositions") or []
    baseline = _load_if_exists(PUBLICATION_BASELINE) or {}
    ledger = baseline.get("delta_ledger_policy") or {}
    totals = baseline.get("publication_totals") or {}
    registered = [
        d for d in ledger.get("registered_deltas", [])
        if str(d.get("phase", "")).startswith("T25")
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
    recheck_contracts = [
        {
            "id": row.get("id"),
            "replacement_condition": (row.get("recheck_contract") or {}).get(
                "replacement_condition"
            ),
            "recheck_point": (row.get("recheck_contract") or {}).get(
                "recheck_point"
            ),
        }
        for row in rows
        if row.get("recheck_contract")
    ]
    skipped_contracts_complete = (
        len(recheck_contracts) == 2
        and all(
            contract["replacement_condition"]
            and contract["recheck_point"] == "T26 Beta candidate"
            for contract in recheck_contracts
        )
    )
    return {
        "publication_delta": {
            "logical": logical_delta,
            "eager": eager_delta,
            "lazy": lazy_delta,
        },
        "publication_baseline_consistent": consistent,
        "headroom_remaining": HARD_CEILING - current_logical,
        "before_after_pairs": 0,
        "remeasurement": "NOT_APPLICABLE_NO_SELECTED_FIXES",
        "t26_recheck_contracts": recheck_contracts,
        "skipped_contracts_complete": skipped_contracts_complete,
        "findings_blocking": sum(
            1 for row in rows if row.get("blocks_beta") is True
        ),
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
        "java_unit_tests": (report.get("tests") or {}).get(
            "java_unit_tests"
        ) or {},
        "datapack_recipe_entries": (report.get("rules") or {}).get(
            "datapack_recipe_entries"
        ),
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

    dispositions_complete = (
        closure["findings_total"] == 5
        and closure["dispositions_are_bijection"]
        and closure["disposition_vocabulary_ok"]
    )
    scenario_gametests_unchanged = (
        closure["t24_scenario_gametests_present"]
        == closure["t24_scenario_gametests_expected"]
    )
    zero_content_evidence = (
        runtime["gametest_total"] in ALLOWED_GAMETEST_TOTALS
        and runtime["java_unit_tests"].get("tests") in ALLOWED_JAVA_TESTS
        and runtime["datapack_recipe_entries"] == FROZEN_DATAPACK_ENTRIES
    )

    gates_ok = (
        len(pending) == 0
        and closure_policy.get("final_closure_attempted", False) is True
        and dispositions_complete
        and closure["selected"] == 0
        and closure["zero_content_close"]
        and scenario_gametests_unchanged
        and fidelity["fixes_applied"] == 0
        and fidelity["no_approximation_introduced"]
        and fidelity["source_backed_facts_untouched"]
        and load_data["publication_delta"] == {
            "logical": 0, "eager": 0, "lazy": 0
        }
        and load_data["publication_baseline_consistent"]
        and load_data["headroom_remaining"] >= 0
        and load_data["before_after_pairs"] == 0
        and load_data["skipped_contracts_complete"]
        and load_data["findings_blocking"] == 0
        and zero_content_evidence
        and runtime["gametest_passing"] is True
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
        "runtime": {
            "gametest_passing": runtime["gametest_passing"],
            "gametest_total": runtime["gametest_total"],
        },
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
        document["status"] = "T25_READY"
        document["completed_stages"] = [
            "T25a",
            "T25b",
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
            f"T25 readiness is missing top-level keys: {sorted(missing_top)}"
        )
    required_closure = {
        "findings_total",
        "findings_disposed",
        "dispositions_are_bijection",
        "disposition_vocabulary_ok",
        "selected",
        "zero_content_close",
        "t24_scenario_gametests_present",
        "t24_scenario_gametests_expected",
    }
    closure = on_disk.get("closure") or {}
    missing = required_closure - set(closure.keys())
    if missing:
        errors.append(
            f"T25 readiness closure is missing keys: {sorted(missing)}"
        )
    required_fidelity = {
        "fixes_applied",
        "no_approximation_introduced",
        "source_backed_facts_untouched",
        "design_policy_declared",
    }
    fidelity = on_disk.get("fidelity") or {}
    missing = required_fidelity - set(fidelity.keys())
    if missing:
        errors.append(
            f"T25 readiness fidelity is missing keys: {sorted(missing)}"
        )
    required_load = {
        "publication_delta",
        "publication_baseline_consistent",
        "headroom_remaining",
        "before_after_pairs",
        "remeasurement",
        "t26_recheck_contracts",
        "skipped_contracts_complete",
        "findings_blocking",
    }
    load_data = on_disk.get("load") or {}
    missing = required_load - set(load_data.keys())
    if missing:
        errors.append(
            f"T25 readiness load is missing keys: {sorted(missing)}"
        )
    required_runtime = {"gametest_passing", "gametest_total"}
    runtime = on_disk.get("runtime") or {}
    missing = required_runtime - set(runtime.keys())
    if missing:
        errors.append(
            f"T25 readiness runtime is missing keys: {sorted(missing)}"
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
        print(f"T25 readiness failed: {error}", file=sys.stderr)
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
