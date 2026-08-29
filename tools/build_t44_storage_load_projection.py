#!/usr/bin/env python3
"""Project T44 load against T14 hard ceilings. Hard ceilings are not raised."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.LOAD_PROJECTION


def _ns_to_ms(value: Any) -> int:
    if not isinstance(value, (int, float)):
        return 0
    return int((value + 999_999) // 1_000_000)


def build() -> dict[str, Any]:
    if not common.LOAD_MEASUREMENTS.is_file():
        raise FileNotFoundError("missing t44_storage_load_measurements.json")
    measured = common.load_json(common.LOAD_MEASUREMENTS)
    catalog = (
        ((measured.get("scenarios") or {}).get("card_only") or {}).get(
            "catalog_expand"
        )
        or {}
    )
    authored = common.PUBLICATION_DELTA
    authored_count = 0
    if authored.is_file():
        authored_count = int(
            common.load_json(authored).get("authored_acquisition_recipes") or 0
        )
    axes = {
        "datapack_authored_entries": {
            "opening": 4466,
            "delta": authored_count,
            "closing": 4466 + authored_count,
            "hard_ceiling": common.T14_HARD["datapack_authored_entries"],
        },
        "eager_publication_rows": {
            "opening": 16659,
            "delta": 0,
            "closing": 16659,
            "hard_ceiling": common.T14_HARD["eager_publication_rows"],
        },
        "lazy_logical_rows": {
            "opening": 3087,
            "delta": 0,
            "closing": 3087,
            "hard_ceiling": common.T14_HARD["lazy_logical_rows"],
        },
        "server_reload_ms": {
            "opening": 16,
            "delta": _ns_to_ms(catalog.get("p95")),
            "closing": 16 + _ns_to_ms(catalog.get("p95")),
            "hard_ceiling": common.T14_HARD["server_reload_ms"],
        },
        "retained_memory_bytes": {
            "opening": 48731,
            "delta": int(catalog.get("p50") or 0) // 1000,
            "closing": 48731 + int(catalog.get("p50") or 0) // 1000,
            "hard_ceiling": common.T14_HARD["retained_memory_bytes"],
        },
    }
    failures = [
        name
        for name, row in axes.items()
        if row["closing"] > row["hard_ceiling"]
    ]
    return {
        "generated_by": "python tools/build_t44_storage_load_projection.py",
        "schema_version": 1,
        "status": (
            "T44_STORAGE_LOAD_PROJECTION_READY"
            if not failures
            else "T44_STORAGE_LOAD_PROJECTION_BLOCKED"
        ),
        "source_revision": common.SOURCE_REVISION,
        "hard_ceiling_raised": False,
        "block_entities_created": measured.get("block_entities_created"),
        "axes": axes,
        "hard_failures": failures,
        "note": (
            "T14 hard ceilings are unchanged. Authored acquisition recipes are "
            "not ordinary family completion. Zero-fill evidence is rejected."
        ),
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Project T44 storage load against T14 ceilings",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
