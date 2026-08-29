#!/usr/bin/env python3
"""Generate T38 Roaster compact recipe families from the frozen R0 source."""
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

from tools import t38_common as common  # noqa: E402

SOURCE = common.SOURCE
OUTPUT_ROOT = common.GENERATED_ROOT
OPERAND_MAP = common.OPERAND_RUNTIME_MAP
PLAYER_PATH = common.PLAYER_PATH
EQUIVALENCE = common.EQUIVALENCE
REQUIRED_FORMS = common.REQUIRED_FORMS
CROSS_REFERENCE = ROOT / "tools" / "gt6_oredict_cross_reference.json"
T21_REACHABILITY = ROOT / "tools" / "t21_operand_reachability.json"

EXPECTED_FAMILY_COUNT = common.FAMILY_COUNT
EXPECTED_RELATION_COUNT = common.SOURCE_ROWS
EXPECTED_FAMILY_NUMBERS = (0, 1, *range(3, 30))
EXPECTED_DUST_DIV72_METAS = frozenset({
    280, 290, 300, 460, 470, 500, 510, 820, 9104,
})
EXPECTED_SMALL_DUST_MATERIALS = frozenset({"copper", "nickel", "zinc"})
ALLOWED_FLUIDS = frozenset({
    "cruciblecraft:air",
    "cruciblecraft:carbon_dioxide",
    "cruciblecraft:carbon_monoxide",
    "cruciblecraft:oxygen",
    "cruciblecraft:sulfur_dioxide",
})
GT_ITEM_PREFIXES = ("gregtech:", "gregapi:", "fixed:")
TEMPLATE_RE = re.compile(r"^gt\.recipe\.roaster#(\d{4})$")
STABLE_ID_RE = re.compile(r"^cruciblecraft:t38/[0-9a-f]{16}$")


def stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def digest_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def digest_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_source() -> dict[str, Any]:
    document = load_json(SOURCE)
    if document.get("status") != "T38_ROASTER_SOURCE_FROZEN":
        raise ValueError("T38 source is not frozen")
    if document.get("source_map") != common.SOURCE_MAP:
        raise ValueError("T38 frozen source map drifted")
    if document.get("target_map") != common.TARGET_MAP:
        raise ValueError("T38 frozen target map drifted")
    relations = document.get("relations")
    if not isinstance(relations, list) or len(relations) != EXPECTED_RELATION_COUNT:
        raise ValueError(
            f"T38 frozen source must contain {EXPECTED_RELATION_COUNT} relations"
        )
    families = document.get("work_set", {}).get("family_ids") or []
    if len(families) != EXPECTED_FAMILY_COUNT:
        raise ValueError(
            f"T38 frozen source must contain {EXPECTED_FAMILY_COUNT} work-set families"
        )
    template_numbers: set[int] = set()
    for relation in relations:
        template_key = relation.get("template_key")
        match = TEMPLATE_RE.fullmatch(str(template_key))
        if match is None:
            raise ValueError(f"unexpected T38 template key: {template_key}")
        template_numbers.add(int(match.group(1)))
        if relation.get("target_map") != common.TARGET_MAP:
            raise ValueError(f"T38 relation target map drifted: {template_key}")
        if not STABLE_ID_RE.fullmatch(str(relation.get("stable_id"))):
            raise ValueError(f"invalid frozen T38 stable id: {relation.get('stable_id')}")
    if tuple(sorted(template_numbers)) != EXPECTED_FAMILY_NUMBERS:
        raise ValueError("T38 frozen family set is not #0000/#0001/#0003-#0029")
    if len({relation["stable_id"] for relation in relations}) != len(relations):
        raise ValueError("T38 frozen source has duplicate stable ids")
    return document


def load_reachable_identities() -> set[str]:
    document = load_json(T21_REACHABILITY)
    closure = document.get("closure")
    if not isinstance(closure, dict):
        raise ValueError("T21 operand reachability closure is invalid")
    identities = closure.get("reachable_identities")
    if not isinstance(identities, list) or not all(
        isinstance(identity, str) for identity in identities
    ):
        raise ValueError("T21 reachable identities are invalid")
    return set(identities)


