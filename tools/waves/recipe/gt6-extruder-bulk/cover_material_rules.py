#!/usr/bin/env python3
"""Delete extruder material_rule files whose sparse rows are all published."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(ROOT))
sys.path.insert(1, str(ROOT / "tools"))
from tools.recipe_bulk.handlers import shape_transform

WAVE = ROOT / "tools/waves/recipe/gt6-extruder-bulk"
RULES = (
    ROOT
    / "src/component_rule_generated/resources/data/cruciblecraft/recipe/extruder/compact"
)
source = json.loads((WAVE / "source.json").read_text(encoding="utf-8"))
keys: set[tuple] = set()
for relation in source["relations"]:
    if not shape_transform.matches(relation, {}):
        continue
    inputs = relation["item_inputs"]
    actions = relation["item_input_actions"]
    consumed = next(
        item
        for item, action in zip(inputs, actions)
        if action.get("kind") == "CONSUME"
    )
    preserved = next(
        item
        for item, action in zip(inputs, actions)
        if action.get("kind") == "PRESERVE"
    )
    shape = str(preserved.get("runtime_id") or "")
    in_count = 0
    for item, action, count in zip(
        inputs, actions, relation.get("item_input_counts") or []
    ):
        if action.get("kind") == "CONSUME":
            in_count = int(count)
    for output in relation["item_outputs"]:
        out_count = int((output.get("source") or {}).get("count") or 1)
        keys.add(
            (
                str(consumed.get("material")),
                str(consumed.get("form")),
                in_count,
                str(output.get("form")),
                out_count,
                int(relation.get("duration") or 0),
                int(relation.get("eut") or 0),
                shape,
            )
        )
print("published_shape_keys", len(keys))
kept = []
deleted = []
for path in sorted(RULES.glob("*.json")):
    document = json.loads(path.read_text(encoding="utf-8"))
    table = document["sparse"]
    shape = str(table["shape_item"])
    missing = 0
    total = 0
    for relation in table["relations"]:
        total += 1
        key = (
            str(relation["material"]),
            str(relation["input"]["prefix"]),
            int(relation["input"]["count"]),
            str(relation["output"]["prefix"]),
            int(relation["output"]["count"]),
            int(relation["duration"]),
            int(relation["eut"]),
            shape,
        )
        if key not in keys:
            missing += 1
    if missing == 0:
        path.unlink()
        deleted.append(f"{path.name} {total}")
    else:
        kept.append(f"{path.name} missing {missing} of {total}")
print("deleted", len(deleted))
for line in deleted:
    print(" ", line)
print("kept", len(kept))
for line in kept:
    print(" ", line)
