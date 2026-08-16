#!/usr/bin/env python3
"""Build the T22 petroleum load projection.

Projects logical / eager / lazy breakdown for every petroleum family,
checks against hard gates, and records per-family strategy with rationale.

All figures are tagged MEASURED (from committed artifacts), STATIC_INFERENCE
(derived from structural constraints), or PROJECTED (B2-B3 will measure).

Strategy options:
  immediate  — all rows eager (high-frequency, small count, or core chain)
  hybrid     — core rows eager, remainder lazy
  on_demand  — all rows lazy (low-frequency or post-1.0)
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
OUTPUT = TOOLS / "t22_load_projection.json"
BUILDER = Path(__file__).resolve()

# Hard gates from T14
HARD_GATES = {
    "logical_ceiling": 21000,
    "eager_soft": 18000,
    "eager_hard": 21000,
    "lazy_soft": 16000,
    "lazy_hard": 56000,
    "datapack_soft": 6000,
    "datapack_hard": 6600,
}

# Pre-T22 baseline (from G1)
BASELINE = {
    "logical": 18879,
    "eager": 16654,
    "lazy": 2225,
    "datapack": 3260,
}


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
# per-family projection
# ---------------------------------------------------------------------------

def _project_families() -> list[dict[str, Any]]:
    """Project load for each petroleum family.

    STATIC_INFERENCE figures are marked; B2-B3 will replace with MEASURED.
    """
    return [
        {
            "family_id": "crude_oil_distillation",
            "strategy": "immediate",
            "projection": {
                "logical": 6,
                "eager": 1,
                "lazy": 5,
            },
            "evidence": "STATIC_INFERENCE",
            "rationale": (
                "6 distillery petroleum rows (A1): 1 v1_required eager "
                "(oil->fuel+lubricant, all fluids registered), "
                "5 ordinary_optional lazy (soulsandoil/liquid_*_oil — "
                "fluids not yet registered, B1 work). "
                "Core chain: oil->fuel+lubricant is high-frequency."
            ),
            "frequency": "high (core petroleum chain)",
            "headroom_impact": "negligible (6 rows)",
        },
        {
            "family_id": "natural_gas_processing",
            "strategy": "immediate",
            "projection": {
                "logical": 9,
                "eager": 9,
                "lazy": 0,
            },
            "evidence": "STATIC_INFERENCE",
            "rationale": (
                "9 centrifuge petroleum rows (T21). Natural_gas processing "
                "is core energy chain (natural_gas->methane->fuels_gas->HU). "
                "All eager — small count, no lazy benefit."
            ),
            "frequency": "high (energy chain)",
            "headroom_impact": "negligible (9 rows)",
        },
        {
            "family_id": "fuel_combustion",
            "strategy": "immediate",
            "projection": {
                "logical": 0,
                "eager": 0,
                "lazy": 0,
            },
            "evidence": "NOT_RECORDED",
            "rationale": (
                "Fuels_engine map is NOT covered by T21 template denominator "
                "(10 maps). Rows are TBD — need separate scan of "
                "gt.recipe.fuels.engine.json. Expected small (<10 rows). "
                "T18 already uses fuel_engine for KU power; any new rows "
                "are immediate eager."
            ),
            "frequency": "high (KU power generation)",
            "headroom_impact": "negligible (TBD, est. <10 rows)",
            "b2_action": "Scan fuels_engine GT6 dump for petroleum rows.",
        },
        {
            "family_id": "oil_sand_processing",
            "strategy": "immediate",
            "projection": {
                "logical": 3,
                "eager": 3,
                "lazy": 0,
            },
            "evidence": "STATIC_INFERENCE",
            "rationale": (
                "3 smelter rows for oil_sand processing. "
                "Feeds into crude_oil_distillation chain. "
                "Small count — all eager."
            ),
            "frequency": "medium (ore processing, gated by worldgen)",
            "headroom_impact": "negligible (3 rows)",
        },
        {
            "family_id": "mixer_petroleum_chemistry",
            "strategy": "hybrid",
            "projection": {
                "logical": 1057,
                "eager": 200,
                "lazy": 857,
            },
            "evidence": "PROJECTED",
            "rationale": (
                "49 mixer templates → 1,057 rows. Largest petroleum family. "
                "Core fuel blending and common precursors (~200 rows) are "
                "high-frequency and must be eager. "
                "Exotic plastic precursors, specialty chemicals (~857 rows) "
                "are low-frequency — lazy via on-demand expansion. "
                "B2 full projection will measure exact eager/lazy split "
                "per template. Current split is a policy placeholder; "
                "do NOT use lazy as default just because budget allows it."
            ),
            "frequency": "mixed (core: high; specialty: low)",
            "headroom_impact": (
                "eager +200 (16654→16854, 1146 below soft cap); "
                "lazy +857 (2225→3082, 12918 below soft cap)"
            ),
            "b2_action": "First family full projection must measure actual "
                         "template-by-template frequency and replace "
                         "PROJECTED figures with MEASURED.",
        },
        {
            "family_id": "compressor_petroleum",
            "strategy": "immediate",
            "projection": {
                "logical": 1,
                "eager": 1,
                "lazy": 0,
            },
            "evidence": "STATIC_INFERENCE",
            "rationale": "Single row. Immediate.",
            "frequency": "low (single recipe)",
            "headroom_impact": "negligible (1 row)",
        },
        {
            "family_id": "electrolyzer_petroleum",
            "strategy": "immediate",
            "projection": {
                "logical": 1,
                "eager": 1,
                "lazy": 0,
            },
            "evidence": "STATIC_INFERENCE",
            "rationale": "Single row. Immediate.",
            "frequency": "low (single recipe)",
            "headroom_impact": "negligible (1 row)",
        },
    ]


# ---------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------


def build() -> dict[str, Any]:
    families = _project_families()

    # Aggregate
    total_logical = sum(f["projection"]["logical"] for f in families)
    total_eager = sum(f["projection"]["eager"] for f in families)
    total_lazy = sum(f["projection"]["lazy"] for f in families)

    post_logical = BASELINE["logical"] + total_logical
    post_eager = BASELINE["eager"] + total_eager
    post_lazy = BASELINE["lazy"] + total_lazy

    # Gate checks
    gates = {
        "logical_within_hard": post_logical <= HARD_GATES["logical_ceiling"],
        "eager_within_soft": post_eager <= HARD_GATES["eager_soft"],
        "eager_within_hard": post_eager <= HARD_GATES["eager_hard"],
        "lazy_within_soft": post_lazy <= HARD_GATES["lazy_soft"],
        "lazy_within_hard": post_lazy <= HARD_GATES["lazy_hard"],
        "all_gates_pass": (
            post_logical <= HARD_GATES["logical_ceiling"]
            and post_eager <= HARD_GATES["eager_hard"]
            and post_lazy <= HARD_GATES["lazy_hard"]
        ),
    }

    all_pass = all(gates.values())

    return {
        "schema_version": 1,
        "status": "PASS" if all_pass else "FAIL",
        "source_revision": GT6_REVISION,
        "baseline": BASELINE,
        "hard_gates": HARD_GATES,
        "families": families,
        "aggregate": {
            "total_logical_added": total_logical,
            "total_eager_added": total_eager,
            "total_lazy_added": total_lazy,
            "post_t22_logical": post_logical,
            "post_t22_eager": post_eager,
            "post_t22_lazy": post_lazy,
            "logical_remaining": HARD_GATES["logical_ceiling"] - post_logical,
            "eager_remaining_to_soft": HARD_GATES["eager_soft"] - post_eager,
            "lazy_remaining_to_soft": HARD_GATES["lazy_soft"] - post_lazy,
        },
        "gates": gates,
        "decision": {
            "ceiling_adjustment_needed": False,
            "reason": (
                f"Total projection {total_logical} rows fits within "
                f"{HARD_GATES['logical_ceiling'] - BASELINE['logical']} "
                f"headroom ({post_logical}/{HARD_GATES['logical_ceiling']}). "
                f"No ceiling adjustment required."
            ),
            "high_frequency_guard": (
                "Mixer core 200 rows are forced eager despite ample lazy "
                "budget. High-frequency main-chain reactions MUST NOT be "
                "defaulted to lazy just because the total budget allows it."
            ),
            "per_family_strategy_summary": {
                "immediate": [
                    "crude_oil_distillation (core only)",
                    "natural_gas_processing",
                    "fuel_combustion",
                    "oil_sand_processing",
                    "compressor_petroleum",
                    "electrolyzer_petroleum",
                ],
                "hybrid": [
                    "mixer_petroleum_chemistry (core eager, specialty lazy)",
                ],
                "on_demand": [],
            },
        },
        "measurement_plan": {
            "STATIC_INFERENCE": (
                "6 of 7 families use STATIC_INFERENCE based on small "
                "row counts where measurement adds no value."
            ),
            "PROJECTED": (
                "Mixer family uses PROJECTED eager/lazy split. "
                "B2 first-family full projection MUST replace with "
                "MEASURED per-template frequency data."
            ),
            "MEASURED": (
                "No families currently at MEASURED. B2-B4 will populate."
            ),
        },
        "inputs": {
            relative(Path(__file__)): sha256(Path(__file__)),
        },
    }


# ---------------------------------------------------------------------------
# validate / check
# ---------------------------------------------------------------------------


def validate_compact(document: dict[str, Any]) -> None:
    if document.get("schema_version") != 1:
        raise ValueError("schema_version != 1")
    if document.get("source_revision") != GT6_REVISION:
        raise ValueError("source_revision mismatch")
    gates = document.get("gates", {})
    if not gates.get("all_gates_pass", False):
        raise ValueError("load projection exceeds hard gates")
    status = document.get("status")
    if status != "PASS":
        raise ValueError(f"projection status is {status}, expected PASS")
    # Ceiling adjustment must be MEASURED, not inferred
    decision = document.get("decision", {})
    if decision.get("ceiling_adjustment_needed"):
        raise ValueError(
            "ceiling adjustment requires four-class measurement per T14"
        )


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
        print(f"T22 load projection failed: {error}", file=sys.stderr)
        return 1
    agg = document.get("aggregate", {})
    gates = document.get("gates", {})
    summary = {
        "schema_version": document.get("schema_version"),
        "status": document.get("status"),
        "post_logical": agg.get("post_t22_logical"),
        "remaining": agg.get("logical_remaining"),
        "all_gates": gates.get("all_gates_pass"),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
