#!/usr/bin/env python3
"""Generate T39 Centrifuge compact recipe families from the frozen R0 source."""
from __future__ import annotations

import argparse
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

from tools import t39_common as common  # noqa: E402

SOURCE = common.SOURCE
OUTPUT_ROOT = common.GENERATED_ROOT
OPERAND_MAP = common.OPERAND_RUNTIME_MAP
PLAYER_PATH = common.PLAYER_PATH
EQUIVALENCE = common.EQUIVALENCE
REQUIRED_FORMS = common.REQUIRED_FORMS
CROSS_REFERENCE = ROOT / "tools" / "gt6_oredict_cross_reference.json"
T21_REACHABILITY = ROOT / "tools" / "t21_operand_reachability.json"

EXPECTED_FAMILY_COUNT = common.CATALOG_FAMILY_COUNT
EXPECTED_RELATION_COUNT = common.CATALOG_RELATION_COUNT
GT_ITEM_PREFIXES = ("gregtech:", "gregapi:", "fixed:")
TEMPLATE_RE = re.compile(r"^gt\.recipe\.centrifuge#(\d{4})$")
STABLE_ID_RE = re.compile(r"^cruciblecraft:t39/[0-9a-f]{16}$")
# Bounded aliases onto already-registered vanilla/CC items. Keys are
# (gt_item, meta). Distinct consume identities are required because the
# centrifuge map fail-closes on shadowed input signatures.
VANILLA_FORM_ITEMS: dict[tuple[str, str], str] = {
    ("iron", "nugget"): "minecraft:iron_nugget",
    ("gold", "nugget"): "minecraft:gold_nugget",
}
T39_ITEM_ALIASES: dict[tuple[str, int], str] = {
    **common.LEGACY_VANILLA_ITEMS,
}
_ACTIVE_SCOPE = "production"


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
    if document.get("status") != "T39_CENTRIFUGE_SOURCE_FROZEN":
        raise ValueError("T39 source is not frozen")
    if document.get("source_map") != common.SOURCE_MAP:
        raise ValueError("T39 frozen source map drifted")
    if document.get("target_map") != common.TARGET_MAP:
        raise ValueError("T39 frozen target map drifted")
    relations = document.get("relations")
    if not isinstance(relations, list) or len(relations) != EXPECTED_RELATION_COUNT:
        raise ValueError(
            f"T39 frozen source must contain {EXPECTED_RELATION_COUNT} relations"
        )
    families = document.get("work_set", {}).get("family_ids") or []
    if len(families) != EXPECTED_FAMILY_COUNT:
        raise ValueError(
            f"T39 frozen source must contain {EXPECTED_FAMILY_COUNT} work-set families"
        )
    template_keys: set[str] = set()
    for relation in relations:
        template_key = str(relation.get("template_key"))
        if TEMPLATE_RE.fullmatch(template_key) is None:
            raise ValueError(f"unexpected T39 template key: {template_key}")
        template_keys.add(template_key)
        if relation.get("target_map") != common.TARGET_MAP:
            raise ValueError(f"T39 relation target map drifted: {template_key}")
        if not STABLE_ID_RE.fullmatch(str(relation.get("stable_id"))):
            raise ValueError(f"invalid frozen T39 stable id: {relation.get('stable_id')}")
    expected_keys = {
        str(row["template_key"])
        for row in load_json(common.WORK_SET).get("families") or []
    }
    if template_keys != expected_keys:
        raise ValueError("T39 frozen family set is not the frozen 157-family work set")
    if len({relation["stable_id"] for relation in relations}) != len(relations):
        raise ValueError("T39 frozen source has duplicate stable ids")
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
    if not isinstance(fluid_id, str) or ":" not in fluid_id:
        raise ValueError(f"{label} is missing a runtime fluid id")
    if not fluid_id.startswith(("minecraft:", "cruciblecraft:")):
        raise ValueError(f"{label} is not a registered vanilla/CC fluid: {fluid_id}")
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
    source = operand.get("source") or {}
    gt_item = source.get("item")
    gt_meta = source.get("meta")
    if isinstance(gt_item, str) and isinstance(gt_meta, int):
        aliased = T39_ITEM_ALIASES.get((gt_item, gt_meta))
        if aliased is None and _ACTIVE_SCOPE == "catalog":
            aliased = common.FIXTURE_ONLY_LOSSY_ITEM_ALIASES.get(
                (gt_item, gt_meta)
            )
        if aliased is not None:
            return aliased, "SOURCE_DERIVED"
    if (
            isinstance(operand.get("form"), str)
            and isinstance(operand.get("material"), str)
            and not str(operand.get("runtime_id") or "").startswith(
                ("minecraft:", "cruciblecraft:")
            )
    ):
        source_class = (
            "SOURCE_DERIVED"
            if operand.get("mapping") in {"canonical_tag", "source_derived_alias"}
            else "SOURCE_BACKED"
        )
        material_id = str(operand["material"])
        form = str(operand["form"])
        aliased = VANILLA_FORM_ITEMS.get((material_id, form))
        if aliased is not None:
            return aliased, source_class
        return (
            f"cruciblecraft:{material_id}/{form}",
            source_class,
        )
    runtime_id = operand.get("runtime_id") or operand.get("alias")
    runtime_id = assert_runtime_item(str(runtime_id), "T39 item operand")
    runtime_id = apply_vanilla_form_item(runtime_id)
    source_class = (
        "SOURCE_DERIVED"
        if operand.get("mapping") == "source_derived_alias"
        else "SOURCE_BACKED"
    )
    return runtime_id, source_class


