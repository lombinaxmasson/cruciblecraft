#!/usr/bin/env python3
"""Classify T41 operands after the strict runtime overlay."""
from __future__ import annotations

import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t41_common as common  # noqa: E402

OUTPUT = common.OPERAND_DISPOSITION


def _classification(operand: dict[str, Any], combinatorial: bool) -> tuple[str, str]:
    runtime_id = operand.get("runtime_id")
    if combinatorial and not runtime_id:
        return "phase_deferred", "combinatorial_player_path_unproven"
    if not runtime_id:
        return "needs_current_expression", "empty_runtime_id"
    try:
        common.assert_runtime_id(str(runtime_id), consume=False)
    except ValueError:
        return "needs_current_expression", "runtime_id_not_current"
    klass = str(operand.get("t41_class") or "")
    if klass == "SOURCE_BACKED":
        return "proven_equivalent", "source_backed_gt_wood_item"
    if operand.get("t41_component") == common.FIREPROOF_COMPONENT:
        return "proven_equivalent", "species_preserving_fireproof_component"
    if operand.get("t41_component") == common.CIRCUIT_CONFIG:
        return "proven_equivalent", "programmed_circuit_config"
    return "proven_equivalent", "vanilla_or_cc_identity"


def build() -> dict[str, Any]:
    source = t35.load_json(common.SOURCE)
    production = set()
    if common.PRODUCTION_LOCK.is_file():
        production = set(common.production_family_ids())
    rows: list[dict[str, Any]] = []
    family_statuses: dict[str, set[str]] = {}
    for relation in source.get("relations") or []:
        family_id = str(relation["family_id"])
        combinatorial = relation["template_key"] in common.COMBINATORIAL_TEMPLATE_KEYS
        for side in ("item_inputs", "item_outputs"):
            for index, operand in enumerate(relation.get(side) or []):
                disposition, reason = _classification(operand, combinatorial)
                family_statuses.setdefault(family_id, set()).add(disposition)
                rows.append({
                    "disposition": disposition,
                    "family_id": family_id,
                    "future_owner": (
                        "later:assembler_combinatorial"
                        if disposition == "phase_deferred"
                        else None
                    ),
                    "operand_index": index,
                    "reason": reason,
                    "side": side,
                    "source": operand.get("source") or {},
                    "template_key": relation["template_key"],
                })
    counts = Counter(row["disposition"] for row in rows)
    production_blockers = sorted(
        family_id
        for family_id, statuses in family_statuses.items()
        if family_id in production
        and statuses - {"proven_equivalent"}
    )
    if production_blockers:
        raise ValueError(
            "T41 production families still have operand blockers: "
            + ", ".join(production_blockers[:20])
        )
    return {
        "counts": dict(sorted(counts.items())),
        "generated_by": "python tools/build_t41_operand_disposition.py",
        "operand_count": len(rows),
        "production_family_count": len(production),
        "rows": rows,
        "schema_version": 1,
        "status": "T41_OPERAND_DISPOSITION",
    }


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Classify T41 assembler operands", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        document = build()
        if args.check:
            errors = common.check_document(OUTPUT, document)
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("T41 operand disposition is current.")
            return 0
        t35.write_stable(OUTPUT, document)
        print(f"Wrote T41 operand disposition ({document['operand_count']} rows).")
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"T41 operand disposition failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
