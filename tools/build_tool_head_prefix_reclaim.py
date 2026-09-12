#!/usr/bin/env python3
"""Build the source-backed GT6 sharpener tool-head reclaim overlay."""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io
from tools import tool_head_prefix as thp

RAW_DUMP = (
    ROOT
    / "gt6_dump"
    / "gt6_recipe_dump"
    / "maps"
    / "gt.recipe.sharpener.json"
)
SOURCE = thp.TOOL_HEAD_RECLAIM_SOURCE


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build_source() -> dict[str, Any]:
    dump = io.load_json(RAW_DUMP)
    identities: set[tuple[str, int]] = set()
    for recipe in dump.get("recipes") or []:
        for side in ("inputs", "outputs"):
            for item in recipe.get(side) or []:
                source_item = str(item.get("item") or "")
                meta = item.get("meta")
                if thp.gt_prefix_name(source_item) and isinstance(meta, int):
                    identities.add((source_item, meta))
    rows = [
        {
            "kind": "tool_head",
            "meta": meta,
            "source_item": source_item,
        }
        for source_item, meta in sorted(identities)
    ]
    return {
        "counts": {"identity_count": len(rows)},
        "generated_by": "python tools/build_tool_head_prefix_reclaim.py",
        "identities": rows,
        "schema_version": 1,
        "source": {
            "path": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.sharpener.json",
            "sha256": sha256(RAW_DUMP),
        },
        "source_revision": io.SOURCE_REVISION,
        "status": "TOOL_HEAD_PREFIX_RECLAIM_SOURCE",
    }


def expected_outputs() -> dict[Path, dict[str, Any]]:
    source = build_source()
    io.write_stable(SOURCE, source)
    thp.clear_cache()
    remap = thp.build_remap()
    return {
        SOURCE: source,
        thp.REMAP_TOOLS: remap,
        thp.REQUIRED_FORMS: thp.required_forms_document(remap),
        thp.BUNDLED_REMAP: remap,
    }


def write() -> None:
    outputs = expected_outputs()
    for path, document in outputs.items():
        io.write_stable(path, document)
    thp.write_prefix_json(outputs[thp.REMAP_TOOLS])
    thp.write_zh_prefix_names(outputs[thp.REMAP_TOOLS])
    thp.copy_source_backed_textures(outputs[thp.REMAP_TOOLS])


def check() -> list[str]:
    source = build_source()
    thp.clear_cache()
    remap = thp.build_remap()
    expected = {
        SOURCE: source,
        thp.REMAP_TOOLS: remap,
        thp.REQUIRED_FORMS: thp.required_forms_document(remap),
        thp.BUNDLED_REMAP: remap,
    }
    errors: list[str] = []
    for path, document in expected.items():
        expected_text = io.stable_json(document)
        actual_text = path.read_text(encoding="utf-8") if path.is_file() else ""
        if actual_text != expected_text:
            errors.append(io.stale_error(path, expected_text, actual_text))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        write()
        print("wrote tool-head prefix reclaim")
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("tool-head prefix reclaim is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
