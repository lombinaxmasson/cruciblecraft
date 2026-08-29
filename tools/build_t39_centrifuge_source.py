#!/usr/bin/env python3
"""Freeze the T39 Centrifuge compact source from classified GT6 rows.

Modes:
  --full-replay           rebuild source, receipt, and review from the GT6 dump
  --check                 validate committed source and receipt without the dump
  --check --full-replay   rebuild from the dump and compare to the committed source

The GT6 dump is intentionally gitignored.  A missing dump is a closed failure
for every full-replay mode; it is never interpreted as a skipped passing check.
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
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(ROOT / "tools") not in sys.path:
    sys.path.insert(0, str(ROOT / "tools"))

from gt6_recipe_templates import _stable_json, recipe_soft_key  # noqa: E402
from tools import build_t37_assembler_source as t37  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t39_common as common  # noqa: E402

TOOLS = common.TOOLS
BUILDER = Path(__file__).resolve()
SCHEMA = common.SCHEMA
OUTPUT = common.SOURCE
RECEIPT = common.RECEIPT
REVIEW = common.REVIEW
WORK_SET = common.WORK_SET
T38_CENSUS_DELTA = common.T38_CENSUS_DELTA
RECIPE_FAMILIES = common.RECIPE_FAMILIES
CENSUS = common.T35_CENSUS
ROW_CLASSIFICATION = common.ROW_CLASSIFICATION
TEMPLATE_DENOMINATOR = common.TEMPLATE_DENOMINATOR
CENTRIFUGE_DUMP = common.CENTRIFUGE_DUMP

SOURCE_REVISION = common.SOURCE_REVISION
HOST = common.HOST
SOURCE_MAP = common.SOURCE_MAP
TARGET_MAP = common.TARGET_MAP
OWNER = common.OWNER
FAMILY_COUNT = common.FAMILY_COUNT
SOURCE_ROWS = common.SOURCE_ROWS

TEMPLATE_RE = re.compile(r"^gt\.recipe\.centrifuge#(\d{4})$")
FAMILY_SUFFIX_RE = re.compile(r"/gt\.recipe\.centrifuge#(\d{4})$")
CENTRIFUGE_MAP_INDEX = 1
ORDINARY_CLASS = "ordinary_optional"
FINGERPRINT_EXCLUDE = frozenset(
    {
        "source_recipe_index",
        "source_row_sha256",
        "stable_id",
        "slot_notes",
        "unsupported_semantics",
        # The order is a source-location diagnostic, not semantic identity.
        # Excluding it keeps a content-identical row stable after a dump reorder.
        "shadow_order",
    }
)
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
CONSUMED_RECIPE_KEYS = t37.CONSUMED_RECIPE_KEYS

# The underlying mapping vocabulary is deliberately shared with the calibrated
# T37 source contract.  In particular, unknown gregtech fixed-prefix operands
# become source_derived_alias entries instead of being silently discarded.
Catalogs = t37.Catalogs
load_catalogs = t37.load_catalogs
classify_item_action = t37.classify_item_action

# Bounded T39 aliases onto already-registered vanilla/CC fluids. Mapping kind
# stays source_derived_alias; DESIGN_POLICY rows are called out in review.
T39_FLUID_ALIASES: dict[str, str] = {
    "cactuswater": "minecraft:water",
    "watergeothermal": "cruciblecraft:water_dirty",
    "ic2pahoehoelava": "minecraft:lava",
    "dragonbreath": "minecraft:lava",
    "mushroomsoup": "cruciblecraft:biomass",
    "potion.ambrosia": "minecraft:water",
    "potion.damage": "minecraft:water",
    "slime": "cruciblecraft:glue",
}
T39_FLUID_ALIAS_POLICY = frozenset({
    "dragonbreath",
    "mushroomsoup",
    "potion.ambrosia",
    "potion.damage",
    "slime",
})


def map_fluid_operand(
    fluid: dict[str, Any],
    catalogs: Catalogs,
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    operand, errors = t37.map_fluid_operand(fluid, catalogs, side=side)
    if operand.get("mapping") != "blocked_unmapped":
        return operand, errors
    fluid_id = str((operand.get("source") or {}).get("fluid") or "")
    mapped = T39_FLUID_ALIASES.get(fluid_id)
    if not mapped:
        return operand, errors
    operand.update(
        {
            "mapping": "source_derived_alias",
            "value": mapped,
            "runtime_id": mapped,
            "alias": mapped,
            "reachable": t37._identity_reachable(catalogs, f"fluid:{mapped}"),
        }
    )
    errors = [
        error
        for error in errors
        if error != f"blocked_unmapped {side} fluid {fluid_id}"
    ]
    return operand, errors


def map_item_operand(
    item: dict[str, Any],
    catalogs: Catalogs,
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    """Reuse T37 mapping, allowing a declared material tag when no item exists.

    Unregistered material forms keep their canonical c: tags instead of being
    discarded. Do not pretend they are registered_material_form entries.
    """
    operand, errors = t37.map_item_operand(item, catalogs, side=side)
    if (
        operand.get("mapping") == "blocked_unmapped"
        and operand.get("material")
        and operand.get("form")
        and operand.get("tag")
    ):
        operand.update(
            {
                "mapping": "canonical_tag",
                "value": operand["tag"],
                "runtime_id": None,
                "alias": None,
                # Tag reachability is not represented in the item identity closure.
                "reachable": None,
            }
        )
        errors = [
            error
            for error in errors
            if not error.startswith("unregistered material form ")
        ]
    return operand, errors


@dataclass(frozen=True)
class WorkFamily:
    family_id: str
    template_key: str
    record: dict[str, Any]


def relative(path: Path) -> str:
    return common.relative(path)


def load_json(path: Path) -> Any:
    return common.load_json(path)


def sha256_file(path: Path) -> str:
    return t35.sha256_file(path)


def sha256_text(payload: str) -> str:
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def dump_exists(path: Path | None = None) -> bool:
    return (path or CENTRIFUGE_DUMP).is_file()


def _validate_family_record(family_id: str, record: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if str(record.get("cc_host_map") or "") != HOST:
        errors.append(f"{family_id}: cc_host_map != {HOST}")
    if str(record.get("classification") or "") != ORDINARY_CLASS:
        errors.append(f"{family_id}: classification != {ORDINARY_CLASS}")
    if str(record.get("membership_kind") or "") != "semantic_template":
        errors.append(f"{family_id}: membership_kind != semantic_template")
    template_key = str(record.get("template_key") or "")
    match = TEMPLATE_RE.fullmatch(template_key)
    if match is None:
        errors.append(f"{family_id}: unexpected template_key {template_key}")
        return errors
    suffix = FAMILY_SUFFIX_RE.search(family_id)
    if suffix is None or f"{SOURCE_MAP}#{suffix.group(1)}" != template_key:
        errors.append(f"{family_id}: family suffix does not match template_key")
    expected_rows = record.get("expanded_count")
    if not isinstance(expected_rows, int) or expected_rows <= 0:
        errors.append(f"{family_id}: expanded_count must be a positive integer")
    return errors


def load_work_set(
    work_set: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
) -> list[WorkFamily]:
    """Cross the frozen T39 work set with the immutable T35 ledger."""
    work_set = work_set if work_set is not None else load_json(WORK_SET)
    families_doc = families_doc if families_doc is not None else load_json(RECIPE_FAMILIES)
    if families_doc.get("source_revision") != SOURCE_REVISION:
        raise ValueError("t35_recipe_families source_revision drifted")
    if work_set.get("selection_sha256") != common.EXPECTED_SELECTION_SHA256:
        raise ValueError("t39_work_set selection_sha256 drifted")
    family_ids = [str(value) for value in (work_set.get("family_ids") or [])]
    template_keys = [str(value) for value in (work_set.get("template_keys") or [])]
    if family_ids != [str(row["family_id"]) for row in common.select_centrifuge_families(families_doc)]:
        raise ValueError("T39 family ids drifted from the frozen T35 ledger rule")
    if template_keys != [family_id.rsplit("/", 1)[-1] for family_id in family_ids]:
        raise ValueError("T39 family ids drifted from the frozen template sequence")

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
                template_key=str(record["template_key"]),
                record=record,
            )
        )
    if missing:
        raise ValueError(
            "T39 work set families missing from t35_recipe_families: "
            + ", ".join(missing)
        )
    if len(work) != FAMILY_COUNT:
        raise ValueError(f"T39 work set must contain {FAMILY_COUNT} families")
    if sum(int(item.record["expanded_count"]) for item in work) != SOURCE_ROWS:
        raise ValueError(f"T39 expanded_count total must be {SOURCE_ROWS}")
    return work


def recipe_template_ids(recipes: list[dict[str, Any]]) -> dict[int, str]:
    """Assign the exact stable T35 grouping ids, skipping disabled recipes."""
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
        template_key = f"{SOURCE_MAP}#{group_index:04d}"
        for recipe_index in members:
            membership[recipe_index] = template_key
    return membership


def load_centrifuge_recipes(
    path: Path | None = None,
) -> tuple[list[dict[str, Any]], str]:
    dump_path = path or CENTRIFUGE_DUMP
    if not dump_path.is_file():
        display = relative(dump_path) if dump_path.is_relative_to(ROOT) else dump_path.as_posix()
        raise OSError("missing GT6 dump map required for full replay: " + display)
    document = load_json(dump_path)
    if document.get("nameInternal") != SOURCE_MAP:
        raise ValueError(f"map identity mismatch: expected {SOURCE_MAP}")
    recipes = list(document.get("recipes") or [])
    if not recipes:
        raise ValueError("Centrifuge dump recipe list is empty")
    return recipes, sha256_file(dump_path)


def load_ordinary_recipe_indices(
    row_classification: dict[str, Any] | None = None,
    template_denominator: dict[str, Any] | None = None,
) -> tuple[set[int], int]:
    """Return ordinary Centrifuge rows and the non-T39 classified remainder."""
    row_classification = (
        row_classification
        if row_classification is not None
        else load_json(ROW_CLASSIFICATION)
    )
    template_denominator = (
        template_denominator
        if template_denominator is not None
        else load_json(TEMPLATE_DENOMINATOR)
    )
    classes = list(row_classification.get("classes") or [])
    if ORDINARY_CLASS not in classes:
        raise ValueError(f"classification artifact is missing {ORDINARY_CLASS}")
    ordinary_index = classes.index(ORDINARY_CLASS)
    maps = list((template_denominator.get("encoding") or {}).get("maps") or [])
    if len(maps) <= CENTRIFUGE_MAP_INDEX or maps[CENTRIFUGE_MAP_INDEX] != SOURCE_MAP:
        raise ValueError(
            f"t21 encoding.maps[{CENTRIFUGE_MAP_INDEX}] must equal {SOURCE_MAP}"
        )
    ordinary: set[int] = set()
    excluded = 0
    seen: set[int] = set()
    for row in row_classification.get("non_mixer_rows") or []:
        if not isinstance(row, list) or len(row) != 3:
            raise ValueError("non_mixer_rows must contain [map_index, recipe_index, class_index]")
        map_index, recipe_index, class_index = row
        if map_index != CENTRIFUGE_MAP_INDEX:
            continue
        if not isinstance(recipe_index, int) or recipe_index < 0:
            raise ValueError("Centrifuge classification recipe_index must be non-negative")
        if recipe_index in seen:
            raise ValueError(f"duplicate Centrifuge classification for recipe index {recipe_index}")
        seen.add(recipe_index)
        if not isinstance(class_index, int) or not 0 <= class_index < len(classes):
            raise ValueError(f"invalid class index for Centrifuge recipe {recipe_index}")
        if class_index == ordinary_index:
            ordinary.add(recipe_index)
        else:
            excluded += 1
    if len(ordinary) != SOURCE_ROWS:
        raise ValueError(
            f"Centrifuge ordinary_optional classification must contain {SOURCE_ROWS}, got {len(ordinary)}"
        )
    return ordinary, excluded


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
    return f"cruciblecraft:t39/{digest}"


def _empty_item(item: Any) -> bool:
    return t37._empty_item(item)


def _looks_like_tool(item_id: str) -> bool:
    return t37._looks_like_tool(item_id)


def compile_relation(
    *,
    work: WorkFamily,
    recipe: dict[str, Any],
    recipe_index: int,
    shadow_order: int,
    catalogs: Catalogs,
) -> tuple[dict[str, Any], list[str]]:
    """Compile one classified source row using the T37 operand field contract."""
    errors: list[str] = []
    unsupported: list[str] = []
    slot_notes: list[dict[str, Any]] = []
    if recipe.get("hidden") is True:
        unsupported.append("hidden")
    if recipe.get("fake") is True:
        unsupported.append("fake")
    if recipe.get("enabled") is False:
        unsupported.append("disabled")
    unsupported.extend(
        f"unclassified_field:{key}"
        for key in sorted(set(recipe) - CONSUMED_RECIPE_KEYS)
    )
    needs_empty_output = bool(recipe.get("needsEmptyOutput"))
    no_nbt_checks = bool(recipe.get("noNbtChecks")) if "noNbtChecks" in recipe else True
    if needs_empty_output:
        unsupported.append("needsEmptyOutput")

    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("inputs") or []):
        if _empty_item(item):
            slot_notes.append({"side": "item_input", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_item_operand(item, catalogs, side="item_input")
        errors.extend(operand_errors)
        action = classify_item_action(item)
        item_id = str(item.get("item") or "")
        if action["kind"] == "PRESERVE":
            operand["slot_class"] = "tool" if _looks_like_tool(item_id) else "catalyst"
            slot_notes.append(
                {"side": "item_input", "slot": slot, "class": operand["slot_class"]}
            )
        elif action["kind"] == "WEAR":
            operand["slot_class"] = "tool_wear"
            slot_notes.append({"side": "item_input", "slot": slot, "class": "tool_wear"})
        count = int(item.get("count") or 0)
        if action["kind"] != "CONSUME":
            count = 0
        item_inputs.append(operand)
        item_input_counts.append(count)
        item_input_actions.append(action)

    item_outputs: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("outputs") or []):
        if _empty_item(item):
            slot_notes.append({"side": "item_output", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_item_operand(item, catalogs, side="item_output")
        errors.extend(operand_errors)
        item_outputs.append(operand)

    fluid_inputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidInputs") or []):
        if not fluid:
            slot_notes.append({"side": "fluid_input", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_fluid_operand(fluid, catalogs, side="fluid_input")
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_inputs.append(operand)

    fluid_outputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidOutputs") or []):
        if not fluid:
            slot_notes.append({"side": "fluid_output", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_fluid_operand(fluid, catalogs, side="fluid_output")
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
    max_chances = [int(value) for value in (recipe.get("maxChances") or [])]
    # GT6 stores independent 80% output chances with 100% maximums.  The
    # compact contract captures the effective per-output chance and accepts a
    # maximum that bounds it; a ceiling below the effective chance is unsafe.
    if max_chances and any(
        maximum < chance
        for chance, maximum in zip(chances, max_chances[: len(chances)])
    ):
        unsupported.append("maxChances_below_chance")
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
        "shadow_order": shadow_order,
        "slot_notes": slot_notes,
        "unsupported_semantics": unsupported,
        "provenance": {
            "kinds": sorted(set(provenance_fields.values())),
            "fields": provenance_fields,
        },
    }
    relation["stable_id"] = stable_id_for(relation)
    if unsupported:
        errors.extend(f"{work.template_key}: unsupported {value}" for value in unsupported)
    operands = item_inputs + item_outputs + fluid_inputs + fluid_outputs
    if any(operand.get("mapping") == "blocked_unmapped" for operand in operands):
        errors.append(f"{work.template_key}: blocked_unmapped operand")
    return relation, errors


def assign_work_rows(
    recipes: list[dict[str, Any]],
    work: list[WorkFamily],
    ordinary_indices: set[int],
) -> tuple[dict[str, list[tuple[int, dict[str, Any]]]], dict[str, Any]]:
    """Assign every ordinary row to its issued family, preserving multi-row groups."""
    membership = recipe_template_ids(recipes)
    wanted = {item.template_key for item in work}
    grouped: dict[str, list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    index_families: dict[int, list[str]] = defaultdict(list)
    extra: list[str] = []
    for recipe_index in sorted(ordinary_indices):
        template_key = membership.get(recipe_index)
        if template_key not in wanted:
            extra.append(
                f"{SOURCE_MAP}@{recipe_index}"
                + (f" ({template_key})" if template_key else " (not_enabled_template)")
            )
            continue
        grouped[template_key].append((recipe_index, recipes[recipe_index]))
        index_families[recipe_index].append(template_key)
    expected = {item.template_key: int(item.record["expanded_count"]) for item in work}
    missing = [
        f"{item.template_key}: expected {expected[item.template_key]}, got {len(grouped[item.template_key])}"
        for item in work
        if len(grouped[item.template_key]) != expected[item.template_key]
    ]
    duplicate = [
        f"{SOURCE_MAP}@{recipe_index}: {', '.join(sorted(template_keys))}"
        for recipe_index, template_keys in sorted(index_families.items())
        if len(set(template_keys)) != 1
    ]
    assigned = sum(len(rows) for rows in grouped.values())
    return dict(grouped), {
        "assigned": assigned,
        "missing": missing,
        "extra": extra,
        "duplicate": duplicate,
    }


def compile_relations(
    recipes: list[dict[str, Any]],
    work: list[WorkFamily],
    catalogs: Catalogs,
    ordinary_indices: set[int],
) -> tuple[list[dict[str, Any]], dict[str, Any], list[str]]:
    assigned, report = assign_work_rows(recipes, work, ordinary_indices)
    errors: list[str] = []
    if report["assigned"] != SOURCE_ROWS:
        errors.append(f"assignment assigned={report['assigned']}, expected {SOURCE_ROWS}")
    if report["missing"] or report["extra"] or report["duplicate"]:
        errors.append(
            "assignment failed "
            f"missing={len(report['missing'])} extra={len(report['extra'])} "
            f"duplicate={len(report['duplicate'])}"
        )
    relations: list[dict[str, Any]] = []
    for item in work:
        rows = sorted(assigned.get(item.template_key, []), key=lambda row: row[0])
        for shadow_order, (recipe_index, recipe) in enumerate(rows):
            relation, row_errors = compile_relation(
                work=item,
                recipe=recipe,
                recipe_index=recipe_index,
                shadow_order=shadow_order,
                catalogs=catalogs,
            )
            errors.extend(row_errors)
            relations.append(relation)
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if len(stable_ids) != len(set(stable_ids)):
        errors.append("stable_id collision across T39 relations")
    return relations, report, errors


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def build_review(
    document: dict[str, Any],
    *,
    non_t39_classified_rows_excluded: int = 0,
) -> dict[str, Any]:
    relations = list(document.get("relations") or [])
    work_set = document.get("work_set") or {}
    family_ids = list(work_set.get("family_ids") or [])
    per_family = Counter(str(relation.get("family_id") or "") for relation in relations)
    family_relation_counts = {
        family_id: per_family.get(family_id, 0) for family_id in family_ids
    }
    relation_distribution = Counter(family_relation_counts.values())
    slot_shapes: Counter[str] = Counter()
    mapping_counts: Counter[str] = Counter()
    action_counts: Counter[str] = Counter()
    chance_values: Counter[int] = Counter()
    special_values: Counter[int] = Counter()
    unregistered_forms: list[dict[str, Any]] = []
    unregistered_outputs: list[dict[str, Any]] = []
    unreachable_inputs: list[dict[str, Any]] = []
    unsupported: list[str] = []
    consume_index: dict[str, list[str]] = defaultdict(list)

    for relation in relations:
        family_id = str(relation.get("family_id") or "")
        slot_shapes[
            f"I{len(relation.get('item_inputs') or [])}/"
            f"O{len(relation.get('item_outputs') or [])}/"
            f"FI{len(relation.get('fluid_inputs') or [])}/"
            f"FO{len(relation.get('fluid_outputs') or [])}"
        ] += 1
        operands = _operands(relation)
        for operand in operands:
            mapping_counts[str(operand.get("mapping") or "blocked_unmapped")] += 1
        for side, operands_on_side in (
            ("item_input", relation.get("item_inputs") or []),
            ("item_output", relation.get("item_outputs") or []),
            ("fluid_input", relation.get("fluid_inputs") or []),
            ("fluid_output", relation.get("fluid_outputs") or []),
        ):
            for operand in operands_on_side:
                source = operand.get("source") or {}
                source_item = str(source.get("item") or "")
                if (
                    source_item == "gregtech:gt.meta.dustDiv72"
                    or operand.get("mapping") == "canonical_tag"
                ):
                    unregistered_forms.append(
                        {
                            "family_id": family_id,
                            "side": side,
                            "source": source,
                            "alias": operand.get("alias"),
                        }
                    )
                if side.endswith("output") and operand.get("runtime_id") is None:
                    unregistered_outputs.append(
                        {
                            "family_id": family_id,
                            "side": side,
                            "source": source,
                            "mapping": operand.get("mapping"),
                        }
                    )
        for operand, count, action in zip(
            relation.get("item_inputs") or [],
            relation.get("item_input_counts") or [],
            relation.get("item_input_actions") or [],
        ):
            action_counts[str(action.get("kind") or "CONSUME")] += 1
            if action.get("kind") == "CONSUME" and operand.get("reachable") is False:
                unreachable_inputs.append(
                    {
                        "family_id": family_id,
                        "source": operand.get("source"),
                        "count": count,
                    }
                )
        for operand in relation.get("fluid_inputs") or []:
            if operand.get("reachable") is False:
                unreachable_inputs.append(
                    {"family_id": family_id, "source": operand.get("source"), "count": None}
                )
        for note in relation.get("slot_notes") or []:
            note_class = str(note.get("class") or "")
            if note_class in {"catalyst", "tool", "tool_wear", "container_return"}:
                action_counts[note_class] += 1
        for chance in relation.get("output_chances") or []:
            chance_values[int(chance)] += 1
        special_values[int(relation.get("special_value") or 0)] += 1
        unsupported.extend(
            f"{family_id}:{value}"
            for value in relation.get("unsupported_semantics") or []
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
        consume_index[consume_key].append(family_id)

    shadow_candidates = sorted(
        {
            family_id
            for family_ids in consume_index.values()
            if len(family_ids) > 1
            for family_id in family_ids
        }
    )
    assignment = document.get("assignment") or {}
    r0_blockers: list[str] = []
    if int(assignment.get("assigned") or 0) != SOURCE_ROWS:
        r0_blockers.append(f"assignment.assigned != {SOURCE_ROWS}")
    if assignment.get("missing") or assignment.get("extra") or assignment.get("duplicate"):
        r0_blockers.append("assignment has missing/extra/duplicate")
    if len(family_relation_counts) != FAMILY_COUNT:
        r0_blockers.append(f"family count != {FAMILY_COUNT}")
    if len(relations) != SOURCE_ROWS:
        r0_blockers.append(f"relation count != {SOURCE_ROWS}")
    if mapping_counts.get("blocked_unmapped"):
        r0_blockers.append(
            f"blocked_unmapped operands: {mapping_counts['blocked_unmapped']}"
        )
    if unsupported:
        r0_blockers.append(f"unsupported semantics: {len(unsupported)}")
    if len({row.get("stable_id") for row in relations}) != len(relations):
        r0_blockers.append("stable ids are not unique")
    if document.get("status") != "T39_CENTRIFUGE_SOURCE_FROZEN":
        r0_blockers.append(f"status={document.get('status')}")
    r1_blockers: list[str] = []
    if unregistered_forms:
        r1_blockers.append(
            f"unregistered form operands require a runtime form decision: {len(unregistered_forms)}"
        )
    if unreachable_inputs:
        r1_blockers.append(f"unreachable inputs: {len(unreachable_inputs)}")
    return {
        "schema_version": 1,
        "status": document.get("status"),
        "source_revision": SOURCE_REVISION,
        "relation_count_by_family": family_relation_counts,
        "relation_count_distribution": {
            str(count): total for count, total in sorted(relation_distribution.items())
        },
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
        "preserve_wear_container_counts": {
            "consume": action_counts.get("CONSUME", 0),
            "preserve": action_counts.get("PRESERVE", 0),
            "wear": action_counts.get("WEAR", 0),
            "container_return": action_counts.get("container_return", 0),
        },
        "action_counts": dict(sorted(action_counts.items())),
        "output_chance_distribution": {
            str(value): count for value, count in sorted(chance_values.items())
        },
        "special_value_distribution": {
            str(value): count for value, count in sorted(special_values.items())
        },
        "non_t39_classified_rows_excluded": non_t39_classified_rows_excluded,
        "unreachable_inputs": unreachable_inputs,
        "unregistered_form_operands": unregistered_forms,
        "unregistered_outputs": unregistered_outputs,
        "unsupported_semantics": unsupported,
        "input_shadow_candidates": shadow_candidates,
        "r0_blockers": r0_blockers,
        "r1_blockers": r1_blockers,
    }


def build_document(
    relations: list[dict[str, Any]],
    work: list[WorkFamily],
    report: dict[str, Any],
    errors: list[str],
) -> dict[str, Any]:
    blocked_unmapped = any(
        operand.get("mapping") == "blocked_unmapped"
        for relation in relations
        for operand in _operands(relation)
    )
    unsupported = any(relation.get("unsupported_semantics") for relation in relations)
    frozen = (
        not errors
        and len(work) == FAMILY_COUNT
        and len(relations) == SOURCE_ROWS
        and report["assigned"] == SOURCE_ROWS
        and not report["missing"]
        and not report["extra"]
        and not report["duplicate"]
        and not blocked_unmapped
        and not unsupported
        and len({relation["stable_id"] for relation in relations}) == SOURCE_ROWS
    )
    return {
        "schema_version": 1,
        "status": (
            "T39_CENTRIFUGE_SOURCE_FROZEN"
            if frozen
            else "T39_CENTRIFUGE_SOURCE_BLOCKED"
        ),
        "source_revision": SOURCE_REVISION,
        "generated_by": "python tools/build_t39_centrifuge_source.py --full-replay",
        "host": HOST,
        "owner": OWNER,
        "source_map": SOURCE_MAP,
        "target_map": TARGET_MAP,
        "work_set": {
            "family_ids": [item.family_id for item in work],
            "template_keys": [item.template_key for item in work],
            "family_count": FAMILY_COUNT,
            "source_rows": SOURCE_ROWS,
        },
        "assignment": report,
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
        "compact_sha256": sha256_text(t35.stable_json(document)),
        "schema_sha256": sha256_file(SCHEMA) if SCHEMA.is_file() else None,
        "builder_sha256": sha256_file(BUILDER),
        "census_sha256": sha256_file(CENSUS),
        "work_set_sha256": sha256_file(WORK_SET),
        "t38_census_delta_sha256": sha256_file(T38_CENSUS_DELTA),
        "recipe_families_sha256": sha256_file(RECIPE_FAMILIES),
        "full_replay": {
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
    }
    if dump_sha256 and dump_path is not None:
        receipt["dump"] = {
            "path": relative(dump_path)
            if dump_path.is_relative_to(ROOT)
            else dump_path.as_posix(),
            "sha256": dump_sha256,
        }
    return receipt


def build_from_dump(
    *,
    dump_path: Path | None = None,
    work_set: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
    row_classification: dict[str, Any] | None = None,
    template_denominator: dict[str, Any] | None = None,
    catalogs: Catalogs | None = None,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], str]:
    work = load_work_set(work_set, families_doc)
    recipes, dump_sha256 = load_centrifuge_recipes(dump_path)
    ordinary_indices, excluded_count = load_ordinary_recipe_indices(
        row_classification, template_denominator
    )
    if any(index >= len(recipes) for index in ordinary_indices):
        raise ValueError("Centrifuge classification references a recipe outside the dump")
    catalogs = catalogs or load_catalogs()
    relations, report, errors = compile_relations(
        recipes, work, catalogs, ordinary_indices
    )
    document = build_document(relations, work, report, errors)
    review = build_review(
        document, non_t39_classified_rows_excluded=excluded_count
    )
    receipt = build_receipt(
        document,
        dump_sha256=dump_sha256,
        dump_path=dump_path or CENTRIFUGE_DUMP,
    )
    return document, receipt, review, dump_sha256


def validate_committed(
    document: dict[str, Any],
    *,
    work_set: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
) -> list[str]:
    errors: list[str] = []
    if document.get("schema_version") != 1:
        errors.append("schema_version != 1")
    for field, expected in (
        ("source_revision", SOURCE_REVISION),
        ("host", HOST),
        ("owner", OWNER),
        ("source_map", SOURCE_MAP),
        ("target_map", TARGET_MAP),
    ):
        if document.get(field) != expected:
            errors.append(f"{field} drifted")
    try:
        work = load_work_set(work_set, families_doc)
    except ValueError as error:
        return [*errors, str(error)]
    work_set = document.get("work_set") or {}
    if list(work_set.get("family_ids") or []) != [item.family_id for item in work]:
        errors.append("work_set.family_ids drifted from t39_work_set")
    if list(work_set.get("template_keys") or []) != [item.template_key for item in work]:
        errors.append("work_set.template_keys drifted")
    if int(work_set.get("family_count") or 0) != FAMILY_COUNT:
        errors.append(f"work_set.family_count != {FAMILY_COUNT}")
    if int(work_set.get("source_rows") or 0) != SOURCE_ROWS:
        errors.append(f"work_set.source_rows != {SOURCE_ROWS}")
    relations = list(document.get("relations") or [])
    if len(relations) != SOURCE_ROWS:
        errors.append(f"relations length {len(relations)} != {SOURCE_ROWS}")
    assignment = document.get("assignment") or {}
    if int(assignment.get("assigned") or 0) != SOURCE_ROWS:
        errors.append(f"assignment.assigned != {SOURCE_ROWS}")
    for key in ("missing", "extra", "duplicate"):
        if assignment.get(key):
            errors.append(f"assignment.{key} is not empty")
    counts = Counter(str(row.get("family_id") or "") for row in relations)
    expected_counts = {item.family_id: int(item.record["expanded_count"]) for item in work}
    if dict(counts) != expected_counts:
        errors.append("relation counts drifted from T35 expanded_count ledger")
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if len(stable_ids) != len(set(stable_ids)):
        errors.append("stable ids are not unique")
    for row in relations:
        family_id = str(row.get("family_id") or "")
        missing_fields = [field for field in REQUIRED_RELATION_FIELDS if field not in row]
        if missing_fields:
            errors.append(f"{family_id} missing {missing_fields}")
            continue
        if not str(row.get("stable_id") or "").startswith("cruciblecraft:t39/"):
            errors.append(f"{family_id} stable_id namespace drifted")
        elif row.get("stable_id") != stable_id_for(row):
            errors.append(f"{family_id} stable_id fingerprint drifted")
        if any(operand.get("mapping") == "blocked_unmapped" for operand in _operands(row)):
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
    if actual != t35.stable_json(document):
        errors.append(f"{relative(OUTPUT)} is not canonical JSON")
    if receipt.get("compact_sha256") != sha256_text(actual):
        errors.append("receipt.compact_sha256 does not match compact source")
    if receipt.get("schema_sha256") != sha256_file(SCHEMA):
        errors.append("receipt schema_sha256 drifted")
    if receipt.get("builder_sha256") != sha256_file(BUILDER):
        errors.append("receipt builder_sha256 drifted")
    if receipt.get("source_revision") != SOURCE_REVISION:
        errors.append("receipt source_revision drifted")
    errors.extend(validate_committed(document))
    if document.get("status") != "T39_CENTRIFUGE_SOURCE_FROZEN":
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
    if OUTPUT.is_file() and OUTPUT.read_text(encoding="utf-8") != t35.stable_json(expected):
        errors.append(f"{relative(OUTPUT)} is stale under full replay")
    receipt = load_json(RECEIPT) if RECEIPT.is_file() else {}
    if (receipt.get("dump") or {}).get("sha256") != dump_sha256:
        errors.append("receipt dump sha256 is stale")
    return errors


def write(
    *,
    dump_path: Path | None = None,
    work_set: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
    row_classification: dict[str, Any] | None = None,
    template_denominator: dict[str, Any] | None = None,
    catalogs: Catalogs | None = None,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    document, receipt, review, _digest = build_from_dump(
        dump_path=dump_path,
        work_set=work_set,
        families_doc=families_doc,
        row_classification=row_classification,
        template_denominator=template_denominator,
        catalogs=catalogs,
    )
    t35.write_stable(OUTPUT, document)
    t35.write_stable(RECEIPT, receipt)
    t35.write_stable(REVIEW, review)
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
            tier = "full replay write"
        else:
            if not OUTPUT.is_file():
                raise ValueError(
                    "compact source is absent; first generation requires --full-replay "
                    "against gt6_dump/.../gt.recipe.centrifuge.json"
                )
            document = load_json(OUTPUT)
            t35.write_stable(REVIEW, build_review(document))
            tier = "review refresh"
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T39 centrifuge source failed: {error}", file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                "assigned": (document.get("assignment") or {}).get("assigned"),
                "blockers": document.get("blockers") or [],
                "proof_tier": tier,
                "status": document.get("status"),
            },
            sort_keys=True,
        )
    )
    return 0 if document.get("status") == "T39_CENTRIFUGE_SOURCE_FROZEN" else 1


if __name__ == "__main__":
    raise SystemExit(main())
