#!/usr/bin/env python3
"""Freeze assembler/compact Assembler singleton families into compact source.

Modes:
  --full-replay           rebuild compact source from the GT6 assembler dump
  --check                 validate committed compact source + receipt (no dump)
  --check --full-replay   rebuild from dump and compare to committed compact

Ordinary --check does not read the gitignored dump. Missing dump with
--full-replay is a closed failure, never SKIP-as-pass.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "tools"))

from gt6_recipe_templates import _stable_json, recipe_soft_key  # noqa: E402

from tools import census_common as census  # noqa: E402
from tools import repair_common as repair  # noqa: E402

TOOLS = census.TOOLS
BUILDER = Path(__file__).resolve()
CENSUS = census.CENSUS
RECIPE_FAMILIES = census.RECIPE_FAMILIES
SCHEMA = TOOLS / "assembler_compact_recipe_family_source.schema.json"
OUTPUT = TOOLS / "assembler_source.json"
RECEIPT = TOOLS / "assembler_source_receipt.json"
REVIEW = TOOLS / "assembler_source_review.json"
DUMP_MAPS = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
ASSEMBLER_DUMP = DUMP_MAPS / "gt.recipe.assembler.json"

SOURCE_MAP = "gt.recipe.assembler"
HOST = "cruciblecraft:assembler"
OWNER = "portfolio:track_a/assembler_compact_pilot"
TARGET_MAP = "cruciblecraft:assembler"
SOURCE_REVISION = census.SOURCE_REVISION

TEMPLATE_RE = re.compile(r"^gt\.recipe\.assembler#(\d{4})$")
FAMILY_SUFFIX_RE = re.compile(r"/gt\.recipe\.assembler#(\d{4})$")
EMPTY_ITEM_MARKERS = ("empty_slot", "gt.meta.empty", "gregtech:gt.meta.empty")
TOOL_MARKERS = (".tool.", "gt.meta.tool", "gt.tool.")
VANILLA_FORM_ITEMS: dict[str, tuple[str, str]] = {
    "minecraft:iron_ingot": ("iron", "ingot"),
    "minecraft:raw_iron": ("iron", "raw_ore"),
    "minecraft:copper_ingot": ("copper", "ingot"),
    "minecraft:raw_copper": ("copper", "raw_ore"),
    "minecraft:gold_ingot": ("gold", "ingot"),
    "minecraft:raw_gold": ("gold", "raw_ore"),
    "minecraft:coal": ("coal", "gem"),
    "cruciblecraft:coal_coke": ("coal_coke", "gem"),
}
# 1.7.10 dump ids that 1.21 renamed. Alias only; do not invent new recipe content.
VANILLA_RUNTIME_ALIASES: dict[str, str] = {
    "minecraft:wooden_button": "minecraft:oak_button",
    "minecraft:wooden_pressure_plate": "minecraft:oak_pressure_plate",
    "minecraft:wooden_door": "minecraft:oak_door",
    "minecraft:trapdoor": "minecraft:oak_trapdoor",
    "minecraft:noteblock": "minecraft:note_block",
}
CIRCUIT_ITEM = "gregapi:gt.integrated_circuit"
GT_FIXED_PREFIXES = ("gregtech:", "gregapi:")
CONSUMED_RECIPE_KEYS = {
    "inputs",
    "outputs",
    "fluidInputs",
    "fluidOutputs",
    "chances",
    "maxChances",
    "duration",
    "euPerTick",
    "specialValue",
    "enabled",
    "hidden",
    "fake",
    "canBeBuffered",
    "needsEmptyOutput",
    "noNbtChecks",
    "notConsumed",
    "displayName",
    "name",
}
REQUIRED_RELATION_FIELDS = (
    "family_id",
    "source_map",
    "source_recipe_index",
    "source_revision",
    "source_row_sha256",
    "stable_id",
    "target_map",
    "item_inputs",
    "item_input_counts",
    "item_input_actions",
    "item_outputs",
    "fluid_inputs",
    "fluid_outputs",
    "output_chances",
    "duration",
    "eut",
    "special_value",
    "can_be_buffered",
    "shadow_order",
    "provenance",
)
FINGERPRINT_EXCLUDE = frozenset(
    {
        "source_recipe_index",
        "source_row_sha256",
        "stable_id",
        "slot_notes",
        "unsupported_semantics",
    }
)


@dataclass(frozen=True)
class Catalogs:
    prefix_item_to_form: dict[str, str]
    material_id_to_cc: dict[int, str]
    registered_forms: dict[str, set[str]]
    form_items: dict[tuple[str, str], str]
    prefix_tags: dict[str, tuple[str, str]]
    fluid_to_cc: dict[str, str]
    reachable: set[str]


@dataclass(frozen=True)
class WorkFamily:
    family_id: str
    template_key: str
    record: dict[str, Any]


def relative(path: Path) -> str:
    try:
        return census.relative(path)
    except ValueError:
        return path.as_posix()


def load_json(path: Path) -> Any:
    return census.load_json(path)


def sha256_file(path: Path) -> str:
    return census.sha256_file(path)


def sha256_text(payload: str) -> str:
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def dump_exists(path: Path | None = None) -> bool:
    return (path or ASSEMBLER_DUMP).is_file()


def load_work_set(
    census: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
) -> list[WorkFamily]:
    """Canonical ids come only from t35_census#assembler_compact_pilot crossed with families."""
    census = census or load_json(CENSUS)
    families_doc = families_doc or load_json(RECIPE_FAMILIES)
    if census.get("source_revision") != SOURCE_REVISION:
        raise ValueError("t35_census source_revision drifted")
    if families_doc.get("source_revision") != SOURCE_REVISION:
        raise ValueError("t35_recipe_families source_revision drifted")
    if repair.SOURCE_REVISION != SOURCE_REVISION:
        raise ValueError("t36 SOURCE_REVISION drifted from t35")

    pilot = census.get("assembler_compact_pilot") or {}
    if str(pilot.get("host_map") or "") != HOST:
        raise ValueError(f"assembler_compact_pilot host_map must be {HOST}")
    family_ids = list(pilot.get("family_ids") or [])
    if len(family_ids) != 50:
        raise ValueError(f"assembler_compact_pilot family_ids must be 50, got {len(family_ids)}")

    by_id = {
        str(row.get("family_id") or ""): row
        for row in families_doc.get("families") or []
        if isinstance(row, dict)
    }
    work: list[WorkFamily] = []
    missing: list[str] = []
    for family_id in family_ids:
        record = by_id.get(family_id)
        if record is None:
            missing.append(family_id)
            continue
        errors = _validate_family_record(family_id, record)
        if errors:
            raise ValueError("; ".join(errors))
        work.append(
            WorkFamily(
                family_id=family_id,
                template_key=str(record.get("template_key") or ""),
                record=record,
            )
        )
    if missing:
        raise ValueError(
            "assembler_compact_pilot families missing from t35_recipe_families: "
            + ", ".join(missing)
        )
    _assert_contiguous_pilot(work)
    return work


