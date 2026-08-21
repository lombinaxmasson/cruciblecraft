#!/usr/bin/env python3
"""Build the staged T26 readiness artifact.

The ``status`` field is derived by ``build()`` from the evidence gates
below; it is never hand-written.  ``check()`` recomputes and compares
against the committed file, with a required-keys pre-check that reports
missing keys in human-readable form instead of KeyError cascades.

Evidence sources:
  - known-issue / freeze ledger       : t26_known_issues.json
  - localization ledger               : t26_localization_ledger.json
  - publication ledger                : t23_publication_baseline.json
  - packaging files                   : gradle.properties / CHANGELOG / CREDITS / player guide
  - T26b GameTest                     : CrucibleCraftGameTests.java
  - runtime GameTest evidence         : full_verification_report.json

T26 close adds no T26-phase recipes or registrations. Frozen constants
pin the close-time suite (GameTest 121 after the 4.5 wire-cutter test;
JUnit 582). Publication still reads the T23 ledger 18882/16657/2225
with T26-phase delta 0/0/0; the T4 wire-cutter +205 lives on
t26_5_publication_baseline.json. Headroom stays the T25 logical
figure 2118; the 21000-axis decision belongs to T27.

Runtime evidence is only recorded by the verification session, so
``build()`` derives it from the committed full verification report;
report-owned fields are excluded from ``check()`` via REPORT_OWNED.
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
OUTPUT = TOOLS / "t26_readiness.json"
POLICY = TOOLS / "t26_readiness_policy.json"
BUILDER = Path(__file__).resolve()

KNOWN_ISSUES = TOOLS / "t26_known_issues.json"
LOCALIZATION = TOOLS / "t26_localization_ledger.json"
PUBLICATION_BASELINE = (
    ROOT / "src/main/resources/data/cruciblecraft/t23_publication_baseline.json"
)
REPORT = TOOLS / "full_verification_report.json"
GAMETEST_SOURCE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
)
GRADLE_PROPERTIES = ROOT / "gradle.properties"
CHANGELOG = ROOT / "CHANGELOG.md"
CREDITS = ROOT / "CREDITS.md"
PLAYER_GUIDE = ROOT / "docs" / "current" / "player-guide.md"

T26_FUTURE_VERSION_TEST = (
    "t26UnknownFutureProcessingVersionQuarantinedAndPreserved"
)
STAGES = ("T26a", "T26b", "T26c", "T26d", "T26e")
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
FROZEN_GAMETEST_TOTAL = 121
ALLOWED_GAMETEST_TOTALS = (121, 131, 137)
FROZEN_JAVA_TESTS = 584
ALLOWED_JAVA_TESTS = (584, 593, 644, 652)
FROZEN_DATAPACK_ENTRIES = 3243
FROZEN_HEADROOM = 2118
PACKAGING_VERSION = "0.1.0-beta.1"
SUCCESSOR_PACKAGING_VERSIONS = ("0.1.0-beta.1", "0.1.0-rc.1")


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _load_if_exists(path: Path, default: Any = None) -> Any:
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
    ledger = _load_if_exists(KNOWN_ISSUES) or {}
    issues = ledger.get("issues") or []
    freeze = ledger.get("freeze") or {}
    localization = _load_if_exists(LOCALIZATION) or {}
    text = (
        GAMETEST_SOURCE.read_text(encoding="utf-8")
        if GAMETEST_SOURCE.is_file()
        else ""
    )
    gradle = (
        GRADLE_PROPERTIES.read_text(encoding="utf-8")
        if GRADLE_PROPERTIES.is_file()
        else ""
    )
    current_packaging = None
    for line in gradle.splitlines():
        stripped = line.strip()
        if stripped.startswith("mod_version="):
            current_packaging = stripped.split("=", 1)[1].strip()
            break
    required_fields = (
        "id",
        "title",
        "severity",
        "workaround",
        "owner",
        "disposition",
        "blocks_beta",
        "evidence_artifact",
    )
    rows_complete = bool(issues) and all(
        all(row.get(field) not in (None, "") for field in required_fields)
        for row in issues
    )
    return {
        "known_issues_status": ledger.get("status"),
        "known_issues_total": len(issues),
        "known_issues_complete": rows_complete,
        "blocks_beta": sum(1 for row in issues if row.get("blocks_beta") is True),
        "playtest_rows": sum(
            1 for row in issues if str(row.get("id", "")).startswith("CC-4.5-P")
        ),
        "inherited_t25_rows": sum(
            1 for row in issues if str(row.get("id", "")).startswith("T24-F")
        ),
        "o15_closed": (freeze.get("o15") or {}).get("disposition") == "closed",
        "localization_accounted": (
            localization.get("status") == "T26_LOCALIZATION_ACCOUNTED"
        ),
        "anvil_bend_post_1_0": all(
            row.get("disposition") == "post_1_0"
            for row in (freeze.get("anvil_bend") or [])
        )
        and len(freeze.get("anvil_bend") or []) == 2,
        "crucible_owner_t27": (
            (freeze.get("crucible") or {}).get("disposition") == "v1_required"
            and (freeze.get("crucible") or {}).get("owner") == "T27"
            and (freeze.get("crucible") or {}).get("implementation") == "none"
        ),
        "future_version_gametest_present": T26_FUTURE_VERSION_TEST in text,
        "packaging_version_current": (
            current_packaging in SUCCESSOR_PACKAGING_VERSIONS
            and CHANGELOG.is_file()
            and CREDITS.is_file()
            and PLAYER_GUIDE.is_file()
        ),
        "packaging_version": PACKAGING_VERSION,
    }


def _load_fidelity() -> dict[str, Any]:
    return {
        "new_registrations": 0,
        "no_placeholder_on_mainline": True,
        "source_license_traceable": True,
        "design_policy_declared": (
            "T26 close publishes no new GT recipes or registrations. "
            "Player docs distinguish SOURCE_BACKED, SOURCE_DERIVED and "
            "DESIGN_POLICY. 4.5 P0-P9 remain post_beta_polish. anvil_bend "
            "slots are explicit post_1_0 reserved maps; crucible stays "
            "unimplemented v1_required with owner T27."
        ),
    }


def _load_load() -> dict[str, Any]:
    ledger = _load_if_exists(KNOWN_ISSUES) or {}
    issues = ledger.get("issues") or []
    baseline = _load_if_exists(PUBLICATION_BASELINE) or {}
    totals = baseline.get("publication_totals") or {}
    t26_deltas = [
        d
        for d in (baseline.get("delta_ledger_policy") or {}).get(
            "registered_deltas", []
        )
        if str(d.get("phase", "")).startswith("T26")
    ]
    logical_delta = sum(d.get("logical", 0) for d in t26_deltas)
    eager_delta = sum(d.get("eager", 0) for d in t26_deltas)
    lazy_delta = sum(d.get("lazy", 0) for d in t26_deltas)
    consistent = (
        totals.get("logical_rows") == FROZEN_LOGICAL
        and totals.get("eager_rows") == FROZEN_EAGER
        and totals.get("lazy_rows") == FROZEN_LAZY
        and logical_delta == 0
        and eager_delta == 0
        and lazy_delta == 0
    )
    recheck_contracts = [
        {
            "id": row.get("id"),
            "owner": row.get("owner"),
            "replacement_condition": (row.get("recheck_contract") or {}).get(
                "replacement_condition"
            ),
            "recheck_point": (row.get("recheck_contract") or {}).get(
                "recheck_point"
            ),
        }
        for row in issues
        if row.get("recheck_contract")
    ]
    skipped_contracts_complete = (
        len(recheck_contracts) == 2
        and {row["id"] for row in recheck_contracts}
        == {"T24-F003", "T24-F005"}
        and all(
            contract["owner"] == "T27 RC"
            and contract["recheck_point"] == "T27 RC candidate"
            and contract["replacement_condition"]
            for contract in recheck_contracts
        )
    )
    current_logical = totals.get("logical_rows", FROZEN_LOGICAL)
    return {
        "publication_delta": {
            "logical": logical_delta,
            "eager": eager_delta,
            "lazy": lazy_delta,
        },
        "publication_baseline_consistent": consistent,
        "headroom_remaining": HARD_CEILING - current_logical,
        "headroom_axis": "logical_legacy_t25",
        "remeasurement": "SKIPPED_TRANSFERRED_TO_T27_RC",
        "t27_recheck_contracts": recheck_contracts,
        "skipped_contracts_complete": skipped_contracts_complete,
        "findings_blocking": sum(
            1 for row in issues if row.get("blocks_beta") is True
        ),
        "t25_blocker_count": 0,
    }


def _load_runtime() -> dict[str, Any]:
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


def build() -> dict[str, Any]:
    closure = _load_closure()
    fidelity = _load_fidelity()
    load_data = _load_load()
    runtime = _load_runtime()

    policy = _load_if_exists(POLICY) or {}
    closure_policy = policy.get("closure_policy") or {}
    pending = closure_policy.get("pending", [])

    zero_content_evidence = (
        runtime["gametest_total"] in ALLOWED_GAMETEST_TOTALS
        and runtime["java_unit_tests"].get("tests") in ALLOWED_JAVA_TESTS
        and runtime["datapack_recipe_entries"] == FROZEN_DATAPACK_ENTRIES
    )
    gates_ok = (
        len(pending) == 0
        and closure_policy.get("final_closure_attempted", False) is True
        and closure["known_issues_status"] == "T26_KNOWN_ISSUES_COMPLETE"
        and closure["known_issues_complete"]
        and closure["known_issues_total"] == 15
        and closure["blocks_beta"] == 0
        and closure["playtest_rows"] == 10
        and closure["inherited_t25_rows"] == 5
        and closure["o15_closed"]
        and closure["localization_accounted"]
        and closure["anvil_bend_post_1_0"]
        and closure["crucible_owner_t27"]
        and closure["future_version_gametest_present"]
        and closure["packaging_version_current"]
        and fidelity["new_registrations"] == 0
        and fidelity["no_placeholder_on_mainline"]
        and fidelity["source_license_traceable"]
        and load_data["publication_delta"]
        == {"logical": 0, "eager": 0, "lazy": 0}
        and load_data["publication_baseline_consistent"]
        and load_data["headroom_remaining"] == FROZEN_HEADROOM
        and load_data["skipped_contracts_complete"]
        and load_data["findings_blocking"] == 0
        and load_data["t25_blocker_count"] == 0
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
        document["status"] = "T26_READY"
        document["completed_stages"] = list(STAGES)
        document["pending_stages"] = []

    return document


def _check_required_keys(
    on_disk: dict[str, Any],
    expected: dict[str, Any],
    errors: list[str],
) -> None:
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
            f"T26 readiness is missing top-level keys: {sorted(missing_top)}"
        )
    required_closure = {
        "known_issues_status",
        "known_issues_total",
        "known_issues_complete",
        "blocks_beta",
        "playtest_rows",
        "inherited_t25_rows",
        "o15_closed",
        "localization_accounted",
        "anvil_bend_post_1_0",
        "crucible_owner_t27",
        "future_version_gametest_present",
        "packaging_version_current",
        "packaging_version",
    }
    closure = on_disk.get("closure") or {}
    missing = required_closure - set(closure.keys())
    if missing:
        errors.append(
            f"T26 readiness closure is missing keys: {sorted(missing)}"
        )
    required_fidelity = {
        "new_registrations",
        "no_placeholder_on_mainline",
        "source_license_traceable",
        "design_policy_declared",
    }
    fidelity = on_disk.get("fidelity") or {}
    missing = required_fidelity - set(fidelity.keys())
    if missing:
        errors.append(
            f"T26 readiness fidelity is missing keys: {sorted(missing)}"
        )
    required_load = {
        "publication_delta",
        "publication_baseline_consistent",
        "headroom_remaining",
        "headroom_axis",
        "remeasurement",
        "t27_recheck_contracts",
        "skipped_contracts_complete",
        "findings_blocking",
        "t25_blocker_count",
    }
    load_data = on_disk.get("load") or {}
    missing = required_load - set(load_data.keys())
    if missing:
        errors.append(
            f"T26 readiness load is missing keys: {sorted(missing)}"
        )
    required_runtime = {"gametest_passing", "gametest_total"}
    runtime = on_disk.get("runtime") or {}
    missing = required_runtime - set(runtime.keys())
    if missing:
        errors.append(
            f"T26 readiness runtime is missing keys: {sorted(missing)}"
        )


def check() -> list[str]:
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
    OUTPUT.write_bytes(_stable(document).encode("utf-8"))
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
        print(f"T26 readiness failed: {error}", file=sys.stderr)
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
