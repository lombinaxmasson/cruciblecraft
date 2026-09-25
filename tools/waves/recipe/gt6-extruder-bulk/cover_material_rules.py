#!/usr/bin/env python3
"""Retire extruder material_rule files once every sparse row is a published GT6 row or not GT6.

Keys compare runtime item ids, so vanilla ``form_items`` and operands without material/form
metadata still match. A rule row with no identical published row has no translatable GT6
counterpart (different mold, amounts, or timing) and is recorded in ``material_rule_retired.json``.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(ROOT))
sys.path.insert(1, str(ROOT / "tools"))
from tools import census_common as census
from tools import compare_gt6_recipes as comparator

WAVE = ROOT / "tools/waves/recipe/gt6-extruder-bulk"
RULES = (
    ROOT
    / "src/component_rule_generated/resources/data/cruciblecraft/recipe/extruder/compact"
)
MATERIALS = comparator.cached_cc_materials()
DRY_RUN = "--dry-run" in sys.argv


def _runtime(material: str, prefix: str) -> str:
    override = (MATERIALS.get(material, {}).get("form_items") or {}).get(prefix)
    return str(override) if override else f"cruciblecraft:{material}/{prefix}"


def _operand(operand: dict) -> str:
    return str(operand.get("runtime_id") or operand.get("value") or "")


source = json.loads((WAVE / "source.json").read_text(encoding="utf-8"))
keys: set[tuple] = set()
for relation in source["relations"]:
    inputs = relation["item_inputs"]
    counts = relation.get("item_input_counts") or []
    outputs = relation["item_outputs"]
    if len(inputs) != 2 or len(outputs) != 1:
        continue
    keys.add(
        (
            _operand(inputs[0]),
            int(counts[0]),
            _operand(outputs[0]),
            int((outputs[0].get("source") or {}).get("count") or 1),
            int(relation.get("duration") or 0),
            int(relation.get("eut") or 0),
            _operand(inputs[1]),
        )
    )
print("published_keys", len(keys))
retired: list[dict] = []
deleted = []
for path in sorted(RULES.glob("*.json")):
    document = json.loads(path.read_text(encoding="utf-8"))
    table = document["sparse"]
    shape = str(table["shape_item"])
    covered = 0
    not_gt6 = []
    for relation in table["relations"]:
        material = str(relation["material"])
        key = (
            _runtime(material, str(relation["input"]["prefix"])),
            int(relation["input"]["count"]),
            _runtime(material, str(relation["output"]["prefix"])),
            int(relation["output"]["count"]),
            int(relation["duration"]),
            int(relation["eut"]),
            shape,
        )
        if key in keys:
            covered += 1
        else:
            not_gt6.append(
                {
                    "file": path.name,
                    "stable_id": str(relation.get("stable_id") or ""),
                    "input": [key[0], key[1]],
                    "output": [key[2], key[3]],
                    "duration": key[4],
                    "eut": key[5],
                    "shape": shape,
                }
            )
    if not DRY_RUN:
        path.unlink()
    retired.extend(not_gt6)
    deleted.append(f"{path.name} covered {covered} not_gt6 {len(not_gt6)}")
if DRY_RUN:
    for row in retired[:40]:
        print(" ", row)
elif deleted:
    census.write_stable(
        WAVE / "material_rule_retired.json",
        {
            "note": (
                "extruder material_rule rows with no identical published GT6 row; "
                "deleted with their files by recipe/gt6-extruder-remainder"
            ),
            "rows": retired,
            "schema_version": 1,
        },
    )
print("deleted", len(deleted))
for line in deleted:
    print(" ", line)
