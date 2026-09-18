#!/usr/bin/env python3
"""Public-exchange prefix identity shared by mill emit and leftover JSON rewrite."""
from __future__ import annotations

import json
import os
import time
from pathlib import Path
from typing import Any

PUBLIC_EXCHANGE_PREFIXES = frozenset({
    "ingot",
    "nugget",
    "dust",
    "small_dust",
    "tiny_dust",
    "plate",
    "rod",
    "long_rod",
    "bolt",
    "screw",
    "ring",
    "gear",
    "small_gear",
    "gem",
    "foil",
    "fine_wire",
})
PREFIX_MATERIAL_COMPONENT = "cruciblecraft:prefix_material"
VANILLA_DEFAULT_COMPONENTS = frozenset({
    "minecraft:attribute_modifiers",
    "minecraft:enchantments",
    "minecraft:lore",
    "minecraft:max_stack_size",
    "minecraft:rarity",
    "minecraft:repair_cost",
})
RECIPE_TREES = (
    Path("src/generated/resources"),
    Path("src/recipe_generated/resources"),
    Path("src/component_rule_generated/resources"),
)


def public_exchange_item_id(item_id: str) -> str | None:
    if not item_id.startswith("cruciblecraft:"):
        return None
    path = item_id.split(":", 1)[1]
    if "/" in path:
        return None
    if path not in PUBLIC_EXCHANGE_PREFIXES:
        return None
    return path


def rewrite_value(value: Any) -> Any:
    if isinstance(value, list):
        return [rewrite_value(item) for item in value]
    if not isinstance(value, dict):
        return value
    rewritten = {key: rewrite_value(item) for key, item in value.items()}
    components = rewritten.get("components")
    if not isinstance(components, dict):
        return rewritten
    material = components.get(PREFIX_MATERIAL_COMPONENT)
    if not isinstance(material, str) or not material:
        return rewritten
    item_id = None
    id_key = None
    for key in ("items", "item", "id"):
        candidate = rewritten.get(key)
        if isinstance(candidate, str):
            item_id = candidate
            id_key = key
            break
    prefix = public_exchange_item_id(item_id or "")
    if prefix is None or id_key is None:
        return rewritten
    unique = f"cruciblecraft:{material}/{prefix}"
    leftover = {
        key: item
        for key, item in components.items()
        if key != PREFIX_MATERIAL_COMPONENT and key not in VANILLA_DEFAULT_COMPONENTS
    }
    updated = dict(rewritten)
    updated[id_key] = unique
    if leftover:
        updated["components"] = leftover
    else:
        updated.pop("components", None)
        if updated.get("type") == "neoforge:components":
            updated.pop("type", None)
            if id_key == "items":
                updated["item"] = unique
                updated.pop("items", None)
            elif id_key == "item":
                updated["item"] = unique
    return updated


def rewrite_document(document: Any) -> Any:
    return rewrite_value(document)


def rewrite_tree(root: Path) -> int:
    changed = 0
    if not root.is_dir():
        return 0
    scanned = 0
    for path in root.rglob("*.json"):
        scanned += 1
        try:
            original = path.read_text(encoding="utf-8")
        except OSError:
            continue
        if PREFIX_MATERIAL_COMPONENT not in original:
            continue
        try:
            document = json.loads(original)
        except json.JSONDecodeError:
            continue
        rewritten = rewrite_document(document)
        if rewritten == document:
            continue
        text = json.dumps(rewritten, indent=2, ensure_ascii=False) + "\n"
        _replace_text(path, text)
        changed += 1
        if changed % 200 == 0:
            print(f"{root}: rewritten {changed} / scanned {scanned}", flush=True)
    return changed


def _replace_text(path: Path, text: str) -> None:
    tmp = path.with_name(path.name + ".rewrite.tmp")
    last_error: OSError | None = None
    for attempt in range(8):
        try:
            tmp.write_text(text, encoding="utf-8")
            os.replace(tmp, path)
            return
        except OSError as error:
            last_error = error
            time.sleep(0.05 * (attempt + 1))
    if tmp.exists():
        try:
            tmp.unlink()
        except OSError:
            pass
    raise last_error or OSError(f"could not rewrite {path}")


def rewrite_trees(roots: tuple[Path, ...] = RECIPE_TREES) -> dict[str, int]:
    return {str(root): rewrite_tree(root) for root in roots}


def main() -> int:
    counts = rewrite_trees()
    for root, count in counts.items():
        print(f"{root}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
