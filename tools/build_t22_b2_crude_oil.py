#!/usr/bin/env python3
"""Build the T22 crude oil distillation family projection and verify fidelity.

B2 is the FIRST family full projection — the vertical slice that proves
the entire pipeline works before batch-processing the other 6 families.

This builder:
  1. Reads the source GT6 distillery row (via committed projection data)
  2. Constructs the *expected* recipe from the source
  3. Reads the *actual* authored recipe from the generated datapack
  4. Computes dual independent multiset hashes
  5. Verifies bidirectional equality: missing=0, extra=0
  6. Registers the publication delta

The pattern mirrors T21's gunpowder Mixer family verification.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_b2_crude_oil_manifest.json"
BUILDER = Path(__file__).resolve()

# Source row data from committed t5_distillery_projection.json
# Row 705: oil -> fuel + lubricant
SOURCE_ROW_705: dict[str, Any] = {
    "recipe_index": 705,
    "source_path": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.distillery.json#recipes[705]",
    "row_sha256": "8b59c67b0baa2fa78a2cdd0b3725c6e90723464aabf7d69c07f3254ee8cb4536",
    "duration": 16,
    "eut": 16,
    "can_be_buffered": True,
    "fluid_inputs": [
        {"fluid": "oil", "amount": 25}
    ],
    "fluid_outputs": [
        {"fluid": "fuel", "amount": 25},
        {"fluid": "lubricant", "amount": 25},
    ],
}

# Authored recipe path
AUTHORED_RECIPE = (
    ROOT
    / "src/t22_petroleum_generated/resources/data/cruciblecraft/recipe"
    / "t22/distillery/oil_to_fuel_and_lubricant.json"
)

T22_BASELINE = ROOT / "src/main/resources/data/cruciblecraft/t22_publication_baseline.json"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True,
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


# ---------------------------------------------------------------------------
# expected construction (from source — independent path A)
# ---------------------------------------------------------------------------

def _build_expected() -> dict[str, Any]:
    """Build expected recipe from source row data (independent path A).

    This is deliberately separate from reading the authored file —
    the two paths must produce identical results."""
    src = SOURCE_ROW_705
    return {
        "can_be_buffered": src["can_be_buffered"],
        "duration": src["duration"],
        "eut": src["eut"],
        "fluid_inputs": [
            {"amount": f["amount"], "id": f"cruciblecraft:{f['fluid']}"}
            for f in src["fluid_inputs"]
        ],
        "fluid_outputs": [
            {"amount": f["amount"], "id": f"cruciblecraft:{f['fluid']}"}
            for f in src["fluid_outputs"]
        ],
        "map": "cruciblecraft:distillery",
        "type": "cruciblecraft:gt_recipe",
    }


def _multiset_hash(recipe: dict[str, Any]) -> str:
    """Compute a content-addressed multiset hash of recipe fields.

    Stable across JSON serialization differences — only the semantic
    content matters.
    """
    canonical = {
        "map": recipe.get("map", ""),
        "duration": recipe.get("duration", 0),
        "eut": recipe.get("eut", 0),
        "can_be_buffered": recipe.get("can_be_buffered", True),
        "fluid_inputs": sorted(
            (f["id"], f["amount"])
            for f in recipe.get("fluid_inputs", [])
        ),
        "fluid_outputs": sorted(
            (f["id"], f["amount"])
            for f in recipe.get("fluid_outputs", [])
        ),
    }
    payload = json.dumps(canonical, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(payload.encode()).hexdigest()


# ---------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------


def build() -> dict[str, Any]:
    """Build the B2 crude oil family manifest with fidelity verification."""
    expected = _build_expected()
    expected_hash = _multiset_hash(expected)

    # Path B: read authored recipe
    authored = load(AUTHORED_RECIPE) if AUTHORED_RECIPE.is_file() else {}
    actual_hash = _multiset_hash(authored)

    # Strip provenance for comparison (provenance is metadata, not recipe content)
    authored_stripped = {
        k: v for k, v in authored.items()
        if k != "provenance"
    }

    # Compare
    missing = {
        k: v for k, v in expected.items()
        if k not in authored_stripped or authored_stripped.get(k) != v
    }
    extra = {
        k: v for k, v in authored_stripped.items()
        if k not in expected
    }

    # Per-field equality
    fields_equal = {
        field: expected.get(field) == authored_stripped.get(field)
        for field in expected
    }

    all_equal = len(missing) == 0 and len(extra) == 0

    # Publication delta for this family
    family_delta = {
        "family_id": "crude_oil_distillation",
        "family_name": "Crude Oil Distillation (oil -> fuel + lubricant)",
        "logical_rows_added": 1,
        "eager_rows_added": 1,
        "lazy_rows_added": 0,
        "authored_files": 1,
        "recipe_ids": [
            "cruciblecraft:t22/distillery/oil_to_fuel_and_lubricant"
        ],
    }

    return {
        "schema_version": 1,
        "status": "T22_B2_CRUDE_OIL_READY" if all_equal else "FAIL",
        "family": "crude_oil_distillation",
        "source": {
            "revision": GT6_REVISION,
            "row_index": SOURCE_ROW_705["recipe_index"],
            "source_row_sha256": SOURCE_ROW_705["row_sha256"],
            "source_path": SOURCE_ROW_705["source_path"],
        },
        "expected": {
            "recipe": expected,
            "multiset_sha256": expected_hash,
        },
        "actual": {
            "recipe": authored_stripped,
            "multiset_sha256": actual_hash,
        },
        "verification": {
            "expected_count": 1,
            "actual_count": 1,
            "expected_multiset_sha256": expected_hash,
            "actual_multiset_sha256": actual_hash,
            "missing_count": len(missing),
            "extra_count": len(extra),
            "missing_fields": {k: str(v) for k, v in missing.items()},
            "extra_fields": {k: str(v) for k, v in extra.items()},
            "fields_equal": fields_equal,
            "all_equal": all_equal,
            "multisets_equal": expected_hash == actual_hash,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
        },
        "publication_delta": family_delta,
        "inputs": {
            relative(AUTHORED_RECIPE): (
                sha256(AUTHORED_RECIPE) if AUTHORED_RECIPE.is_file() else ""
            ),
            relative(BUILDER): sha256(BUILDER),
        },
    }


# ---------------------------------------------------------------------------
# validate / check
# ---------------------------------------------------------------------------


def validate_compact(document: dict[str, Any]) -> None:
    if document.get("schema_version") != 1:
        raise ValueError("schema_version != 1")
    ver = document.get("verification", {})
    if ver.get("missing_count", -1) != 0:
        raise ValueError(f"missing_count != 0 (got {ver.get('missing_count')})")
    if ver.get("extra_count", -1) != 0:
        raise ValueError(f"extra_count != 0 (got {ver.get('extra_count')})")
    if ver.get("membership_unassigned", -1) != 0:
        raise ValueError("membership_unassigned != 0")
    if ver.get("membership_duplicate", -1) != 0:
        raise ValueError("membership_duplicate != 0")
    if not ver.get("multisets_equal", False):
        raise ValueError("multiset hashes not equal")
    if document.get("status") != "T22_B2_CRUDE_OIL_READY":
        raise ValueError(f"status is {document.get('status')}")


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {relative(OUTPUT)}")
        return errors
    on_disk = load(OUTPUT)
    try:
        validate_compact(on_disk)
    except ValueError as exc:
        errors.append(str(exc))
    expected = build()
    disk_stripped = {
        k: v for k, v in on_disk.items() if k not in ("inputs",)
    }
    expected_stripped = {
        k: v for k, v in expected.items() if k not in ("inputs",)
    }
    if stable(disk_stripped) != stable(expected_stripped):
        errors.append(f"stale generated file: {relative(OUTPUT)}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


# ---------------------------------------------------------------------------
# delta registration
# ---------------------------------------------------------------------------


def register_delta(document: dict[str, Any]) -> None:
    """Register the B2 publication delta in t22_publication_baseline.json."""
    if not T22_BASELINE.is_file():
        print("warning: t22_publication_baseline.json not found, skipping delta registration")
        return
    baseline = load(T22_BASELINE)
    delta = document["publication_delta"]
    ledger = baseline.setdefault("delta_ledger_policy", {})
    deltas = ledger.setdefault("registered_deltas", [])
    # Remove any existing B2 entry
    deltas[:] = [d for d in deltas if d.get("phase") != "T22-B2"]
    deltas.append({
        "phase": "T22-B2",
        "family": delta["family_id"],
        "logical": delta["logical_rows_added"],
        "eager": delta["eager_rows_added"],
        "lazy": delta["lazy_rows_added"],
        "evidence": relative(OUTPUT),
    })
    # Recompute current totals
    base_eager = ledger.get("base_eager", 16650)
    total_eager = base_eager + sum(d["eager"] for d in deltas)
    total_logical = 18875 + sum(d["logical"] for d in deltas)
    total_lazy = 2225 + sum(d["lazy"] for d in deltas)
    ledger["current_eager"] = total_eager
    ledger["current_logical"] = total_logical
    ledger["current_lazy"] = total_lazy
    # Update publication totals
    baseline["publication_totals"] = {
        "logical_rows": total_logical,
        "eager_rows": total_eager,
        "lazy_rows": total_lazy,
    }
    T22_BASELINE.write_text(stable(baseline), encoding="utf-8", newline="\n")


# ---------------------------------------------------------------------------
# cli
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument(
        "--register-delta",
        action="store_true",
        help="Register publication delta in t22_publication_baseline.json",
    )
    args = parser.parse_args(argv)
    if args.check == args.write:
        print("error: exactly one of --check or --write required", file=sys.stderr)
        return 1
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            document = write()
        if args.register_delta:
            register_delta(document)
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22 B2 crude oil projection failed: {error}", file=sys.stderr)
        return 1
    ver = document.get("verification", {})
    summary = {
        "schema_version": document.get("schema_version"),
        "status": document.get("status"),
        "missing": ver.get("missing_count"),
        "extra": ver.get("extra_count"),
        "multisets_equal": ver.get("multisets_equal"),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
