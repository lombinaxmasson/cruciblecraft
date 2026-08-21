#!/usr/bin/env python3
"""Build the T29 publication-delta ledger from the load projection.

GT RecipeMap publication stays 0/0/0. Measured copies the projection once
the large-crucible controller, structure JSON and vanilla recipe exist.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t29_publication_delta.json"
BUILDER = Path(__file__).resolve()
PROJECTION = TOOLS / "t29_load_projection.json"
BLOCK = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/block/LargeCrucibleBlock.java"
)
STRUCTURE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/multiblock_structures/large_crucible.json"
)
RECIPE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/recipe/machines/large_crucible.json"
)


def build() -> dict[str, Any]:
    if not PROJECTION.is_file():
        raise FileNotFoundError(common.relative(PROJECTION))
    projection = common.load_json(PROJECTION)
    projected = projection["publication_delta"]
    if projected != {"eager": 0, "lazy": 0, "logical": 0}:
        raise ValueError("T29 projected GT publication delta must be 0/0/0")
    measured_ready = BLOCK.is_file() and STRUCTURE.is_file() and RECIPE.is_file()
    measured = {
        "eager": 0 if measured_ready else None,
        "lazy": 0 if measured_ready else None,
        "logical": 0 if measured_ready else None,
        "status": "measured" if measured_ready else "pending",
        "verdict": None if measured_ready else common.PENDING_LOAD_VERDICT,
    }
    if measured["status"] == "pending" and any(
        measured[key] == 0 for key in ("eager", "logical", "lazy")
    ):
        raise ValueError("pending measured publication delta must not be filled with 0")
    return {
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(PROJECTION): common.sha256_file(PROJECTION),
            }
        },
        "generated_by": "python tools/build_t29_publication_delta.py --write",
        "measured": measured,
        "opening_publication": projection["opening_publication"],
        "projected": projected,
        "registration_present": measured_ready,
        "schema_version": 1,
        "same_sign_as_projection": measured_ready,
        "source_revision": common.SOURCE_REVISION,
        "status": "T29_PUBLICATION_DELTA",
        "vanilla_crafting": projection["vanilla_crafting"],
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    if common.stable_json(build()) != OUTPUT.read_text(encoding="utf-8"):
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"projected={document['projected']} "
                f"measured={document['measured']['status']}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
