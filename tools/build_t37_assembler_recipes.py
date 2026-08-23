#!/usr/bin/env python3
"""Generate T37 R3 Assembler compact families from the frozen R0 source."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

SOURCE = ROOT / "tools/t37_assembler_source.json"
OUTPUT_ROOT = (
    ROOT
    / "src/t37_recipe_generated/resources/data/cruciblecraft/recipe/t37/assembler"
)
OPERAND_MAP = ROOT / "tools/t37_operand_runtime_map.json"
PLAYER_PATH = ROOT / "tools/t37_player_path.json"
EQUIVALENCE = ROOT / "tools/t37_assembler_equivalence.json"

EXPECTED_FAMILY_COUNT = 50
EXPECTED_FAMILY_NUMBERS = range(2, 52)
PLANK_ITEM = "gregtech:gt.block.planks"
CIRCUIT_ITEM = "gregapi:gt.integrated_circuit"
PROGRAMMED_CIRCUIT = "cruciblecraft:programmed_circuit"
CIRCUIT_CONFIG = "cruciblecraft:circuit_config"
VANILLA_PLANKS = [
    "minecraft:oak_planks",
    "minecraft:spruce_planks",
    "minecraft:birch_planks",
    "minecraft:jungle_planks",
    "minecraft:acacia_planks",
    "minecraft:dark_oak_planks",
    "minecraft:mangrove_planks",
    "minecraft:cherry_planks",
    "minecraft:bamboo_planks",
    "minecraft:crimson_planks",
    "minecraft:warped_planks",
]
NAME_HINTS = (
    ("rubber", "minecraft:oak_planks"),
    ("dark oak", "minecraft:dark_oak_planks"),
    ("darkoak", "minecraft:dark_oak_planks"),
    ("spruce", "minecraft:spruce_planks"),
    ("birch", "minecraft:birch_planks"),
    ("jungle", "minecraft:jungle_planks"),
    ("acacia", "minecraft:acacia_planks"),
    ("mangrove", "minecraft:mangrove_planks"),
    ("cherry", "minecraft:cherry_planks"),
    ("bamboo", "minecraft:bamboo_planks"),
    ("crimson", "minecraft:crimson_planks"),
    ("warped", "minecraft:warped_planks"),
    ("oak", "minecraft:oak_planks"),
)


def stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def digest_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def digest_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_source() -> dict[str, Any]:
    document = json.loads(SOURCE.read_text(encoding="utf-8"))
    relations = document.get("relations") or []
    if len(relations) != EXPECTED_FAMILY_COUNT:
        raise ValueError(
            f"Frozen T37 source must contain {EXPECTED_FAMILY_COUNT} relations"
        )
    numbers = []
    for relation in relations:
        template = relation["template_key"]
        if not template.startswith("gt.recipe.assembler#"):
            raise ValueError(f"Unexpected template_key: {template}")
        numbers.append(int(template.rsplit("#", 1)[1]))
    if sorted(numbers) != list(EXPECTED_FAMILY_NUMBERS):
        raise ValueError("Frozen T37 source family set drifted from #0002-#0051")
    return document


def source_operand_key(source: dict[str, Any]) -> tuple[str, int | None, str]:
    return (
        str(source.get("item") or ""),
        source.get("meta"),
        str(source.get("displayName") or ""),
    )


def collect_source_operands(
        relations: list[dict[str, Any]],
) -> dict[str, list[tuple[str, int | None, str]]]:
    groups: dict[str, set[tuple[str, int | None, str]]] = {
        "planks": set(),
        "circuits": set(),
        "outputs": set(),
        "other_inputs": set(),
    }
    for relation in relations:
        for operand in relation.get("item_inputs") or []:
            source = operand.get("source") or {}
            key = source_operand_key(source)
            if key[0] == PLANK_ITEM:
                groups["planks"].add(key)
            elif key[0] == CIRCUIT_ITEM:
                groups["circuits"].add(key)
            else:
                groups["other_inputs"].add(key)
        for operand in relation.get("item_outputs") or []:
            source = operand.get("source") or {}
            groups["outputs"].add(source_operand_key(source))
    return {name: sorted(values, key=lambda row: (row[0], row[1] or -1, row[2]))
            for name, values in groups.items()}


def assign_planks(
        unique: list[tuple[str, int | None, str]],
) -> dict[int, dict[str, Any]]:
    assigned: dict[int, dict[str, Any]] = {}
    used: set[str] = set()
    for _item, meta, display_name in unique:
        if meta is None:
            raise ValueError("Plank operand is missing meta")
        lowered = display_name.lower()
        chosen = None
        klass = "SOURCE_DERIVED"
        reason = (
            "GT6 wood plank block has no CC twin; vanilla plank is S0 and "
            "pairs with the already-aliased vanilla wood product."
        )
        for hint, plank in NAME_HINTS:
            if hint in lowered:
                chosen = plank
                if hint == "rubber":
                    reason = (
                        "Rubberwood rows already emit oak wood products, so "
                        "the consume plank is SOURCE_DERIVED to oak_planks."
                    )
                break
        if chosen is None or chosen in used:
            unused = [plank for plank in VANILLA_PLANKS if plank not in used]
            if not unused:
                raise ValueError("No unused vanilla plank remains for T37 woods")
            chosen = unused[0]
            klass = "DESIGN_POLICY"
            reason = (
                "No unused name-matched vanilla twin remained; assigned the "
                "next unused vanilla plank so consume identities stay distinct."
            )
        used.add(chosen)
        assigned[meta] = {
            "class": klass,
            "display_name": display_name,
            "reason": reason,
            "runtime_id": chosen,
        }
    return assigned


def assign_circuits(
        unique: list[tuple[str, int | None, str]],
) -> dict[int, dict[str, Any]]:
    assigned: dict[int, dict[str, Any]] = {}
    for _item, meta, display_name in unique:
        if meta is None:
            raise ValueError("Circuit operand is missing meta")
        assigned[meta] = {
            "class": "DESIGN_POLICY",
            "display_name": display_name,
            "reason": (
                "CC has no GT6 Selector Tag; a programmed circuit item plus "
                f"{CIRCUIT_CONFIG} preserves the catalyst config that keeps "
                "same-plank families from shadowing each other."
            ),
            "runtime_id": PROGRAMMED_CIRCUIT,
            "component": CIRCUIT_CONFIG,
            "value": meta,
        }
    return assigned


def runtime_item_id(operand: dict[str, Any]) -> str:
    runtime_id = operand.get("runtime_id") or operand.get("alias")
    if not isinstance(runtime_id, str) or ":" not in runtime_id:
        raise ValueError(f"Operand is missing a runtime id: {operand}")
    if runtime_id.startswith("fixed:") or runtime_id.startswith("gregtech:"):
        raise ValueError(f"Refusing to emit a GT id as a runtime item: {runtime_id}")
    if runtime_id.startswith("gregapi:"):
        raise ValueError(f"Refusing to emit a GT id as a runtime item: {runtime_id}")
    return runtime_id


def runtime_stable_id(source_stable_id: str) -> str:
    hex16 = source_stable_id.rsplit("/", 1)[-1]
    if len(hex16) != 16 or any(char not in "0123456789abcdef" for char in hex16):
        raise ValueError(f"Frozen stable_id suffix is not 16 hex chars: {source_stable_id}")
    return f"cruciblecraft:t37/{hex16}"


def family_filename(template_key: str) -> str:
    return template_key.replace(".", "_").replace("#", "_") + ".json"


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action["kind"]).lower()
    emitted: dict[str, Any] = {"kind": kind}
    if kind == "wear":
        emitted["damage"] = int(action.get("damage") or 0)
    return emitted


def emit_item_input(
        operand: dict[str, Any],
        plank_map: dict[int, dict[str, Any]],
        circuit_map: dict[int, dict[str, Any]],
) -> tuple[dict[str, Any], str]:
    source = operand.get("source") or {}
    item = source.get("item")
    meta = source.get("meta")
    if item == PLANK_ITEM:
        mapped = plank_map[int(meta)]
        return {"item": mapped["runtime_id"]}, mapped["class"]
    if item == CIRCUIT_ITEM:
        mapped = circuit_map[int(meta)]
        return {
            "type": "neoforge:components",
            "items": PROGRAMMED_CIRCUIT,
            "components": {CIRCUIT_CONFIG: mapped["value"]},
        }, mapped["class"]
    runtime_id = runtime_item_id(operand)
    if not (runtime_id.startswith("minecraft:") or runtime_id.startswith("cruciblecraft:")):
        raise ValueError(f"Consume operand is not player-reachable: {runtime_id}")
    klass = (
        "SOURCE_DERIVED"
        if operand.get("mapping") == "source_derived_alias"
        else "SOURCE_BACKED"
    )
    return {"item": runtime_id}, klass


def emit_item_output(operand: dict[str, Any]) -> tuple[dict[str, Any], str]:
    runtime_id = runtime_item_id(operand)
    if not (runtime_id.startswith("minecraft:") or runtime_id.startswith("cruciblecraft:")):
        raise ValueError(f"Output operand is not a registered CC/vanilla item: {runtime_id}")
    count = int((operand.get("source") or {}).get("count") or 1)
    klass = (
        "SOURCE_DERIVED"
        if operand.get("mapping") == "source_derived_alias"
        else "SOURCE_BACKED"
    )
    return {"count": count, "id": runtime_id}, klass


def relation_source_kind(classes: list[str]) -> str:
    if "DESIGN_POLICY" in classes:
        return "DESIGN_POLICY"
    if "SOURCE_DERIVED" in classes:
        return "SOURCE_DERIVED"
    return "SOURCE_BACKED"


def emit_family(
        relation: dict[str, Any],
        plank_map: dict[int, dict[str, Any]],
        circuit_map: dict[int, dict[str, Any]],
) -> dict[str, Any]:
    classes: list[str] = []
    item_inputs: list[dict[str, Any]] = []
    for operand in relation["item_inputs"]:
        emitted, klass = emit_item_input(operand, plank_map, circuit_map)
        item_inputs.append(emitted)
        classes.append(klass)
    item_outputs: list[dict[str, Any]] = []
    for operand in relation["item_outputs"]:
        emitted, klass = emit_item_output(operand)
        item_outputs.append(emitted)
        classes.append(klass)
    counts = [int(value) for value in relation["item_input_counts"]]
    actions = [emit_action(action) for action in relation["item_input_actions"]]
    for count, action in zip(counts, actions, strict=True):
        if action["kind"] in {"preserve", "wear"} and count != 0:
            raise ValueError("PRESERVE/WEAR counts must be 0")
        if action["kind"] == "consume" and count <= 0:
            raise ValueError("CONSUME counts must be positive")
    template_key = relation["template_key"]
    return {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": relation["target_map"],
        "source_revision": relation["source_revision"],
        "relations": [
            {
                "stable_id": runtime_stable_id(relation["stable_id"]),
                "item_inputs": item_inputs,
                "item_input_counts": counts,
                "item_input_actions": actions,
                "item_outputs": item_outputs,
                "fluid_inputs": [],
                "fluid_outputs": [],
                "output_chances": [int(value) for value in relation["output_chances"]],
                "duration": int(relation["duration"]),
                "eut": int(relation["eut"]),
                "special_value": int(relation["special_value"]),
                "can_be_buffered": bool(relation["can_be_buffered"]),
                "shadow_order": int(relation["shadow_order"]),
                "provenance": {
                    "source_kind": relation_source_kind(classes),
                    "selected_source_recipe": template_key,
                    "evidence_hashes": [relation["source_row_sha256"]],
                },
            }
        ],
    }


def consume_identity(document: dict[str, Any]) -> str:
    relation = document["relations"][0]
    return json.dumps(
        {
            "item_inputs": relation["item_inputs"],
            "item_input_counts": relation["item_input_counts"],
            "item_input_actions": relation["item_input_actions"],
        },
        sort_keys=True,
    )


def operand_map_document(
        unique: dict[str, list[tuple[str, int | None, str]]],
        plank_map: dict[int, dict[str, Any]],
        circuit_map: dict[int, dict[str, Any]],
        relations: list[dict[str, Any]],
) -> dict[str, Any]:
    operands: list[dict[str, Any]] = []
    for item, meta, display_name in unique["planks"]:
        mapped = plank_map[int(meta)]
        operands.append({
            "class": mapped["class"],
            "reason": mapped["reason"],
            "runtime": {"id": mapped["runtime_id"], "kind": "item"},
            "source": {
                "display_name": display_name,
                "item": item,
                "meta": meta,
            },
        })
    for item, meta, display_name in unique["circuits"]:
        mapped = circuit_map[int(meta)]
        operands.append({
            "class": mapped["class"],
            "reason": mapped["reason"],
            "runtime": {
                "component": mapped["component"],
                "id": mapped["runtime_id"],
                "kind": "component_item",
                "value": mapped["value"],
            },
            "source": {
                "display_name": display_name,
                "item": item,
                "meta": meta,
            },
        })
    for item, meta, display_name in unique["other_inputs"]:
        operands.append({
            "class": "SOURCE_BACKED",
            "reason": (
                "R0 already pinned this consume operand to a registered "
                "vanilla item id."
            ),
            "runtime": {"id": item, "kind": "item"},
            "source": {
                "display_name": display_name,
                "item": item,
                "meta": meta,
            },
        })
    seen_outputs: set[tuple[str, int | None, str, str]] = set()
    for relation in relations:
        for operand in relation["item_outputs"]:
            source = operand.get("source") or {}
            runtime_id = runtime_item_id(operand)
            key = (*source_operand_key(source), runtime_id)
            if key in seen_outputs:
                continue
            seen_outputs.add(key)
            operands.append({
                "class": (
                    "SOURCE_DERIVED"
                    if operand.get("mapping") == "source_derived_alias"
                    else "SOURCE_BACKED"
                ),
                "reason": (
                    "R0 already aliased this GT6 wood product to a registered "
                    "vanilla item."
                ),
                "runtime": {"id": runtime_id, "kind": "item"},
                "source": {
                    "display_name": source.get("displayName"),
                    "item": source.get("item"),
                    "meta": source.get("meta"),
                },
            })
    operands.append({
        "class": "DESIGN_POLICY",
        "reason": (
            "Paper is S0; one paper crafts the programmed circuit that stands "
            "in for GT6 Selector Tags."
        ),
        "runtime": {"id": PROGRAMMED_CIRCUIT, "kind": "crafted_item"},
        "source": {
            "display_name": "Paper",
            "item": "minecraft:paper",
            "meta": 0,
        },
    })
    operands.sort(key=lambda row: (
        row["source"]["item"] or "",
        row["source"]["meta"] if row["source"]["meta"] is not None else -1,
        row["runtime"]["id"],
    ))
    return {
        "circuit_config_component": CIRCUIT_CONFIG,
        "circuit_item": PROGRAMMED_CIRCUIT,
        "operand_count": len(operands),
        "operands": operands,
    }


def player_path_document(
        families: list[dict[str, Any]],
) -> dict[str, Any]:
    rows: list[dict[str, Any]] = []
    for document in families:
        relation = document["relations"][0]
        consume_ids: list[str] = []
        preserve_ids: list[str] = []
        for ingredient, action in zip(
                relation["item_inputs"],
                relation["item_input_actions"],
                strict=True,
        ):
            item_id = ingredient.get("item") or ingredient.get("items")
            if action["kind"] == "preserve":
                preserve_ids.append(item_id)
            else:
                consume_ids.append(item_id)
        output_ids = [stack["id"] for stack in relation["item_outputs"]]
        reachable = all(
            item_id.startswith("minecraft:") or item_id.startswith("cruciblecraft:")
            for item_id in consume_ids + preserve_ids
        )
        registered = all(
            item_id.startswith("minecraft:") or item_id.startswith("cruciblecraft:")
            for item_id in output_ids
        )
        rows.append({
            "consume_ids": consume_ids,
            "inputs_reachable": reachable,
            "output_ids": output_ids,
            "outputs_registered": registered,
            "preserve_ids": preserve_ids,
            "stable_id": relation["stable_id"],
            "template_key": document["family_id"],
        })
    return {
        "families": len(rows),
        "frozen_review_unmodified": True,
        "inputs_reachable": sum(1 for row in rows if row["inputs_reachable"]),
        "note": (
            "R0 review still lists inputs_reachable false because frozen "
            "source relations were not rewritten. This file is the R3 "
            "player-path proof after plank and circuit mapping."
        ),
        "outputs_registered": sum(1 for row in rows if row["outputs_registered"]),
        "rows": rows,
    }


def planned_documents() -> dict[str, str]:
    source = load_source()
    relations = source["relations"]
    unique = collect_source_operands(relations)
    unknown_inputs = [
        key for key in unique["other_inputs"]
        if not key[0].startswith("minecraft:")
        and not key[0].startswith("cruciblecraft:")
    ]
    if unknown_inputs:
        raise ValueError(f"Unmapped consume operands: {unknown_inputs}")
    plank_map = assign_planks(unique["planks"])
    circuit_map = assign_circuits(unique["circuits"])
    files: dict[str, str] = {}
    families: list[dict[str, Any]] = []
    identities: dict[str, str] = {}
    for relation in sorted(relations, key=lambda row: row["template_key"]):
        document = emit_family(relation, plank_map, circuit_map)
        if "parameterized" in document:
            raise ValueError("Generated compact families must omit parameterized")
        identity = consume_identity(document)
        previous = identities.get(identity)
        if previous is not None:
            raise ValueError(
                "Consume-side identity collision between "
                f"{previous} and {document['family_id']}"
            )
        identities[identity] = document["family_id"]
        filename = family_filename(document["family_id"])
        files[filename] = stable(document)
        families.append(document)
    operand_document = operand_map_document(unique, plank_map, circuit_map, relations)
    player_document = player_path_document(families)
    if player_document["inputs_reachable"] != EXPECTED_FAMILY_COUNT:
        raise ValueError("Player-path inputs are not fully reachable")
    if player_document["outputs_registered"] != EXPECTED_FAMILY_COUNT:
        raise ValueError("Player-path outputs are not fully registered")
    generated_hashes = {
        name: digest_text(content) for name, content in sorted(files.items())
    }
    stable_ids = [
        document["relations"][0]["stable_id"] for document in families
    ]
    equivalence = {
        "generated": {
            "file_count": len(files),
            "files": generated_hashes,
            "root": OUTPUT_ROOT.relative_to(ROOT).as_posix(),
            "sha256": digest_text(stable(generated_hashes)),
        },
        "runtime_expected": {
            "logical_ids": len(stable_ids),
            "sha256": digest_text(stable(stable_ids)),
            "stable_ids": stable_ids,
        },
        "source": {
            "family_count": len(relations),
            "path": SOURCE.relative_to(ROOT).as_posix(),
            "relation_count": len(relations),
            "sha256": digest_file(SOURCE),
            "source_revision": relations[0]["source_revision"],
        },
    }
    files["__sidecars__"] = json.dumps({
        "equivalence": stable(equivalence),
        "operand_map": stable(operand_document),
        "player_path": stable(player_document),
    })
    return files


def split_planned(files: dict[str, str]) -> tuple[dict[str, str], dict[str, str]]:
    sidecars = json.loads(files["__sidecars__"])
    recipes = {name: content for name, content in files.items() if name != "__sidecars__"}
    sidecar_files = {
        OPERAND_MAP.name: sidecars["operand_map"],
        PLAYER_PATH.name: sidecars["player_path"],
        EQUIVALENCE.name: sidecars["equivalence"],
    }
    return recipes, sidecar_files


def actual_recipe_files() -> dict[str, Path]:
    if not OUTPUT_ROOT.is_dir():
        return {}
    return {
        path.name: path
        for path in OUTPUT_ROOT.glob("*.json")
        if path.is_file()
    }


def check() -> list[str]:
    planned = planned_documents()
    recipes, sidecars = split_planned(planned)
    actual = actual_recipe_files()
    errors = [
        f"generated file set drift: {name}"
        for name in sorted(set(recipes) ^ set(actual))
    ]
    for name in sorted(set(recipes) & set(actual)):
        if actual[name].read_text(encoding="utf-8") != recipes[name]:
            errors.append(f"stale generated recipe: {name}")
    sidecar_paths = {
        OPERAND_MAP.name: OPERAND_MAP,
        PLAYER_PATH.name: PLAYER_PATH,
        EQUIVALENCE.name: EQUIVALENCE,
    }
    for name, path in sidecar_paths.items():
        if not path.is_file() or path.read_text(encoding="utf-8") != sidecars[name]:
            errors.append(f"stale T37 sidecar: {name}")
    return errors


def write() -> None:
    planned = planned_documents()
    recipes, sidecars = split_planned(planned)
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    for path in actual_recipe_files().values():
        if path.name not in recipes:
            path.unlink()
    for name, content in recipes.items():
        (OUTPUT_ROOT / name).write_text(content, encoding="utf-8", newline="\n")
    OPERAND_MAP.write_text(sidecars[OPERAND_MAP.name], encoding="utf-8", newline="\n")
    PLAYER_PATH.write_text(sidecars[PLAYER_PATH.name], encoding="utf-8", newline="\n")
    EQUIVALENCE.write_text(sidecars[EQUIVALENCE.name], encoding="utf-8", newline="\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        errors = check()
        if errors:
            print("T37 assembler recipes are stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T37 assembler recipes match the frozen compact source.")
        return 0
    write()
    print(f"Wrote {EXPECTED_FAMILY_COUNT} T37 assembler compact families.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
