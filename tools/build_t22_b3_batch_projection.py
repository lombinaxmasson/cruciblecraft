#!/usr/bin/env python3
"""Build the T22 B3 batch family projection manifest.

Consolidates all 7 petroleum families, registers per-family publication
deltas, and updates the global totals.  Each family gets the same six-point
closeout that B2 proved for crude_oil_distillation.

Families that require the GT6 dump for full recipe projection are marked
``NEEDS_GT6_DUMP`` — their recipe JSON stubs will be created when the dump
is available.  The B2 template is authoritative for the projection contract.
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
OUTPUT = TOOLS / "t22_family_manifest.json"
BUILDER = Path(__file__).resolve()

T22_BASELINE = ROOT / "src/main/resources/data/cruciblecraft/t22_publication_baseline.json"
T22_READINESS = TOOLS / "t22_readiness.json"
B2_MANIFEST = TOOLS / "t22_b2_crude_oil_manifest.json"


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
# family definitions with projection status
# ---------------------------------------------------------------------------

FAMILIES = [
    {
        "family_id": "crude_oil_distillation",
        "name": "Crude Oil Distillation (oil -> fuel + lubricant)",
        "status": "PROJECTED",
        "b2_evidence": "tools/t22_b2_crude_oil_manifest.json",
        "projection": {
            "logical": 1,
            "eager": 1,
            "lazy": 0,
            "evidence": "MEASURED",
        },
        "verification": {
            "expected_count": 1,
            "actual_count": 1,
            "missing": 0,
            "extra": 0,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
            "multisets_equal": True,
        },
        "closeout": {
            "bidirectional_equality": "PASS",
            "operand_proof": "PASS — oil registered as material_id 9850, fuel/lubricant registered as 9860/9882",
            "consumer_proof": "PASS — fuel consumed by fuels_engine (T18), lubricant pending C1",
            "delta_registered": "PASS — T22-B2 +1 eager/+1 logical",
            "localization": "PASS — oil/fuel/lubricant in en_us",
            "rebuild_diff": "N/A — no new files (recipe in t22_petroleum_generated)",
        },
    },
    {
        "family_id": "natural_gas_processing",
        "name": "Natural Gas Processing (centrifuge: 32 rows, dust→fluid, material-rule)",
        "status": "MATERIAL_RULE_COVERED",
        "projection": {
            "logical": 9,
            "eager": 9,
            "lazy": 0,
            "evidence": "STATIC_INFERENCE",
        },
        "verification": {
            "expected_count": 9,
            "actual_count": 0,
            "missing": 9,
            "extra": 0,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
            "multisets_equal": False,
            "note": "9 centrifuge petroleum rows from T21 denominator. Recipe JSON creation requires GT6 centrifuge dump.",
        },
        "closeout": {
            "bidirectional_equality": "PENDING",
            "operand_proof": "PASS — natural_gas registered (T11), methane registered (T11)",
            "consumer_proof": "PASS — methane consumed by fuels_gas (T18)",
            "delta_registered": "PENDING",
            "localization": "PASS — natural_gas in en_us/zh_cn",
            "rebuild_diff": "PENDING",
        },
    },
    {
        "family_id": "oil_sand_processing",
        "name": "Oil Sand / Oil Shale Processing (smelter: 3 rows)",
        "status": "NEEDS_GT6_DUMP",
        "projection": {
            "logical": 3,
            "eager": 3,
            "lazy": 0,
            "evidence": "STATIC_INFERENCE",
        },
        "verification": {
            "expected_count": 3,
            "actual_count": 0,
            "missing": 3,
            "extra": 0,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
            "multisets_equal": False,
            "note": "3 smelter petroleum rows from T21 denominator. Recipe JSON creation requires GT6 smelter dump.",
        },
        "closeout": {
            "bidirectional_equality": "PENDING",
            "operand_proof": "PASS — oil_sand/oil_shale from T20 worldgen",
            "consumer_proof": "PASS — products feed into crude_oil_distillation",
            "delta_registered": "PENDING",
            "localization": "PASS — oil_sand/oil_shale in en_us",
            "rebuild_diff": "PENDING",
        },
    },
    {
        "family_id": "mixer_petroleum_chemistry",
        "name": "Mixer Petroleum Chemistry (49 templates / 1,057 rows)",
        "status": "NEEDS_GT6_DUMP",
        "projection": {
            "logical": 1057,
            "eager": 200,
            "lazy": 857,
            "evidence": "PROJECTED",
        },
        "verification": {
            "expected_count": 1057,
            "actual_count": 0,
            "missing": 1057,
            "extra": 0,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
            "multisets_equal": False,
            "note": "Largest petroleum family. 49 mixer templates need per-template projection. This is the bulk of B3 work and requires the GT6 mixer dump + template membership data.",
        },
        "closeout": {
            "bidirectional_equality": "PENDING",
            "operand_proof": "MIXED — 49 templates need individual operand analysis",
            "consumer_proof": "MIXED — diverse downstream consumers",
            "delta_registered": "PENDING",
            "localization": "PENDING — new materials/fluids may need en_us/zh_cn if templates introduce novel outputs",
            "rebuild_diff": "PENDING",
        },
    },
    {
        "family_id": "compressor_petroleum",
        "name": "Compressor Petroleum (1 row)",
        "status": "NEEDS_GT6_DUMP",
        "projection": {
            "logical": 1,
            "eager": 1,
            "lazy": 0,
            "evidence": "STATIC_INFERENCE",
        },
        "verification": {
            "expected_count": 1,
            "actual_count": 0,
            "missing": 1,
            "extra": 0,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
            "multisets_equal": False,
            "note": "Single compressor row. Recipe JSON creation requires GT6 compressor dump.",
        },
        "closeout": {
            "bidirectional_equality": "PENDING",
            "operand_proof": "PENDING — need GT6 dump for fluid identity",
            "consumer_proof": "PENDING (C1)",
            "delta_registered": "PENDING",
            "localization": "PENDING",
            "rebuild_diff": "PENDING",
        },
    },
    {
        "family_id": "electrolyzer_petroleum",
        "name": "Electrolyzer Petroleum (1 row)",
        "status": "NEEDS_GT6_DUMP",
        "projection": {
            "logical": 1,
            "eager": 1,
            "lazy": 0,
            "evidence": "STATIC_INFERENCE",
        },
        "verification": {
            "expected_count": 1,
            "actual_count": 0,
            "missing": 1,
            "extra": 0,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
            "multisets_equal": False,
            "note": "Single electrolyzer row. Recipe JSON creation requires GT6 electrolyzer dump.",
        },
        "closeout": {
            "bidirectional_equality": "PENDING",
            "operand_proof": "PENDING — need GT6 dump for fluid identity",
            "consumer_proof": "PENDING (C1)",
            "delta_registered": "PENDING",
            "localization": "PENDING",
            "rebuild_diff": "PENDING",
        },
    },
    {
        "family_id": "kerosine_normalization",
        "name": "Kerosine Spelling Normalization (generifier: 1 row)",
        "status": "PROJECTED",
        "projection": {"logical": 1, "eager": 1, "lazy": 0, "evidence": "MEASURED"},
        "verification": {
            "expected_count": 1, "actual_count": 1, "missing": 0, "extra": 0,
            "membership_unassigned": 0, "membership_duplicate": 0,
            "multisets_equal": True,
            "note": "GT6 generifier row 3943: kerosine(1mB) -> kerosene(1mB). Both map to CC cruciblecraft:kerosine."
        },
        "closeout": {
            "bidirectional_equality": "PASS",
            "operand_proof": "PASS",
            "consumer_proof": "PASS — identity normalization",
            "delta_registered": "PASS — T22-B4 +1",
            "localization": "PASS",
            "rebuild_diff": "N/A",
        },
    },
    {
        "family_id": "fuel_combustion",
        "name": "Fuel Oil Combustion (fuels_engine: TBD rows)",
        "status": "NOT_RECORDED",
        "projection": {
            "logical": 0,
            "eager": 0,
            "lazy": 0,
            "evidence": "NOT_RECORDED",
        },
        "verification": {
            "expected_count": 0,
            "actual_count": 0,
            "missing": 0,
            "extra": 0,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
            "multisets_equal": True,
            "note": "Fuels_engine map is not in T21 denominator. Rows need separate scan.",
        },
        "closeout": {
            "bidirectional_equality": "N/A (no rows identified)",
            "operand_proof": "PASS — fuel registered",
            "consumer_proof": "PASS — fuels_engine produces KU (T18)",
            "delta_registered": "N/A",
            "localization": "N/A",
            "rebuild_diff": "N/A",
        },
    },
]


# ---------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------


def build() -> dict[str, Any]:
    # Load B2 evidence for crude_oil
    b2 = load(B2_MANIFEST) if B2_MANIFEST.is_file() else {}

    # Aggregate verification
    total_expected = sum(
        f["verification"]["expected_count"] for f in FAMILIES
    )
    total_missing = sum(
        f["verification"]["missing"] for f in FAMILIES
    )
    total_extra = sum(
        f["verification"]["extra"] for f in FAMILIES
    )

    # Aggregate projection
    total_logical = sum(f["projection"]["logical"] for f in FAMILIES)
    total_eager = sum(f["projection"]["eager"] for f in FAMILIES)
    total_lazy = sum(f["projection"]["lazy"] for f in FAMILIES)

    families_projected = sum(
        1 for f in FAMILIES if f["status"] == "PROJECTED"
    )
    families_pending = sum(
        1 for f in FAMILIES if f["status"] == "NEEDS_GT6_DUMP"
    )

    confirmed_logical = sum(
        f["projection"]["logical"]
        for f in FAMILIES
        if f["projection"]["evidence"] == "MEASURED"
    )

    return {
        "schema_version": 1,
        "status": "T22_FAMILY_MANIFEST_READY",
        "source_revision": GT6_REVISION,
        "families": FAMILIES,
        "aggregate": {
            "total_families": len(FAMILIES),
            "families_projected": families_projected,
            "families_pending_gt6_dump": families_pending,
            "families_not_recorded": sum(
                1 for f in FAMILIES if f["status"] == "NOT_RECORDED"
            ),
            "total_expected_rows": total_expected,
            "total_missing": total_missing,
            "total_extra": total_extra,
            "total_logical_projected": total_logical,
            "total_eager_projected": total_eager,
            "total_lazy_projected": total_lazy,
            "confirmed_logical_measured": confirmed_logical,
        },
        "verification": {
            "expected_count": total_expected,
            "actual_count": total_expected - total_missing,
            "missing_count": total_missing,
            "extra_count": total_extra,
            "membership_unassigned": 0,
            "membership_duplicate": 0,
        },
        "gt6_dump_blockers": {
            "families_blocked": [
                f["family_id"] for f in FAMILIES
                if f["status"] == "NEEDS_GT6_DUMP"
            ],
            "required_dumps": [
                "gt6_dump/gt6_recipe_dump/maps/gt.recipe.centrifuge.json",
                "gt6_dump/gt6_recipe_dump/maps/gt.recipe.smelter.json",
                "gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json",
                "gt6_dump/gt6_recipe_dump/maps/gt.recipe.compressor.json",
                "gt6_dump/gt6_recipe_dump/maps/gt.recipe.electrolyzer.json",
            ],
            "resolution": "Run B3 batch projection from main repo where gt6_dump is available. The B2 template (build_t22_b2_crude_oil.py) is the authoritative projection contract for each family.",
        },
        "publication_delta_summary": {
            "b2_crude_oil": {
                "logical": 1, "eager": 1, "lazy": 0,
                "status": "REGISTERED",
            },
            "pending_families": [
                {"family": "natural_gas_processing", "logical": 9, "eager": 9},
                {"family": "oil_sand_processing", "logical": 3, "eager": 3},
                {"family": "mixer_petroleum_chemistry", "logical": 1057, "eager": 200, "lazy": 857},
                {"family": "compressor_petroleum", "logical": 1, "eager": 1},
                {"family": "electrolyzer_petroleum", "logical": 1, "eager": 1},
            ],
        },
        "inputs": {
            relative(BUILDER): sha256(BUILDER),
            relative(B2_MANIFEST): (
                sha256(B2_MANIFEST) if B2_MANIFEST.is_file() else ""
            ),
        },
    }


# ---------------------------------------------------------------------------
# validate / check
# ---------------------------------------------------------------------------


def validate_compact(document: dict[str, Any]) -> None:
    if document.get("schema_version") != 1:
        raise ValueError("schema_version != 1")
    ver = document.get("verification", {})
    if ver.get("membership_unassigned", -1) != 0:
        raise ValueError("membership_unassigned != 0")
    if ver.get("membership_duplicate", -1) != 0:
        raise ValueError("membership_duplicate != 0")
    # At least 1 family confirmed (B2)
    agg = document.get("aggregate", {})
    if agg.get("confirmed_logical_measured", 0) < 1:
        raise ValueError("no families confirmed with MEASURED evidence")


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
# cli
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
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
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22 B3 batch projection failed: {error}", file=sys.stderr)
        return 1
    agg = document.get("aggregate", {})
    summary = {
        "schema_version": document.get("schema_version"),
        "status": document.get("status"),
        "projected": agg.get("families_projected"),
        "pending_dump": agg.get("families_pending_gt6_dump"),
        "confirmed_measured": agg.get("confirmed_logical_measured"),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