def _validate_family_record(family_id: str, record: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if str(record.get("cc_host_map") or "") != HOST:
        errors.append(f"{family_id}: cc_host_map != {HOST}")
    if str(record.get("classification") or "") != "ordinary_optional":
        errors.append(f"{family_id}: classification != ordinary_optional")
    if str(record.get("membership_kind") or "") != "semantic_template":
        errors.append(f"{family_id}: membership_kind != semantic_template")
    if int(record.get("expanded_count") or 0) != 1:
        errors.append(f"{family_id}: expanded_count != 1")
    template_key = str(record.get("template_key") or "")
    matched = TEMPLATE_RE.fullmatch(template_key)
    if matched is None:
        errors.append(f"{family_id}: unexpected template_key {template_key}")
        return errors
    suffix = FAMILY_SUFFIX_RE.search(family_id)
    if suffix is None or f"{SOURCE_MAP}#{suffix.group(1)}" != template_key:
        errors.append(f"{family_id}: family suffix does not match template_key")
    number = int(matched.group(1))
    if number in (0, 1):
        errors.append(f"{family_id}: #0000/#0001 are excluded from assembler/compact")
    return errors


def _assert_contiguous_pilot(work: list[WorkFamily]) -> None:
    """Derived check only: the crossed census set must be #0002-#0051."""
    numbers: list[int] = []
    for item in work:
        matched = TEMPLATE_RE.fullmatch(item.template_key)
        if matched is None:
            raise ValueError(f"invalid template_key {item.template_key}")
        numbers.append(int(matched.group(1)))
    if sorted(numbers) != list(range(2, 52)):
        raise ValueError(
            "assembler/compact work set template numbers must be contiguous #0002-#0051"
        )
    if len({item.template_key for item in work}) != 50:
        raise ValueError("duplicate template keys in assembler/compact work set")


def recipe_template_ids(recipes: list[dict[str, Any]]) -> dict[int, str]:
    """Reuse T35/gt6_recipe_templates grouping for gt.recipe.assembler."""
    groups: dict[tuple[Any, ...], list[int]] = defaultdict(list)
    for index, recipe in enumerate(recipes):
        if recipe.get("enabled") is False:
            continue
        groups[recipe_soft_key(recipe)].append(index)
    sorted_groups = sorted(
        groups.items(),
        key=lambda item: (-len(item[1]), _stable_json(item[0])),
    )
    membership: dict[int, str] = {}
    for group_index, (_key, members) in enumerate(sorted_groups):
        template_id = f"{SOURCE_MAP}#{group_index:04d}"
        for recipe_index in members:
            membership[recipe_index] = template_id
    return membership


def load_assembler_recipes(
    path: Path | None = None,
) -> tuple[list[dict[str, Any]], str]:
    dump_path = path or ASSEMBLER_DUMP
    if not dump_path.is_file():
        display = (
            relative(dump_path)
            if dump_path.is_relative_to(ROOT)
            else dump_path.as_posix()
        )
        raise OSError(
            "missing GT6 dump map required for full replay: " + display
        )
    document = load_json(dump_path)
    if document.get("nameInternal") != SOURCE_MAP:
        raise ValueError(f"map identity mismatch: {SOURCE_MAP}")
    return list(document.get("recipes") or []), sha256_file(dump_path)


def load_catalogs(*, reachable: set[str] | None = None) -> Catalogs:
    oredict = load_json(TOOLS / "gt6_oredict_cross_reference.json")
    prefix_item_to_form = {
        str(key): str(value)
        for key, value in (oredict.get("prefix_item_to_form") or {}).items()
    }
    material_id_to_cc: dict[int, str] = {}
    for key, value in (oredict.get("material_id_to_cc") or {}).items():
        try:
            material_id_to_cc[int(key)] = str(value)
        except (TypeError, ValueError):
            continue

    gate = load_json(
        ROOT
        / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
    )
    registered_forms = {
        str(material): {str(form) for form in forms}
        for material, forms in (gate.get("materials") or {}).items()
        if isinstance(forms, list)
    }

    materials_root = ROOT / "src/main/resources/data/cruciblecraft/materials"
    form_items: dict[tuple[str, str], str] = {}
    for path in sorted(materials_root.glob("*.json")):
        if path.name == "index.json":
            continue
        material = load_json(path)
        material_id = str(material.get("id") or path.stem)
        for form, item_id in (material.get("form_items") or {}).items():
            form_items[(material_id, str(form))] = str(item_id)

    prefix_tags: dict[str, tuple[str, str]] = {}
    prefixes_root = (
        ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
    )
    for path in sorted(prefixes_root.glob("*.json")):
        if path.name == "index.json":
            continue
        prefix = load_json(path)
        form = str(prefix.get("serialized_path") or path.stem)
        namespace = str(prefix.get("tag_namespace") or "c")
        directory = str(prefix.get("tag_directory") or f"{form}s")
        prefix_tags[form] = (namespace, directory)

    fluid_to_cc: dict[str, str] = {}
    fluid_doc = load_json(TOOLS / "machine_fluid_mapping.json")
    for row in fluid_doc.get("mapping") or []:
        if not isinstance(row, dict):
            continue
        if str(row.get("disposition") or "") != "mapped":
            continue
        fluid_id = str(row.get("fluid") or "")
        cc_id = str(row.get("cc_fluid_id") or "")
        if fluid_id and cc_id:
            fluid_to_cc[fluid_id] = cc_id
            fluid_to_cc[f"cruciblecraft:{fluid_id}"] = cc_id
    builtins = (fluid_doc.get("derivation_chain") or {}).get("builtins") or {}
    for builtin, cc_id in builtins.items():
        fluid_to_cc[str(builtin)] = str(cc_id)

    if reachable is None:
        reach_doc = load_json(TOOLS / "chemical_axis_operand_reachability.json")
        reachable = {
            str(identity)
            for identity in (
                (reach_doc.get("closure") or {}).get("reachable_identities")
                or []
            )
        }
    return Catalogs(
        prefix_item_to_form=prefix_item_to_form,
        material_id_to_cc=material_id_to_cc,
        registered_forms=registered_forms,
        form_items=form_items,
        prefix_tags=prefix_tags,
        fluid_to_cc=fluid_to_cc,
        reachable=set(reachable),
    )


def _empty_item(item: Any) -> bool:
    if not item:
        return True
    item_id = str(item.get("item") or "")
    if not item_id:
        return True
    return any(marker in item_id for marker in EMPTY_ITEM_MARKERS)


def _looks_like_tool(item_id: str) -> bool:
    lowered = item_id.lower()
    return any(marker in lowered for marker in TOOL_MARKERS)


def _identity_reachable(catalogs: Catalogs, identity: str | None) -> bool | None:
    if not identity:
        return None
    if not catalogs.reachable:
        return None
    return identity in catalogs.reachable


def _material_runtime_id(catalogs: Catalogs, material: str, form: str) -> str:
    return catalogs.form_items.get(
        (material, form), f"cruciblecraft:{material}/{form}"
    )


def _material_tag(catalogs: Catalogs, material: str, form: str) -> str | None:
    tag = catalogs.prefix_tags.get(form)
    if tag is None:
        return None
    namespace, directory = tag
    return f"{namespace}:{directory}/{material}"


def _material_operand(
    catalogs: Catalogs,
    material: str,
    form: str,
    preferred_runtime: str | None,
) -> dict[str, Any]:
    runtime_id = preferred_runtime or _material_runtime_id(catalogs, material, form)
    registered = form in catalogs.registered_forms.get(material, set())
    tag = _material_tag(catalogs, material, form)
    if not registered:
        return {
            "mapping": "blocked_unmapped",
            "value": f"{material}:{form}",
            "runtime_id": None,
            "material": material,
            "form": form,
            "tag": tag,
            "alias": None,
            "reachable": False,
        }
    mapping = "registered_material_form"
    if preferred_runtime and preferred_runtime.startswith("minecraft:"):
        mapping = "exact_runtime_id"
    reachable = _identity_reachable(catalogs, f"item:{runtime_id}")
    if reachable is False:
        reachable = _identity_reachable(
            catalogs, f"item:cruciblecraft:{material}/{form}"
        )
    return {
        "mapping": mapping,
        "value": f"{material}:{form}",
        "runtime_id": runtime_id,
        "material": material,
        "form": form,
        "tag": tag,
        "alias": None,
        "reachable": reachable,
    }


def map_item_operand(
    item: dict[str, Any],
    catalogs: Catalogs,
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    errors: list[str] = []
    item_id = str(item.get("item") or "")
    meta = item.get("meta")
    count = int(item.get("count") or 0)
    operand: dict[str, Any] = {
        "mapping": "blocked_unmapped",
        "source": {
            "item": item_id,
            "count": count,
            "meta": meta,
            "displayName": item.get("displayName"),
        },
        "value": None,
        "runtime_id": None,
        "material": None,
        "form": None,
        "tag": None,
        "alias": None,
        "reachable": None,
        "slot_class": "occupied",
    }

    vanilla = VANILLA_FORM_ITEMS.get(item_id)
    if vanilla is not None:
        material, form = vanilla
        operand.update(_material_operand(catalogs, material, form, item_id))
        return operand, errors

    alias_id = VANILLA_RUNTIME_ALIASES.get(item_id)
    if alias_id:
        operand.update(
            {
                "mapping": "source_derived_alias",
                "value": alias_id,
                "runtime_id": alias_id,
                "alias": alias_id,
                "reachable": _identity_reachable(catalogs, f"item:{alias_id}"),
            }
        )
        return operand, errors

    form = catalogs.prefix_item_to_form.get(item_id)
    if form and isinstance(meta, int):
        material = catalogs.material_id_to_cc.get(meta)
        if material:
            operand.update(_material_operand(catalogs, material, form, None))
            if operand["mapping"] == "blocked_unmapped":
                errors.append(
                    f"unregistered material form {material}:{form} from {item_id}@{meta}"
                )
            return operand, errors
        errors.append(f"unknown material meta {meta} for {item_id}")
        operand["value"] = f"{item_id}@{meta}"
        return operand, errors

    if item_id.startswith("minecraft:") or item_id.startswith("cruciblecraft:"):
        operand.update(
            {
                "mapping": "exact_runtime_id",
                "value": item_id,
                "runtime_id": item_id,
                "reachable": _identity_reachable(catalogs, f"item:{item_id}"),
            }
        )
        return operand, errors

    if item_id.startswith(GT_FIXED_PREFIXES):
        alias = f"fixed:{item_id}@{meta if meta is not None else 'none'}"
        operand.update(
            {
                "mapping": "source_derived_alias",
                "value": alias,
                "runtime_id": None,
                "alias": alias,
                "reachable": False,
                "slot_class": "catalyst" if item_id == CIRCUIT_ITEM else "occupied",
            }
        )
        return operand, errors

    errors.append(f"blocked_unmapped {side} item {item_id}@{meta}")
    operand["value"] = f"{item_id}@{meta}"
    return operand, errors


def map_fluid_operand(
    fluid: dict[str, Any],
    catalogs: Catalogs,
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    errors: list[str] = []
    fluid_id = str(fluid.get("fluid") or fluid.get("id") or "")
    amount = int(fluid.get("amount") or 0)
    operand: dict[str, Any] = {
        "mapping": "blocked_unmapped",
        "source": {"fluid": fluid_id, "amount": amount},
        "value": None,
        "runtime_id": None,
        "material": None,
        "form": None,
        "tag": None,
        "alias": None,
        "reachable": None,
        "slot_class": "occupied",
    }
    if not fluid_id or amount <= 0:
        operand["slot_class"] = "empty"
        return operand, errors

    mapped = catalogs.fluid_to_cc.get(fluid_id) or catalogs.fluid_to_cc.get(
        fluid_id.split(":", 1)[-1]
    )
    if mapped:
        alias = None
        mapping = "exact_runtime_id"
        if mapped != fluid_id:
            mapping = "source_derived_alias"
            alias = mapped
        operand.update(
            {
                "mapping": mapping,
                "value": mapped,
                "runtime_id": mapped,
                "alias": alias,
                "reachable": _identity_reachable(catalogs, f"fluid:{mapped}"),
            }
        )
        return operand, errors

    if fluid_id.startswith("minecraft:") or fluid_id.startswith("cruciblecraft:"):
        operand.update(
            {
                "mapping": "exact_runtime_id",
                "value": fluid_id,
                "runtime_id": fluid_id,
                "reachable": _identity_reachable(catalogs, f"fluid:{fluid_id}"),
            }
        )
        return operand, errors

    errors.append(f"blocked_unmapped {side} fluid {fluid_id}")
    operand["value"] = fluid_id
    return operand, errors


def classify_item_action(item: dict[str, Any]) -> dict[str, Any]:
    item_id = str(item.get("item") or "")
    count = int(item.get("count") or 0)
    damage = item.get("damage")
    not_consumed = bool(item.get("notConsumed") or item.get("not_consumed"))
    if isinstance(damage, int) and damage > 0:
        return {"kind": "WEAR", "damage": damage}
    if not_consumed or count == 0:
        if _looks_like_tool(item_id) and isinstance(damage, int) and damage > 0:
            return {"kind": "WEAR", "damage": damage}
        return {"kind": "PRESERVE", "damage": 0}
    return {"kind": "CONSUME", "damage": 0}


def _source_row_hash(recipe: dict[str, Any]) -> str:
    return sha256_text(_stable_json(recipe))


def fingerprint_payload(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        key: value
        for key, value in relation.items()
        if key not in FINGERPRINT_EXCLUDE
    }


def stable_id_for(relation: dict[str, Any]) -> str:
    digest = sha256_text(_stable_json(fingerprint_payload(relation)))[:16]
    family_id = str(relation.get("family_id") or "unknown")
    return f"assembler/compact/{SOURCE_REVISION}/{family_id}/{digest}"


def compile_relation(
    *,
    work: WorkFamily,
    recipe: dict[str, Any],
    recipe_index: int,
    catalogs: Catalogs,
) -> tuple[dict[str, Any], list[str]]:
    errors: list[str] = []
    unsupported: list[str] = []
    slot_notes: list[dict[str, Any]] = []

    if recipe.get("hidden") is True:
        unsupported.append("hidden")
    if recipe.get("fake") is True:
        unsupported.append("fake")
    if recipe.get("enabled") is False:
        unsupported.append("disabled")
    extra_keys = sorted(set(recipe) - CONSUMED_RECIPE_KEYS)
    unsupported.extend(f"unclassified_field:{key}" for key in extra_keys)
    needs_empty_output = bool(recipe.get("needsEmptyOutput"))
    no_nbt_checks = (
        bool(recipe.get("noNbtChecks")) if "noNbtChecks" in recipe else True
    )
    if needs_empty_output:
        unsupported.append("needsEmptyOutput")

    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("inputs") or []):
        if _empty_item(item):
            slot_notes.append(
                {"side": "item_input", "slot": slot, "class": "empty_slot"}
            )
            continue
        operand, operand_errors = map_item_operand(
            item, catalogs, side="item_input"
        )
        errors.extend(operand_errors)
        action = classify_item_action(item)
        item_id = str(item.get("item") or "")
        if action["kind"] == "PRESERVE":
            operand["slot_class"] = (
                "tool" if _looks_like_tool(item_id) else "catalyst"
            )
            slot_notes.append(
                {
                    "side": "item_input",
                    "slot": slot,
                    "class": operand["slot_class"],
                }
            )
        elif action["kind"] == "WEAR":
            operand["slot_class"] = "tool_wear"
            slot_notes.append(
                {"side": "item_input", "slot": slot, "class": "tool_wear"}
            )
        count = int(item.get("count") or 0)
        if action["kind"] != "CONSUME":
            count = 0
        item_inputs.append(operand)
        item_input_counts.append(count)
        item_input_actions.append(action)

    item_outputs: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("outputs") or []):
        if _empty_item(item):
            slot_notes.append(
                {"side": "item_output", "slot": slot, "class": "empty_slot"}
            )
            continue
        operand, operand_errors = map_item_operand(
            item, catalogs, side="item_output"
        )
        errors.extend(operand_errors)
        item_outputs.append(operand)

    fluid_inputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidInputs") or []):
        if not fluid:
            slot_notes.append(
                {"side": "fluid_input", "slot": slot, "class": "empty_slot"}
            )
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, side="fluid_input"
        )
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_inputs.append(operand)

    fluid_outputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidOutputs") or []):
        if not fluid:
            slot_notes.append(
                {"side": "fluid_output", "slot": slot, "class": "empty_slot"}
            )
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, side="fluid_output"
        )
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_outputs.append(operand)

    chances = [int(value) for value in (recipe.get("chances") or [])]
    if chances and len(chances) > len(item_outputs):
        chances = chances[: len(item_outputs)]
    if not chances and item_outputs:
        chances = [10000] * len(item_outputs)
    elif chances and len(chances) < len(item_outputs):
        chances.extend([10000] * (len(item_outputs) - len(chances)))
    max_chances = list(recipe.get("maxChances") or [])
    if max_chances and max_chances[: len(chances)] != chances:
        unsupported.append("maxChances_mismatch")

    duration = int(recipe.get("duration") or 0)
    if duration <= 0:
        errors.append(f"{work.template_key}: duration must be positive")
    if "canBeBuffered" in recipe:
        can_be_buffered = bool(recipe.get("canBeBuffered"))
        buffer_kind = "SOURCE_BACKED"
    else:
        can_be_buffered = True
        buffer_kind = "DESIGN_POLICY"

    provenance_fields = {
        "item_inputs": "SOURCE_DERIVED",
        "item_input_counts": "SOURCE_BACKED",
        "item_input_actions": "SOURCE_DERIVED",
        "item_outputs": "SOURCE_DERIVED",
        "fluid_inputs": "SOURCE_DERIVED",
        "fluid_outputs": "SOURCE_DERIVED",
        "output_chances": "SOURCE_BACKED",
        "duration": "SOURCE_BACKED",
        "eut": "SOURCE_BACKED",
        "special_value": "SOURCE_BACKED",
        "can_be_buffered": buffer_kind,
        "needs_empty_output": "SOURCE_BACKED",
        "no_nbt_checks": "SOURCE_BACKED",
        "shadow_order": "SOURCE_DERIVED",
        "stable_id": "SOURCE_DERIVED",
    }
    relation = {
        "family_id": work.family_id,
        "template_key": work.template_key,
        "source_map": SOURCE_MAP,
        "source_recipe_index": recipe_index,
        "source_revision": SOURCE_REVISION,
        "source_row_sha256": _source_row_hash(recipe),
        "target_map": TARGET_MAP,
        "item_inputs": item_inputs,
        "item_input_counts": item_input_counts,
        "item_input_actions": item_input_actions,
        "item_outputs": item_outputs,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "output_chances": chances,
        "duration": duration,
        "eut": int(recipe.get("euPerTick") or 0),
        "special_value": int(recipe.get("specialValue") or 0),
        "can_be_buffered": can_be_buffered,
        "needs_empty_output": needs_empty_output,
        "no_nbt_checks": no_nbt_checks,
        "shadow_order": 0,
        "slot_notes": slot_notes,
        "unsupported_semantics": unsupported,
        "provenance": {
            "kinds": sorted(set(provenance_fields.values())),
            "fields": provenance_fields,
        },
    }
    relation["stable_id"] = stable_id_for(relation)
    if unsupported:
        errors.extend(
            f"{work.template_key}: unsupported {item}" for item in unsupported
        )
    operands = item_inputs + item_outputs + fluid_inputs + fluid_outputs
    if any(op.get("mapping") == "blocked_unmapped" for op in operands):
        errors.append(f"{work.template_key}: blocked_unmapped operand")
    return relation, errors


