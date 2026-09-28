#!/usr/bin/env python3
"""Freeze the current GT6 material-form openable set for a later active card."""
from __future__ import annotations

import argparse
import json
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
CENSUS = ROOT / "tools" / "waves" / "prep" / "material-form-demand-census" / "census.json"
OUTPUT = ROOT / "tools" / "waves" / "prep" / "gt6-form-open-followup" / "required_forms.json"
GATE = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "material_registration_gate.json"
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def build() -> dict[str, Any]:
    census = json.loads(CENSUS.read_text(encoding="utf-8"))
    openable = [{"material": "magic", "form": "small_casing"}]
    required: dict[str, set[str]] = defaultdict(set)
    for row in openable:
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
            "tools/waves/prep/gt6-form-open-followup/build.py "
            "from material-form-demand-census/census.json"
        ),
        "note": (
            "Standard-gate slice only: magic/small_casing. Metadata-only "
            "container forms require a separate Java registration path."
        ),
        "required_forms": required_forms,
        "source_revision": SOURCE_REVISION,
        "source_census": (
            "tools/waves/prep/material-form-demand-census/census.json"
        ),
        "status": "PREP_BOUNDED_FORM_OPEN",
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
    if expected.get("status") == "LANDED_BOUNDED_FORM_OPEN":
        gate = json.loads(GATE.read_text(encoding="utf-8"))
        landed = gate.get("gt6_form_open_followup_required_forms") or {}
        for material, forms in (expected.get("required_forms") or {}).items():
            if not set(forms) <= set(landed.get(material) or []):
                raise SystemExit(
                    f"landed form gate is missing {material}/{sorted(set(forms) - set(landed.get(material) or []))}"
                )
        print("bounded form-open landing artifact is current")
        return 0
    if expected != actual:
        raise SystemExit("bounded form-open prep artifact is stale")
    print("bounded form-open prep artifact is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