def load_cross_reference() -> dict[str, str]:
    cross = load_json(CROSS_REFERENCE)
    mapping = cross.get("material_id_to_cc")
    if not isinstance(mapping, dict):
        raise ValueError("GT6 cross reference has no material_id_to_cc map")
    return {str(key): str(value) for key, value in mapping.items()}


def family_filename(template_key: str) -> str:
    return template_key.replace(".", "_").replace("#", "_") + ".json"


def assert_runtime_item(item_id: str, label: str) -> str:
    if not isinstance(item_id, str) or ":" not in item_id:
        raise ValueError(f"{label} is missing a runtime item id")
    if item_id.startswith(GT_ITEM_PREFIXES):
        raise ValueError(f"refusing to emit GT runtime item id: {item_id}")
    if not item_id.startswith(("minecraft:", "cruciblecraft:")):
        raise ValueError(f"{label} is not a registered vanilla/CC item: {item_id}")
    return item_id


def assert_runtime_fluid(fluid_id: str, label: str) -> str:
    if fluid_id not in ALLOWED_FLUIDS:
        raise ValueError(f"{label} is not a registered T38 fluid: {fluid_id}")
    return fluid_id


def dust_div72_material(
        operand: dict[str, Any],
        cc_by_source_id: dict[str, str],
) -> str | None:
    source = operand.get("source") or {}
    if source.get("item") != "gregtech:gt.meta.dustDiv72":
        return None
    meta = source.get("meta")
    if not isinstance(meta, int):
        raise ValueError(f"dustDiv72 operand has no integer GT6 material meta: {operand}")
    material = cc_by_source_id.get(str(meta))
    if material is None:
        raise ValueError(f"dustDiv72 material is absent from cross reference: {meta}")
    return material


def derived_runtime_item(
        operand: dict[str, Any],
        cc_by_source_id: dict[str, str],
) -> tuple[str, str]:
    """Return a runtime id plus its source class without mutating frozen R0."""
    dust_div72 = dust_div72_material(operand, cc_by_source_id)
    if dust_div72 is not None:
        return f"cruciblecraft:{dust_div72}/dust_div72", "SOURCE_DERIVED"
    runtime_id = operand.get("runtime_id") or operand.get("alias")
    if runtime_id is None and (
            operand.get("form") == "small_dust"
            and isinstance(operand.get("material"), str)
    ):
        return (
            f"cruciblecraft:{operand['material']}/small_dust",
            "SOURCE_DERIVED",
        )
    runtime_id = assert_runtime_item(str(runtime_id), "T38 item operand")
    source_class = (
        "SOURCE_DERIVED"
        if operand.get("mapping") == "source_derived_alias"
        else "SOURCE_BACKED"
    )
    return runtime_id, source_class


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action.get("kind") or "").lower()
    if kind not in {"consume", "preserve", "wear"}:
        raise ValueError(f"unsupported T38 item action: {action}")
    emitted: dict[str, Any] = {"kind": kind}
    if kind == "wear":
        emitted["damage"] = int(action.get("damage") or 0)
    return emitted


def emit_item_input(
        operand: dict[str, Any],
        cc_by_source_id: dict[str, str],
) -> tuple[dict[str, Any], str]:
    runtime_id, source_class = derived_runtime_item(operand, cc_by_source_id)
    return {"item": runtime_id}, source_class


def emit_item_output(
        operand: dict[str, Any],
        cc_by_source_id: dict[str, str],
) -> tuple[dict[str, Any], str]:
    runtime_id, source_class = derived_runtime_item(operand, cc_by_source_id)
    count = int((operand.get("source") or {}).get("count") or 1)
    if count <= 0:
        raise ValueError(f"T38 output count must be positive: {operand}")
    return {"count": count, "id": runtime_id}, source_class


def emit_fluid_stack(operand: dict[str, Any]) -> tuple[dict[str, Any], str]:
    runtime_id = assert_runtime_fluid(
        str(operand.get("runtime_id") or operand.get("alias") or ""),
        "T38 fluid operand",
    )
    amount = int((operand.get("source") or {}).get("amount") or 0)
    if amount <= 0:
        raise ValueError(f"T38 fluid amount must be positive: {operand}")
    source_class = (
        "SOURCE_DERIVED"
        if operand.get("mapping") == "source_derived_alias"
        else "SOURCE_BACKED"
    )
    return {"amount": amount, "id": runtime_id}, source_class


