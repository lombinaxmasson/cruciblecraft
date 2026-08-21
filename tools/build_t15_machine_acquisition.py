#!/usr/bin/env python3
"""Build the independent T15c machine acquisition-closure evidence."""
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
POLICY = TOOLS / "t15_machine_acquisition_policy.json"
OUTPUT = TOOLS / "t15_machine_acquisition.json"
GENERATED_RECIPE_ROOT = (
    ROOT / "src/generated/resources/data/cruciblecraft/recipe"
)
COMPONENT_RECIPE_ROOT = (
    ROOT
    / "src/component_rule_generated/resources/data/cruciblecraft/recipe"
)
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
MATERIAL_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MOD_ITEMS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
)
MACHINE_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
REQUIRED_PRODUCER_FORMS = {
    "cable",
    "double_plate",
    "fine_wire",
    "foil",
    "gear",
    "long_rod",
    "plate",
    "rod",
    "spring",
    "wire",
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
    if match is None:
        return None
    return match.group(1), match.group(2)


def recipe_path(recipe_id: str) -> Path:
    namespace, separator, path = recipe_id.partition(":")
    if separator != ":" or namespace != "cruciblecraft" or not path:
        raise ValueError(f"invalid T15c recipe id {recipe_id!r}")
    return GENERATED_RECIPE_ROOT / f"{path}.json"


def mod_item_registrations() -> dict[str, str]:
    source = MOD_ITEMS.read_text(encoding="utf-8")
    rows = re.findall(
        r"DeferredItem<[^>]+>\s+([A-Z0-9_]+)\s*=\s*"
        r"(?:ITEMS\.registerSimple(?:Block)?Item\(\s*\"([^\"]+)\""
        r"|tieredProcessingItem\(\s*\"([^\"]+)\")",
        source,
        flags=re.DOTALL,
    )
    result: dict[str, str] = {}
    for field, simple_id, tiered_id in rows:
        item_id = simple_id or tiered_id
        if field in result and result[field] != item_id:
            raise ValueError("ModItems block-item declarations are duplicated")
        result[field] = item_id
    return result


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T15C_MACHINE_ACQUISITION_POLICY"
        or policy.get("expected_set_source")
        != "independent_t15c_acceptance_policy"
        or policy.get("production_mapping_reused") is not False
    ):
        raise ValueError("T15c acquisition policy header drifted")
    casings = policy.get("casing_recipes")
    machines = policy.get("machine_variant_recipes")
    if not isinstance(casings, dict) or len(casings) != 6:
        raise ValueError("T15c policy must independently name six casing recipes")
    if not isinstance(machines, dict) or len(machines) != 9:
        raise ValueError("T15c policy must independently name nine variant recipes")
    producers = policy.get("producer_rules")
    if not isinstance(producers, dict) or set(producers) != REQUIRED_PRODUCER_FORMS:
        raise ValueError("T15c producer-rule form set drifted")
    sources = policy.get("source_declarations")
    if not isinstance(sources, dict) or not sources:
        raise ValueError("T15c source declarations are empty")
    for identity, source in sources.items():
        if (
            material_item(identity) is None
            or source.get("classification") != "registered_survival_source"
            or not str(source.get("evidence") or "").strip()
        ):
            raise ValueError(f"{identity}: invalid survival source declaration")


def validate_registered_material_item(
    identity: str,
    gate: dict[str, list[str]],
) -> tuple[str, str]:
    parsed = material_item(identity)
    if parsed is None:
        raise ValueError(f"{identity}: expected a material item identity")
    material, form = parsed
    if form not in gate.get(material, []):
        raise ValueError(f"{identity}: material form is not registered")
    return material, form


def validate_recipe_document(
    recipe_id: str,
    expected: dict[str, Any],
) -> tuple[Path, dict[str, Any]]:
    path = recipe_path(recipe_id)
    document = load(path)
    actual_key = document.get("key")
    if not isinstance(actual_key, dict):
        raise ValueError(f"{recipe_id}: shaped recipe key is missing")
    normalized_key: dict[str, str] = {}
    for symbol, ingredient in actual_key.items():
        if (
            not isinstance(symbol, str)
            or len(symbol) != 1
            or not isinstance(ingredient, dict)
            or set(ingredient) != {"item"}
            or not isinstance(ingredient["item"], str)
        ):
            raise ValueError(f"{recipe_id}: ingredient {symbol!r} is not an exact item")
        normalized_key[symbol] = ingredient["item"]
    result = document.get("result")
    if (
        document.get("type") != "minecraft:crafting_shaped"
        or document.get("category") != "misc"
        or document.get("pattern") != expected["pattern"]
        or normalized_key != expected["key"]
        or not isinstance(result, dict)
        or result.get("id") != expected["result"]
        or result.get("count") != 1
        or set(result) != {"count", "id"}
    ):
        raise ValueError(f"{recipe_id}: shaped recipe structure or result drifted")
    used_symbols = {
        symbol
        for row in expected["pattern"]
        for symbol in row
        if symbol != " "
    }
    if used_symbols != set(normalized_key):
        raise ValueError(f"{recipe_id}: pattern and key symbols differ")
    return path, document


