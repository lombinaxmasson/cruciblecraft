#!/usr/bin/env python3
"""Classify every T49 remainder family as production, reclassified, or blocked."""
from __future__ import annotations

import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import recycling_candidate
from tools import t35_common as t35
from tools import wave_bath_tiny_purified as common

OUTPUT = common.CANDIDATE_SELECTION


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def _blocking_axes(
    family: dict[str, Any],
    relations: list[dict[str, Any]],
) -> list[str]:
    axes: set[str] = set()
    if not relations:
        axes.add("source_reconstruction")
        return sorted(axes)
    if any(relation.get("unsupported_semantics") for relation in relations):
        axes.add("unsupported_semantics")
    consume = 0
    missing_runtime = False
    unmapped_item = False
    unmapped_fluid = False
    leaked_source = False
    b0 = common.b0_identities()
    b0_open = False
    for relation in relations:
        for operand, count, action in zip(
            relation.get("item_inputs") or [],
            relation.get("item_input_counts") or [],
            relation.get("item_input_actions") or [],
            strict=False,
        ):
            kind = str((action or {}).get("kind") or "").lower()
            if kind == "consume" and int(count or 0) > 0:
                consume += 1
                runtime = operand.get("runtime_id")
                if runtime and f"item:{runtime}" not in b0 and f"fluid:{runtime}" not in b0:
                    b0_open = True
        for operand in _operands(relation):
            mapping = str(operand.get("mapping") or "")
            runtime = operand.get("runtime_id")
            source_row = operand.get("source") or {}
            if mapping == "blocked_unmapped" or not runtime:
                missing_runtime = True
                if source_row.get("fluid"):
                    unmapped_fluid = True
                elif source_row.get("item"):
                    unmapped_item = True
            elif str(runtime).startswith(("gregtech:", "gregapi:")):
                leaked_source = True
    if consume < 1:
        axes.add("consume_preserve_catalyst")
    if missing_runtime:
        axes.add("identity_or_operand")
    if unmapped_item:
        axes.add("object_identity")
    if unmapped_fluid:
        axes.add("fluid_identity")
    if leaked_source:
        axes.add("lossy_or_leaked_runtime")
    if b0_open:
        axes.add("b0_acquisition")
    return sorted(axes)


def _outcome(
    family: dict[str, Any],
    relations: list[dict[str, Any]],
    axes: list[str],
    *,
    recycling_flag: bool,
) -> tuple[str, str]:
    if len(relations) != int(family.get("expanded_count") or 0):
        return "blocked", "source_relation_count_mismatch"
    reconstruction = [
        axis
        for axis in axes
        if axis in {"source_reconstruction", "unsupported_semantics"}
    ]
    if reconstruction:
        return "blocked", ",".join(reconstruction)
    identity_open = [
        axis
        for axis in axes
        if axis
        in {
            "identity_or_operand",
            "object_identity",
            "fluid_identity",
            "material_form",
            "lossy_or_leaked_runtime",
            "consume_preserve_catalyst",
        }
    ]
    if identity_open:
        return "blocked", ",".join(identity_open)
    if recycling_flag:
        return "blocked", "recycling_candidate_unproven_not_R"
    if "b0_acquisition" in axes:
        return "production", "mapped_needs_b1_support"
    if axes:
        return "blocked", ",".join(axes)
    return "production", "all_source_relations_mapped_family_atomic"


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    source = common.load_json(common.SOURCE)
    if source.get("status") != "T49_BATH_SOURCE_FROZEN":
        raise ValueError("T49 candidate selection requires a frozen remainder source")
    if int((source.get("work_set") or {}).get("source_rows") or 0) != common.CANDIDATE_RELATION_COUNT:
        raise ValueError("T49 source is not the 5/95 remainder universe")
    overlay_by_id = recycling_candidate.overlay_by_id()
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in source.get("relations") or []:
        by_family[str(row["family_id"])].append(row)
    families: list[dict[str, Any]] = []
    outcomes: Counter[str] = Counter()
    seen: set[str] = set()
    for row in work_set.get("families") or []:
        family_id = str(row["family_id"])
        if family_id in seen:
            raise ValueError(f"duplicate remainder family {family_id}")
        seen.add(family_id)
        relations = by_family.get(family_id) or []
        axes = _blocking_axes(row, relations)
        recycling_flag = recycling_candidate.effective_recycling_candidate(
            family_id,
            overlay_by_id.get(family_id),
        )
        outcome, reason = _outcome(
            row,
            relations,
            axes,
            recycling_flag=recycling_flag,
        )
        outcomes[outcome] += 1
        families.append(
            {
                "blocking_axes": axes,
                "candidate_outcome": outcome,
                "candidate_reason": reason,
                "current_owner": row.get("current_owner"),
                "expanded_count": int(row.get("expanded_count") or 0),
                "family_id": family_id,
                "host": row.get("host"),
                "missing_fluid_identities": [],
                "missing_item_identities": [],
                "representation": row.get("representation"),
                "secondary_owner_tracks": row.get("secondary_owner_tracks") or [],
                "source_relation_count": len(relations),
                "template_key": row.get("template_key"),
                "unique_kinds": row.get("unique_kinds") or [],
            }
        )
    work_ids = list(work_set.get("family_ids") or [])
    if sorted(seen) != sorted(work_ids) or len(families) != common.CANDIDATE_FAMILY_COUNT:
        raise ValueError("T49 candidate outcomes do not cover the remainder universe exactly once")
    production_ids = [
        row["family_id"] for row in families if row["candidate_outcome"] == "production"
    ]
    if set(production_ids) != set(common.LOCK_FAMILY_IDS):
        raise ValueError("T49 candidate production is not the 5 Bath remainder families")
    if outcomes.get("blocked", 0) != 0 or outcomes.get("reclassified", 0) != 0:
        raise ValueError("T49 remainder candidate must be all production after correction")
    return {
        "coverage": {
            "blocked": outcomes.get("blocked", 0),
            "family_count": len(families),
            "production": outcomes.get("production", 0),
            "reclassified": outcomes.get("reclassified", 0),
        },
        "families": families,
        "generated_by": "python tools/build_t49_candidate_selection.py",
        "host": common.HOST,
        "note": (
            "Candidate outcomes use effective_recycling_candidate. "
            "T42 overlay recycling_candidate stays true."
        ),
        "production_authority": False,
        "production_family_ids": production_ids,
        "schema_version": 1,
        "selection_sha256": common.selection_sha256(work_ids),
        "size_exception": common.size_exception(),
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_CANDIDATE_SELECTION",
        "work_set_sha256": t35.sha256_file(common.WORK_SET),
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Classify T49 Bath remainder candidate outcomes",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
