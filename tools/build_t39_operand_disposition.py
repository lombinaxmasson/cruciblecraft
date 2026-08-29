#!/usr/bin/env python3
"""Classify every non-direct T39 operand without manufacturing equivalence."""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t39_common as t39  # noqa: E402

OUTPUT = t39.OPERAND_DISPOSITION
FUEL_ROD_METAS = {9441, 9319, 9329, 9339, 9349, 9360, 9361}


def _classification(operand: dict[str, Any]) -> tuple[str, str]:
    source = operand.get("source") or {}
    item = source.get("item")
    meta = source.get("meta")
    fluid = str(source.get("fluid") or source.get("id") or "")
    if item == "gregtech:gt.multitileentity" and meta in FUEL_ROD_METAS:
        return "phase_deferred", "post_1x:nuclear"
    if item == "minecraft:dye" and meta == 15:
        return "proven_equivalent", "legacy_id_translation"
    if isinstance(fluid, str) and "pahoehoe" in fluid.lower():
        return "unsupported", "pahoehoe_lava_identity_collapse"
    if t39.source_operand_is_unproven_lossy_alias(operand):
        return "needs_current_expression", "hosted_or_stateful_alias"
    return "proven_equivalent", "registered_material_or_fluid_translation"


def build() -> dict[str, Any]:
    source = t35.load_json(t39.SOURCE)
    production = set(t39.production_family_ids())
    rows: list[dict[str, Any]] = []
    family_statuses: dict[str, set[str]] = {}
    for relation in source.get("relations") or []:
        family_id = str(relation["family_id"])
        template_key = str(relation["template_key"])
        for side in ("item_inputs", "item_outputs", "fluid_inputs", "fluid_outputs"):
            for index, operand in enumerate(relation.get(side) or []):
                if not isinstance(operand, dict):
                    continue
                if (
                    operand.get("mapping") != "source_derived_alias"
                    and not t39.source_operand_is_unproven_lossy_alias(operand)
                ):
                    continue
                disposition, reason = _classification(operand)
                source_operand = operand.get("source") or {}
                rows.append({
                    "disposition": disposition,
                    "family_id": family_id,
                    "future_owner": (
                        "post_1x:nuclear" if disposition == "phase_deferred" else None
                    ),
                    "operand_index": index,
                    "reason": reason,
                    "side": side,
                    "source": source_operand,
                    "stable_id": str(relation["stable_id"]),
                    "template_key": template_key,
                })
                family_statuses.setdefault(family_id, set()).add(disposition)
    rows.sort(key=lambda row: (
        row["template_key"], row["stable_id"], row["side"], row["operand_index"]
    ))
    counts = Counter(row["disposition"] for row in rows)
    blocked_production = sorted(
        family_id
        for family_id, statuses in family_statuses.items()
        if family_id in production
        and statuses & {"phase_deferred", "needs_current_expression", "unsupported"}
    )
    if blocked_production:
        raise ValueError(
            "T39 production lock contains unresolved operands: "
            + ", ".join(blocked_production)
        )
    return {
        "schema_version": 1,
        "status": "T39_OPERAND_DISPOSITION_REVIEWED",
        "source_revision": t39.SOURCE_REVISION,
        "catalog": {
            "families": t39.CATALOG_FAMILY_COUNT,
            "relations": t39.CATALOG_RELATION_COUNT,
        },
        "production_lock_sha256": t39.production_lock_sha256(),
        "counts": dict(sorted(counts.items())),
        "blocked_production_families": blocked_production,
        "rows": rows,
        "note": (
            "A runtime id is not semantic equivalence. Stateful reactor rods "
            "are deferred to nuclear; hosted/state collapses remain unresolved."
        ),
    }


def main(argv: list[str] | None = None) -> int:
    args = t39.parse_write_check(__doc__, argv)
    document = build()
    if args.write:
        t35.write_stable(OUTPUT, document)
        return 0
    errors = t39.check_document(OUTPUT, document)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