class Closure:
    def __init__(
        self,
        policy: dict[str, Any],
        gate: dict[str, list[str]],
        casing_by_result: dict[str, str],
        casing_specs: dict[str, dict[str, Any]],
    ) -> None:
        self.policy = policy
        self.gate = gate
        self.casing_by_result = casing_by_result
        self.casing_specs = casing_specs
        self.cache: dict[str, dict[str, Any]] = {}
        self.producer_hashes: dict[str, str] = {}
        self.material_hashes: dict[str, str] = {}
        self.tag_hashes: dict[str, str] = {}

    def resolve(
        self,
        identity: str,
        visiting: tuple[str, ...] = (),
    ) -> dict[str, Any]:
        cached = self.cache.get(identity)
        if cached is not None:
            return cached
        if identity in visiting:
            return self.unreachable(identity, "producer cycle")
        source = self.policy["source_declarations"].get(identity)
        if source is not None:
            material, form = validate_registered_material_item(identity, self.gate)
            evidence = ROOT / source["evidence"]
            document = load(evidence)
            if document.get("id") != material:
                raise ValueError(f"{identity}: source material evidence drifted")
            row = {
                "item": identity,
                "classification": "declared_survival_source",
                "producer": source["evidence"],
                "registered": True,
                "reachable": True,
                "inputs": [],
            }
            self.material_hashes[relative(evidence)] = sha256(evidence)
            self.cache[identity] = row
            return row

        casing_recipe_id = self.casing_by_result.get(identity)
        if casing_recipe_id is not None:
            spec = self.casing_specs[casing_recipe_id]
            inputs = [
                self.resolve(item, (*visiting, identity))
                for item in spec["key"].values()
            ]
            path = recipe_path(casing_recipe_id)
            row = {
                "item": identity,
                "classification": "concrete_shaped_recipe",
                "producer": casing_recipe_id,
                "producer_path": relative(path),
                "registered": True,
                "reachable": all(child["reachable"] for child in inputs),
                "inputs": inputs,
            }
            self.cache[identity] = row
            return row

        parsed = material_item(identity)
        if parsed is None:
            return self.unreachable(identity, "no source or producer declaration")
        material, form = validate_registered_material_item(identity, self.gate)
        producer = self.policy["producer_rules"].get(form)
        if producer is None:
            return self.unreachable(identity, f"no producer rule for form {form}")
        if producer.get("compact_relation"):
            producer_path, input_identities = self.validate_compact_relation(
                identity, material, form, producer
            )
        else:
            producer_path = producer["path"]
            document = load(ROOT / producer_path)
            if document.get("type") != "cruciblecraft:material_rule":
                raise ValueError(
                    f"{producer_path}: producer is not a material rule"
                )
            input_identities = self.validate_material_rule(
                identity, material, form, document, producer
            )
        inputs = [
            self.resolve(child, (*visiting, identity))
            for child in input_identities
        ]
        row = {
            "item": identity,
            "classification": (
                "compact_material_rule"
                if producer.get("compact_relation")
                else "material_rule"
            ),
            "producer": producer_path,
            "registered": True,
            "reachable": all(child["reachable"] for child in inputs),
            "inputs": inputs,
        }
        self.producer_hashes[producer_path] = sha256(ROOT / producer_path)
        self.cache[identity] = row
        return row

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

    def validate_compact_relation(
        self,
        identity: str,
        material: str,
        form: str,
        producer: dict[str, Any],
    ) -> tuple[str, list[str]]:
        matches: list[tuple[str, dict[str, Any]]] = []
        for producer_path in producer.get("paths", []):
            document = load(ROOT / producer_path)
            if document.get("type") != "cruciblecraft:material_rule":
                raise ValueError(
                    f"{producer_path}: producer is not a material rule"
                )
            sparse = document.get("sparse")
            if not isinstance(sparse, dict):
                raise ValueError(
                    f"{producer_path}: compact producer has no sparse payload"
                )
            matches.extend(
                (producer_path, relation)
                for relation in sparse.get("relations", [])
                if relation.get("material") == material
                and relation.get("output", {}).get("prefix") == form
            )
        if len(matches) != 1:
            raise ValueError(
                f"{identity}: expected exactly one compact producer relation"
            )
        producer_path, relation = matches[0]
        actual_inputs = [relation.get("input", {}).get("prefix")]
        if actual_inputs != producer["input_prefixes"]:
            raise ValueError(f"{identity}: compact producer inputs drifted")
        if not str(relation.get("stable_id") or "").startswith(
            f"cruciblecraft:extruder/{form}/{material}/"
        ):
            raise ValueError(f"{identity}: compact producer id drifted")
        return producer_path, [
            f"cruciblecraft:{material}/{prefix}" for prefix in actual_inputs
        ]

    def validate_material_rule(
        self,
        identity: str,
        material: str,
        form: str,
        document: dict[str, Any],
        producer: dict[str, Any],
    ) -> list[str]:
        outputs = document.get("item_outputs")
        inputs = document.get("item_inputs")
        if not isinstance(outputs, list) or not isinstance(inputs, list):
            raise ValueError(f"{identity}: producer item IO is missing")
        output_rows = [
            row
            for row in outputs
            if row.get("prefix") == f"cruciblecraft:{form}"
        ]
        if len(output_rows) != 1:
            raise ValueError(f"{identity}: producer output prefix drifted")
        selector = output_rows[0].get("material_selector")
        if selector is not None:
            prefix, separator, target = selector.partition(":")
            material_path = MATERIAL_ROOT / f"{material}.json"
            material_document = load(material_path)
            selected = (
                material_document.get("gt6_metadata", {})
                .get("processing_targets", {})
                .get(target, {})
                .get("material")
            )
            if (
                separator != ":"
                or prefix != "processing_target"
                or selected != material
            ):
                raise ValueError(
                    f"{identity}: material selector does not preserve identity"
                )
            self.material_hashes[relative(material_path)] = sha256(material_path)
        actual_prefixes = [
            row["prefix"].removeprefix("cruciblecraft:")
            for row in inputs
            if "prefix" in row
        ]
        actual_tags = [
            row["tag"]
            for row in inputs
            if "tag" in row
        ]
        if (
            actual_prefixes != producer["input_prefixes"]
            or actual_tags != producer["input_tags"]
            or len(inputs) != len(actual_prefixes) + len(actual_tags)
        ):
            raise ValueError(f"{identity}: producer input signature drifted")
        result = [
            f"cruciblecraft:{material}/{prefix}"
            for prefix in actual_prefixes
        ]
        for tag in actual_tags:
            declaration = self.policy["tag_sources"].get(tag)
            if declaration is None:
                raise ValueError(f"{identity}: tag {tag} has no source declaration")
            tag_path = ROOT / declaration["tag_resource"]
            tag_document = load(tag_path)
            if declaration["required_value"] not in tag_document.get("values", []):
                raise ValueError(f"{identity}: tag source resource drifted")
            self.tag_hashes[relative(tag_path)] = sha256(tag_path)
            result.append(declaration["item"])
        return result


