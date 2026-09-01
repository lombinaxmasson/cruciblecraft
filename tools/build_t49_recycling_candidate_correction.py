#!/usr/bin/env python3
"""Freeze the T49 append-only recycling_candidate correction. Does not rewrite T42 overlay."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import recycling_candidate
from tools import t35_common as t35
from tools import t42_common as t42
from tools import t47_common as t47

OUTPUT = recycling_candidate.CORRECTION_PATH

BATH_TINY = (
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0072",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0098",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0107",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0110",
    "portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0188",
)
CENTRIFUGE_SANDS = (
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0085",
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0086",
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0087",
)
CENTRIFUGE_FOOD = (
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0092",
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0093",
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0095",
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0105",
    "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#0109",
)
EXPECTED_IDS = frozenset(BATH_TINY + CENTRIFUGE_SANDS + CENTRIFUGE_FOOD)

REASONS = {
    **{
        family_id: {
            "reason": "heuristic_unique_object_now_material_form",
            "unique_kind": "gt_prefix:crushedPurifiedTiny",
            "lock_this_wave": True,
            "note": (
                "T48 registered tiny_washed_crushed_ore. Sibling full-size "
                "chemical-refine families are already production-locked."
            ),
        }
        for family_id in BATH_TINY
    },
    **{
        family_id: {
            "reason": "heuristic_unique_object_is_not_mte_recovery",
            "unique_kind": "block",
            "lock_this_wave": False,
            "note": "gt.block.sands centrifuge. T45 excluded these for the overlay flag.",
        }
        for family_id in CENTRIFUGE_SANDS
    },
    **{
        family_id: {
            "reason": "heuristic_unique_object_is_not_mte_recovery",
            "unique_kind": "multiitem",
            "lock_this_wave": False,
            "note": "gt.multiitem.food centrifuge. Still needs multiitem identity.",
        }
        for family_id in CENTRIFUGE_FOOD
    },
}


def build() -> dict[str, Any]:
    overlay = t35.load_json(t42.BLOCKER_OVERLAY)
    flagged = {
        str(row["family_id"]): row
        for row in overlay.get("families") or []
        if row.get("recycling_candidate")
    }
    if set(flagged) != EXPECTED_IDS:
        raise ValueError(
            "T42 overlay recycling_candidate set drifted: "
            f"got {sorted(flagged)} expected {sorted(EXPECTED_IDS)}"
        )
    families = []
    for family_id in sorted(EXPECTED_IDS):
        row = flagged[family_id]
        extra = REASONS[family_id]
        families.append(
            {
                "family_id": family_id,
                "host": row.get("host") or row.get("cc_host_map"),
                "lock_this_wave": extra["lock_this_wave"],
                "overlay_recycling_candidate": True,
                "reason": extra["reason"],
                "recycling_candidate": False,
                "template_key": row.get("template_key"),
                "unique_kind": extra["unique_kind"],
                "unique_kinds": list(row.get("unique_kinds") or []),
                "note": extra["note"],
            }
        )
    return {
        "deferred_recycling_untouched": 1817,
        "family_count": len(families),
        "families": families,
        "generated_by": "python tools/build_t49_recycling_candidate_correction.py",
        "lock_family_ids": list(BATH_TINY),
        "note": (
            "Append-only. T42 overlay recycling_candidate stays true. "
            "effective_recycling_candidate is false for these 13 families."
        ),
        "overlay_path": "tools/t42_blocker_overlay.json",
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "T49_RECYCLING_CANDIDATE_CORRECTION",
        "t42_overlay_unmodified": True,
    }


def main(argv: list[str] | None = None) -> int:
    return t47.run_managed(
        "Freeze T49 recycling_candidate correction",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
