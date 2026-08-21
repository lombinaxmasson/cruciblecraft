#!/usr/bin/env python3
"""Build the T29 large-crucible design and T14 load projection.

GT RecipeMap publication stays 0/0/0. Vanilla crafting and the structure
JSON are non-GT axes. Unmeasured T14 axes stay BLOCKED_PENDING_MEASUREMENT.
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
OUTPUT = TOOLS / "t29_load_projection.json"
BUILDER = Path(__file__).resolve()
EVIDENCE = TOOLS / "t29_crucible_source_evidence.json"
OPENING = TOOLS / "t27_opening_snapshot.json"
T28_DELTA = TOOLS / "t28_publication_delta.json"
BUDGET_POLICY = TOOLS / "t14_load_budget_policy.json"
T23_POLICY = TOOLS / "t23_multiblock_policy.json"

PENDING_AXES = (
    "lazy_cache_ceiling_rows",
    "sync_bytes",
    "server_reload_ms",
    "server_index_ms",
    "client_reload_ms",
    "client_index_ms",
    "retained_memory_bytes",
    "allocation_bytes",
    "lookup_p95_ns",
    "lookup_candidate_count",
)

T29_GAME_TESTS = (
    "formation",
    "teardown",
    "output_jam",
    "power_loss",
    "reload",
    "save_quarantine",
    "capacity_432",
    "steelmaking",
    "mold_cast",
    "layer_ports",
)


def _require(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _crucible_positions() -> int:
    for row in common.load_json(T23_POLICY).get("kinds") or []:
        if row.get("canonical_id") == "crucible":
            return int(row["schema_expressibility"]["positions"])
    raise ValueError("T23 policy is missing the crucible kind")


def _projected_axis(
    *,
    axis: str,
    base: int,
    delta: int,
    evidence: str,
    hard_ceiling: int | None,
    kind: str,
) -> dict[str, Any]:
    projected = base + delta
    if hard_ceiling is not None and projected > hard_ceiling:
        raise ValueError(
            f"{axis} projected {projected} exceeds hard ceiling {hard_ceiling}"
        )
    return {
        "axis": axis,
        "base": base,
        "delta": delta,
        "evidence": evidence,
        "hard_ceiling": hard_ceiling,
        "kind": kind,
        "projected": projected,
        "sign": "negative" if delta < 0 else ("positive" if delta > 0 else "zero"),
        "status": "projected",
        "upper_bound": projected,
        "verdict": None,
    }


def _pending_axis(*, axis: str, evidence: str, hard_ceiling: int | None) -> dict[str, Any]:
    return {
        "axis": axis,
        "base": None,
        "delta": None,
        "evidence": evidence,
        "hard_ceiling": hard_ceiling,
        "kind": "unmeasured",
        "projected": None,
        "sign": None,
        "status": "pending",
        "upper_bound": None,
        "verdict": common.PENDING_LOAD_VERDICT,
    }


def build() -> dict[str, Any]:
    for path in (EVIDENCE, OPENING, T28_DELTA, BUDGET_POLICY, T23_POLICY):
        _require(path)
    evidence = common.load_json(EVIDENCE)
    if evidence["structure"]["positions"] != 27:
        raise ValueError("R2 requires R1 structure.positions=27")
    if evidence["capacity"]["ingot_units"] != 432:
        raise ValueError("R2 requires R1 capacity 432")
    if evidence["fidelity_layering"]["not_a_processing_host"] is not True:
        raise ValueError("R2 forbids treating the crucible as a processing_host")
    if _crucible_positions() != 27:
        raise ValueError("T23 crucible positions must already be 27 before projection")

    opening = common.load_json(OPENING)
    t28 = common.load_json(T28_DELTA)
    budgets = (common.load_json(BUDGET_POLICY).get("budgets") or {})
    t27_publication = (opening.get("publication") or {}).get("current") or {}
    t28_measured = t28["measured"]
    if t28_measured.get("status") != "measured":
        raise ValueError("T29 opening requires a measured T28 publication delta")
    opening_publication = {
        "eager": int(t27_publication["eager"]) + int(t28_measured["eager"]),
        "lazy": int(t27_publication["lazy"]) + int(t28_measured["lazy"]),
        "logical": int(t27_publication["logical"]) + int(t28_measured["logical"]),
    }
    # T28 deleted one authored cooling rule. T27 opening authored=3267.
    authored_base = int(
        ((opening.get("datapack") or {}).get("t14_authored_files") or {})["value"]
    ) - 1
    eager_ceiling = int(budgets["eager_publication_rows"]["hard_ceiling"])

    axes = {
        "logical_publication_rows": _projected_axis(
            axis="logical_publication_rows",
            base=opening_publication["logical"],
            delta=0,
            evidence=(
                "STATIC_INFERENCE: large_crucible is not a RecipeMap host; "
                "GT logical publication does not grow"
            ),
            hard_ceiling=eager_ceiling,
            kind="STATIC_INFERENCE",
        ),
        "eager_publication_rows": _projected_axis(
            axis="eager_publication_rows",
            base=opening_publication["eager"],
            delta=0,
            evidence=(
                "STATIC_INFERENCE: no new GT RecipeMap or MaterialRule expansion"
            ),
            hard_ceiling=eager_ceiling,
            kind="STATIC_INFERENCE",
        ),
        "lazy_logical_rows": _projected_axis(
            axis="lazy_logical_rows",
            base=opening_publication["lazy"],
            delta=0,
            evidence=(
                "STATIC_INFERENCE: T29 adds no Hybrid Extruder family or EMI "
                "processing-map enumeration"
            ),
            hard_ceiling=int(budgets["lazy_logical_rows"]["hard_ceiling"]),
            kind="STATIC_INFERENCE",
        ),
        "datapack_authored_entries": _projected_axis(
            axis="datapack_authored_entries",
            base=authored_base,
            delta=1,
            evidence=(
                "One authored multiblock_structures/large_crucible.json. "
                "Vanilla controller crafting is a non-GT axis, not GT eager."
            ),
            hard_ceiling=int(budgets["datapack_authored_entries"]["hard_ceiling"]),
            kind="PROJECTED_FILE_ADDITION",
        ),
    }
    pending_evidence = (
        "T29 does not guess sync, lookup, reload, cache, or retained-memory "
        "deltas before R7 measurement"
    )
    for axis in PENDING_AXES:
        axes[axis] = _pending_axis(
            axis=axis,
            evidence=pending_evidence,
            hard_ceiling=int(budgets[axis]["hard_ceiling"]),
        )
    missing_budgets = sorted(set(budgets) - set(axes) - {"logical_publication_rows"})
    extra = sorted(set(PENDING_AXES) - set(budgets))
    if missing_budgets or extra:
        raise ValueError(
            f"T14 axis coverage drifted missing={missing_budgets} extra={extra}"
        )
    for axis, row in axes.items():
        if row["status"] == "pending" and row["delta"] == 0:
            raise ValueError(f"{axis} pending delta must not be filled with 0")

    return {
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(EVIDENCE): common.sha256_file(EVIDENCE),
                common.relative(OPENING): common.sha256_file(OPENING),
                common.relative(T28_DELTA): common.sha256_file(T28_DELTA),
                common.relative(T23_POLICY): common.sha256_file(T23_POLICY),
            }
        },
        "fidelity_layering": evidence["fidelity_layering"],
        "generated_by": "python tools/build_t29_load_projection.py --write",
        "opening_publication": opening_publication,
        "opening_publication_authority": (
            "T27 opening current plus T28 measured cooling retirement"
        ),
        "plugin": {
            "id": "cruciblecraft:thermal_steelmaking_host",
            "not": [
                "cruciblecraft:processing_host",
                "cruciblecraft:storage_host",
                "cruciblecraft:steam_conversion",
            ],
            "reused": [
                "cruciblecraft:heat_energy_input",
                "cruciblecraft:shared_port_supply",
            ],
        },
        "publication_delta": {
            "eager": 0,
            "lazy": 0,
            "logical": 0,
        },
        "registration": {
            "block_entities": 1,
            "controller_blocks": 1,
            "controller_id": "cruciblecraft:large_crucible",
            "new_plugins": 1,
            "single_block_crucible_retained": True,
            "structure_json": 1,
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T29_LOAD_PROJECTION",
        "structure_positions": 27,
        "t14_axes": axes,
        "tests": {
            "gametest": {
                "axis": "gametest",
                "base": 121,
                "delta": len(T29_GAME_TESTS),
                "evidence": (
                    "T23 six-lifecycle paths plus capacity, steelmaking, "
                    "mold cast, and layer-port GameTests"
                ),
                "ids": list(T29_GAME_TESTS),
                "kind": "PROJECTED_TEST_ADDITION",
                "projected": 121 + len(T29_GAME_TESTS),
                "status": "projected",
                "verdict": None,
            },
            "junit": _pending_axis(
                axis="junit",
                evidence=(
                    "R7a measures plugin whitelist, structure schema, capacity "
                    "and quarantine counts; do not reuse the T28 584 figure"
                ),
                hard_ceiling=None,
            ),
        },
        "vanilla_crafting": {
            "controller": 1,
            "gt_eager": False,
            "note": (
                "Controller recipe is vanilla crafting. Walls reuse firebrick "
                "plus existing energy/item-fluid ports; no new wall item."
            ),
            "status": "projected",
            "walls": 0,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
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
            delta = document["publication_delta"]
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"logical={delta['logical']} eager={delta['eager']} lazy={delta['lazy']}"
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
