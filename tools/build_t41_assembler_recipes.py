#!/usr/bin/env python3
"""Generate T41 Assembler compact families from the frozen catalog source."""
from __future__ import annotations

import hashlib
import json
import re
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t41_common as common  # noqa: E402
from tools.recipe_bulk.write_guard import assert_legacy_production_write_forbidden  # noqa: E402

SOURCE = common.SOURCE
OUTPUT_ROOT = common.GENERATED_ROOT
FIXTURE_ROOT = common.CATALOG_FIXTURE_ROOT
OPERAND_MAP = common.OPERAND_RUNTIME_MAP
PLAYER_PATH = common.PLAYER_PATH
EQUIVALENCE = common.EQUIVALENCE
REQUIRED_FORMS = common.REQUIRED_FORMS
STABLE_ID_RE = re.compile(r"^cruciblecraft:t41/[0-9a-f]{16}$")
TEMPLATE_RE = re.compile(r"^gt\.recipe\.assembler#(\d{4})$")
REACHABLE_PREFIXES = ("minecraft:", "cruciblecraft:")


def stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def digest_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def runtime_stable_id(source_stable_id: str) -> str:
    hex16 = source_stable_id.rsplit("/", 1)[-1]
    if len(hex16) != 16 or any(char not in "0123456789abcdef" for char in hex16):
        raise ValueError(f"Frozen stable_id suffix is not 16 hex chars: {source_stable_id}")
    return f"cruciblecraft:t41/{hex16}"


def family_filename(template_key: str) -> str:
    return template_key.replace(".", "_").replace("#", "_") + ".json"


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action["kind"]).lower()
    emitted: dict[str, Any] = {"kind": kind}
    if kind == "wear":
        emitted["damage"] = int(action.get("damage") or 0)
    return emitted


def emit_item_input(operand: dict[str, Any], *, consume: bool) -> tuple[dict[str, Any], str]:
    source = operand.get("source") or {}
    klass = str(operand.get("t41_class") or operand.get("expression_class") or "SOURCE_DERIVED")
    if source.get("item") == common.CIRCUIT_ITEM:
        meta = source.get("meta")
        if not isinstance(meta, int):
            raise ValueError(f"T41 circuit operand is missing meta: {operand}")
        return {
            "type": "neoforge:components",
            "items": common.PROGRAMMED_CIRCUIT,
            "components": {common.CIRCUIT_CONFIG: meta},
        }, klass
    runtime_id = operand.get("runtime_id") or operand.get("alias")
    if isinstance(runtime_id, str):
        runtime_id = common.VANILLA_RUNTIME_ALIASES.get(runtime_id, runtime_id)
    if consume:
        runtime_id = common.assert_runtime_id(runtime_id, consume=True)
    else:
        runtime_id = common.assert_runtime_id(runtime_id, consume=False)
    fireproof = bool(operand.get("fireproof")) or (
        operand.get("t41_component") == common.FIREPROOF_COMPONENT
        or operand.get("component") == common.FIREPROOF_COMPONENT
    )
    if fireproof:
        return {
            "type": "neoforge:components",
            "items": runtime_id,
            "components": {common.FIREPROOF_COMPONENT: 1},
        }, klass
    return {"item": runtime_id}, klass


def emit_item_output(operand: dict[str, Any]) -> tuple[dict[str, Any], str]:
    runtime_id = operand.get("runtime_id") or operand.get("alias")
    if isinstance(runtime_id, str):
        runtime_id = common.VANILLA_RUNTIME_ALIASES.get(runtime_id, runtime_id)
    runtime_id = common.assert_runtime_id(runtime_id, consume=False)
    count = int((operand.get("source") or {}).get("count") or 1)
    klass = str(operand.get("t41_class") or "SOURCE_DERIVED")
    return {"count": count, "id": runtime_id}, klass


