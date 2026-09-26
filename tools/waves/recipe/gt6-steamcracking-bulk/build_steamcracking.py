#!/usr/bin/env python3
"""Import the whole gt.recipe.steamcracking dump onto the live steam cracker.

Follow-up of the chemical-misc bulk card. The steam cracker host landed with
the basic-machine batch, so this wave publishes every host-accepted translated
row. Missing fluids stay blocked. published + existing + blocked = source.
"""
from __future__ import annotations

import importlib.util
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
WAVE = ROOT / "tools" / "waves" / "recipe" / "gt6-steamcracking-bulk"
SAMPLES = ROOT / "src" / "test" / "resources" / "gt6_steamcracking_samples.json"
CHEM_PATH = (
    ROOT / "tools" / "waves" / "recipe" / "gt6-chemical-misc-bulk" / "build_chemical_misc.py"
)
SLUG = "steam-cracker/steamcracking"
COHORT = "steamcracking"
# Tier-1 steel steam cracker accepts at most 64 HU/t.
TIER_ONE_EUT = 64
GENERATOR = "tools/waves/recipe/gt6-steamcracking-bulk/build_steamcracking.py"

for entry in (ROOT, TOOLS):
    if str(entry) not in sys.path:
        sys.path.insert(0, str(entry))

from tools import census_common as census  # noqa: E402


def load_chemical_misc():
    spec = importlib.util.spec_from_file_location("build_chemical_misc", CHEM_PATH)
    if spec is None or spec.loader is None:
        raise SystemExit(f"cannot load {CHEM_PATH}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.WAVE = WAVE
    module.COHORT = COHORT
    module.SAMPLES = SAMPLES
    module._slug = lambda _spec: SLUG
    return module


def steam_spec(chem) -> dict[str, Any]:
    return {
        "dump": "gt.recipe.steamcracking",
        "machine": "steam_cracker",
        "block": "cruciblecraft:steam_cracker",
        "validators": (
            (
                "steam_cracker",
                chem.chemical(
                    1,
                    3,
                    2,
                    9,
                    64_000,
                    2_147_483_647,
                    preserve=True,
                    max_eut=4096,
                ),
            ),
        ),
    }


def patch_lock() -> None:
    path = WAVE / "steam_cracker" / "production_lock.json"
    if not path.is_file():
        return
    lock = json.loads(path.read_text(encoding="utf-8"))
    lock["generated_by"] = GENERATOR
    lock["note"] = (
        f"live compile for {SLUG}; host-accepted translated steamcracking rows; "
        "missing fluids stay blocked; on-demand matrix publication"
    )
    census.write_stable(path, lock)


def prefer_tier_one(chem, sample: dict[str, Any] | None) -> dict[str, Any] | None:
    """Keep a sample the steel steam cracker can actually run."""
    if sample is None or not sample.get("samples"):
        return sample
    current = sample["samples"][0]
    if int(current.get("eut") or 0) <= TIER_ONE_EUT and current.get("execute"):
        return sample
    source = json.loads((WAVE / "steam_cracker" / "source.json").read_text(encoding="utf-8"))
    pool = [row for row in source["relations"] if chem._simple(row)]
    pool = [
        row
        for row in pool
        if 0 < int(row.get("eut") or 0) <= TIER_ONE_EUT
        and 0 < int(row.get("duration") or 0) <= chem.SAMPLE_MAX_TICKS
    ]
    pool.sort(key=lambda row: (int(row.get("duration") or 0), int(row.get("source_recipe_index") or 0)))
    if not pool:
        return sample
    picked = chem._sample("short", pool[0], sample["block"])
    sample["samples"] = [picked]
    return sample


def write_samples(sample: dict[str, Any] | None, counts: dict[str, int]) -> None:
    payload = {
        "maps": [sample] if sample and sample.get("samples") else [],
        "published_rows": counts["published"],
        "schema_version": 1,
        "source_rows": counts["source"],
    }
    census.write_stable(SAMPLES, payload)
    census.write_stable(WAVE / "samples.json", payload)
    print(f"samples {sum(len(row['samples']) for row in payload['maps'])}", flush=True)


def summarize_blocked() -> None:
    path = WAVE / "steam_cracker" / "blocked.json"
    if not path.is_file():
        return
    rows = json.loads(path.read_text(encoding="utf-8"))["rows"]
    counts = Counter(str(row["reason"]).split(" ", 1)[0] for row in rows)
    for reason, count in counts.most_common(12):
        print(f"blocked {count} {reason}", flush=True)


def main() -> None:
    print("loading chemical-misc selector", flush=True)
    chem = load_chemical_misc()
    spec = steam_spec(chem)
    print("loading translator maps", flush=True)
    maps = chem.coverage.translator_maps()
    print("indexing live steam_cracker recipes", flush=True)
    live = chem.live_index({spec["machine"]})
    print("selecting steamcracking rows", flush=True)
    counts = chem.select_map(spec, maps, live[spec["machine"]])
    sample = chem.finish_map(spec, counts)
    patch_lock()
    write_samples(prefer_tier_one(chem, sample), counts)
    summarize_blocked()
    print(
        "steamcracking "
        f"source {counts['source']} published {counts['published']} "
        f"existing {counts['existing']} blocked {counts['blocked']} "
        f"families {counts['families']}",
        flush=True,
    )


if __name__ == "__main__":
    main()