def ingredient_rows(
    spec: dict[str, Any],
    closure: Closure,
) -> list[dict[str, Any]]:
    occurrences = Counter(
        symbol
        for row in spec["pattern"]
        for symbol in row
        if symbol != " "
    )
    return [
        {
            "symbol": symbol,
            "occurrences": occurrences[symbol],
            **closure.resolve(identity),
        }
        for symbol, identity in sorted(spec["key"].items())
    ]


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    validate_policy(policy)
    registration_document = load(MATERIAL_GATE)
    gate = registration_document.get("materials")
    if not isinstance(gate, dict):
        raise ValueError("material registration gate has no material map")
    registrations = mod_item_registrations()
    casing_specs = policy["casing_recipes"]
    machine_specs = policy["machine_variant_recipes"]
    expected_variants = {
        spec["result"] for spec in machine_specs.values()
    }
    tiers = load(MACHINE_TIERS)
    variants = tiers.get("variants")
    if not isinstance(variants, list):
        raise ValueError("machine tier catalog has no variant rows")
    variant_by_id = {
        row["id"]: row
        for row in variants
        if row.get("id") in expected_variants
    }
    if set(variant_by_id) != expected_variants:
        raise ValueError(
            "independent T15c recipe set differs from its catalog subset"
        )

    casing_by_result = {
        spec["result"]: recipe_id
        for recipe_id, spec in casing_specs.items()
    }
    if len(casing_by_result) != 6:
        raise ValueError("T15c casing result identities are duplicated")
    recipe_hashes: dict[str, str] = {}
    item_rows: dict[str, dict[str, str]] = {}
    for recipe_id, spec in {**casing_specs, **machine_specs}.items():
        path, _ = validate_recipe_document(recipe_id, spec)
        recipe_hashes[relative(path)] = sha256(path)
        registered = registrations.get(spec["item_field"])
        expected_path = spec["result"].partition(":")[2]
        if registered != expected_path:
            raise ValueError(
                f"{recipe_id}: ModItems.{spec['item_field']} registration drifted"
            )
        item_rows[spec["result"]] = {
            "field": spec["item_field"],
            "registered_id": f"cruciblecraft:{registered}",
        }

    for recipe_id, spec in machine_specs.items():
        selected_casing = spec["selected_casing"]
        if selected_casing not in casing_by_result:
            raise ValueError(f"{recipe_id}: selected casing is outside T15c")
        if list(spec["key"].values()).count(selected_casing) != 1:
            raise ValueError(
                f"{recipe_id}: selected casing must occur in exactly one key slot"
            )
        variant = variant_by_id[spec["result"]]
        casing_spec = casing_specs[casing_by_result[selected_casing]]
        casing_material, _ = validate_registered_material_item(
            next(iter(casing_spec["key"].values())), gate
        )
        if variant["material"] != f"cruciblecraft:{casing_material}":
            raise ValueError(
                f"{recipe_id}: selected casing does not match variant material"
            )

    closure = Closure(
        policy, gate, casing_by_result, casing_specs
    )
    casing_rows = []
    machine_rows = []
    for recipe_id, spec in sorted(casing_specs.items()):
        ingredients = ingredient_rows(spec, closure)
        casing_rows.append({
            "recipe_id": recipe_id,
            "result_item": spec["result"],
            "recipe_path": relative(recipe_path(recipe_id)),
            "pattern": spec["pattern"],
            "ingredients": ingredients,
            "reachable": all(row["reachable"] for row in ingredients),
        })
    for recipe_id, spec in sorted(machine_specs.items()):
        ingredients = ingredient_rows(spec, closure)
        machine_rows.append({
            "recipe_id": recipe_id,
            "result_item": spec["result"],
            "selected_casing": spec["selected_casing"],
            "recipe_path": relative(recipe_path(recipe_id)),
            "pattern": spec["pattern"],
            "ingredients": ingredients,
            "reachable": all(row["reachable"] for row in ingredients),
        })

    all_ingredient_rows = [
        ingredient
        for recipe in (*casing_rows, *machine_rows)
        for ingredient in recipe["ingredients"]
    ]
    unreachable = [
        {
            "recipe_id": recipe["recipe_id"],
            "item": ingredient["item"],
            "reason": ingredient.get("reason", "producer input is unreachable"),
        }
        for recipe in (*casing_rows, *machine_rows)
        for ingredient in recipe["ingredients"]
        if not ingredient["reachable"]
    ]
    if unreachable:
        raise ValueError(f"T15c acquisition has unreachable operands: {unreachable}")
    source_rows = [
        closure.resolve(identity)
        for identity in sorted(policy["source_declarations"])
    ]
    if not all(row["reachable"] for row in source_rows):
        raise ValueError("T15c source declarations are not all reachable")

    currentness = {
        "owned_inputs": {
            relative(BUILDER): sha256(BUILDER),
            relative(POLICY): sha256(POLICY),
        },
        "registration_sources": {
            relative(MATERIAL_GATE): sha256(MATERIAL_GATE),
            relative(MOD_ITEMS): sha256(MOD_ITEMS),
            relative(MACHINE_TIERS): sha256(MACHINE_TIERS),
        },
        "crafting_recipes": dict(sorted(recipe_hashes.items())),
        "producer_rules": dict(sorted(closure.producer_hashes.items())),
        "source_materials": dict(sorted(closure.material_hashes.items())),
        "tag_sources": dict(sorted(closure.tag_hashes.items())),
    }
    return {
        "schema_version": 1,
        "status": "T15C_MACHINE_ACQUISITION_READY",
        "delivery": "T15c",
        "expected_set": {
            "source": policy["expected_set_source"],
            "production_mapping_reused": policy["production_mapping_reused"],
            "casing_recipe_ids": sorted(casing_specs),
            "machine_variant_recipe_ids": sorted(machine_specs),
        },
        "counts": {
            "casing_recipes": len(casing_rows),
            "machine_variant_recipes": len(machine_rows),
            "ingredient_rows": len(all_ingredient_rows),
            "registered_results": len(item_rows),
            "source_declarations": len(source_rows),
            "unreachable": len(unreachable),
        },
        "item_registrations": dict(sorted(item_rows.items())),
        "source_declarations": source_rows,
        "casing_recipes": casing_rows,
        "machine_variant_recipes": machine_rows,
        "unreachable": unreachable,
        "currentness": currentness,
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
    OUTPUT.write_bytes(stable(document).encode("utf-8"))
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T15c acquisition artifact is stale",
    )
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
        print(f"T15c acquisition failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "casings": document["counts"]["casing_recipes"],
        "variants": document["counts"]["machine_variant_recipes"],
        "unreachable": document["counts"]["unreachable"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
