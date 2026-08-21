#!/usr/bin/env python3
"""Derive the T27 opening snapshot from the bound T26 READY report.

Current publication comes from the T26.5 ledger referenced by the T26
READY report inputs. The T23 18882/16657 ledger is labeled historical
and is never used as the T27 opening current value.
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
OUTPUT = TOOLS / "t27_opening_snapshot.json"
POLICY = TOOLS / "t27_opening_snapshot_policy.json"
BUILDER = Path(__file__).resolve()
CONTRACT = TOOLS / "phase5_portfolio_contract.json"


def _measurement_slice(row: dict[str, Any] | None) -> dict[str, Any] | None:
    if not isinstance(row, dict):
        return None
    keys = (
        "sync_bytes",
        "server_reload_p95_ns",
        "server_index_p95_ns",
        "dedicated_client_reexpansion_p95_ns",
        "dedicated_client_index_p95_ns",
        "retained_total_bytes_p50",
        "retained_status",
        "jfr_allocation_bytes_p50",
        "jfr_allocation_status",
        "lookup_p95_ns",
        "lookup_candidates_p95",
        "lookup_candidates_max",
    )
    sliced = {key: row[key] for key in keys if key in row}
    return sliced or None


def _t13_tables() -> dict[str, Any]:
    tables: dict[str, Any] = {}
    total = 0
    for name, spec in common.TABLE_SPECS.items():
        path = ROOT / spec["artifact"]
        document = common.load_json(path)
        ids = common.t13_canonical_ids(name, document)
        counts: dict[str, int] = {}
        field = spec["classification_field"]
        for row in common.t13_rows(name, document):
            label = str(row.get(field) or "unclassified")
            counts[label] = counts.get(label, 0) + 1
        tables[name] = {
            "artifact": spec["artifact"],
            "artifact_sha256": common.sha256_file(path),
            "canonical_count": len(ids),
            "classification_counts": counts,
            "id_field": spec["id_field"],
            "unclassified": counts.get("unclassified", 0),
        }
        total += len(ids)
    tables["canonical_identities_total"] = total
    return tables


FROZEN_T26_REPORT_KEYS = (
    "datapack",
    "measurements",
    "t22_5",
    "t26_gate",
    "tests",
)


def build() -> dict[str, Any]:
    policy = common.load_json(POLICY)
    report_path = ROOT / policy["report"]
    report = common.load_json(report_path)
    t26 = report.get("t26_readiness_acceptance") or {}
    if report.get("status") != "READY" or t26.get("status") != "T26_READY":
        raise ValueError("T27 opening requires a bound T26_READY report")
    frozen_sha = policy["t26_report_sha256"]
    if not isinstance(frozen_sha, str) or len(frozen_sha) != 64:
        raise ValueError("opening policy t26_report_sha256 is not a SHA-256")
    live_sha = common.sha256_file(report_path)
    manifest = common.load_json(TOOLS / "t13_denominator_manifest.json")
    if (manifest.get("source") or {}).get("revision") != common.SOURCE_REVISION:
        raise ValueError("T13 denominator revision is not the fixed T27 revision")

    inputs = {}
    for relative in policy["inputs"]:
        path = ROOT / relative
        if not path.is_file():
            raise ValueError(f"missing opening input: {relative}")
        if relative == policy["report"]:
            inputs[relative] = frozen_sha
            continue
        inputs[relative] = common.sha256_file(path)

    current_ledger = common.load_json(ROOT / policy["t26_5_publication_baseline"])
    historical_ledger = common.load_json(ROOT / policy["t23_publication_baseline"])
    current_delta = current_ledger.get("delta_ledger_policy") or {}
    historical_delta = historical_ledger.get("delta_ledger_policy") or {}
    t26_5_delta = current_ledger.get("t26_5_publication_delta") or {}
    rules = report.get("rules") or {}
    tests = report.get("tests") or {}
    t14 = report.get("t14_load_acceptance") or {}
    load_gate = t14.get("load_gate") or {}
    measurements = t14.get("measurements") or {}
    t22_5 = report.get("t22_5_readiness_acceptance") or {}
    known = common.load_json(TOOLS / "t26_known_issues.json")
    budgets = common.load_json(TOOLS / "t14_load_budget_policy.json").get("budgets") or {}
    contract = common.load_json(CONTRACT)

    selected_1x = _measurement_slice(measurements.get("selected_1x"))
    selected_20x = _measurement_slice(measurements.get("selected_20x"))

    document = {
        "schema_version": 1,
        "status": "T27_OPENING_SNAPSHOT",
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t27_opening_snapshot.py",
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(POLICY): common.sha256_file(POLICY),
                common.relative(CONTRACT): common.sha256_file(CONTRACT),
            }
        },
        "t26_gate": {
            "report": policy["report"],
            "report_sha256": frozen_sha,
            "report_status": report.get("status"),
            "t26_status": t26.get("status"),
            "session": (
                (tests.get("production_game_tests") or {}).get("evidence_log") or ""
            ).split("/")[-2]
            if (tests.get("production_game_tests") or {}).get("evidence_log")
            else None,
            "ready_binding": report.get("ready_binding") or {},
            "tooling_snapshot_sha256": (
                (report.get("ready_binding") or {}).get("tooling_snapshot_sha256")
            ),
        },
        "input_artifacts": inputs,
        "publication": {
            "authority": policy["t26_5_publication_baseline"],
            "current": {
                "eager": current_delta.get("current_eager"),
                "lazy": current_delta.get("current_lazy"),
                "logical": current_delta.get("current_logical"),
            },
            "t26_5_delta": {
                "eager": t26_5_delta.get("eager_rows_added"),
                "lazy": t26_5_delta.get("lazy_rows_added"),
                "logical": t26_5_delta.get("logical_rows_added"),
            },
            "report_rules_all_published_total": {
                "authority": "tools/full_verification_report.json#rules.all_published_total",
                "current_baseline": False,
                "value": rules.get("all_published_total"),
            },
        },
        "datapack": {
            "report_counter": {
                "authority": "tools/full_verification_report.json#rules.datapack_recipe_entries",
                "name": "concrete_datapack_recipe_entries",
                "value": rules.get("datapack_recipe_entries"),
            },
            "t14_authored_files": {
                "authority": "tools/full_verification_report.json#t14_load_acceptance.load_gate.datapack_authored_entries",
                "name": "datapack_authored_entries",
                "value": load_gate.get("datapack_authored_entries"),
            },
        },
        "tests": {
            "java_unit_tests": (tests.get("java_unit_tests") or {}).get("tests"),
            "production_game_tests": (
                tests.get("production_game_tests") or {}
            ).get("required_tests"),
            "python_unit_tests": (tests.get("python_unit_tests") or {}).get("tests"),
            "java_result": (tests.get("java_unit_tests") or {}).get("result"),
            "gametest_result": (tests.get("production_game_tests") or {}).get("result"),
            "python_result": (tests.get("python_unit_tests") or {}).get("result"),
        },
        "measurements": {
            "authority": "tools/full_verification_report.json#t14_load_acceptance.measurements",
            "snapshot": "T14 selected hybrid evidence retained in the T26 READY report",
            "selected_1x": selected_1x,
            "selected_20x": selected_20x,
        },
        "t13_tables": _t13_tables(),
        "t14_budgets": {
            "authority": "tools/t14_load_budget_policy.json",
            "eager_hard_ceiling": (budgets.get("eager_publication_rows") or {}).get(
                "hard_ceiling"
            ),
            "eager_soft_budget": (budgets.get("eager_publication_rows") or {}).get(
                "soft_budget"
            ),
            "axes": {
                name: {
                    "hard_ceiling": axis.get("hard_ceiling"),
                    "soft_budget": axis.get("soft_budget"),
                    "unit": axis.get("unit"),
                }
                for name, axis in budgets.items()
            },
        },
        "t22_5": {
            "ordinary_optional": (
                ((t22_5.get("evidence") or {}).get("b1_row_classification") or {}).get(
                    "by_class"
                )
                or {}
            ).get("ordinary_optional"),
            "row_classification_total": (
                ((t22_5.get("evidence") or {}).get("b1_row_classification") or {}).get(
                    "total"
                )
            ),
        },
        "t26_known_issues": {
            "blocks_beta": (known.get("counts") or {}).get("blocks_beta"),
            "total": (known.get("counts") or {}).get("total"),
            "o15": (known.get("freeze") or {}).get("o15", {}).get("disposition"),
            "crucible_owner": (known.get("freeze") or {}).get("crucible", {}).get(
                "owner"
            ),
            "anvil_bend": [
                {
                    "id": row.get("id"),
                    "disposition": row.get("disposition"),
                    "owner": row.get("owner"),
                }
                for row in (known.get("freeze") or {}).get("anvil_bend") or []
            ],
        },
        "publication_delta": {
            "eager": 0,
            "lazy": 0,
            "logical": 0,
            "scope": "T27_CARD",
        },
        "historical_baselines": {
            "t23_ledger": {
                "authority": policy["t23_publication_baseline"],
                "current_baseline": False,
                "eager": historical_delta.get("current_eager"),
                "lazy": historical_delta.get("current_lazy"),
                "logical": historical_delta.get("current_logical"),
            },
            "phase5_plan_2026_08_15": {
                "current_baseline": False,
                "note": "Fifth-phase planning prose opening is not T27 current; use publication.current.",
            },
        },
        "contract_publication_delta": contract.get("publication_delta"),
        "t28_plus_card_count": (contract.get("execution_policy") or {}).get(
            "t28_plus_card_count"
        ),
    }
    if live_sha != frozen_sha:
        if not OUTPUT.is_file():
            raise ValueError(
                "opening snapshot missing; cannot keep the frozen T26 report slice"
            )
        committed = common.load_json(OUTPUT)
        for key in FROZEN_T26_REPORT_KEYS:
            document[key] = committed[key]
        publication = dict(document["publication"])
        publication["report_rules_all_published_total"] = committed["publication"][
            "report_rules_all_published_total"
        ]
        document["publication"] = publication
        document["t26_gate"] = dict(document["t26_gate"])
        document["t26_gate"]["report_sha256"] = frozen_sha
    if document["t13_tables"]["canonical_identities_total"] != 765:
        raise ValueError("T13 canonical identity total is not 765")
    if document["publication"]["current"]["logical"] == document[
        "historical_baselines"
    ]["t23_ledger"]["logical"]:
        raise ValueError("opening current publication copied the historical T23 ledger")
    if document["t14_budgets"]["eager_hard_ceiling"] != 21000:
        raise ValueError("T14 eager hard ceiling is not 21000")
    if document["publication_delta"] != {
        "eager": 0,
        "lazy": 0,
        "logical": 0,
        "scope": "T27_CARD",
    }:
        raise ValueError("T27 publication delta must remain 0/0/0")
    return document


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        errors.append("t27_opening_snapshot.json is stale")
    return errors


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
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
            document = common.load_json(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T27 opening snapshot failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "status": document.get("status"),
        "report_sha256": (document.get("t26_gate") or {}).get("report_sha256"),
        "logical": ((document.get("publication") or {}).get("current") or {}).get(
            "logical"
        ),
        "eager": ((document.get("publication") or {}).get("current") or {}).get(
            "eager"
        ),
        "canonical_identities": (document.get("t13_tables") or {}).get(
            "canonical_identities_total"
        ),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