def assign_work_rows(
    recipes: list[dict[str, Any]],
    work: list[WorkFamily],
) -> tuple[dict[str, tuple[int, dict[str, Any]]], dict[str, list[str]]]:
    membership = recipe_template_ids(recipes)
    wanted = {item.template_key for item in work}
    grouped: dict[str, list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    for index, template_id in membership.items():
        if template_id in wanted:
            grouped[template_id].append((index, recipes[index]))
    missing = [item.template_key for item in work if item.template_key not in grouped]
    duplicate = [key for key, rows in grouped.items() if len(rows) != 1]
    extra = sorted(set(membership.values()) - wanted - {f"{SOURCE_MAP}#0000", f"{SOURCE_MAP}#0001"})
    extra = [key for key in extra if key.startswith(f"{SOURCE_MAP}#")]
    assigned = {key: rows[0] for key, rows in grouped.items() if len(rows) == 1}
    return assigned, {
        "missing": missing,
        "extra": [],
        "duplicate": duplicate,
    }


def compile_relations(
    recipes: list[dict[str, Any]],
    work: list[WorkFamily],
    catalogs: Catalogs,
) -> tuple[list[dict[str, Any]], dict[str, list[str]], list[str]]:
    assigned, report = assign_work_rows(recipes, work)
    errors: list[str] = []
    if report["missing"] or report["duplicate"] or len(assigned) != 50:
        errors.append(
            "assignment failed assigned="
            f"{len(assigned)} missing={len(report['missing'])} "
            f"duplicate={len(report['duplicate'])}"
        )
    relations: list[dict[str, Any]] = []
    for item in work:
        row = assigned.get(item.template_key)
        if row is None:
            continue
        index, recipe = row
        relation, row_errors = compile_relation(
            work=item,
            recipe=recipe,
            recipe_index=index,
            catalogs=catalogs,
        )
        errors.extend(row_errors)
        relations.append(relation)
    return relations, report, errors


def build_review(document: dict[str, Any]) -> dict[str, Any]:
    relations = document.get("relations") or []
    slot_shapes: Counter[str] = Counter()
    mapping_counts: Counter[str] = Counter()
    action_counts: Counter[str] = Counter()
    chance_values: Counter[int] = Counter()
    special_values: Counter[int] = Counter()
    unmapped: list[str] = []
    unsupported: list[str] = []
    consume_index: dict[str, list[str]] = defaultdict(list)
    reachability: list[dict[str, Any]] = []

    for relation in relations:
        slot_shapes[
            f"I{len(relation.get('item_inputs') or [])}/"
            f"O{len(relation.get('item_outputs') or [])}/"
            f"FI{len(relation.get('fluid_inputs') or [])}/"
            f"FO{len(relation.get('fluid_outputs') or [])}"
        ] += 1
        operands = (
            (relation.get("item_inputs") or [])
            + (relation.get("item_outputs") or [])
            + (relation.get("fluid_inputs") or [])
            + (relation.get("fluid_outputs") or [])
        )
        for operand in operands:
            mapping = str(operand.get("mapping") or "blocked_unmapped")
            mapping_counts[mapping] += 1
            if mapping == "blocked_unmapped":
                unmapped.append(
                    f"{relation['family_id']}:{operand.get('value') or operand.get('source')}"
                )
        for action in relation.get("item_input_actions") or []:
            action_counts[str(action.get("kind") or "CONSUME")] += 1
        for note in relation.get("slot_notes") or []:
            if note.get("class") in {
                "catalyst",
                "tool",
                "tool_wear",
                "container_return",
            }:
                action_counts[str(note.get("class"))] += 1
        for chance in relation.get("output_chances") or []:
            chance_values[int(chance)] += 1
        special_values[int(relation.get("special_value") or 0)] += 1
        unsupported.extend(
            f"{relation['family_id']}:{item}"
            for item in relation.get("unsupported_semantics") or []
        )
        consume_key = _stable_json(
            [
                (operand.get("value"), count)
                for operand, count, action in zip(
                    relation.get("item_inputs") or [],
                    relation.get("item_input_counts") or [],
                    relation.get("item_input_actions") or [],
                )
                if action.get("kind") == "CONSUME"
            ]
        )
        consume_index[consume_key].append(str(relation.get("family_id")))
        consume_actions = {
            index
            for index, action in enumerate(relation.get("item_input_actions") or [])
            if str(action.get("kind") or "CONSUME") == "CONSUME"
        }
        consumed_items = [
            operand
            for index, operand in enumerate(relation.get("item_inputs") or [])
            if index in consume_actions
        ]
        input_reachable = [
            bool(operand.get("reachable"))
            for operand in consumed_items + (relation.get("fluid_inputs") or [])
            if operand.get("mapping") != "blocked_unmapped"
        ]
        output_known = [
            operand.get("runtime_id") is not None
            for operand in (relation.get("item_outputs") or [])
            + (relation.get("fluid_outputs") or [])
        ]
        reachability.append(
            {
                "family_id": relation.get("family_id"),
                "inputs_reachable": all(input_reachable) if input_reachable else False,
                "outputs_registered": all(output_known) if output_known else False,
                "blocked_unmapped": any(
                    operand.get("mapping") == "blocked_unmapped" for operand in operands
                ),
            }
        )

    shadow_candidates = [
        family_id
        for family_ids in consume_index.values()
        if len(family_ids) > 1
        for family_id in family_ids
    ]
    blockers = list(document.get("blockers") or [])
    r3 = []
    assignment = document.get("assignment") or {}
    if int(assignment.get("assigned") or 0) != 50:
        r3.append("assignment.assigned != 50")
    if assignment.get("missing") or assignment.get("extra") or assignment.get("duplicate"):
        r3.append("assignment has missing/extra/duplicate")
    if unmapped:
        r3.append(f"blocked_unmapped operands: {len(unmapped)}")
    if unsupported:
        r3.append(f"unsupported semantics: {len(unsupported)}")
    unreachable = [
        row["family_id"]
        for row in reachability
        if not row.get("inputs_reachable") or not row.get("outputs_registered")
    ]
    if unreachable:
        r3.append(f"player-path gaps: {len(unreachable)}")
    if document.get("status") != "ASSEMBLER_COMPACT_ASSEMBLER_SOURCE_FROZEN":
        r3.append(f"status={document.get('status')}")

    return {
        "schema_version": 1,
        "status": document.get("status"),
        "source_revision": SOURCE_REVISION,
        "slot_shape_distribution": dict(sorted(slot_shapes.items())),
        "mapping_counts": {
            "exact_runtime_id": mapping_counts.get("exact_runtime_id", 0),
            "registered_material_form": mapping_counts.get(
                "registered_material_form", 0
            ),
            "canonical_tag": mapping_counts.get("canonical_tag", 0),
            "source_derived_alias": mapping_counts.get("source_derived_alias", 0),
            "blocked_unmapped": mapping_counts.get("blocked_unmapped", 0),
        },
        "action_counts": dict(sorted(action_counts.items())),
        "output_chance_distribution": {
            str(key): value for key, value in sorted(chance_values.items())
        },
        "special_value_distribution": {
            str(key): value for key, value in sorted(special_values.items())
        },
        "unmapped_operands": unmapped,
        "unsupported_semantics": unsupported,
        "input_shadow_candidates": sorted(set(shadow_candidates)),
        "per_row_reachability": reachability,
        "blockers": blockers,
        "r3_blockers": r3,
    }


def build_document(
    relations: list[dict[str, Any]],
    work: list[WorkFamily],
    report: dict[str, list[str]],
    errors: list[str],
) -> dict[str, Any]:
    frozen = (
        not errors
        and len(relations) == 50
        and not report["missing"]
        and not report["duplicate"]
    )
    return {
        "schema_version": 1,
        "status": (
            "ASSEMBLER_COMPACT_ASSEMBLER_SOURCE_FROZEN"
            if frozen
            else "ASSEMBLER_COMPACT_ASSEMBLER_SOURCE_BLOCKED"
        ),
        "source_revision": SOURCE_REVISION,
        "generated_by": "python tools/build_assembler_compact_assembler_source.py --full-replay",
        "host": HOST,
        "owner": OWNER,
        "source_map": SOURCE_MAP,
        "target_map": TARGET_MAP,
        "work_set": {
            "family_ids": [item.family_id for item in work],
            "template_keys": [item.template_key for item in work],
            "family_count": 50,
            "source_rows": 50,
        },
        "assignment": {
            "assigned": len(relations),
            "missing": report["missing"],
            "extra": report["extra"],
            "duplicate": report["duplicate"],
        },
        "relations": relations,
        "provenance_kinds": ["DESIGN_POLICY", "SOURCE_BACKED", "SOURCE_DERIVED"],
        "blockers": errors,
    }


def build_receipt(
    document: dict[str, Any],
    *,
    dump_sha256: str | None,
    dump_path: Path | None,
) -> dict[str, Any]:
    receipt: dict[str, Any] = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "host": HOST,
        "owner": OWNER,
        "compact_source": relative(OUTPUT),
        "compact_sha256": sha256_text(census.stable_json(document)),
        "schema_sha256": sha256_file(SCHEMA) if SCHEMA.is_file() else None,
        "builder_sha256": sha256_file(BUILDER),
        "census_sha256": sha256_file(CENSUS),
        "recipe_families_sha256": sha256_file(RECIPE_FAMILIES),
        "full_replay": {
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
    }
    if dump_sha256 and dump_path is not None:
        receipt["dump"] = {
            "path": (
                relative(dump_path)
                if dump_path.is_relative_to(ROOT)
                else dump_path.as_posix()
            ),
            "sha256": dump_sha256,
        }
    return receipt


def build_from_dump(
    *,
    dump_path: Path | None = None,
    census: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
    catalogs: Catalogs | None = None,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], str]:
    work = load_work_set(census, families_doc)
    recipes, dump_sha256 = load_assembler_recipes(dump_path)
    catalogs = catalogs or load_catalogs()
    relations, report, errors = compile_relations(recipes, work, catalogs)
    document = build_document(relations, work, report, errors)
    review = build_review(document)
    receipt = build_receipt(
        document,
        dump_sha256=dump_sha256,
        dump_path=dump_path or ASSEMBLER_DUMP,
    )
    return document, receipt, review, dump_sha256


