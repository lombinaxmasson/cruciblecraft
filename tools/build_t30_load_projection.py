#!/usr/bin/env python3
"""Build the T30 pre-RC logistics load projection.

GT RecipeMap publication stays 0/0/0 by static inference. Vanilla hopper-family
recipes, loot, and the runtime catalog are non-GT datapack axes. Unmeasured T14
axes stay BLOCKED_PENDING_MEASUREMENT. Test-count deltas are ranges until R8/R9
measurement.
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
from tools import t35_common as t35  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t30_load_projection.json"
BUILDER = Path(__file__).resolve()
EVIDENCE = TOOLS / "t30_hopper_source_evidence.json"
CONTRACT = TOOLS / "phase5_1_pre_rc_logistics_contract.json"
T29_DELTA = TOOLS / "t29_publication_delta.json"
T29_LOAD = TOOLS / "t29_load_projection.json"
BUDGET_POLICY = TOOLS / "t14_load_budget_policy.json"
POLICY = TOOLS / "t30_hopper_source_policy.json"

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


def _require(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _projected_axis(
    *,
    axis: str,
    base: int,
    delta: int,
    evidence: str,
    hard_ceiling: int | None,
    kind: str,
    upper_bound: int | None = None,
) -> dict[str, Any]:
    projected = base + delta
    bound = projected if upper_bound is None else upper_bound
    if hard_ceiling is not None and bound > hard_ceiling:
        raise ValueError(
            f"{axis} projected/upper {bound} exceeds hard ceiling {hard_ceiling}"
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
        "upper_bound": bound,
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


def _range_axis(
    *,
    axis: str,
    base: int,
    delta_min: int,
    delta_max: int,
    evidence: str,
) -> dict[str, Any]:
    if delta_min > delta_max or delta_min < 0:
        raise ValueError(f"{axis} range is invalid")
    return {
        "axis": axis,
        "base": base,
        "delta": None,
        "delta_max": delta_max,
        "delta_min": delta_min,
        "evidence": evidence,
        "hard_ceiling": None,
        "kind": "PROJECTED_RANGE",
        "projected": None,
        "sign": "positive" if delta_max > 0 else "zero",
        "status": "projected_range",
        "upper_bound": base + delta_max,
        "verdict": None,
    }


def build() -> dict[str, Any]:
    for path in (EVIDENCE, CONTRACT, T29_DELTA, T29_LOAD, BUDGET_POLICY, POLICY):
        _require(path)
    evidence = common.load_json(EVIDENCE)
    contract = common.load_json(CONTRACT)
    t29 = common.load_json(T29_DELTA)
    t29_load = common.load_json(T29_LOAD)
    policy = common.load_json(POLICY)
    if evidence["counts"]["rows"] != 60:
        raise ValueError("R1 load projection requires 60 source rows")
    if evidence["counts"]["unresolved"] != 0:
        raise ValueError("R1 load projection forbids unresolved materials")
    if contract["work_set"]["current_active_t"] != "T30":
        raise ValueError("Phase 5.1 contract work set is not T30")
    if contract["rc_policy"]["rc_numbering"] != "forbidden_until_T30_READY":
        raise ValueError("RC numbering must stay forbidden until T30_READY")
    measured = t29["measured"]
    if measured.get("status") != "measured":
        raise ValueError("T30 opening requires a measured T29 publication delta")
    opening_publication = {
        "eager": int(t29["opening_publication"]["eager"]) + int(measured["eager"]),
        "lazy": int(t29["opening_publication"]["lazy"]) + int(measured["lazy"]),
        "logical": int(t29["opening_publication"]["logical"]) + int(measured["logical"]),
    }
    budgets = common.load_json(BUDGET_POLICY).get("budgets") or {}
    authored_base = int(t29_load["t14_axes"]["datapack_authored_entries"]["projected"])
    kinds = len(policy["catalog"]["kinds"])
    rows = int(evidence["counts"]["rows"])
    hopper_family = rows * kinds
    dust_funnel = 1
    identities = hopper_family + dust_funnel
    vanilla_recipes = identities
    loot = identities
    runtime_catalog = 1
    datapack_delta = vanilla_recipes + loot + runtime_catalog
    eager_ceiling = int(budgets["eager_publication_rows"]["hard_ceiling"])

    axes = {
        "logical_publication_rows": _projected_axis(
            axis="logical_publication_rows",
            base=opening_publication["logical"],
            delta=0,
            evidence=(
                "STATIC_INFERENCE: Hopper, Queue Hopper and Dust Funnel use vanilla "
                "crafting; GT logical publication does not grow"
            ),
            hard_ceiling=eager_ceiling,
            kind="STATIC_INFERENCE",
        ),
        "eager_publication_rows": _projected_axis(
            axis="eager_publication_rows",
            base=opening_publication["eager"],
            delta=0,
            evidence=(
                "STATIC_INFERENCE: T30 adds no GT RecipeMap or MaterialRule expansion"
            ),
            hard_ceiling=eager_ceiling,
            kind="STATIC_INFERENCE",
        ),
        "lazy_logical_rows": _projected_axis(
            axis="lazy_logical_rows",
            base=opening_publication["lazy"],
            delta=0,
            evidence="STATIC_INFERENCE: T30 adds no Hybrid Extruder or EMI map family",
            hard_ceiling=int(budgets["lazy_logical_rows"]["hard_ceiling"]),
            kind="STATIC_INFERENCE",
        ),
        "datapack_authored_entries": _projected_axis(
            axis="datapack_authored_entries",
            base=authored_base,
            delta=datapack_delta,
            evidence=(
                f"Upper bound: {vanilla_recipes} vanilla recipes + {loot} loot tables "
                f"+ {runtime_catalog} hopper_variants.json. Non-GT axes, not GT eager."
            ),
            hard_ceiling=int(budgets["datapack_authored_entries"]["hard_ceiling"]),
            kind="PROJECTED_FILE_ADDITION_UPPER_BOUND",
        ),
    }
    pending_evidence = (
        "T30 does not guess sync, lookup, reload, cache, or retained-memory "
        "deltas before R9 measurement"
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
                common.relative(CONTRACT): common.sha256_file(CONTRACT),
                common.relative(T29_DELTA): common.sha256_file(T29_DELTA),
                common.relative(T29_LOAD): common.sha256_file(T29_LOAD),
                common.relative(POLICY): common.sha256_file(POLICY),
            }
        },
        "generated_by": "python tools/build_t30_load_projection.py --write",
        "opening_publication": opening_publication,
        "opening_publication_authority": (
            "T29 opening publication plus T29 measured GT delta"
        ),
        "publication_delta": {
            "eager": 0,
            "lazy": 0,
            "logical": 0,
        },
        "registration": {
            "block_entity_types": 2,
            "blocks": identities,
            "dust_funnel": dust_funnel,
            "hopper": rows,
            "items": identities,
            "menu_types": 1,
            "queue_hopper": rows,
            "runtime_catalog": runtime_catalog,
            "shared_models_not_per_material": True,
        },
        "resources": {
            "blockstate": identities,
            "item_model": identities,
            "loot": loot,
            "per_material_texture": 0,
            "vanilla_recipe": vanilla_recipes,
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T30_LOAD_PROJECTION",
        "t14_axes": axes,
        "tests": {
            "gametest": _range_axis(
                axis="gametest",
                base=131,
                delta_min=30,
                delta_max=45,
                evidence=(
                    "R8 lists at least 30 Hopper/Queue/Dust GameTest scenarios; "
                    "final count is measured after the GameTest wave"
                ),
            ),
            "junit": _pending_axis(
                axis="junit",
                evidence=(
                    "R8 measures catalog, transfer-core, BE, menu, Dust and resource "
                    "tests; do not reuse the T29 593 figure"
                ),
                hard_ceiling=None,
            ),
            "python": _pending_axis(
                axis="python",
                evidence=(
                    "R9 measures T30 builder tests plus existing Python closure; "
                    "do not reuse the T29 790 figure"
                ),
                hard_ceiling=None,
            ),
        },
        "vanilla_crafting": {
            "dust_funnel": dust_funnel,
            "gt_eager": False,
            "hopper_family": hopper_family,
            "note": (
                "Hopper uses 5 plate + 1 chest; Queue Hopper uses 5 plate + 2 chest; "
                "tool catalysts and plateCurved are omitted as SOURCE_DERIVED."
            ),
            "status": "projected",
            "total": vanilla_recipes,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return t35.check_compact(OUTPUT, build(), encode=common.stable_json)


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
                f"logical={delta['logical']} eager={delta['eager']} lazy={delta['lazy']} "
                f"blocks={document['registration']['blocks']}"
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