def relation_source_kind(classes: list[str]) -> str:
    return "SOURCE_DERIVED" if "SOURCE_DERIVED" in classes else "SOURCE_BACKED"


def emit_relation(
        relation: dict[str, Any],
        cc_by_source_id: dict[str, str],
) -> dict[str, Any]:
    classes: list[str] = []
    item_inputs: list[dict[str, Any]] = []
    for operand in relation.get("item_inputs") or []:
        emitted, source_class = emit_item_input(operand, cc_by_source_id)
        item_inputs.append(emitted)
        classes.append(source_class)
    item_outputs: list[dict[str, Any]] = []
    for operand in relation.get("item_outputs") or []:
        emitted, source_class = emit_item_output(operand, cc_by_source_id)
        item_outputs.append(emitted)
        classes.append(source_class)
    fluid_inputs: list[dict[str, Any]] = []
    for operand in relation.get("fluid_inputs") or []:
        emitted, source_class = emit_fluid_stack(operand)
        fluid_inputs.append(emitted)
        classes.append(source_class)
    fluid_outputs: list[dict[str, Any]] = []
    for operand in relation.get("fluid_outputs") or []:
        emitted, source_class = emit_fluid_stack(operand)
        fluid_outputs.append(emitted)
        classes.append(source_class)

    counts = [int(value) for value in relation.get("item_input_counts") or []]
    actions = [emit_action(action) for action in relation.get("item_input_actions") or []]
    if not (len(item_inputs) == len(counts) == len(actions)):
        raise ValueError(f"T38 input arity drifted: {relation['stable_id']}")
    for count, action in zip(counts, actions, strict=True):
        if action["kind"] == "consume" and count <= 0:
            raise ValueError("T38 consume counts must be positive")
        if action["kind"] in {"preserve", "wear"} and count != 0:
            raise ValueError("T38 preserve/wear counts must be zero")

    chances = [int(value) for value in relation.get("output_chances") or []]
    if len(chances) != len(item_outputs):
        raise ValueError(f"T38 output-chance arity drifted: {relation['stable_id']}")
    return {
        "stable_id": relation["stable_id"],
        "item_inputs": item_inputs,
        "item_input_counts": counts,
        "item_input_actions": actions,
        "item_outputs": item_outputs,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "output_chances": chances,
        "duration": int(relation["duration"]),
        "eut": int(relation["eut"]),
        "special_value": int(relation["special_value"]),
        "can_be_buffered": bool(relation["can_be_buffered"]),
        "shadow_order": int(relation["shadow_order"]),
        "provenance": {
            "source_kind": relation_source_kind(classes),
            "selected_source_recipe": relation["template_key"],
            "evidence_hashes": [relation["source_row_sha256"]],
        },
    }


def consume_identity(relation: dict[str, Any]) -> str:
    return json.dumps(
        {
            "fluid_inputs": relation["fluid_inputs"],
            "item_input_actions": relation["item_input_actions"],
            "item_input_counts": relation["item_input_counts"],
            "item_inputs": relation["item_inputs"],
        },
        sort_keys=True,
        separators=(",", ":"),
    )


def required_forms_document(
        relations: list[dict[str, Any]],
        cc_by_source_id: dict[str, str],
) -> dict[str, Any]:
    forms: dict[str, set[str]] = defaultdict(set)
    dust_div72_metas: set[int] = set()
    small_dust_materials: set[str] = set()
    for relation in relations:
        for operand in relation.get("item_outputs") or []:
            material = dust_div72_material(operand, cc_by_source_id)
            if material is not None:
                forms[material].add("dust_div72")
                dust_div72_metas.add(int(operand["source"]["meta"]))
            elif (
                    operand.get("form") == "small_dust"
                    and isinstance(operand.get("material"), str)
                    and operand["material"] in EXPECTED_SMALL_DUST_MATERIALS
            ):
                material = operand["material"]
                forms[material].add("small_dust")
                small_dust_materials.add(material)
    if dust_div72_metas != EXPECTED_DUST_DIV72_METAS:
        raise ValueError(
            "T38 dustDiv72 source materials drifted: "
            f"{sorted(dust_div72_metas)}"
        )
    if small_dust_materials != EXPECTED_SMALL_DUST_MATERIALS:
        raise ValueError(
            "T38 missing small-dust materials drifted: "
            f"{sorted(small_dust_materials)}"
        )
    serialized = {
        material: sorted(material_forms)
        for material, material_forms in sorted(forms.items())
    }
    return {
        "schema_version": 1,
        "status": "T38_REQUIRED_FORMS_FROZEN",
        "source": {
            "path": SOURCE.relative_to(ROOT).as_posix(),
            "sha256": digest_file(SOURCE),
        },
        "counts": {
            "dust_div72_materials": len(dust_div72_metas),
            "required_form_pairs": sum(map(len, serialized.values())),
            "small_dust_materials": len(small_dust_materials),
        },
        "dust_div72_source_metas": sorted(dust_div72_metas),
        "required_forms": serialized,
    }


