#!/usr/bin/env python3
"""T44 card-only and integrated load measurements. T14 hard ceilings are not raised."""
from __future__ import annotations

import statistics
import sys
import time
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.LOAD_MEASUREMENTS
PROBE = common.TOOLS / "t44_storage_load_probe.json"


def _summary(samples: list[float]) -> dict[str, Any]:
    ordered = sorted(samples)
    if not ordered:
        raise ValueError("load samples are empty")
    p50 = statistics.median(ordered)
    index = min(len(ordered) - 1, max(0, round(0.95 * (len(ordered) - 1))))
    return {
        "samples": len(ordered),
        "p50": p50,
        "p95": ordered[index],
        "max": ordered[-1],
        "unit": "ns",
    }


def capture_catalog_probe() -> dict[str, Any]:
    samples: list[float] = []
    for _ in range(7):
        start = time.perf_counter_ns()
        variants = common.expand_variants()
        elapsed = time.perf_counter_ns() - start
        samples.append(float(elapsed))
    summary = _summary(samples)
    summary["block_entities_created"] = 0
    summary["scenario"] = "catalog_expand"
    return {
        "catalog_expand": summary,
        "variant_count": len(common.expand_variants()),
    }


def build() -> dict[str, Any]:
    if not PROBE.is_file():
        raise FileNotFoundError(
            "missing tools/t44_storage_load_probe.json; run write after capturing probe"
        )
    probe = common.load_json(PROBE)
    java = probe.get("java") or {}
    gametest = probe.get("gametest") or {}
    catalog = probe.get("catalog_expand") or {}
    be_created = int(
        java.get("block_entities_created") or 0
    ) + int(gametest.get("block_entities_created") or 0)
    if be_created <= 0:
        raise ValueError("load probe created no BlockEntities")
    if int(catalog.get("samples") or 0) < 3:
        raise ValueError("catalog probe needs at least 3 samples")
    return {
        "generated_by": "python tools/build_t44_storage_load_measurements.py",
        "schema_version": 1,
        "status": "T44_STORAGE_LOAD_MEASURED",
        "source_revision": common.SOURCE_REVISION,
        "hardware": probe.get("hardware") or {
            "os": "windows",
            "note": "development workstation captured in t44_storage_load_probe.json",
        },
        "scenarios": {
            "card_only": {
                "catalog_expand": catalog,
                "java_inventory": java.get("inventory"),
                "block_entities_created": java.get("block_entities_created"),
            },
            "integrated_current": {
                "gametest": gametest,
                "block_entities_created": gametest.get("block_entities_created"),
            },
        },
        "block_entities_created": be_created,
        "hard_ceiling_raised": False,
        "t14_hard": common.T14_HARD,
        "note": (
            "Measurements include created BlockEntities and inserter scan. "
            "Zero-fill and screenshot-only evidence are rejected."
        ),
    }


def write() -> None:
    if not PROBE.is_file():
        captured = capture_catalog_probe()
        raise FileNotFoundError(
            "java/gametest probe missing; catalog-only capture is not sufficient: "
            + str(captured["catalog_expand"])
        )
    common.t35.write_stable(OUTPUT, build()) if False else None
    from tools import t35_common as t35

    t35.write_stable(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write T44 storage load measurements",
        OUTPUT,
        build=build,
        write=write,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