def emit_family(relation: dict[str, Any]) -> dict[str, Any]:
    classes: list[str] = []
    item_inputs: list[dict[str, Any]] = []
    actions = [emit_action(action) for action in relation["item_input_actions"]]
    counts = [int(value) for value in relation["item_input_counts"]]
    for operand, action, count in zip(relation["item_inputs"], actions, counts, strict=True):
        emitted, klass = emit_item_input(operand, consume=action["kind"] == "consume")
        item_inputs.append(emitted)
        classes.append(klass)
        if action["kind"] in {"preserve", "wear"} and count != 0:
            raise ValueError("PRESERVE/WEAR counts must be 0")
        if action["kind"] == "consume" and count <= 0:
            raise ValueError("CONSUME counts must be positive")
    item_outputs: list[dict[str, Any]] = []
    for operand in relation["item_outputs"]:
        emitted, klass = emit_item_output(operand)
        item_outputs.append(emitted)
        classes.append(klass)
    template_key = relation["template_key"]
    group = relation.get("publication_group") or common.publication_group_for_relation(relation)
    if group not in common.PUBLICATION_GROUPS:
        raise ValueError(f"unexpected T41 publication group {group} for {template_key}")
    source_kind = (
        "DESIGN_POLICY"
        if "DESIGN_POLICY" in classes
        else "SOURCE_DERIVED"
        if "SOURCE_DERIVED" in classes
        else "SOURCE_BACKED"
    )
    return {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": common.TARGET_MAP,
        "source_revision": relation["source_revision"],
        "publication_group": group,
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
                    "source_kind": source_kind,
                    "selected_source_recipe": template_key,
                    "evidence_hashes": [relation["source_row_sha256"]],
                },
            }
        ],
    }