def operand_map_document(
        source_relations: list[dict[str, Any]],
        families: list[dict[str, Any]],
) -> dict[str, Any]:
    rows: list[dict[str, Any]] = []
    source_by_stable_id = {
        relation["stable_id"]: relation for relation in source_relations
    }
    for family in families:
        for relation in family["relations"]:
            source = source_by_stable_id[relation["stable_id"]]
            for direction, source_operands, emitted_operands in (
                    (
                        "item_input",
                        source.get("item_inputs") or [],
                        relation["item_inputs"],
                    ),
                    (
                        "item_output",
                        source.get("item_outputs") or [],
                        relation["item_outputs"],
                    ),
            ):
                for source_operand, emitted in zip(
                        source_operands, emitted_operands, strict=True
                ):
                    runtime_id = emitted.get("item") or emitted.get("id")
                    rows.append({
                        "class": relation["provenance"]["source_kind"],
                        "direction": direction,
                        "runtime": {"id": runtime_id, "kind": "item"},
                        "source": {
                            "display_name": (source_operand.get("source") or {}).get(
                                "displayName"
                            ),
                            "item": (source_operand.get("source") or {}).get("item"),
                            "meta": (source_operand.get("source") or {}).get("meta"),
                        },
                        "stable_id": relation["stable_id"],
                    })
            for direction, source_operands, emitted_operands in (
                    (
                        "fluid_input",
                        source.get("fluid_inputs") or [],
                        relation["fluid_inputs"],
                    ),
                    (
                        "fluid_output",
                        source.get("fluid_outputs") or [],
                        relation["fluid_outputs"],
                    ),
            ):
                for source_operand, emitted in zip(
                        source_operands, emitted_operands, strict=True
                ):
                    rows.append({
                        "class": relation["provenance"]["source_kind"],
                        "direction": direction,
                        "runtime": {"id": emitted["id"], "kind": "fluid"},
                        "source": {
                            "amount": (
                                source_operand.get("source") or {}
                            ).get("amount"),
                            "fluid": (
                                source_operand.get("source") or {}
                            ).get("fluid"),
                        },
                        "stable_id": relation["stable_id"],
                    })
    rows.sort(key=lambda row: (
        row["stable_id"],
        row["direction"],
        row["runtime"]["id"],
        str(row["source"]),
    ))
    return {
        "operand_count": len(rows),
        "operands": rows,
        "source": SOURCE.relative_to(ROOT).as_posix(),
    }


