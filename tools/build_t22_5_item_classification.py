#!/usr/bin/env python3
"""Build the T22.5 A3 artifact: classification of unmapped GT6 meta items.

Rules are committed first (``t22_5_item_classification_rules.json``) and
this builder only applies them.  Every class is a category definition
with a discriminant and a recheck point; classification reasons are
class definitions, never "cannot translate".

Modes:
  --check --reference-only   validate the committed artifact (no dump)
  --write                    write the artifact (no dump needed)
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_5_item_classification.json"
RULES = TOOLS / "t22_5_item_classification_rules.json"
OPERANDS = TOOLS / "gt6_l1b_selected_recipe_operands.json"
BUILDER = Path(__file__).resolve()


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def _class_by_discriminant(rules_doc: dict[str, Any]) -> dict[str, str]:
    """Validate class definitions and return item-key -> class."""
    classes = rules_doc.get("classes")
    if not isinstance(classes, list) or not classes:
        raise ValueError("rules classes missing")
    assignments: dict[str, str] = {}
    for entry in classes:
        for field in ("class", "definition", "discriminant",
                      "recheck_point"):
            if not entry.get(field):
                raise ValueError(f"class {entry.get('class')} lacks {field}")
        for discriminant in entry["discriminant"]:
            if not isinstance(discriminant, str) or not discriminant:
                raise ValueError(
                    f"class {entry['class']} has invalid discriminant"
                )
            if discriminant in assignments:
                raise ValueError(
                    f"duplicate discriminant {discriminant} in "
                    f"{assignments[discriminant]} and {entry['class']}"
                )
            assignments[discriminant] = entry["class"]
    return assignments


def classify(rules_doc: dict[str, Any],
             items: dict[str, int]) -> dict[str, Any]:
    """Apply the committed rules to the unmapped meta-item counts.

    Returns the classification document core: per-key rows, per-class
    totals, and zero unclassified."""
    discriminant_class = _class_by_discriminant(rules_doc)
    rules_keys = rules_doc.get("keys")
    if not isinstance(rules_keys, dict):
        raise ValueError("rules keys missing")

    item_keys = set(items.keys())
    declared = set(rules_keys.keys())
    if item_keys != declared:
        raise ValueError(
            "rules keys do not match the operands artifact: "
            f"undecided={sorted(item_keys - declared)}, "
            f"stale={sorted(declared - item_keys)}"
        )

    rows: list[dict[str, Any]] = []
    class_totals: dict[str, int] = {}
    unclassified: list[str] = []
    for item, occurrences in sorted(items.items()):
        assigned = rules_keys[item]
        matched = [
            assigned
            for discriminant, klass in discriminant_class.items()
            if item.startswith(discriminant) and assigned == klass
        ]
        if not matched:
            raise ValueError(
                f"key {item} assigned {assigned} but matches no "
                "class discriminant"
            )
        rows.append(
            {
                "item": item,
                "class": assigned,
                "occurrences": occurrences,
            }
        )
        class_totals[assigned] = class_totals.get(assigned, 0) + occurrences

    for discriminant, klass in discriminant_class.items():
        if not any(item.startswith(discriminant) for item in items):
            raise ValueError(
                f"discriminant {discriminant} of class {klass} "
                "matches no item key"
            )

    total = sum(items.values())
    return {
        "records": rows,
        "counts": {
            "total_keys": len(rows),
            "total_occurrences": total,
            "class_totals": dict(sorted(class_totals.items())),
            "unclassified": len(unclassified),
        },
    }


def build() -> dict[str, Any]:
    rules_doc = _load(RULES)
    operands = _load(OPERANDS)
    items = operands.get("unmapped_gt_meta_items")
    if not isinstance(items, dict):
        raise ValueError("operands unmapped_gt_meta_items missing")
    classification = classify(rules_doc, items)
    if classification["counts"]["unclassified"] != 0:
        raise ValueError("unclassified item keys remain")
    return {
        "schema_version": 1,
        "status": "T22_5_ITEM_CLASSIFICATION_READY",
        "rules": _relative(RULES),
        "class_definitions": rules_doc["classes"],
        "principle": rules_doc.get("principle"),
        "records": classification["records"],
        "counts": classification["counts"],
        "inputs": {
            _relative(RULES): _sha256(RULES),
            _relative(OPERANDS): _sha256(OPERANDS),
            _relative(BUILDER): _sha256(BUILDER),
        },
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status") != "T22_5_ITEM_CLASSIFICATION_READY":
        errors.append("status != T22_5_ITEM_CLASSIFICATION_READY")
    try:
        expected = build()
    except ValueError as error:
        errors.append(str(error))
        return errors
    if _stable(on_disk) != _stable(expected):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        elif args.write:
            document = write()
        else:
            parser.error("choose --check or --write")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22.5 item classification failed: {error}",
              file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                "schema_version": document.get("schema_version"),
                "status": document.get("status"),
                "counts": document.get("counts"),
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
