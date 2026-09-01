#!/usr/bin/env python3
"""Read-only analyzer: cluster remaining gap by resolver/template holes."""
from __future__ import annotations

from collections import Counter
from typing import Any

from tools import recycling_candidate
from tools import t45_common as common


def analyze() -> dict[str, Any]:
    overlay = common.load_json(common.T42_OWNER_OVERLAY)
    blocker = common.load_json(common.T42_BLOCKER)
    by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    clusters: Counter[str] = Counter()
    unresolved_hosts: Counter[str] = Counter()
    for row in overlay.get("families") or []:
        owner = str(row.get("current_owner") or "")
        host = str(row.get("host") or "")
        blocker_row = by_id.get(str(row.get("family_id") or ""), {})
        kinds = tuple(blocker_row.get("unique_kinds") or [])
        if owner == "object_expression/block":
            cluster = "block_object_resolver"
        elif owner.startswith("object_expression/"):
            cluster = "object_expression_other"
        elif owner.startswith("material_expression/"):
            cluster = "material_form_or_fluid"
        elif owner.startswith("recycling/"):
            cluster = "recycling"
        elif owner.startswith("coordination/"):
            cluster = "multi_axis"
        elif owner.startswith("recipe_wave/"):
            cluster = "ready_wave_residual"
        else:
            cluster = owner or "unclassified"
        if kinds == ("block",) and recycling_candidate.effective_recycling_candidate(
            str(row.get("family_id") or ""),
            blocker_row,
        ):
            cluster = "block_tagged_recycling"
        clusters[cluster] += 1
        unresolved_hosts[host] += 1
    return {
        "clusters": dict(sorted(clusters.items())),
        "family_count": int(overlay.get("family_count") or 0),
        "generated_by": "python tools/build_recipe_bulk.py analyze",
        "hosts": dict(sorted(unresolved_hosts.items())),
        "note": "Analyze is diagnostic only. It must not select production families.",
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "schema_version": 1,
        "status": "T45_ANALYZE_REPORT",
    }
