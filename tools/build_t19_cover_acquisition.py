#!/usr/bin/env python3
"""Build T19 selected-cover recipe, producer and resource reachability."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t19_cover_acquisition_policy.json"
OUTPUT = TOOLS / "t19_cover_acquisition.json"
MOD_ITEMS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T19_COVER_ACQUISITION_POLICY"
        or policy.get("expected_set_source")
        != "independent_t19_selected_cover_policy"
    ):
        raise ValueError("T19 cover acquisition policy header drifted")
    definitions = policy.get("definitions") or {}
    if set(definitions) != {
        "conveyor",
        "retriever_item",
        "robot_arm",
        "pressure_valve",
        "selector_manual",
    }:
        raise ValueError("T19 selected acquisition set drifted")
    if len({row.get("item") for row in definitions.values()}) != 5:
        raise ValueError("T19 selected item identities are not unique")
    if set(policy.get("language_resources") or {}) != {"en_us", "zh_cn"}:
        raise ValueError("T19 language resource set drifted")
    if len(set(policy.get("survival_sources") or [])) != 10:
        raise ValueError("T19 survival source declaration set drifted")


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    selected_policy = load(POLICY) if policy is None else policy
    validate_policy(selected_policy)
    mod_items = MOD_ITEMS.read_text(encoding="utf-8")
    languages = {
        locale: load(ROOT / path)
        for locale, path in selected_policy["language_resources"].items()
    }
    source_ids = set(selected_policy["survival_sources"])
    rows = []
    resources: dict[str, str] = {}
    unreachable = []
    operand_count = 0
    for canonical_id, spec in sorted(
        selected_policy["definitions"].items()
    ):
        item_path = spec["item"].split(":", 1)[1]
        registration_tokens = (
            spec["item_field"],
            f'"{item_path}"',
            f'"cruciblecraft:{canonical_id}"',
        )
        registered = all(token in mod_items for token in registration_tokens)
        recipe_path = ROOT / spec["recipe"]
        model_path = ROOT / spec["model"]
        recipe = load(recipe_path)
        model = load(model_path)
        result = recipe.get("result") or {}
        ingredients = []
        for symbol, ingredient in sorted((recipe.get("key") or {}).items()):
            identity = ingredient.get("item")
            reachable = identity in source_ids
            ingredients.append(
                {
                    "symbol": symbol,
                    "identity": identity,
                    "producer": "minecraft_survival",
                    "registered": identity is not None,
                    "reachable": reachable,
                }
            )
            if not reachable:
                unreachable.append(
                    f"{canonical_id}:{symbol}:{identity or 'missing'}"
                )
        operand_count += len(ingredients)
        language_keys = {
            locale: f"item.cruciblecraft.{item_path}" in document
            for locale, document in languages.items()
        }
        resource_ok = (
            model.get("parent") == "minecraft:item/generated"
            and isinstance(model.get("textures", {}).get("layer0"), str)
            and all(language_keys.values())
        )
        recipe_ok = (
            recipe.get("type") == "minecraft:crafting_shaped"
            and result.get("id") == spec["item"]
            and len(recipe.get("pattern") or []) in {2, 3}
            and bool(ingredients)
            and all(row["reachable"] for row in ingredients)
        )
        reachable = registered and resource_ok and recipe_ok
        if not reachable:
            unreachable.append(canonical_id)
        for path in (recipe_path, model_path):
            resources[relative(path)] = sha256(path)
        rows.append(
            {
                "canonical_id": canonical_id,
                "item": spec["item"],
                "registered": registered,
                "recipe": relative(recipe_path),
                "recipe_id": f"cruciblecraft:{item_path}",
                "ingredients": ingredients,
                "resources": {
                    "model": relative(model_path),
                    "languages": language_keys,
                },
                "reachable": reachable,
            }
        )
    for path in selected_policy["language_resources"].values():
        resource = ROOT / path
        resources[relative(resource)] = sha256(resource)
    if unreachable:
        raise ValueError(
            "T19 selected cover acquisition is unreachable: "
            + ", ".join(unreachable)
        )
    return {
        "schema_version": 1,
        "status": "T19_COVER_ACQUISITION_READY",
        "expected_set": {
            "source": selected_policy["expected_set_source"],
            "canonical_ids": sorted(selected_policy["definitions"]),
            "item_ids": sorted(
                row["item"]
                for row in selected_policy["definitions"].values()
            ),
        },
        "counts": {
            "selected_kinds": 5,
            "registered_items": 5,
            "vanilla_crafting_recipes": 5,
            "recipe_operand_rows": operand_count,
            "item_models": 5,
            "language_rows": 10,
            "unreachable": 0,
        },
        "rows": rows,
        "unreachable": [],
        "currentness": {
            "owned_inputs": [
                relative(POLICY),
                relative(Path(__file__).resolve()),
            ],
            "registration_source": {
                "path": relative(MOD_ITEMS),
                "sha256": sha256(MOD_ITEMS),
            },
            "resources": resources,
        },
    }


def check(document: dict[str, Any] | None = None) -> list[str]:
    expected = stable(build() if document is None else document)
    if not OUTPUT.is_file():
        return [f"missing {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != expected:
        return [f"stale {relative(OUTPUT)}"]
    return []


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    document = build()
    if args.check:
        errors = check(document)
        if errors:
            print("\n".join(errors))
            return 1
        print("T19 cover acquisition is current")
        return 0
    if args.write:
        OUTPUT.write_bytes(stable(document).encode("utf-8"))
        print(f"wrote {relative(OUTPUT)}")
        return 0
    print(stable(document), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