def player_path_document(
    families: list[dict[str, Any]],
    source_relations: list[dict[str, Any]],
) -> dict[str, Any]:
    source_by_id = {row["stable_id"]: row for row in source_relations}
    reachable_identities = load_reachable_identities()
    rows: list[dict[str, Any]] = []
    unreachable_item_occurrences = 0
    unreachable_fluid_occurrences = 0
    for family in families:
        for relation in family["relations"]:
            source_relation = source_by_id.get(relation["stable_id"])
            if source_relation is None:
                raise ValueError(
                    f"T38 player-path missing frozen source for {relation['stable_id']}"
                )
            consume_ids: list[str] = []
            preserve_ids: list[str] = []
            for ingredient, action in zip(
                    relation["item_inputs"],
                    relation["item_input_actions"],
                    strict=True,
            ):
                item_id = ingredient["item"]
                if action["kind"] == "consume":
                    consume_ids.append(item_id)
                else:
                    preserve_ids.append(item_id)
            fluid_input_ids = [stack["id"] for stack in relation["fluid_inputs"]]
            output_ids = [stack["id"] for stack in relation["item_outputs"]]
            fluid_output_ids = [stack["id"] for stack in relation["fluid_outputs"]]
            unreachable_items = [
                item_id
                for item_id in consume_ids
                if f"item:{item_id}" not in reachable_identities
            ]
            unreachable_fluids = [
                fluid_id
                for fluid_id in fluid_input_ids
                if f"fluid:{fluid_id}" not in reachable_identities
            ]
            item_inputs_reachable = not unreachable_items
            fluid_inputs_reachable = not unreachable_fluids
            unreachable_item_occurrences += len(unreachable_items)
            unreachable_fluid_occurrences += len(unreachable_fluids)
            item_outputs_registered = all(
                item_id.startswith(("minecraft:", "cruciblecraft:"))
                for item_id in output_ids
            )
            fluid_outputs_registered = all(
                fluid_id in ALLOWED_FLUIDS for fluid_id in fluid_output_ids
            )
            rows.append({
                "consume_ids": consume_ids,
                "fluid_input_ids": fluid_input_ids,
                "fluid_inputs_reachable": fluid_inputs_reachable,
                "fluid_output_ids": fluid_output_ids,
                "fluid_outputs_registered": fluid_outputs_registered,
                "inputs_reachable": item_inputs_reachable and fluid_inputs_reachable,
                "item_inputs_reachable": item_inputs_reachable,
                "output_ids": output_ids,
                "outputs_registered": (
                    item_outputs_registered and fluid_outputs_registered
                ),
                "preserve_ids": preserve_ids,
                "stable_id": relation["stable_id"],
                "template_key": family["family_id"],
            })
    rows.sort(key=lambda row: row["stable_id"])
    return {
        "families": len(families),
        "frozen_review_unmodified": True,
        "inputs_reachable": sum(1 for row in rows if row["inputs_reachable"]),
        "note": (
            "Namespace or fluid whitelist is not player reachability. Inputs "
            "are checked against the current typed T21 identity closure; the "
            "frozen T38 source remains the immutable recipe evidence."
        ),
        "outputs_registered": sum(1 for row in rows if row["outputs_registered"]),
        "reachability_source": (
            "tools/t21_operand_reachability.json#closure.reachable_identities"
        ),
        "relations": len(rows),
        "relations_with_unreachable_inputs": sum(
            1 for row in rows if not row["inputs_reachable"]
        ),
        "unreachable_fluid_input_occurrences": unreachable_fluid_occurrences,
        "unreachable_inputs": (
            unreachable_item_occurrences + unreachable_fluid_occurrences
        ),
        "unreachable_item_input_occurrences": unreachable_item_occurrences,
        "rows": rows,
    }