def validate_committed(
    document: dict[str, Any],
    *,
    census: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
) -> list[str]:
    errors: list[str] = []
    if document.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if document.get("source_revision") != SOURCE_REVISION:
        errors.append("source_revision drifted")
    if document.get("host") != HOST:
        errors.append(f"host != {HOST}")
    if document.get("owner") != OWNER:
        errors.append(f"owner != {OWNER}")
    try:
        work = load_work_set(census, families_doc)
    except ValueError as error:
        errors.append(str(error))
        return errors
    work_set = document.get("work_set") or {}
    if list(work_set.get("family_ids") or []) != [item.family_id for item in work]:
        errors.append("work_set.family_ids drifted from t35_census#assembler_compact_pilot")
    if list(work_set.get("template_keys") or []) != [
        item.template_key for item in work
    ]:
        errors.append("work_set.template_keys drifted")
    relations = document.get("relations") or []
    if len(relations) != 50:
        errors.append(f"relations length {len(relations)} != 50")
    assignment = document.get("assignment") or {}
    if int(assignment.get("assigned") or 0) != 50:
        errors.append("assignment.assigned != 50")
    for key in ("missing", "extra", "duplicate"):
        if assignment.get(key):
            errors.append(f"assignment.{key} is not empty")
    family_ids = [row.get("family_id") for row in relations]
    if len(family_ids) != len(set(family_ids)):
        errors.append("duplicate family_id in relations")
    for row in relations:
        family_id = str(row.get("family_id") or "")
        if family_id.endswith("#0000") or family_id.endswith("#0001"):
            errors.append("excluded family #0000/#0001 present")
        missing_fields = [
            name for name in REQUIRED_RELATION_FIELDS if name not in row
        ]
        if missing_fields:
            errors.append(f"{family_id} missing {missing_fields}")
        operands = (
            (row.get("item_inputs") or [])
            + (row.get("item_outputs") or [])
            + (row.get("fluid_inputs") or [])
            + (row.get("fluid_outputs") or [])
        )
        if any(op.get("mapping") == "blocked_unmapped" for op in operands):
            errors.append(f"{family_id} has blocked_unmapped operand")
        if row.get("unsupported_semantics"):
            errors.append(f"{family_id} has unsupported semantics")
    return errors


