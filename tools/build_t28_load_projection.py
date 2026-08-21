#!/usr/bin/env python3
"""Build the T28 hot-ingot retirement design and T14 load projection.

Publication deltas are derived from the measured cooling MaterialRule
expansion. Unmeasured T14 axes stay BLOCKED_PENDING_MEASUREMENT and are
never filled with 0.
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
OUTPUT = TOOLS / "t28_load_projection.json"
BUILDER = Path(__file__).resolve()
EVIDENCE = TOOLS / "t28_hot_ingot_source_evidence.json"
OPENING = TOOLS / "t27_opening_snapshot.json"
T10_PREFLIGHT = TOOLS / "t10_preflight_projection.json"
REPORT = TOOLS / "full_verification_report.json"
BUDGET_POLICY = TOOLS / "t14_load_budget_policy.json"
COOLING_RULE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "t10"
    / "cooling"
    / "hot_ingot_to_ingot.json"
)

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


def _cooling_expansion() -> dict[str, Any]:
    t10 = common.load_json(T10_PREFLIGHT)
    hot = (t10.get("route_projections") or {}).get("hot_ingot") or {}
    materials = hot.get("materials")
    if not isinstance(materials, list) or not materials:
        raise ValueError("T10 hot_ingot material list is missing")
    count = len(materials)
    report = common.load_json(REPORT)
    published = ((report.get("rules") or {}).get("expanded_recipes_per_map") or {}).get(
        "cruciblecraft:cooling"
    )
    if COOLING_RULE.is_file() and published != count:
        raise ValueError(
            "cooling expansion mismatch: T10 materials="
            f"{count} report={published}"
        )
    record = {
        "authority": "tools/t10_preflight_projection.json#route_projections.hot_ingot.materials",
        "count": count,
        "cross_check": (
            "tools/full_verification_report.json"
            "#rules.expanded_recipes_per_map.cruciblecraft:cooling"
        ),
        "material_set_sha256": hot.get("material_set_sha256"),
        "rule_path": common.relative(COOLING_RULE),
        "rule_present": COOLING_RULE.is_file(),
    }
    if COOLING_RULE.is_file():
        record["rule_sha256"] = common.sha256_file(COOLING_RULE)
    return record


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
    if delta > 0:
        raise ValueError(f"{axis} T28 retirement cannot increase load ({delta})")
    return {
        "axis": axis,
        "base": base,
        "delta": delta,
        "evidence": evidence,
        "hard_ceiling": hard_ceiling,
        "kind": kind,
        "projected": projected,
        "sign": "negative" if delta < 0 else "zero",
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
    _require(EVIDENCE)
    _require(OPENING)
    _require(T10_PREFLIGHT)
    _require(REPORT)
    _require(BUDGET_POLICY)
    evidence = common.load_json(EVIDENCE)
    if evidence.get("conclusion", {}).get("gt6_passive_ingotHot_to_ingot") is not False:
        raise ValueError("R2 requires R1 conclusion gt6_passive_ingotHot_to_ingot=false")
    if evidence.get("freezer", {}).get("in_card_scope") is not False:
        raise ValueError("R2 forbids putting freezer in T28 implementation scope")

    opening = common.load_json(OPENING)
    publication = (opening.get("publication") or {}).get("current") or {}
    budgets = (common.load_json(BUDGET_POLICY).get("budgets") or {})
    expansion = _cooling_expansion()
    retired = expansion["count"]
    eager_ceiling = int(budgets["eager_publication_rows"]["hard_ceiling"])

    axes = {
        "logical_publication_rows": _projected_axis(
            axis="logical_publication_rows",
            base=int(publication["logical"]),
            delta=-retired,
            evidence=(
                "T27 opening logical minus the measured cruciblecraft:cooling expansion"
            ),
            hard_ceiling=eager_ceiling,
            kind="MEASURED_EXPANSION",
        ),
        "eager_publication_rows": _projected_axis(
            axis="eager_publication_rows",
            base=int(publication["eager"]),
            delta=-retired,
            evidence=(
                "Cooling rows are eager MaterialRule expansions, not Hybrid Extruder lazy"
            ),
            hard_ceiling=eager_ceiling,
            kind="MEASURED_EXPANSION",
        ),
        "lazy_logical_rows": _projected_axis(
            axis="lazy_logical_rows",
            base=int(publication["lazy"]),
            delta=0,
            evidence=(
                "STATIC_INFERENCE: cruciblecraft:cooling is absent from the EMI "
                "processing-map enumeration and is not a Hybrid Extruder family"
            ),
            hard_ceiling=int(budgets["lazy_logical_rows"]["hard_ceiling"]),
            kind="STATIC_INFERENCE",
        ),
        "datapack_authored_entries": _projected_axis(
            axis="datapack_authored_entries",
            base=int(
                ((opening.get("datapack") or {}).get("t14_authored_files") or {})["value"]
            ),
            delta=-1,
            evidence=(
                "One authored MaterialRule file will be deleted; confirm the T14 "
                "authored counter at R7 record"
            ),
            hard_ceiling=int(budgets["datapack_authored_entries"]["hard_ceiling"]),
            kind="PROJECTED_FILE_DELETION",
        ),
    }
    pending_evidence = (
        "T28 does not guess sync, lookup, reload, cache, or retained-memory "
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
        "cooling_expansion": expansion,
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(EVIDENCE): common.sha256_file(EVIDENCE),
                common.relative(OPENING): common.sha256_file(OPENING),
                common.relative(T10_PREFLIGHT): common.sha256_file(T10_PREFLIGHT),
            }
        },
        "freezer_in_card_scope": False,
        "generated_by": "python tools/build_t28_load_projection.py --write",
        "opening_publication": {
            "eager": int(publication["eager"]),
            "lazy": int(publication["lazy"]),
            "logical": int(publication["logical"]),
        },
        "product_decision": "strict_no_conversion",
        "publication_delta": {
            "eager": -retired,
            "lazy": 0,
            "logical": -retired,
        },
        "retirement": {
            "cooling_map": "retain_empty",
            "cooling_map_reason": (
                "Keep cruciblecraft:cooling as an empty leftover map; EMI already "
                "enumerates configured processing machines, not this map. Do not "
                "turn it into a generic cooler."
            ),
            "cooling_rule": "delete",
            "cool_if_ready": "delete_class_and_call",
            "keep": [
                "HotIngotProcessing.prepareOutputs",
                "HotIngotProcessing.initializeIfMissing",
                "MaterialContactHeat.damage",
                "ItemHeat.clearIfCooled",
                "MoldCastingRules.cool",
            ],
            "o36_replacement": {
                "backpack_keeps_ingot_hot_id": True,
                "cooling_rule_absent": True,
                "contact_heat_from_prefix_and_material": True,
            },
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T28_LOAD_PROJECTION",
        "t14_axes": axes,
        "tests": {
            "gametest": _pending_axis(
                axis="gametest",
                evidence="R4 will rewrite hotIngotSmeltsHurtsAndKeepsIdentity; count is measured at R6b",
                hard_ceiling=None,
            ),
            "junit": _pending_axis(
                axis="junit",
                evidence="R4 will rewrite cooling assertions; count is measured at R6a",
                hard_ceiling=None,
            ),
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