def planned_documents() -> dict[str, str]:
    source = load_source()
    source_relations = source["relations"]
    cc_by_source_id = load_cross_reference()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source_relations:
        grouped[relation["template_key"]].append(relation)
    if len(grouped) != EXPECTED_FAMILY_COUNT:
        raise ValueError("T38 compact source does not group into 29 families")

    files: dict[str, str] = {}
    families: list[dict[str, Any]] = []
    emitted_by_stable_id: dict[str, dict[str, Any]] = {}
    identities: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for template_key in sorted(grouped):
        source_rows = sorted(
            grouped[template_key],
            key=lambda row: (
                int(row["source_recipe_index"]),
                row["stable_id"],
            ),
        )
        emitted_relations = [
            emit_relation(relation, cc_by_source_id) for relation in source_rows
        ]
        for emitted in emitted_relations:
            if emitted["stable_id"] in emitted_by_stable_id:
                raise ValueError(f"T38 stable id emitted twice: {emitted['stable_id']}")
            emitted_by_stable_id[emitted["stable_id"]] = emitted
            identities[consume_identity(emitted)].append(
                (template_key, emitted["stable_id"])
            )
        family = {
            "type": "cruciblecraft:compact_gt_recipe_family",
            "family_id": template_key,
            "target_map": common.TARGET_MAP,
            "source_revision": source_rows[0]["source_revision"],
            "relations": emitted_relations,
        }
        if "parameterized" in family:
            raise ValueError("T38 compact families must not be parameterized")
        filename = family_filename(template_key)
        files[filename] = stable(family)
        families.append(family)

    if len(emitted_by_stable_id) != EXPECTED_RELATION_COUNT:
        raise ValueError("T38 generator did not emit every frozen relation")
    required_forms = required_forms_document(source_relations, cc_by_source_id)
    player_path = player_path_document(families, source_relations)
    if player_path["relations"] != EXPECTED_RELATION_COUNT:
        raise ValueError("T38 player-path relation count drifted")
    if player_path["outputs_registered"] != EXPECTED_RELATION_COUNT:
        raise ValueError("T38 player-path outputs are not fully registered")

    generated_hashes = {
        name: digest_text(content) for name, content in sorted(files.items())
    }
    stable_ids = sorted(emitted_by_stable_id)
    relation_fingerprints = {
        stable_id: digest_text(stable(emitted_by_stable_id[stable_id]))
        for stable_id in stable_ids
    }
    shadow_notes = {
        digest: [
            {"stable_id": stable_id, "template_key": template_key}
            for template_key, stable_id in rows
        ]
        for digest, rows in sorted(identities.items())
        if len({template_key for template_key, _stable_id in rows}) > 1
    }
    equivalence = {
        "generated": {
            "file_count": len(files),
            "files": generated_hashes,
            "root": common.relative(OUTPUT_ROOT),
            "sha256": digest_text(stable(generated_hashes)),
        },
        "runtime_expected": {
            "logical_ids": len(stable_ids),
            "sha256": digest_text(stable(stable_ids)),
            "stable_ids": stable_ids,
        },
        "source": {
            "family_count": len(grouped),
            "path": SOURCE.relative_to(ROOT).as_posix(),
            "relation_count": len(source_relations),
            "sha256": digest_file(SOURCE),
            "source_revision": source["source_revision"],
        },
        "relation_fingerprints": relation_fingerprints,
        "cross_family_consume_identity_shadow_notes": shadow_notes,
    }
    sidecars = {
        "equivalence": stable(equivalence),
        "operand_map": stable(operand_map_document(source_relations, families)),
        "player_path": stable(player_path),
        "required_forms": stable(required_forms),
    }
    files["__sidecars__"] = json.dumps(sidecars)
    return files


def split_planned(
        files: dict[str, str],
) -> tuple[dict[str, str], dict[str, str]]:
    sidecars = json.loads(files["__sidecars__"])
    recipes = {name: content for name, content in files.items() if name != "__sidecars__"}
    sidecar_files = {
        OPERAND_MAP.name: sidecars["operand_map"],
        PLAYER_PATH.name: sidecars["player_path"],
        EQUIVALENCE.name: sidecars["equivalence"],
        REQUIRED_FORMS.name: sidecars["required_forms"],
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
            errors.append(f"stale generated T38 recipe: {name}")
    sidecar_paths = {
        OPERAND_MAP.name: OPERAND_MAP,
        PLAYER_PATH.name: PLAYER_PATH,
        EQUIVALENCE.name: EQUIVALENCE,
        REQUIRED_FORMS.name: REQUIRED_FORMS,
    }
    for name, path in sidecar_paths.items():
        if not path.is_file() or path.read_text(encoding="utf-8") != sidecars[name]:
            errors.append(f"stale T38 sidecar: {name}")
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
    for name, content in sidecars.items():
        {
            OPERAND_MAP.name: OPERAND_MAP,
            PLAYER_PATH.name: PLAYER_PATH,
            EQUIVALENCE.name: EQUIVALENCE,
            REQUIRED_FORMS.name: REQUIRED_FORMS,
        }[name].write_text(content, encoding="utf-8", newline="\n")


def main(argv: list[str] | None = None) -> int:
    args = common.parse_write_check(
        "Generate T38 Roaster compact recipe families.", argv
    )
    if args.check:
        errors = check()
        if errors:
            print("T38 Roaster compact recipes are stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T38 Roaster compact recipes match the frozen compact source.")
        return 0
    write()
    print(
        f"Wrote {EXPECTED_FAMILY_COUNT} T38 Roaster compact families "
        f"({EXPECTED_RELATION_COUNT} logical relations)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
