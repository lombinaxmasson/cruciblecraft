#!/usr/bin/env python3
"""Build the source-backed non-tool-head forms used by GT6 sharpener."""
from __future__ import annotations

import argparse
import hashlib
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io
from tools.recipe_bulk.dialects import gt6

RAW_DUMP = (
    ROOT
    / "gt6_dump"
    / "gt6_recipe_dump"
    / "maps"
    / "gt.recipe.sharpener.json"
)
OUTPUT = (
    ROOT
    / "tools"
    / "waves"
    / "machines"
    / "sanding"
    / "required_forms.json"
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build() -> dict[str, Any]:
    dump = io.load_json(RAW_DUMP)
    maps = gt6._maps()
    required: dict[str, set[str]] = defaultdict(set)
    for index, recipe in enumerate(dump.get("recipes") or []):
        relation, _errors = gt6.compile_row(
            recipe,
            host="cruciblecraft:sanding",
            target_map="cruciblecraft:sanding",
            source_map="gt.recipe.sharpener",
            family_id=(
                "portfolio:track_a/cruciblecraft:sanding/"
                "gt.recipe.sharpener#0000"
            ),
            template_key="gt.recipe.sharpener#0000",
            recipe_index=index,
            shadow_order=0,
            source_revision=io.SOURCE_REVISION,
            source_row_sha256="",
            maps=maps,
        )
        for side in ("item_inputs", "item_outputs"):
            for operand in relation.get(side) or []:
                material = str(operand.get("material") or "")
                form = str(operand.get("form") or "")
                if material and form and not form.startswith("tool_head_"):
                    required[material].add(form)
    form_ids = sorted({form for forms in required.values() for form in forms})
    return {
        "counts": {
            "material_count": len(required),
            "form_pairs": sum(map(len, required.values())),
            "form_count": len(form_ids),
        },
        "generated_by": "python tools/build_sanding_reclaim_forms.py",
        "form_ids": form_ids,
        "required_forms": {
            material: sorted(forms)
            for material, forms in sorted(required.items())
        },
        "schema_version": 1,
        "source": {
            "path": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.sharpener.json",
            "sha256": sha256(RAW_DUMP),
        },
        "source_revision": io.SOURCE_REVISION,
        "status": "SANDING_REQUIRED_FORMS",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    document = build()
    expected = io.stable_json(document)
    actual = OUTPUT.read_text(encoding="utf-8") if OUTPUT.is_file() else ""
    if args.write:
        io.write_stable(OUTPUT, document)
        print(f"wrote {OUTPUT}")
        return 0
    if actual != expected:
        print(io.stale_error(OUTPUT, expected, actual), file=sys.stderr)
        return 1
    print("sanding reclaim forms are current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