def emit_parameterized_stub(template_key: str) -> dict[str, Any]:
    return {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": common.TARGET_MAP,
        "source_revision": common.SOURCE_REVISION,
        "relations": [],
        "parameterized": {
            "template": template_key,
            "status": "fail_closed",
            "reason": "combinatorial_player_path_unproven",
        },
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


def player_path_document(families: list[dict[str, Any]]) -> dict[str, Any]:
    rows: list[dict[str, Any]] = []
    reachable = 0
    registered = 0
    for family in families:
        relation = family["relations"][0]
        consume_ids: list[str] = []
        preserve_ids: list[str] = []
        for ingredient, count, action in zip(
            relation["item_inputs"],
            relation["item_input_counts"],
            relation["item_input_actions"],
            strict=True,
        ):
            item_id = common.ingredient_identity(ingredient).removeprefix("item:")
            if str(action.get("kind")) == "consume" and int(count) > 0:
                consume_ids.append(item_id)
            else:
                preserve_ids.append(item_id)
        output_ids = [str(stack["id"]) for stack in relation["item_outputs"]]
        inputs_ok = all(item_id.startswith(REACHABLE_PREFIXES) for item_id in consume_ids)
        outputs_ok = all(item_id.startswith(REACHABLE_PREFIXES) for item_id in output_ids)
        if inputs_ok:
            reachable += 1
        if outputs_ok:
            registered += 1
        rows.append({
            "alias_fail_closed": False,
            "consume_ids": consume_ids,
            "fluid_input_ids": [],
            "fluid_output_ids": [],
            "inputs_reachable": inputs_ok,
            "output_ids": output_ids,
            "outputs_registered": outputs_ok,
            "preserve_ids": preserve_ids,
            "publication_group": family["publication_group"],
            "stable_id": relation["stable_id"],
            "template_key": family["family_id"],
        })
    return {
        "families": len(families),
        "frozen_review_unmodified": True,
        "inputs_reachable": reachable,
        "note": (
            "B0/B1 proof is tools/t41_layered_player_path.json. "
            "This sidecar records consume/preserve identities after mapping."
        ),
        "outputs_registered": registered,
        "relations": len(families),
        "rows": rows,
        "schema_version": 1,
        "status": "T41_PLAYER_PATH",
    }


def operand_map_document(relations: list[dict[str, Any]]) -> dict[str, Any]:
    seen: set[tuple[Any, ...]] = set()
    operands: list[dict[str, Any]] = []
    for relation in relations:
        for side in ("item_inputs", "item_outputs"):
            for operand in relation.get(side) or []:
                source = operand.get("source") or {}
                key = (
                    source.get("item"),
                    source.get("meta"),
                    source.get("displayName"),
                    operand.get("runtime_id"),
                    operand.get("t41_component"),
                    operand.get("t41_value"),
                )
                if key in seen:
                    continue
                seen.add(key)
                runtime: dict[str, Any] = {
                    "id": operand.get("runtime_id"),
                    "kind": "item",
                }
                if operand.get("t41_component") == common.CIRCUIT_CONFIG:
                    runtime = {
                        "component": common.CIRCUIT_CONFIG,
                        "id": common.PROGRAMMED_CIRCUIT,
                        "kind": "component_item",
                        "value": operand.get("t41_value"),
                    }
                elif operand.get("t41_component") == common.FIREPROOF_COMPONENT:
                    runtime = {
                        "component": common.FIREPROOF_COMPONENT,
                        "id": operand.get("runtime_id"),
                        "kind": "component_item",
                        "value": 1,
                    }
                operands.append({
                    "class": operand.get("t41_class") or operand.get("mapping"),
                    "reason": "T41 source overlay",
                    "runtime": runtime,
                    "source": source,
                })
    operands.sort(
        key=lambda row: (
            str((row.get("source") or {}).get("item") or ""),
            (row.get("source") or {}).get("meta") if isinstance((row.get("source") or {}).get("meta"), int) else -1,
        )
    )
    return {
        "circuit_config_component": common.CIRCUIT_CONFIG,
        "circuit_item": common.PROGRAMMED_CIRCUIT,
        "fireproof_component": common.FIREPROOF_COMPONENT,
        "operand_count": len(operands),
        "operands": operands,
        "schema_version": 1,
        "status": "T41_OPERAND_RUNTIME_MAP",
    }


def required_forms_document(source: dict[str, Any]) -> dict[str, Any]:
    return {
        "counts": {
            "dust_div72_materials": 0,
            "required_form_pairs": 0,
            "small_dust_materials": 0,
        },
        "dust_div72_source_metas": [],
        "note": (
            "T41 plank/circuit expressions are items and components, not "
            "material forms. GT plank blocks are not extras."
        ),
        "required_forms": {},
        "schema_version": 1,
        "source": {
            "path": common.relative(SOURCE),
            "sha256": t35.sha256_file(SOURCE),
        },
        "status": "T41_REQUIRED_FORMS_FROZEN",
    }


def load_source() -> dict[str, Any]:
    document = t35.load_json(SOURCE)
    if document.get("status") != "T41_ASSEMBLER_SOURCE_FROZEN":
        raise ValueError("T41 source is not frozen")
    relations = document.get("relations") or []
    if len(relations) != common.CATALOG_RELATION_COUNT:
        raise ValueError("T41 frozen source is not 1531 relations")
    return document


def production_relations(source: dict[str, Any]) -> list[dict[str, Any]]:
    selected = [
        relation
        for relation in source["relations"]
        if relation["template_key"] not in common.COMBINATORIAL_TEMPLATE_KEYS
    ]
    if len(selected) != common.PRODUCTION_RELATION_COUNT:
        raise ValueError(
            f"T41 production relations drifted: {len(selected)}"
        )
    return selected


def write_tree(root: Path, files: dict[str, str]) -> None:
    if root.exists():
        for path in root.glob("*.json"):
            path.unlink()
    root.mkdir(parents=True, exist_ok=True)
    for name, content in files.items():
        (root / name).write_text(content, encoding="utf-8", newline="\n")


def planned_documents(scope: str = "production") -> dict[str, str]:
    source = load_source()
    if scope == "production":
        selected = production_relations(source)
        root = OUTPUT_ROOT
    elif scope == "catalog":
        selected = list(source["relations"])
        root = FIXTURE_ROOT
    else:
        raise ValueError(f"unknown T41 generation scope: {scope}")
    files: dict[str, str] = {}
    families: list[dict[str, Any]] = []
    production_selected = [
        relation
        for relation in selected
        if relation["template_key"] not in common.COMBINATORIAL_TEMPLATE_KEYS
    ]
    emit_rows = production_selected if scope == "catalog" else selected
    identities: dict[str, str] = {}
    for relation in sorted(emit_rows, key=lambda row: row["template_key"]):
        document = emit_family(relation)
        identity = consume_identity(document)
        previous = identities.get(identity)
        if previous is not None:
            raise ValueError(
                "Consume-side identity collision between "
                f"{previous} and {document['family_id']}"
            )
        identities[identity] = document["family_id"]
        files[family_filename(document["family_id"])] = stable(document)
        families.append(document)
    if scope == "catalog":
        for template_key in common.COMBINATORIAL_TEMPLATE_KEYS:
            files[family_filename(template_key)] = stable(
                emit_parameterized_stub(template_key)
            )
    player = player_path_document(families)
    operand = operand_map_document(production_selected)
    required = required_forms_document(source)
    generated_hashes = {
        name: digest_text(content) for name, content in sorted(files.items())
    }
    stable_ids = [
        json.loads(content)["relations"][0]["stable_id"]
        for name, content in sorted(files.items())
        if json.loads(content).get("relations")
    ]
    equivalence = {
        "generated": {
            "file_count": len(files) if scope == "production" else len(families),
            "files": {
                name: digest
                for name, digest in generated_hashes.items()
                if scope == "production" or name in {
                    family_filename(row["template_key"]) for row in production_selected
                }
            },
            "root": common.relative(root),
            "sha256": digest_text(stable(generated_hashes)),
        },
        "runtime_expected": {
            "logical_ids": len(stable_ids) if scope == "production" else len(families),
            "sha256": digest_text(stable(stable_ids)),
            "stable_ids": stable_ids,
        },
        "schema_version": 1,
        "source": {
            "family_count": len(families) if scope == "production" else common.CATALOG_FAMILY_COUNT,
            "path": common.relative(SOURCE),
            "relation_count": len(emit_rows),
            "scope": scope,
            "sha256": t35.sha256_file(SOURCE),
            "source_revision": common.SOURCE_REVISION,
        },
        "status": "T41_ASSEMBLER_EQUIVALENCE",
    }
    sidecars = {
        "equivalence": stable(equivalence),
        "operand_map": stable(operand),
        "player_path": stable(player),
        "required_forms": stable(required),
    }
    files["__sidecars__"] = json.dumps(sidecars)
    return files


def split_planned(files: dict[str, str]) -> tuple[dict[str, str], dict[str, str]]:
    sidecars = json.loads(files["__sidecars__"])
    recipes = {name: content for name, content in files.items() if name != "__sidecars__"}
    sidecar_files = {
        OPERAND_MAP.name: sidecars["operand_map"],
        PLAYER_PATH.name: sidecars["player_path"],
        EQUIVALENCE.name: sidecars["equivalence"],
        REQUIRED_FORMS.name: sidecars["required_forms"],
    }
    return recipes, sidecar_files


def build() -> dict[str, Any]:
    production = planned_documents("production")
    catalog = planned_documents("catalog")
    recipes, sidecars = split_planned(production)
    fixture, _fixture_sidecars = split_planned(catalog)
    player = json.loads(sidecars[PLAYER_PATH.name])
    if player["inputs_reachable"] != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError("T41 production consume identities are incomplete")
    if player["outputs_registered"] != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError("T41 production outputs are not registered identities")
    return {
        "equivalence": json.loads(sidecars[EQUIVALENCE.name]),
        "family_count": len(recipes),
        "fixture_count": len(fixture),
        "recipes": recipes,
        "fixture": fixture,
        "sidecars": sidecars,
    }


def write() -> dict[str, Any]:
    assert_legacy_production_write_forbidden("T41")
    return {}


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Generate T41 Assembler compact families", argv)
    if common.handle_rebind(args, EQUIVALENCE):
        return 0
    try:
        if args.check:
            document = build()
            errors = []
            if document["family_count"] != common.PRODUCTION_FAMILY_COUNT:
                errors.append("T41 generated family count drifted")
            if document["fixture_count"] != common.CATALOG_FAMILY_COUNT:
                errors.append("T41 catalog fixture count drifted")
            if OUTPUT_ROOT.is_dir():
                actual = {path.name: path.read_text(encoding="utf-8") for path in OUTPUT_ROOT.glob("*.json")}
                if actual != document["recipes"]:
                    errors.append("T41 production recipe tree drifted")
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("T41 assembler recipes are current.")
            return 0
        document = write()
        print(
            f"Wrote {document['family_count']} T41 production families and "
            f"{document['fixture_count']} catalog fixture families."
        )
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T41 assembler recipes failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
