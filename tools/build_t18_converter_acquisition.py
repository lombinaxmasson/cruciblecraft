#!/usr/bin/env python3
"""Build the independent T18d converter acquisition and resource closure."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t18_converter_acquisition_policy.json"
OUTPUT = TOOLS / "t18_converter_acquisition.json"
CONVERTERS = (
    ROOT / "src/main/resources/data/cruciblecraft/energy_converters.json"
)
MATERIAL_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MOD_BLOCKS = (
    ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java"
)
MOD_ITEMS = (
    ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
)
EXPECTED_PROFILE_COUNT = 6
EXPECTED_RESOURCE_KINDS = {
    "blockstate",
    "block_model",
    "item_model",
    "loot",
}


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


def material_item(identity: str) -> tuple[str, str] | None:
    match = re.fullmatch(r"cruciblecraft:([^/]+)/([^/]+)", identity)
    return match.groups() if match is not None else None


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status")
        != "T18D_CONVERTER_ACQUISITION_POLICY"
        or policy.get("expected_set_source")
        != "independent_t18d_converter_acceptance_policy"
        or policy.get("production_mapping_reused") is not False
    ):
        raise ValueError("T18d converter acquisition policy header drifted")
    profiles = policy.get("profiles")
    if (
        not isinstance(profiles, dict)
        or len(profiles) != EXPECTED_PROFILE_COUNT
        or len({row.get("result") for row in profiles.values()})
        != EXPECTED_PROFILE_COUNT
    ):
        raise ValueError("T18d policy must independently name six converters")
    for profile_id, row in profiles.items():
        if (
            not profile_id.startswith("cruciblecraft:")
            or not str(row.get("runtime_binding") or "").strip()
            or not str(row.get("result") or "").startswith("cruciblecraft:")
            or not str(row.get("block_field") or "").strip()
            or not str(row.get("item_field") or "").strip()
            or set(row.get("resources") or {}) != EXPECTED_RESOURCE_KINDS
        ):
            raise ValueError(f"{profile_id}: incomplete converter declaration")
        state_models = row.get("state_models")
        if state_models is not None and (
            not isinstance(state_models, dict)
            or not state_models
            or any(
                not str(model_id).startswith("cruciblecraft:block/")
                or not str(path).endswith(".json")
                for model_id, path in state_models.items()
            )
            or not str(row.get("item_model_parent") or "").startswith(
                "cruciblecraft:block/"
            )
        ):
            raise ValueError(f"{profile_id}: invalid state-model declaration")
        recipe = row.get("recipe") or {}
        if (
            not str(recipe.get("id") or "").startswith("cruciblecraft:")
            or not str(recipe.get("path") or "").endswith(".json")
            or len(recipe.get("pattern") or []) != 3
            or not recipe.get("key")
        ):
            raise ValueError(f"{profile_id}: incomplete acquisition recipe")
    source_rows = policy.get("source_declarations") or {}
    if (
        len(source_rows) != 9
        or source_rows.get("cruciblecraft:bronze/ingot", {}).get(
            "classification"
        ) != "registered_survival_source"
        or sum(
            row.get("classification") == "minecraft_survival_source"
            for row in source_rows.values()
        ) != 8
    ):
        raise ValueError("T18d source declarations drifted")
    if set(policy.get("producer_rules") or {}) != {"plate", "rod"}:
        raise ValueError("T18d producer rule set must be plate and rod")
    if set(policy.get("tag_sources") or {}) != {
        "c:plates/bronze",
        "c:rods/bronze",
    }:
        raise ValueError("T18d recipe tag source set drifted")
    if set(policy.get("concrete_recipes") or {}) != {
        "cruciblecraft:firebrick"
    }:
        raise ValueError("T18d concrete producer recipe set drifted")


def complete_profiles() -> dict[str, dict[str, Any]]:
    document = load(CONVERTERS)
    profiles = {
        row["id"]: row
        for row in document.get("profiles") or []
        if row.get("status") == "COMPLETE"
    }
    if (
        document.get("schemaVersion") != 1
        or len(profiles) != EXPECTED_PROFILE_COUNT
    ):
        raise ValueError("complete converter catalog set drifted")
    return profiles


def registrations(path: Path, register: str) -> dict[str, str]:
    source = path.read_text(encoding="utf-8")
    if register == "block":
        pattern = (
            r"\b([A-Z][A-Z0-9_]*)\s*=\s*"
            r"BLOCKS\.register(?:SimpleBlock)?\(\s*\"([^\"]+)\""
        )
    else:
        pattern = (
            r"\b([A-Z][A-Z0-9_]*)\s*=\s*"
            r"ITEMS\.registerSimpleBlockItem\(\s*\"([^\"]+)\""
        )
    rows = re.findall(pattern, source, flags=re.DOTALL)
    result = dict(rows)
    if len(result) != len(rows):
        raise ValueError(f"{path.name}: duplicate parsed {register} fields")
    return result


def normalized_recipe(
    spec: dict[str, Any],
    *,
    expected_result: str,
) -> tuple[Path, dict[str, dict[str, str]]]:
    path = ROOT / spec["path"]
    document = load(path)
    actual_key = document.get("key")
    if not isinstance(actual_key, dict):
        raise ValueError(f"{spec['id']}: shaped recipe key is missing")
    key: dict[str, dict[str, str]] = {}
    for symbol, ingredient in actual_key.items():
        if (
            not isinstance(symbol, str)
            or len(symbol) != 1
            or not isinstance(ingredient, dict)
            or len(ingredient) != 1
            or not set(ingredient).issubset({"item", "tag"})
        ):
            raise ValueError(f"{spec['id']}: invalid exact ingredient")
        kind, identity = next(iter(ingredient.items()))
        if not isinstance(identity, str) or not identity:
            raise ValueError(f"{spec['id']}: invalid ingredient identity")
        key[symbol] = {kind: identity}
    result = document.get("result") or {}
    if (
        document.get("type") != "minecraft:crafting_shaped"
        or document.get("category", "misc") != "misc"
        or document.get("pattern") != spec["pattern"]
        or key != spec["key"]
        or result != {
            "id": expected_result,
            "count": spec.get("result_count", 1),
        }
    ):
        raise ValueError(f"{spec['id']}: recipe structure/result drifted")
    used = {
        symbol
        for pattern_row in spec["pattern"]
        for symbol in pattern_row
        if symbol != " "
    }
    if used != set(key):
        raise ValueError(f"{spec['id']}: pattern/key symbols differ")
    return path, key


class Closure:
    def __init__(
        self,
        policy: dict[str, Any],
        gate: dict[str, list[str]],
    ) -> None:
        self.policy = policy
        self.gate = gate
        self.cache: dict[str, dict[str, Any]] = {}
        self.hashes: dict[str, dict[str, str]] = {
            "producer_rules": {},
            "source_materials": {},
            "tag_contracts": {},
            "concrete_recipes": {},
        }

    def source(self, identity: str) -> dict[str, Any] | None:
        declaration = self.policy["source_declarations"].get(identity)
        if declaration is None:
            return None
        classification = declaration["classification"]
        if classification == "minecraft_survival_source":
            return {
                "item": identity,
                "classification": classification,
                "producer": "minecraft",
                "registered": True,
                "reachable": True,
                "inputs": [],
            }
        parsed = material_item(identity)
        if parsed is None:
            raise ValueError(f"{identity}: material source identity is invalid")
        material, form = parsed
        if form not in self.gate.get(material, []):
            raise ValueError(f"{identity}: source material form is not registered")
        path = ROOT / declaration["evidence"]
        document = load(path)
        if document.get("id") != material:
            raise ValueError(f"{identity}: source material evidence drifted")
        self.hashes["source_materials"][relative(path)] = sha256(path)
        return {
            "item": identity,
            "classification": classification,
            "producer": declaration["evidence"],
            "registered": True,
            "reachable": True,
            "inputs": [],
        }

    def resolve_tag(
        self,
        tag: str,
        visiting: tuple[str, ...],
    ) -> dict[str, Any]:
        declaration = self.policy["tag_sources"].get(tag)
        if declaration is None:
            return self.unreachable(f"#{tag}", "tag has no source declaration")
        prefix_path = ROOT / declaration["prefix_resource"]
        prefix = load(prefix_path)
        expected_tag = (
            f"{prefix.get('tag_namespace')}:{prefix.get('tag_directory')}/"
            f"{declaration['material']}"
        )
        dynamic_path = ROOT / declaration["dynamic_pack_contract"]
        dynamic_source = dynamic_path.read_text(encoding="utf-8")
        if (
            expected_tag != tag
            or prefix.get("serialized_path") != declaration["form"]
            or declaration["form"] not in self.gate.get(
                declaration["material"], []
            )
            or "tags/item/" not in dynamic_source
            or "material.tagName()" not in dynamic_source
        ):
            raise ValueError(f"{tag}: dynamic material tag contract drifted")
        self.hashes["tag_contracts"][relative(prefix_path)] = sha256(
            prefix_path
        )
        self.hashes["tag_contracts"][relative(dynamic_path)] = sha256(
            dynamic_path
        )
        child = self.resolve_item(declaration["item"], visiting)
        return {
            "item": f"#{tag}",
            "classification": "dynamic_material_tag",
            "producer": declaration["dynamic_pack_contract"],
            "registered": child["registered"],
            "reachable": child["reachable"],
            "inputs": [child],
        }

    def resolve_item(
        self,
        identity: str,
        visiting: tuple[str, ...] = (),
    ) -> dict[str, Any]:
        cached = self.cache.get(identity)
        if cached is not None:
            return cached
        if identity in visiting:
            return self.unreachable(identity, "producer cycle")
        source = self.source(identity)
        if source is not None:
            self.cache[identity] = source
            return source

        concrete = self.policy["concrete_recipes"].get(identity)
        if concrete is not None:
            path, key = normalized_recipe(
                concrete, expected_result=concrete["result"]
            )
            inputs = [
                self.resolve_operand(
                    ingredient, (*visiting, identity)
                )
                for ingredient in key.values()
            ]
            row = {
                "item": identity,
                "classification": "concrete_shaped_recipe",
                "producer": concrete["id"],
                "producer_path": relative(path),
                "registered": True,
                "reachable": all(child["reachable"] for child in inputs),
                "inputs": inputs,
            }
            self.hashes["concrete_recipes"][relative(path)] = sha256(path)
            self.cache[identity] = row
            return row

        parsed = material_item(identity)
        if parsed is None:
            return self.unreachable(identity, "no source or producer declaration")
        material, form = parsed
        if form not in self.gate.get(material, []):
            return self.unreachable(identity, "material form is not registered")
        producer = self.policy["producer_rules"].get(form)
        if producer is None:
            return self.unreachable(
                identity, f"no material producer rule for {form}"
            )
        producer_path = ROOT / producer["path"]
        document = load(producer_path)
        outputs = document.get("item_outputs") or []
        inputs = document.get("item_inputs") or []
        matching_outputs = [
            row for row in outputs
            if row.get("prefix") == f"cruciblecraft:{form}"
        ]
        input_prefixes = [
            row.get("prefix", "").removeprefix("cruciblecraft:")
            for row in inputs
        ]
        if (
            document.get("type") != "cruciblecraft:material_rule"
            or len(matching_outputs) != 1
            or input_prefixes != producer["input_prefixes"]
            or len(inputs) != len(input_prefixes)
        ):
            raise ValueError(f"{identity}: material producer rule drifted")
        children = [
            self.resolve_item(
                f"cruciblecraft:{material}/{prefix}",
                (*visiting, identity),
            )
            for prefix in input_prefixes
        ]
        row = {
            "item": identity,
            "classification": "material_rule",
            "producer": producer["path"],
            "registered": True,
            "reachable": all(child["reachable"] for child in children),
            "inputs": children,
        }
        self.hashes["producer_rules"][relative(producer_path)] = sha256(
            producer_path
        )
        self.cache[identity] = row
        return row

    def resolve_operand(
        self,
        ingredient: dict[str, str],
        visiting: tuple[str, ...] = (),
    ) -> dict[str, Any]:
        kind, identity = next(iter(ingredient.items()))
        return (
            self.resolve_item(identity, visiting)
            if kind == "item"
            else self.resolve_tag(identity, visiting)
        )

    def unreachable(self, identity: str, reason: str) -> dict[str, Any]:
        row = {
            "item": identity,
            "classification": "unreachable",
            "producer": None,
            "registered": False,
            "reachable": False,
            "reason": reason,
            "inputs": [],
        }
        self.cache.setdefault(identity, row)
        return row


def ingredient_rows(
    pattern: list[str],
    key: dict[str, dict[str, str]],
    closure: Closure,
) -> list[dict[str, Any]]:
    occurrences = Counter(
        symbol
        for pattern_row in pattern
        for symbol in pattern_row
        if symbol != " "
    )
    return [
        {
            "symbol": symbol,
            "occurrences": occurrences[symbol],
            **closure.resolve_operand(ingredient),
        }
        for symbol, ingredient in sorted(key.items())
    ]


def resource_closure(
    policy: dict[str, Any],
    profile_rows: dict[str, dict[str, Any]],
    block_registrations: dict[str, str],
    item_registrations: dict[str, str],
) -> tuple[dict[str, Any], dict[str, str]]:
    languages = {
        language: load(ROOT / path)
        for language, path in policy["resource_contract"]["languages"].items()
    }
    tag_path = ROOT / policy["resource_contract"]["block_tag"]["path"]
    tag_values = set(load(tag_path).get("values") or [])
    resources: dict[str, dict[str, Any]] = {}
    hashes: dict[str, str] = {
        relative(tag_path): sha256(tag_path),
        **{
            path: sha256(ROOT / path)
            for path in policy["resource_contract"]["languages"].values()
        },
    }
    component_sets: dict[str, set[str]] = {
        "profiles": set(profile_rows),
        "blocks": set(),
        "items": set(),
        "blockstates": set(),
        "block_models": set(),
        "item_models": set(),
        "loot": set(),
        "block_tags": set(),
    }
    for profile_id, spec in policy["profiles"].items():
        result = spec["result"]
        path_id = result.partition(":")[2]
        if (
            block_registrations.get(spec["block_field"]) != path_id
            or item_registrations.get(spec["item_field"]) != path_id
        ):
            raise ValueError(f"{profile_id}: block/item registration drifted")
        paths = {
            kind: ROOT / path
            for kind, path in spec["resources"].items()
        }
        documents = {kind: load(path) for kind, path in paths.items()}
        model_id = f"cruciblecraft:block/{spec['runtime_binding']}"
        state_models = spec.get("state_models") or {model_id: spec["resources"]["block_model"]}
        expected_model_ids = set(state_models)
        blockstate_text = json.dumps(documents["blockstate"], sort_keys=True)
        if not expected_model_ids.issubset(
            {
                value
                for value in re.findall(
                    r'"model":\s*"([^"]+)"', blockstate_text
                )
            }
        ):
            raise ValueError(f"{profile_id}: blockstate/model binding drifted")
        parent = documents["item_model"].get("parent")
        if not str(parent or "").strip():
            raise ValueError(f"{profile_id}: item model parent drifted")
        expected_item_parent = spec.get("item_model_parent")
        if expected_item_parent and parent != expected_item_parent:
            raise ValueError(f"{profile_id}: item model parent drifted")
        loot_names = set(re.findall(
            r'"name":\s*"([^"]+)"',
            json.dumps(documents["loot"], sort_keys=True),
        ))
        if loot_names != {result}:
            raise ValueError(f"{profile_id}: loot result drifted")
        lang_key = f"block.cruciblecraft.{path_id}"
        if any(
            not str(document.get(lang_key) or "").strip()
            for document in languages.values()
        ):
            raise ValueError(f"{profile_id}: bilingual lang closure is missing")
        if result not in tag_values:
            raise ValueError(f"{profile_id}: pickaxe block tag is missing")
        for kind, path in paths.items():
            hashes[relative(path)] = sha256(path)
            component_sets[
                {
                    "blockstate": "blockstates",
                    "block_model": "block_models",
                    "item_model": "item_models",
                    "loot": "loot",
                }[kind]
            ].add(profile_id)
        for state_model_path in state_models.values():
            path = ROOT / state_model_path
            if not path.is_file():
                raise ValueError(
                    f"{profile_id}: state model is missing: {state_model_path}"
                )
            hashes[relative(path)] = sha256(path)
        component_sets["blocks"].add(profile_id)
        component_sets["items"].add(profile_id)
        component_sets["block_tags"].add(profile_id)
        resources[profile_id] = {
            "result": result,
            "block_field": spec["block_field"],
            "item_field": spec["item_field"],
            "lang_key": lang_key,
            "languages": sorted(languages),
            "block_tag": policy["resource_contract"]["block_tag"]["id"],
            "paths": spec["resources"],
            "state_models": state_models,
            "item_model_parent": expected_item_parent,
        }
    expected = set(profile_rows)
    if any(values != expected for values in component_sets.values()):
        raise ValueError("converter profile/resource sets are not bidirectional")
    return {
        "status": "BIDIRECTIONAL_CLOSURE",
        "component_sets": {
            name: sorted(values)
            for name, values in sorted(component_sets.items())
        },
        "profiles": resources,
    }, dict(sorted(hashes.items()))


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    validate_policy(policy)
    profiles = complete_profiles()
    if set(profiles) != set(policy["profiles"]):
        raise ValueError("policy and complete converter profiles are not bidirectional")
    for profile_id, spec in policy["profiles"].items():
        profile = profiles[profile_id]
        if (
            profile.get("runtimeBinding") != spec["runtime_binding"]
            or profile.get("status") != "COMPLETE"
        ):
            raise ValueError(f"{profile_id}: runtime profile binding drifted")

    gate_document = load(MATERIAL_GATE)
    gate = gate_document.get("materials")
    if not isinstance(gate, dict):
        raise ValueError("material registration gate has no material map")
    block_rows = registrations(MOD_BLOCKS, "block")
    item_rows = registrations(MOD_ITEMS, "item")
    closure = Closure(policy, gate)
    recipe_hashes: dict[str, str] = {}
    recipes: list[dict[str, Any]] = []
    for profile_id, spec in sorted(policy["profiles"].items()):
        recipe_path, key = normalized_recipe(
            spec["recipe"], expected_result=spec["result"]
        )
        ingredients = ingredient_rows(
            spec["recipe"]["pattern"], key, closure
        )
        recipes.append({
            "profile_id": profile_id,
            "runtime_binding": spec["runtime_binding"],
            "recipe_id": spec["recipe"]["id"],
            "recipe_path": relative(recipe_path),
            "result_item": spec["result"],
            "ingredients": ingredients,
            "reachable": all(row["reachable"] for row in ingredients),
        })
        recipe_hashes[relative(recipe_path)] = sha256(recipe_path)

    unreachable = [
        {
            "profile_id": recipe["profile_id"],
            "recipe_id": recipe["recipe_id"],
            "item": ingredient["item"],
            "reason": ingredient.get(
                "reason", "producer input is unreachable"
            ),
        }
        for recipe in recipes
        for ingredient in recipe["ingredients"]
        if not ingredient["reachable"]
    ]
    if unreachable:
        raise ValueError(
            f"T18d converter acquisition has unreachable operands: {unreachable}"
        )
    source_rows = [
        closure.resolve_item(identity)
        for identity in sorted(policy["source_declarations"])
    ]
    if not all(row["reachable"] for row in source_rows):
        raise ValueError("T18d declared sources are not all reachable")

    resource_rows, resource_hashes = resource_closure(
        policy, profiles, block_rows, item_rows
    )
    operand_rows = [
        ingredient
        for recipe in recipes
        for ingredient in recipe["ingredients"]
    ]
    return {
        "schema_version": 1,
        "status": "T18D_CONVERTER_ACQUISITION_READY",
        "delivery": "T18d",
        "expected_set": {
            "source": policy["expected_set_source"],
            "production_mapping_reused":
                policy["production_mapping_reused"],
            "profile_ids": sorted(profiles),
            "recipe_ids": sorted(
                row["recipe_id"] for row in recipes
            ),
        },
        "counts": {
            "converter_profiles": len(profiles),
            "registered_blocks": len(profiles),
            "registered_items": len(profiles),
            "vanilla_crafting_recipes": len(recipes),
            "recipe_operand_rows": len(operand_rows),
            "concrete_producer_recipes":
                len(policy["concrete_recipes"]),
            "producer_rules": len(policy["producer_rules"]),
            "recipe_tag_sources": len(policy["tag_sources"]),
            "material_source_declarations": sum(
                row["classification"] == "registered_survival_source"
                for row in policy["source_declarations"].values()
            ),
            "minecraft_source_declarations": sum(
                row["classification"] == "minecraft_survival_source"
                for row in policy["source_declarations"].values()
            ),
            "profile_resource_rows":
                len(profiles) * len(EXPECTED_RESOURCE_KINDS),
            "language_rows":
                len(profiles)
                * len(policy["resource_contract"]["languages"]),
            "block_tag_rows": len(profiles),
            "gt_recipe_rows_added": 0,
            "unreachable": 0,
        },
        "profiles": [
            {
                "id": profile_id,
                "runtime_binding": profiles[profile_id]["runtimeBinding"],
                "result_item": policy["profiles"][profile_id]["result"],
                "source_id": profiles[profile_id]["source"]["sourceId"],
            }
            for profile_id in sorted(profiles)
        ],
        "recipes": recipes,
        "source_declarations": source_rows,
        "resource_closure": resource_rows,
        "unreachable": unreachable,
        "currentness": {
            "owned_inputs": {
                relative(BUILDER): sha256(BUILDER),
                relative(POLICY): sha256(POLICY),
            },
            "catalog": {
                relative(CONVERTERS): sha256(CONVERTERS),
            },
            "registrations": {
                relative(MOD_BLOCKS): sha256(MOD_BLOCKS),
                relative(MOD_ITEMS): sha256(MOD_ITEMS),
                relative(MATERIAL_GATE): sha256(MATERIAL_GATE),
            },
            "crafting_recipes": dict(sorted(recipe_hashes.items())),
            **{
                owner: dict(sorted(rows.items()))
                for owner, rows in closure.hashes.items()
            },
            "resources": resource_hashes,
        },
    }


def check() -> list[str]:
    encoded = stable(build())
    if not OUTPUT.is_file():
        return [f"missing generated file: {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [f"stale generated file: {relative(OUTPUT)}"]
    return []


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T18d converter acquisition failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "profiles": document["counts"]["converter_profiles"],
        "recipes": document["counts"]["vanilla_crafting_recipes"],
        "gt_recipe_rows_added":
            document["counts"]["gt_recipe_rows_added"],
        "unreachable": document["counts"]["unreachable"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