def apply_vanilla_form_item(item_id: str) -> str:
    if not item_id.startswith("cruciblecraft:") or "/" not in item_id:
        return item_id
    material, form = item_id.split(":", 1)[1].split("/", 1)
    return VANILLA_FORM_ITEMS.get((material, form), item_id)


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action.get("kind") or "").lower()
    if kind not in {"consume", "preserve", "wear"}:
        raise ValueError(f"unsupported T39 item action: {action}")
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
        raise ValueError(f"T39 output count must be positive: {operand}")
    return {"count": count, "id": runtime_id}, source_class


T39_FLUID_SOURCE_OVERRIDES: dict[str, str] = {
    # Frozen source aliased pahoehoe onto minecraft:lava; keep vanilla lava
    # distinct from the T10 GT6 lava-material fluid.
    "ic2pahoehoelava": "cruciblecraft:lava",
}


def emit_fluid_stack(operand: dict[str, Any]) -> tuple[dict[str, Any], str]:
    source_fluid = str((operand.get("source") or {}).get("fluid") or "")
    runtime_id = T39_FLUID_SOURCE_OVERRIDES.get(source_fluid) or str(
        operand.get("runtime_id") or operand.get("alias") or ""
    )
    runtime_id = assert_runtime_fluid(runtime_id, "T39 fluid operand")
    amount = int((operand.get("source") or {}).get("amount") or 0)
    if amount <= 0:
        raise ValueError(f"T39 fluid amount must be positive: {operand}")
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
        raise ValueError(f"T39 input arity drifted: {relation['stable_id']}")
    for count, action in zip(counts, actions, strict=True):
        if action["kind"] == "consume" and count <= 0:
            raise ValueError("T39 consume counts must be positive")
        if action["kind"] in {"preserve", "wear"} and count != 0:
            raise ValueError("T39 preserve/wear counts must be zero")

    chances = [int(value) for value in relation.get("output_chances") or []]
    if len(chances) != len(item_outputs):
        raise ValueError(f"T39 output-chance arity drifted: {relation['stable_id']}")
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
    overlay_forms = {
        "dust_div72",
        "nugget",
        "small_dust",
        "tiny_centrifuged_crushed_ore",
    }
    for relation in relations:
        for operand in (relation.get("item_inputs") or []) + (
                relation.get("item_outputs") or []
        ):
            material = dust_div72_material(operand, cc_by_source_id)
            if material is not None:
                forms[material].add("dust_div72")
                dust_div72_metas.add(int(operand["source"]["meta"]))
                continue
            form = operand.get("form")
            material_id = operand.get("material")
            if not isinstance(form, str) or not isinstance(material_id, str):
                continue
            if form == "nugget" and (material_id, form) in VANILLA_FORM_ITEMS:
                continue
            if form in overlay_forms or (form == "dust" and material_id == "slimy_bone"):
                forms[material_id].add(form)
                if form == "small_dust":
                    small_dust_materials.add(material_id)
    for material_id, support_forms in common.PLAYER_PATH_SUPPORT_FORMS.items():
        for form in support_forms:
            forms[material_id].add(form)
            if form == "small_dust":
                small_dust_materials.add(material_id)
    serialized = {
        material: sorted(material_forms)
        for material, material_forms in sorted(forms.items())
    }
    return {
        "schema_version": 1,
        "status": "T39_REQUIRED_FORMS_FROZEN",
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
                    f"T39 player-path missing frozen source for {relation['stable_id']}"
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
            unproven_aliases = [
                operand.get("runtime_id") or (operand.get("source") or {}).get("item")
                for operand in list(source_relation.get("item_inputs") or [])
                + list(source_relation.get("fluid_inputs") or [])
                if isinstance(operand, dict)
                and common.source_operand_is_unproven_lossy_alias(operand)
            ]
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
            alias_fail_closed = bool(unproven_aliases)
            unreachable_item_occurrences += len(unreachable_items)
            unreachable_fluid_occurrences += len(unreachable_fluids)
            item_outputs_registered = all(
                item_id.startswith(("minecraft:", "cruciblecraft:"))
                for item_id in output_ids
            )
            fluid_outputs_registered = all(
                fluid_id.startswith(("minecraft:", "cruciblecraft:"))
                for fluid_id in fluid_output_ids
            )
            rows.append({
                "consume_ids": consume_ids,
                "fluid_input_ids": fluid_input_ids,
                "fluid_inputs_reachable": fluid_inputs_reachable,
                "fluid_output_ids": fluid_output_ids,
                "fluid_outputs_registered": fluid_outputs_registered,
                "alias_fail_closed": alias_fail_closed,
                "inputs_reachable": (
                    item_inputs_reachable
                    and fluid_inputs_reachable
                    and not alias_fail_closed
                ),
                "item_inputs_reachable": item_inputs_reachable,
                "output_ids": output_ids,
                "outputs_registered": (
                    item_outputs_registered and fluid_outputs_registered
                ),
                "preserve_ids": preserve_ids,
                "stable_id": relation["stable_id"],
                "template_key": family["family_id"],
                "unproven_lossy_aliases": [
                    alias for alias in unproven_aliases if alias
                ],
            })
    rows.sort(key=lambda row: row["stable_id"])
    return {
        "families": len(families),
        "frozen_review_unmodified": True,
        "inputs_reachable": sum(1 for row in rows if row["inputs_reachable"]),
        "note": (
            "Namespace or fluid whitelist is not player reachability. Inputs "
            "are checked against the current typed T21 identity closure. "
            "Unproven source_derived_alias hosted/state collapses are "
            "fail-closed even when the aliased vanilla/CC id is in T21. "
            "The frozen T39 source remains the immutable recipe evidence."
        ),
        "outputs_registered": sum(1 for row in rows if row["outputs_registered"]),
        "reachability_source": (
            "tools/t21_operand_reachability.json#closure.reachable_identities"
        ),
        "relations": len(rows),
        "alias_fail_closed_relations": sum(
            1 for row in rows if row.get("alias_fail_closed")
        ),
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


def planned_documents(scope: str = "production") -> dict[str, str]:
    global _ACTIVE_SCOPE
    if scope not in {"production", "catalog"}:
        raise ValueError(f"unknown T39 generation scope: {scope}")
    _ACTIVE_SCOPE = scope
    source = load_source()
    catalog_relations = source["relations"]
    selected_templates = (
        set(common.production_template_keys())
        if scope == "production"
        else {
            str(row["template_key"])
            for row in load_json(common.WORK_SET).get("families") or []
        }
    )
    source_relations = [
        dict(relation)
        for relation in catalog_relations
        if relation["template_key"] in selected_templates
    ]
    if scope == "catalog":
        for relation in source_relations:
            stable_id = str(relation["stable_id"])
            relation["stable_id"] = stable_id.replace(
                "cruciblecraft:t39/",
                "cruciblecraft:t39_catalog/",
                1,
            )
    expected_family_count = (
        common.production_family_count()
        if scope == "production"
        else common.CATALOG_FAMILY_COUNT
    )
    expected_relation_count = (
        common.production_relation_count()
        if scope == "production"
        else common.CATALOG_RELATION_COUNT
    )
    output_root = (
        common.GENERATED_ROOT
        if scope == "production"
        else common.CATALOG_FIXTURE_ROOT
    )
    cc_by_source_id = load_cross_reference()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source_relations:
        grouped[relation["template_key"]].append(relation)
    if len(grouped) != expected_family_count:
        raise ValueError(
            f"T39 {scope} source groups into {len(grouped)} families, "
            f"expected {expected_family_count}"
        )

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
                raise ValueError(f"T39 stable id emitted twice: {emitted['stable_id']}")
            emitted_by_stable_id[emitted["stable_id"]] = emitted
            identities[consume_identity(emitted)].append(
                (template_key, emitted["stable_id"])
            )
        family = {
            "type": "cruciblecraft:compact_gt_recipe_family",
            "family_id": template_key,
            "target_map": common.TARGET_MAP,
            "publication_group": common.publication_group_for_expanded_count(
                len(emitted_relations)
            ),
            "source_revision": source_rows[0]["source_revision"],
            "relations": emitted_relations,
        }
        if "parameterized" in family:
            raise ValueError("T39 compact families must not be parameterized")
        filename = family_filename(template_key)
        files[filename] = stable(family)
        families.append(family)

    if len(emitted_by_stable_id) != expected_relation_count:
        raise ValueError(
            f"T39 {scope} generator emitted {len(emitted_by_stable_id)} "
            f"relations, expected {expected_relation_count}"
        )
    required_forms = required_forms_document(source_relations, cc_by_source_id)
    player_path = player_path_document(families, source_relations)
    if player_path["relations"] != expected_relation_count:
        raise ValueError("T39 player-path relation count drifted")
    if player_path["outputs_registered"] != expected_relation_count:
        raise ValueError("T39 player-path outputs are not fully registered")

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
    if shadow_notes:
        raise ValueError(
            "T39 compact consume identities collide across families: "
            + json.dumps(shadow_notes, ensure_ascii=False, sort_keys=True)
        )
    equivalence = {
        "generated": {
            "file_count": len(files),
            "files": generated_hashes,
            "root": output_root.relative_to(ROOT).as_posix(),
            "sha256": digest_text(stable(generated_hashes)),
        },
        "runtime_expected": {
            "logical_ids": len(stable_ids),
            "sha256": digest_text(stable(stable_ids)),
            "stable_ids": stable_ids,
        },
        "source": {
            "catalog_family_count": common.CATALOG_FAMILY_COUNT,
            "catalog_relation_count": len(catalog_relations),
            "family_count": len(grouped),
            "path": SOURCE.relative_to(ROOT).as_posix(),
            "relation_count": len(source_relations),
            "scope": scope,
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


def actual_recipe_files(scope: str = "production") -> dict[str, Path]:
    output_root = (
        common.GENERATED_ROOT
        if scope == "production"
        else common.CATALOG_FIXTURE_ROOT
    )
    if not output_root.is_dir():
        return {}
    return {
        path.name: path
        for path in output_root.glob("*.json")
        if path.is_file()
    }


def check(scope: str = "production") -> list[str]:
    planned = planned_documents(scope)
    recipes, sidecars = split_planned(planned)
    actual = actual_recipe_files(scope)
    errors = [
        f"generated file set drift: {name}"
        for name in sorted(set(recipes) ^ set(actual))
    ]
    for name in sorted(set(recipes) & set(actual)):
        if actual[name].read_text(encoding="utf-8") != recipes[name]:
            errors.append(f"stale generated T39 recipe: {name}")
    if scope == "production":
        sidecar_paths = {
            OPERAND_MAP.name: OPERAND_MAP,
            PLAYER_PATH.name: PLAYER_PATH,
            EQUIVALENCE.name: EQUIVALENCE,
            REQUIRED_FORMS.name: REQUIRED_FORMS,
        }
        for name, path in sidecar_paths.items():
            if not path.is_file() or path.read_text(encoding="utf-8") != sidecars[name]:
                errors.append(f"stale T39 sidecar: {name}")
    return errors


def overlay_required_forms_into_gate() -> None:
    """Fail closed when T39 would add a new form; already-registered extras are fine."""
    from tools.build_gt6_material_form_gate import GATE_OUT, load

    required = json.loads(REQUIRED_FORMS.read_text(encoding="utf-8"))
    forms = required.get("required_forms") or {}
    if not isinstance(forms, dict):
        raise ValueError("T39 required forms document is missing required_forms")
    gate = load(GATE_OUT)
    materials = gate["materials"]
    missing: list[str] = []
    for material, extra in forms.items():
        current = set(materials.get(material) or [])
        for form in extra:
            if form not in current:
                missing.append(f"{material}/{form}")
    if missing:
        raise ValueError(
            "T39 production lock requires new material forms; refusing to "
            "write MaterialRegistrationGate: " + ", ".join(sorted(missing))
        )


def write(scope: str = "production") -> None:
    planned = planned_documents(scope)
    recipes, sidecars = split_planned(planned)
    output_root = (
        common.GENERATED_ROOT
        if scope == "production"
        else common.CATALOG_FIXTURE_ROOT
    )
    output_root.mkdir(parents=True, exist_ok=True)
    for path in actual_recipe_files(scope).values():
        if path.name not in recipes:
            path.unlink()
    for name, content in recipes.items():
        (output_root / name).write_text(content, encoding="utf-8", newline="\n")
    if scope == "production":
        for name, content in sidecars.items():
            {
                OPERAND_MAP.name: OPERAND_MAP,
                PLAYER_PATH.name: PLAYER_PATH,
                EQUIVALENCE.name: EQUIVALENCE,
                REQUIRED_FORMS.name: REQUIRED_FORMS,
            }[name].write_text(content, encoding="utf-8", newline="\n")
        overlay_required_forms_into_gate()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Generate T39 Centrifuge compact recipe families."
    )
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--scope", choices=("production", "catalog", "all"), default="production"
    )
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    scopes = ("production", "catalog") if args.scope == "all" else (args.scope,)
    if args.check:
        errors = [
            error
            for scope in scopes
            for error in check(scope)
        ]
        if errors:
            print("T39 Centrifuge compact recipes are stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T39 Centrifuge compact recipes match the frozen compact source.")
        return 0
    for scope in scopes:
        write(scope)
    production_families = common.production_family_count()
    production_relations = common.production_relation_count()
    print(
        f"Wrote T39 scope={args.scope}; production is {production_families} "
        f"families / {production_relations} logical relations."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
