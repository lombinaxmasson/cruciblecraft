#!/usr/bin/env python3
"""Read-only Bath MTE treatment/reconditioning source-object boundary audit."""
from __future__ import annotations

import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t45_common as common

OUTPUT = common.BATH_AUDIT


def build() -> dict[str, Any]:
    overlay = common.load_json(common.T42_OWNER_OVERLAY)
    blocker = common.load_json(common.T42_BLOCKER)
    by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    owners: Counter[str] = Counter()
    unique_kinds: Counter[str] = Counter()
    fluids = 0
    mte = 0
    reusable: list[str] = []
    blocked_examples: list[dict[str, Any]] = []
    for row in overlay.get("families") or []:
        if row.get("host") != "cruciblecraft:bath":
            continue
        family_id = str(row["family_id"])
        owner = str(row.get("current_owner") or "")
        owners[owner] += 1
        blocker_row = by_id.get(family_id) or {}
        kinds = list(blocker_row.get("unique_kinds") or [])
        for kind in kinds:
            unique_kinds[str(kind)] += 1
        if blocker_row.get("missing_fluids"):
            fluids += 1
        if "mte" in kinds:
            mte += 1
        if kinds == ["block"] and not blocker_row.get("missing_fluids"):
            reusable.append(family_id)
        if len(blocked_examples) < 8:
            blocked_examples.append(
                {
                    "current_owner": owner,
                    "family_id": family_id,
                    "missing_fluids": bool(blocker_row.get("missing_fluids")),
                    "unique_kinds": kinds,
                    "unique_objects": (blocker_row.get("unique_objects") or [])[:4],
                }
            )
    bath_count = sum(owners.values())
    return {
        "answers": {
            "must_not_enter_t45_lock": True,
            "ordinary_block_object_reusable": reusable,
            "ordinary_block_object_reusable_count": len(reusable),
            "rows_depending_on_mte_bath_fluid_or_treatment": bath_count - len(reusable),
        },
        "bath_family_count": bath_count,
        "blocked_examples": blocked_examples,
        "current_owner_counts": dict(sorted(owners.items())),
        "fluid_missing_count": fluids,
        "generated_by": "python tools/build_t45_bath_mte_audit.py",
        "mte_kind_count": mte,
        "note": (
            "Read-only diagnostic. Bath families stay on their current owners. "
            "None may be rewritten into object_expression/block to clear this audit."
        ),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T45_BATH_MTE_AUDIT",
        "unique_kind_counts": dict(sorted(unique_kinds.items())),
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Audit Bath MTE treatment/reconditioning boundaries",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