def reference_only_check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing compact source: {relative(OUTPUT)}"]
    if not RECEIPT.is_file():
        return [f"missing receipt: {relative(RECEIPT)}"]
    try:
        document = load_json(OUTPUT)
        receipt = load_json(RECEIPT)
    except json.JSONDecodeError as error:
        return [f"malformed JSON: {error}"]
    errors: list[str] = []
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != census.stable_json(document):
        errors.append(f"{relative(OUTPUT)} is not canonical JSON")
    if receipt.get("compact_sha256") != sha256_text(actual):
        errors.append("receipt.compact_sha256 does not match compact source")
    if receipt.get("source_revision") != SOURCE_REVISION:
        errors.append("receipt source_revision drifted")
    errors.extend(validate_committed(document))
    if document.get("status") != "ASSEMBLER_COMPACT_ASSEMBLER_SOURCE_FROZEN":
        errors.append(f"compact status is {document.get('status')}, not FROZEN")
    if document.get("blockers"):
        errors.append("compact source still has blockers")
    return errors


def full_replay_check() -> list[str]:
    errors = reference_only_check()
    try:
        expected, _receipt, _review, dump_sha256 = build_from_dump()
    except OSError as error:
        return [str(error)]
    if OUTPUT.is_file():
        actual = OUTPUT.read_text(encoding="utf-8")
        if actual != census.stable_json(expected):
            errors.append(f"{relative(OUTPUT)} is stale under full replay")
    receipt = load_json(RECEIPT) if RECEIPT.is_file() else {}
    recorded = (receipt.get("dump") or {}).get("sha256")
    if recorded != dump_sha256:
        errors.append("receipt dump sha256 is stale")
    return errors


