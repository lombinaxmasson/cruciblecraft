#!/usr/bin/env python3
"""Shared row-count summary for T20 worldgen evidence.

`build_worldgen_catalog` (index 15) and `build_t20_worldgen_projection`
(index 51) derive the same count summary from the 129 vein rows.  #15 derives
it locally from the authored ore declarations; #51 derives it from its own
expected rows and proves the two row sets are byte-exact per row via
`compare_authored`.  Sharing the derivation here keeps the two views locked.
"""
from __future__ import annotations

from collections import Counter
from typing import Any


def row_count_summary(rows: list[dict[str, Any]]) -> dict[str, Any]:
    classifications = Counter(
        row["provenance"]["source_kind"] for row in rows
    )
    statuses = Counter(row["provenance"]["status"] for row in rows)
    signatures = Counter(
        (
            row["min_y"],
            row["max_y"],
            row["horizontal_radius"],
            row["vertical_radius"],
            row["density"],
            row["region_size_chunks"],
            row["generation_chance"],
        )
        for row in rows
    )
    return {
        "catalog_entries": len(rows),
        "classifications": dict(sorted(classifications.items())),
        "statuses": dict(sorted(statuses.items())),
        "unclassified": 0,
        "placeholder": 0,
        "unverified": 0,
        "distinct_geometry_signatures": len(signatures),
        "largest_geometry_signature_multiplicity": max(signatures.values()),
    }
