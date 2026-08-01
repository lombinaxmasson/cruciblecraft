#!/usr/bin/env python3
"""Build the committed GT6 recipe-operand projection and material form gate."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
MATERIALS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
OPERANDS_OUT = TOOLS / "gt6_l1b_selected_recipe_operands.json"
ORE_CHAIN_OPERANDS = TOOLS / "gt6_ore_chain_operands.json"
GATE_OUT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def compact_json(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def semantic_summary(recipe: dict[str, Any], operands: list[dict[str, Any]]) -> str:
    fixed_items = []
    for side in ("inputs", "outputs"):
        for item in recipe.get(side) or []:
            fixed_items.append({
                "side": side,
                "item": str((item or {}).get("item") or ""),
                "meta": (item or {}).get("meta"),
                "count": (item or {}).get("count"),
            })
    fluids = []
    for side in ("fluidInputs", "fluidOutputs"):
        for fluid in recipe.get(side) or []:
            fluids.append({
                "side": side,
                "fluid": str((fluid or {}).get("fluid") or ""),
                "amount": (fluid or {}).get("amount"),
            })
    canonical = {
        "eu_per_tick": recipe.get("euPerTick"),
        "duration": recipe.get("duration"),
        "special_value": recipe.get("specialValue"),
        "items": fixed_items,
        "fluids": fluids,
        "operands": operands,
    }
    encoded = json.dumps(
        canonical, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()[:20]


def material_documents() -> tuple[dict[str, dict[str, Any]], dict[str, set[str]]]:
    sys.path.insert(0, str(TOOLS))
    import gt6_l3_materials  # noqa: WPS433

    l3_document = load(gt6_l3_materials.OUT)
    documents: dict[str, dict[str, Any]] = {}
    forms: dict[str, set[str]] = {}
    for filename in load(MATERIALS / "index.json"):
        document = load(MATERIALS / filename)
        material_id = document["id"]
        documents[material_id] = document
        forms[material_id] = gt6_l3_materials.resolve_material_forms(
            document, l3_document
        )
    return documents, forms


def ore_source_required_forms() -> set[str]:
    sys.path.insert(0, str(TOOLS))
    import gt6_l3_materials  # noqa: WPS433

    return {
        "ore",
        "raw_ore",
        *gt6_l3_materials.prefix_implication_closures()["ore"],
    }


def build_documents() -> tuple[dict[str, Any], dict[str, Any]]:
    selected_path = TOOLS / "gt6_l1b_selected.json"
    cross_path = TOOLS / "gt6_oredict_cross_reference.json"
    mapping_path = TOOLS / "gt6_prefix_mapping.json"
    policy_path = TOOLS / "gt6_material_activation_policy.json"
    selected = load(selected_path)
    cross = load(cross_path)
    policy = load(policy_path)
    material_docs, factual_forms = material_documents()

    selected_ids = {
        int(record["source_id"])
        for record in selected["records"].values()
        if record.get("layer") == "CORE" and int(record.get("source_id", -1)) >= 0
    }
    prefix_by_item = cross["prefix_item_to_gt_prefix"]
    prefix_dictionary = sorted(set(prefix_by_item.values()))
    prefix_indexes = {
        prefix: index for index, prefix in enumerate(prefix_dictionary)
    }
    fluid_map = load(DUMP / "oredict" / "fluid_map.json")
    map_index_path = DUMP / "index.json"
    map_names = sorted(
        entry["nameInternal"]
        for entry in load(map_index_path)["maps"]
        if entry.get("nameInternal")
    )
    map_indexes = {map_name: index for index, map_name in enumerate(map_names)}

    recipes: list[dict[str, Any]] = []
    projected_pairs: set[tuple[int, str]] = set()
    map_hashes: dict[str, str] = {}
    selected_recipe_counts: Counter[str] = Counter()
    unmapped_item_counts: Counter[str] = Counter()

    for map_name in map_names:
        path = DUMP / "maps" / f"{map_name}.json"
        map_hashes[map_name] = sha256(path)
        data = load(path)
        if data.get("nameInternal") != map_name:
            raise ValueError(f"recipe map identity mismatch: {map_name}")
        duplicate_ordinals: Counter[str] = Counter()
        for recipe in data.get("recipes") or []:
            if recipe.get("enabled") is False:
                continue
            operands: set[tuple[int, str, str]] = set()
            touched_ids: set[int] = set()
            for side, gate_side in (("inputs", "input"), ("outputs", "output")):
                for item in recipe.get(side) or []:
                    item_id = str((item or {}).get("item") or "")
                    material_id = (item or {}).get("meta")
                    if not isinstance(material_id, int) or material_id < 0:
                        continue
                    if item_id.startswith("gregtech:gt.meta."):
                        touched_ids.add(material_id)
                    gt_prefix = prefix_by_item.get(item_id)
                    if gt_prefix is None:
                        if item_id.startswith("gregtech:gt.meta."):
                            unmapped_item_counts[item_id] += 1
                        continue
                    operands.add((material_id, gt_prefix, gate_side))
            for side in ("fluidInputs", "fluidOutputs"):
                for fluid in recipe.get(side) or []:
                    mapped = fluid_map.get(str((fluid or {}).get("fluid") or ""))
                    if mapped and int(mapped.get("materialId", -1)) >= 0:
                        touched_ids.add(int(mapped["materialId"]))
            if not (touched_ids & selected_ids):
                continue
            normalized_for_summary = [
                {
                    "source_id": material_id,
                    "gt_prefix": gt_prefix,
                    "side": side,
                }
                for material_id, gt_prefix, side in sorted(operands)
            ]
            summary = semantic_summary(recipe, normalized_for_summary)
            ordinal = duplicate_ordinals[summary]
            duplicate_ordinals[summary] += 1
            recipes.append([
                map_indexes[map_name],
                summary,
                ordinal,
                [
                    [
                        material_id,
                        prefix_indexes[gt_prefix],
                        0 if side == "input" else 1,
                    ]
                    for material_id, gt_prefix, side in sorted(operands)
                ],
            ])
            selected_recipe_counts[map_name] += 1
            projected_pairs.update((material_id, prefix) for material_id, prefix, _ in operands)

    cc_by_source_id = {
        int(source_id): cc_id
        for source_id, cc_id in cross["material_id_to_cc"].items()
    }
    cc_form_by_gt_prefix = cross["gt_prefix_to_cc"]
    recipe_forms: dict[str, set[str]] = defaultdict(set)
    ignored_pairs: list[dict[str, Any]] = []
    for source_id, gt_prefix in sorted(projected_pairs):
        material_id = cc_by_source_id.get(source_id)
        cc_form = cc_form_by_gt_prefix.get(gt_prefix)
        reason = None
        if material_id is None:
            reason = "source material is not in the stable imported catalog"
        elif cc_form is None:
            reason = "GT6 prefix has no CC prefix mapping"
        elif cc_form not in factual_forms[material_id]:
            reason = "operand form is absent from the material's GT6 factual forms"
        if reason is not None:
            ignored_pairs.append({
                "source_id": source_id,
                "gt_prefix": gt_prefix,
                "reason": reason,
            })
            continue
        recipe_forms[material_id].add(cc_form)

    pre_gate_forms = {
        record["cc_id"]: set(record.get("pre_gate_registered_forms") or [])
        for record in policy["records"]
        if record.get("cc_id")
    }
    ore_chain_forms: dict[str, set[str]] = defaultdict(set)
    if ORE_CHAIN_OPERANDS.is_file():
        for row in load(ORE_CHAIN_OPERANDS)["recipes"]:
            for operand in row["operands"]:
                material_id = operand["material"]
                form = operand["form"]
                if material_id not in factual_forms:
                    raise ValueError(
                        f"ore-chain operand references unknown material: {material_id}"
                    )
                if form not in factual_forms[material_id]:
                    raise ValueError(
                        f"ore-chain operand exceeds factual forms: {material_id}/{form}"
                    )
                ore_chain_forms[material_id].add(form)
    # Ore blocks are a registration source in their own right. Do not infer this
    # domain from crusher/raw-ore recipe operands: only factual `ore` declarations
    # may allocate the paired stone and deepslate registries.
    required_ore_source_forms = ore_source_required_forms()
    ore_source_forms: dict[str, set[str]] = {}
    for material_id, forms in factual_forms.items():
        if "ore" not in forms:
            continue
        missing = required_ore_source_forms - forms
        if missing:
            raise ValueError(
                f"factual ore source lacks required source forms: "
                f"{material_id}/{sorted(missing)}"
            )
        ore_source_forms[material_id] = set(required_ore_source_forms)
    compatibility_forms: dict[str, list[str]] = {}
    gated_forms: dict[str, list[str]] = {}
    for material_id in sorted(material_docs):
        factual = factual_forms[material_id]
        recipe_selected = recipe_forms.get(material_id, set())
        ore_chain_selected = ore_chain_forms.get(material_id, set())
        ore_source_selected = ore_source_forms.get(material_id, set())
        selected_forms = recipe_selected | ore_chain_selected | ore_source_selected
        compatibility = (
            pre_gate_forms.get(material_id, set()) - selected_forms
        ) & factual
        if compatibility:
            compatibility_forms[material_id] = sorted(compatibility)
        gated_forms[material_id] = sorted(selected_forms | compatibility)

    inputs = {
        "selection": {
            "path": "tools/gt6_l1b_selected.json",
            "sha256": sha256(selected_path),
            "selected_layer": "CORE",
        },
        "prefix_mapping": {
            "path": "tools/gt6_prefix_mapping.json",
            "sha256": sha256(mapping_path),
        },
        "cross_reference": {
            "path": "tools/gt6_oredict_cross_reference.json",
            "sha256": sha256(cross_path),
        },
        "material_policy": {
            "path": "tools/gt6_material_activation_policy.json",
            "sha256": sha256(policy_path),
        },
        "recipe_map_index": {
            "path": "gt6_dump/gt6_recipe_dump/index.json",
            "sha256": sha256(map_index_path),
        },
        "recipe_maps": map_hashes,
    }
    operand_document = {
        "schema_version": 1,
        "encoding": {
            "recipe": "[map_index, semantic_summary, duplicate_ordinal, operands]",
            "operand": "[source_id, gt_prefix_index, side_index]",
            "side_dictionary": ["input", "output"],
            "map_dictionary": map_names,
            "gt_prefix_dictionary": prefix_dictionary,
        },
        "selection_rule": (
            "enabled recipe touches at least one CORE material; project all mappable "
            "GT6 material-prefix item operands"
        ),
        "inputs": inputs,
        "counts": {
            "selected_materials": len(selected_ids),
            "selected_recipes": len(recipes),
            "projected_material_prefix_pairs": len(projected_pairs),
            "ignored_gate_pairs": len(ignored_pairs),
        },
        "selected_recipes_by_map": dict(sorted(selected_recipe_counts.items())),
        "unmapped_gt_meta_items": dict(sorted(unmapped_item_counts.items())),
        "ignored_gate_pairs": ignored_pairs,
        "recipes": recipes,
    }
    gate_document = {
        "schema_version": 1,
        "sources": {
            "l1b_recipe_operands": {
                "path": "tools/gt6_l1b_selected_recipe_operands.json",
                "sha256": hashlib.sha256(
                    compact_json(operand_document).encode("utf-8")
                ).hexdigest(),
            },
            "ore_chain_operands": (
                {
                    "path": "tools/gt6_ore_chain_operands.json",
                    "sha256": sha256(ORE_CHAIN_OPERANDS),
                }
                if ORE_CHAIN_OPERANDS.is_file()
                else None
            ),
            "factual_ore_sources": {
                "path": "src/main/resources/data/cruciblecraft/materials/*.json",
                "field": "resolved factual prefix `ore`",
            },
        },
        "policy": (
            "recipe-projected forms plus factual ore-source closure registrations and "
            "explicit compatibility retention from the pre-gate registered catalog"
        ),
        "compatibility_baseline": {
            "path": "tools/gt6_material_activation_policy.json",
            "sha256": sha256(policy_path),
            "field": "records[].pre_gate_registered_forms",
            "recursive": False,
            "note": (
                "audit-only legacy baseline; the builder never reads the previous "
                "material_registration_gate.json"
            ),
        },
        "counts": {
            "materials": len(gated_forms),
            "metadata_only_materials": sum(
                bool(document.get("metadata_only")) for document in material_docs.values()
            ),
            "recipe_gated_forms": sum(map(len, recipe_forms.values())),
            "ore_chain_forms": sum(map(len, ore_chain_forms.values())),
            "ore_source_materials": len(ore_source_forms),
            "ore_source_forms": sum(map(len, ore_source_forms.values())),
            "ore_source_blocks": len(ore_source_forms) * 2,
            "compatibility_forms": sum(map(len, compatibility_forms.values())),
            "registered_forms": sum(map(len, gated_forms.values())),
        },
        "compatibility_forms": compatibility_forms,
        "materials": gated_forms,
    }
    return operand_document, gate_document


def check_or_write(path: Path, content: str, write: bool) -> bool:
    if write:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return True
    return path.is_file() and path.read_text(encoding="utf-8") == content


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--review", action="store_true")
    args = parser.parse_args()
    if args.write and args.check:
        parser.error("--write and --check are mutually exclusive")
    operands, gate = build_documents()
    if args.review:
        print(stable_json({
            "operand_counts": operands["counts"],
            "gate_counts": gate["counts"],
            "selected_recipes_by_map": operands["selected_recipes_by_map"],
            "compatibility_materials": len(gate["compatibility_forms"]),
        }), end="")
        return 0
    write = args.write
    results = [
        check_or_write(OPERANDS_OUT, compact_json(operands), write),
        check_or_write(GATE_OUT, stable_json(gate), write),
    ]
    if write:
        print(f"Wrote {OPERANDS_OUT}")
        print(f"Wrote {GATE_OUT}")
        return 0
    if all(results):
        print("GT6 material form gate artifacts are current")
        return 0
    print("GT6 material form gate artifacts are stale; run with --write", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
