#!/usr/bin/env python3
"""Freeze metadata-only container-form demand for a later Java-owned card."""
from __future__ import annotations

import argparse
import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
CENSUS = ROOT / "tools/waves/prep/material-form-demand-census/census.json"
MATERIAL_INDEX = ROOT / "src/main/resources/data/cruciblecraft/materials/index.json"
MATERIAL_ROOT = MATERIAL_INDEX.parent
OUTPUT = ROOT / "tools/waves/prep/gt6-container-chem-tube-forms/required_forms.json"
FORMS = {"chem_tube", "arrow_gt_plastic", "arrow_gt_wood"}
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def load_metadata_only() -> set[str]:
    materials = set()
    for filename in json.loads(MATERIAL_INDEX.read_text(encoding="utf-8")):
        document = json.loads(
            (MATERIAL_ROOT / filename).read_text(encoding="utf-8")
        )
        if document.get("metadata_only"):
            materials.add(str(document["id"]))
    return materials


def build() -> dict:
    metadata_only = load_metadata_only()
    census = json.loads(CENSUS.read_text(encoding="utf-8"))
    required: dict[str, set[str]] = defaultdict(set)
    for row in census.get("openable") or []:
        if row.get("material") in metadata_only and row.get("form") in FORMS:
            required[str(row["material"])].add(str(row["form"]))
    required_forms = {
        material: sorted(forms)
        for material, forms in sorted(required.items())
    }
    return {
        "counts": {
            "required_form_pairs": sum(map(len, required_forms.values())),
            "required_materials": len(required_forms),
        },
        "generated_by": (
            "tools/waves/prep/gt6-container-chem-tube-forms/"
            "build_from_census.py"
        ),
        "note": (
            "Prep-only. These metadata_only materials cannot enter the normal "
            "material_registration_gate; a Java container-form registration "
            "path is required. No stand-in and no generation-flag expansion."
        ),
        "required_forms": required_forms,
        "source_revision": SOURCE_REVISION,
        "source_census": (
            "tools/waves/prep/material-form-demand-census/census.json"
        ),
        "status": "PREP_METADATA_ONLY_CONTAINER_FORMS",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    actual = build()
    if args.write:
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        OUTPUT.write_text(
            json.dumps(actual, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"Wrote {OUTPUT}")
        return 0
    if not OUTPUT.is_file():
        raise SystemExit(f"missing {OUTPUT}")
    expected = json.loads(OUTPUT.read_text(encoding="utf-8"))
    if expected != actual:
        raise SystemExit("metadata-only container-form prep artifact is stale")
    print("metadata-only container-form prep artifact is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
