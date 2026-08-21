#!/usr/bin/env python3
"""Build the staged T22.5 readiness artifact.

The ``status`` field is derived by ``build()`` from the evidence gates
below; it is never hand-written.  ``check()`` recomputes and compares
against the committed file, with a required-keys pre-check that reports
missing keys in human-readable form instead of KeyError cascades.

All evidence is tools-only and committed before the verification
session, so a single ``run_full_verification.py --record --new-session``
derives ``T22_5_READY`` — no relaxed gate on the first record.
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
OUTPUT = TOOLS / "t22_5_readiness.json"
POLICY = TOOLS / "t22_5_readiness_policy.json"
BUILDER = Path(__file__).resolve()

A1 = TOOLS / "t22_5_shape_analysis.json"
A2 = TOOLS / "t22_5_fluid_mapping.json"
A3 = TOOLS / "t22_5_item_classification.json"
B1 = TOOLS / "t22_5_row_classification.json"
B2 = TOOLS / "t22_5_fluid_gap_disposition.json"
C0 = TOOLS / "t22_5_machine_playability.json"
C1 = TOOLS / "t22_5_denominator_recompute.json"
LEDGER_1 = TOOLS / "t21_source_denominator.json"
PROJECT_PLAN = ROOT / "CrucibleCraft-总体规划.md"
MATERIALS_DIR = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)

BETA_WORDING = (
    "T21 的普通化学与 T22 的石油化工中，`v1_required` 全部发布；"
    "`ordinary_optional` 属于 1.0 后 portfolio，逐类登记 owner 与 "
    "replacement condition。"
)


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


def material_tree_digest() -> str:
    rows = [
        (path.relative_to(ROOT).as_posix(), _sha256(path))
        for path in sorted(MATERIALS_DIR.glob("*.json"))
    ]
    return hashlib.sha256(
        json.dumps(rows, sort_keys=True).encode("utf-8")
    ).hexdigest()


# ---------------------------------------------------------------------------
# evidence loaders
# ---------------------------------------------------------------------------


def _load_ledger_superseded() -> dict[str, Any]:
    ledger = _load_if_exists(LEDGER_1) or {}
    superseded = ledger.get("superseded") or {}
    return {
        "marked": bool(
            superseded.get("disposition") == "ordinary_v1_required"
            and superseded.get("superseded_by")
            == "tools/t21_template_denominator.json"
        ),
        "superseded_by": superseded.get("superseded_by"),
    }


def _load_a1() -> dict[str, Any]:
    document = _load_if_exists(A1) or {}
    per_map = document.get("per_map") or []
    return {
        "maps": len(per_map),
        "unassigned": sum(
            (row.get("membership") or {}).get("unassigned", 1)
            for row in per_map
        ),
        "duplicate": sum(
            (row.get("membership") or {}).get("duplicate", 1)
            for row in per_map
        ),
        "publication_delta": sum(
            row.get("publication_delta", 1) for row in per_map
        ),
        "bath_rows_per_unit": next(
            (
                row.get("rows_per_unit", 0)
                for row in per_map
                if row.get("map") == "gt.recipe.bath"
            ),
            0,
        ),
    }


def _load_a2() -> dict[str, Any]:
    document = _load_if_exists(A2) or {}
    return {
        "unclassified": document.get("counts", {}).get("unclassified", -1),
        "total": document.get("counts", {}).get("total", -1),
        "mapped": document.get("counts", {}).get("mapped", -1),
        "no_cc_fluid": document.get("counts", {}).get("no_cc_fluid", -1),
        "out_of_scope": document.get("counts", {}).get(
            "out_of_scope", -1
        ),
        "publication_delta": document.get("publication_delta", -1),
    }


def _load_a3() -> dict[str, Any]:
    document = _load_if_exists(A3) or {}
    return {
        "unclassified": document.get("counts", {}).get("unclassified", -1),
        "total_occurrences": document.get("counts", {}).get(
            "total_occurrences", -1
        ),
    }


def _load_b1() -> dict[str, Any]:
    document = _load_if_exists(B1) or {}
    return {
        "unclassified": document.get("counts", {}).get("unclassified", -1),
        "total": document.get("counts", {}).get("total", -1),
        "by_class": document.get("counts", {}).get("by_class", {}),
    }


def _load_b2() -> dict[str, Any]:
    document = _load_if_exists(B2) or {}
    counts = document.get("counts") or {}
    tree = document.get("material_tree") or {}
    return {
        "materials": counts.get("materials", -1),
        "register": counts.get("register", -1),
        "no_registration": counts.get("no_registration", -1),
        "material_tree_matches": (
            tree.get("sha256") == material_tree_digest()
        ),
    }


def _load_c0() -> dict[str, Any]:
    document = _load_if_exists(C0) or {}
    counts = document.get("counts") or {}
    blockers = counts.get("blockers", [])
    owned = [
        row
        for row in document.get("records", [])
        if row.get("status") == "registered_zero_logical"
        and row.get("blocker")
        and row.get("owner")
    ]
    return {
        "registered_maps": counts.get("registered_maps", -1),
        "zero_blockers": len(blockers),
        "blockers_all_owned": (
            len(blockers) > 0 and len(owned) == len(blockers)
        )
        or len(blockers) == 0,
        "blockers": blockers,
    }


def _load_c1() -> dict[str, Any]:
    document = _load_if_exists(C1) or {}
    columns = document.get("columns") or {}
    ceiling = document.get("ceiling") or {}
    return {
        "column_sum": sum(columns.values()),
        "v1": columns.get("v1", -1),
        "ceiling_adjustment": ceiling.get("adjustment"),
        "justification_present": bool(ceiling.get("justification")),
    }


def _load_c2() -> dict[str, Any]:
    if not PROJECT_PLAN.is_file():
        raise ValueError(
            f"missing project plan document: {PROJECT_PLAN}"
        )
    text = PROJECT_PLAN.read_text(encoding="utf-8")
    return {"wording_updated": BETA_WORDING in text}


# ---------------------------------------------------------------------------
# build / check / write
# ---------------------------------------------------------------------------


def build() -> dict[str, Any]:
    a0 = _load_ledger_superseded()
    a1 = _load_a1()
    a2 = _load_a2()
    a3 = _load_a3()
    b1 = _load_b1()
    b2 = _load_b2()
    c0 = _load_c0()
    c1 = _load_c1()
    c2 = _load_c2()

    policy = _load_if_exists(POLICY) or {}
    closure_policy = policy.get("closure_policy") or {}
    pending = closure_policy.get("pending", [])

    gates_ok = (
        len(pending) == 0
        and closure_policy.get("final_closure_attempted", False) is True
        and a0["marked"]
        and a1["maps"] == 9
        and a1["unassigned"] == 0
        and a1["duplicate"] == 0
        and a1["publication_delta"] == 0
        and a2["unclassified"] == 0
        and a2["total"] == 322
        and a2["publication_delta"] == 0
        and a3["unclassified"] == 0
        and a3["total_occurrences"] == 115481
        and b1["unclassified"] == 0
        and b1["total"] == 146841
        and b2["materials"] == 15
        and b2["no_registration"] == 15
        and b2["register"] == 0
        and b2["material_tree_matches"]
        and c0["registered_maps"] == 32
        and c0["blockers_all_owned"]
        and c1["column_sum"] == 146841
        and c1["v1"] == 0
        and c1["ceiling_adjustment"] == "not required"
        and c1["justification_present"]
        and c2["wording_updated"]
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
        "evidence": {
            "a0_ledger_terminology": a0,
            "a1_shape_analysis": a1,
            "a2_fluid_mapping": a2,
            "a3_item_classification": a3,
            "b1_row_classification": b1,
            "b2_fluid_gap_disposition": b2,
            "c0_machine_playability": c0,
            "c1_denominator_recompute": c1,
            "c2_beta_wording": c2,
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
        document["status"] = "T22_5_READY"
        document["completed_stages"] = [
            "T22_5a",
            "T22_5b",
            "T22_5c",
            "T22_5d",
        ]
        document["pending_stages"] = []

    return document


def _check_required_keys(
    on_disk: dict[str, Any],
    expected: dict[str, Any],
    errors: list[str],
) -> None:
    """Verify all required top-level and evidence keys exist before
    field comparison — a missing key yields one human-readable error,
    never a KeyError cascade."""
    required_top = {
        "schema_version",
        "status_owner",
        "evidence",
        "closure_policy",
        "currentness",
    }
    missing_top = required_top - set(on_disk.keys())
    if missing_top:
        errors.append(
            f"T22.5 readiness is missing top-level keys: "
            f"{sorted(missing_top)}"
        )
    required_evidence = {
        "a0_ledger_terminology",
        "a1_shape_analysis",
        "a2_fluid_mapping",
        "a3_item_classification",
        "b1_row_classification",
        "b2_fluid_gap_disposition",
        "c0_machine_playability",
        "c1_denominator_recompute",
        "c2_beta_wording",
    }
    evidence = on_disk.get("evidence") or {}
    missing_evidence = required_evidence - set(evidence.keys())
    if missing_evidence:
        errors.append(
            "T22.5 readiness evidence is missing keys: "
            f"{sorted(missing_evidence)}"
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
        k: v for k, v in on_disk.items() if k != "currentness"
    }
    expected_stripped = {
        k: v for k, v in expected.items() if k != "currentness"
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
        print(f"T22.5 readiness failed: {error}", file=sys.stderr)
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