def write(
    *,
    dump_path: Path | None = None,
    census: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
    catalogs: Catalogs | None = None,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    document, receipt, review, _digest = build_from_dump(
        dump_path=dump_path,
        census=census,
        families_doc=families_doc,
        catalogs=catalogs,
    )
    census.write_stable(OUTPUT, document)
    census.write_stable(RECEIPT, receipt)
    census.write_stable(REVIEW, review)
    return document, receipt, review


def check() -> list[str]:
    return reference_only_check()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check and args.full_replay:
            errors = full_replay_check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load_json(OUTPUT)
            tier = "full replay"
        elif args.check:
            errors = reference_only_check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load_json(OUTPUT)
            tier = "compact"
        elif args.full_replay:
            document, _receipt, _review = write()
            if document.get("status") != "ASSEMBLER_COMPACT_ASSEMBLER_SOURCE_FROZEN":
                raise ValueError(
                    "source freeze blocked: "
                    + "; ".join(document.get("blockers") or ["unspecified"])
                )
            tier = "full replay write"
        else:
            if not OUTPUT.is_file():
                raise ValueError(
                    "compact source is absent; first generation requires "
                    "--full-replay against gt6_dump/.../gt.recipe.assembler.json"
                )
            document = load_json(OUTPUT)
            census.write_stable(REVIEW, build_review(document))
            tier = "review refresh"
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"assembler/compact assembler source failed: {error}", file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                "status": document.get("status"),
                "assigned": (document.get("assignment") or {}).get("assigned"),
                "proof_tier": tier,
                "blockers": document.get("blockers") or [],
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
