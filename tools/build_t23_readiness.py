#!/usr/bin/env python3
"""Build the staged T23 readiness artifact.

The ``status`` field is derived by ``build()`` from the evidence gates
below; it is never hand-written.  ``check()`` recomputes and compares
against the committed file, with a required-keys pre-check that reports
missing keys in human-readable form instead of KeyError cascades.

Evidence sources:
  - classification/selection/fidelity : t23_multiblock_behavior_classification.json
  - plugin whitelist                    : t23_plugin_whitelist.json
  - load bounds                         : t23_load_bounds.json
  - publication ledger                  : src/main/resources/data/cruciblecraft/t23_publication_baseline.json
  - lifecycle/supply GameTests          : scanned from CrucibleCraftGameTests.java
  - runtime GameTest evidence           : full_verification_report.json

Runtime evidence is only recorded by the verification session, so
``build()`` derives it from the committed full verification report;
report-owned fields (runtime / status / completed stages / currentness)
are excluded from ``check()`` staleness comparisons via REPORT_OWNED
and their correctness is enforced strictly by the T23 gate in
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
OUTPUT = TOOLS / "t23_readiness.json"
POLICY = TOOLS / "t23_readiness_policy.json"
BUILDER = Path(__file__).resolve()

CLASSIFICATION = TOOLS / "t23_multiblock_behavior_classification.json"
PLUGINS = TOOLS / "t23_plugin_whitelist.json"
LOAD_BOUNDS = TOOLS / "t23_load_bounds.json"
PUBLICATION_BASELINE = (
    ROOT / "src/main/resources/data/cruciblecraft/t23_publication_baseline.json"
)
REPORT = TOOLS / "full_verification_report.json"
GAMETEST_SOURCE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java"
)

LIFECYCLE_SUFFIXES = (
    "Formation",
    "Teardown",
    "OutputJam",
    "PowerLoss",
    "Reload",
    "SaveQuarantine",
)
SUPPLY_SUFFIX = "PortSupplySingleHost"
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


def _camel(structure_id: str) -> str:
    """distillation_tower -> DistillationTower (for GameTest method names)."""
    return "".join(part.capitalize() for part in structure_id.split("_"))


def _load_classification() -> dict[str, Any]:
    document = _load_if_exists(CLASSIFICATION) or {}
    counts = document.get("counts") or {}
    selected = document.get("selected") or []
    return {
        "behavior_unclassified": counts.get("unclassified", -1),
        "canonical_total": counts.get("canonical", -1),
        "selected_structure_count": len(selected),
        "selected_ids": [row.get("canonical_id") for row in selected],
        "acquisition_unreachable": sum(
            1
            for row in selected
            if (row.get("acquisition") or {}).get("reachable") is False
        ),
        "consumer_operand_proof": (
            len(selected) > 0
            and all(
                (row.get("acquisition") or {}).get("reachable") is True
                and bool(row.get("consumers"))
                for row in selected
            )
        ),
        "geometry_equivalence": (
            len(selected) > 0
            and all(
                (row.get("fidelity_evidence") or {}).get(
                    "geometry_bidirectional"
                )
                is True
                for row in selected
            )
        ),
    }


def _load_plugins() -> dict[str, Any]:
    document = _load_if_exists(PLUGINS) or {}
    plugins = document.get("plugins") or []
    if not plugins:
        return {
            "plugin_count": 0,
            "every_plugin_consumed": False,
            "consumers_within_selected": False,
            "quarantine_declared": False,
        }
    classification = _load_if_exists(CLASSIFICATION) or {}
    allowed_structures = {
        row.get("structure_id")
        for row in (classification.get("selected") or [])
    } | {"large_centrifuge", "coke_oven", "large_crucible"}
    consumed = []
    for row in plugins:
        consumers = row.get("consumers") or []
        consumed.append(bool(consumers))
        if not allowed_structures.issuperset(set(consumers)):
            return {
                "plugin_count": len(plugins),
                "every_plugin_consumed": False,
                "consumers_within_selected": False,
                "quarantine_declared": bool(
                    document.get("quarantine_policy")
                ),
            }
    return {
        "plugin_count": len(plugins),
        "every_plugin_consumed": all(consumed),
        "consumers_within_selected": True,
        "quarantine_declared": bool(document.get("quarantine_policy")),
    }


def _load_load() -> dict[str, Any]:
    bounds = _load_if_exists(LOAD_BOUNDS) or {}
    structures = bounds.get("structures") or {}
    classification = _load_if_exists(CLASSIFICATION) or {}
    selected_ids = {
        row.get("canonical_id")
        for row in (classification.get("selected") or [])
    }
    covered = all(
        key in structures
        and (structures[key].get("validation_ops_max") or 0) > 0
        for key in selected_ids
    ) if selected_ids else False

    baseline = _load_if_exists(PUBLICATION_BASELINE) or {}
    ledger = baseline.get("delta_ledger_policy") or {}
    totals = baseline.get("publication_totals") or {}
    delta = baseline.get("t23_publication_delta") or {}
    registered = [
        d for d in ledger.get("registered_deltas", [])
        if str(d.get("phase", "")).startswith("T23")
    ]
    logical_delta = sum(d.get("logical", 0) for d in registered)
    eager_delta = sum(d.get("eager", 0) for d in registered)
    lazy_delta = sum(d.get("lazy", 0) for d in registered)
    current_logical = totals.get("logical_rows", FROZEN_LOGICAL)
    consistent = (
        totals.get("logical_rows") == FROZEN_LOGICAL + logical_delta
        and totals.get("eager_rows") == FROZEN_EAGER + eager_delta
        and totals.get("lazy_rows") == FROZEN_LAZY + lazy_delta
        and delta.get("logical_rows_added") == logical_delta
        and delta.get("eager_rows_added") == eager_delta
        and delta.get("lazy_rows_added") == lazy_delta
    )
    return {
        "projection_status": bounds.get("status"),
        "worst_case_bounds": (
            bounds.get("worst_case") is True
            and bounds.get("status") == "MEASURED"
            and covered
        ),
        "publication_baseline_consistent": consistent,
        "headroom_remaining": HARD_CEILING - current_logical,
        "publication_delta": {
            "logical": logical_delta,
            "eager": eager_delta,
            "lazy": lazy_delta,
        },
        "evidence": {
            "load_bounds": "tools/t23_load_bounds.json",
            "publication_baseline": (
                "src/main/resources/data/cruciblecraft/"
                "t23_publication_baseline.json"
            ),
        },
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


def _scan_gametest_source() -> dict[str, Any]:
    """Count the lifecycle / supply GameTests per selected structure in
    the committed GameTest source (mirrors the report's own
    @GameTest-count convention)."""
    text = (
        GAMETEST_SOURCE.read_text(encoding="utf-8")
        if GAMETEST_SOURCE.is_file()
        else ""
    )
    classification = _load_if_exists(CLASSIFICATION) or {}
    selected = classification.get("selected") or []
    found = 0
    expected = 0
    supply_found = 0
    for row in selected:
        structure_id = row.get("structure_id") or ""
        prefix = "t23" + _camel(structure_id)
        for suffix in LIFECYCLE_SUFFIXES:
            expected += 1
            if f"{prefix}{suffix}" in text:
                found += 1
        if f"{prefix}{SUPPLY_SUFFIX}" in text:
            supply_found += 1
    return {
        "lifecycle_paths_measured": found,
        "lifecycle_paths_expected": expected,
        "port_supply_assertions": supply_found,
    }


# ---------------------------------------------------------------------------
# build / check / write
# ---------------------------------------------------------------------------


def build() -> dict[str, Any]:
    classification = _load_classification()
    plugins = _load_plugins()
    load_data = _load_load()
    runtime = _load_runtime()
    gametest_scan = _scan_gametest_source()

    policy = _load_if_exists(POLICY) or {}
    closure_policy = policy.get("closure_policy") or {}
    pending = closure_policy.get("pending", [])

    lifecycle_complete = (
        gametest_scan["lifecycle_paths_expected"] > 0
        and gametest_scan["lifecycle_paths_measured"]
        == gametest_scan["lifecycle_paths_expected"]
    )
    port_supply_not_per_block = (
        len(classification["selected_ids"]) > 0
        and gametest_scan["port_supply_assertions"]
        == len(classification["selected_ids"])
    )

    gates_ok = (
        len(pending) == 0
        and closure_policy.get("final_closure_attempted", False) is True
        and classification["behavior_unclassified"] == 0
        and classification["canonical_total"] == 30
        and classification["selected_structure_count"] in (2, 3)
        and classification["acquisition_unreachable"] == 0
        and classification["consumer_operand_proof"]
        and classification["geometry_equivalence"]
        and plugins["every_plugin_consumed"]
        and plugins["consumers_within_selected"]
        and plugins["quarantine_declared"]
        and load_data["projection_status"] == "MEASURED"
        and load_data["worst_case_bounds"]
        and load_data["publication_baseline_consistent"]
        and load_data["headroom_remaining"] >= 0
        and lifecycle_complete
        and port_supply_not_per_block
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
        "closure": {
            "behavior_unclassified": classification[
                "behavior_unclassified"
            ],
            "canonical_total": classification["canonical_total"],
            "selected_structure_count": classification[
                "selected_structure_count"
            ],
            "selected_ids": classification["selected_ids"],
            "acquisition_unreachable": classification[
                "acquisition_unreachable"
            ],
            "consumer_operand_proof": classification[
                "consumer_operand_proof"
            ],
            "lifecycle_paths_measured": gametest_scan[
                "lifecycle_paths_measured"
            ],
            "lifecycle_paths_expected": gametest_scan[
                "lifecycle_paths_expected"
            ],
        },
        "fidelity": {
            "geometry_equivalence": classification[
                "geometry_equivalence"
            ],
            "plugin_count": plugins["plugin_count"],
            "every_plugin_consumed": plugins["every_plugin_consumed"],
            "consumers_within_selected": plugins[
                "consumers_within_selected"
            ],
            "quarantine_declared": plugins["quarantine_declared"],
            "port_supply_not_per_block": port_supply_not_per_block,
        },
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
        document["status"] = "T23_READY"
        document["completed_stages"] = [
            "T23a",
            "T23b",
            "T23c",
            "T23d",
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
            f"T23 readiness is missing top-level keys: {sorted(missing_top)}"
        )
    required_closure = {
        "behavior_unclassified",
        "canonical_total",
        "selected_structure_count",
        "selected_ids",
        "acquisition_unreachable",
        "consumer_operand_proof",
        "lifecycle_paths_measured",
        "lifecycle_paths_expected",
    }
    closure = on_disk.get("closure") or {}
    missing = required_closure - set(closure.keys())
    if missing:
        errors.append(
            f"T23 readiness closure is missing keys: {sorted(missing)}"
        )
    required_fidelity = {
        "geometry_equivalence",
        "plugin_count",
        "every_plugin_consumed",
        "consumers_within_selected",
        "quarantine_declared",
        "port_supply_not_per_block",
    }
    fidelity = on_disk.get("fidelity") or {}
    missing = required_fidelity - set(fidelity.keys())
    if missing:
        errors.append(
            f"T23 readiness fidelity is missing keys: {sorted(missing)}"
        )
    required_load = {
        "projection_status",
        "worst_case_bounds",
        "publication_baseline_consistent",
        "headroom_remaining",
        "publication_delta",
    }
    load_data = on_disk.get("load") or {}
    missing = required_load - set(load_data.keys())
    if missing:
        errors.append(
            f"T23 readiness load is missing keys: {sorted(missing)}"
        )
    required_runtime = {"gametest_passing", "gametest_total"}
    runtime = on_disk.get("runtime") or {}
    missing = required_runtime - set(runtime.keys())
    if missing:
        errors.append(
            f"T23 readiness runtime is missing keys: {sorted(missing)}"
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
        print(f"T23 readiness failed: {error}", file=sys.stderr)
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
