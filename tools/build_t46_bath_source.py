#!/usr/bin/env python3
"""Freeze T46 Bath MTE compact source from classified GT6 rows.

Modes:
  --full-replay           rebuild source, receipt, and review from the GT6 dump
  --check                 validate committed source and receipt without the dump
  --check --full-replay   rebuild from the dump and compare to the committed source
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import Counter, defaultdict
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
from tools import t46_common as common  # noqa: E402
from tools import t46_identities as identities  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = common.SOURCE
RECEIPT = common.RECEIPT
REVIEW = common.REVIEW
SOURCE_PACK = common.SOURCE_PACK
WORK_SET = common.WORK_SET
ORDINARY_CLASS = common.ORDINARY_CLASS
FINGERPRINT_EXCLUDE = frozenset(
    {
        "source_recipe_index",
        "source_row_sha256",
        "stable_id",
        "slot_notes",
        "unsupported_semantics",
        "shadow_order",
        "publication_group",
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


def sha256_text(payload: str) -> str:
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def load_mte_runtime_map(catalog: dict[str, Any] | None = None) -> dict[tuple[str, int], str]:
    catalog = catalog if catalog is not None else common.load_json(common.MTE_CATALOG)
    mapped: dict[tuple[str, int], str] = {}
    for identity in catalog.get("identities") or []:
        mapped[(str(identity["source_item"]), int(identity["meta"]))] = str(
            identity["runtime_id"]
        )
    if len(mapped) != common.EXPECTED_MTE_METAS:
        raise ValueError("T46 MTE catalog variant map drifted")
    return mapped


def load_fluid_overlay(mapping: dict[str, Any] | None = None) -> dict[str, str]:
    mapping = mapping if mapping is not None else common.load_json(common.FLUID_MAPPING)
    overlay = {
        str(row["source_fluid"]): str(row["cc_fluid_id"])
        for row in mapping.get("mapping") or []
        if isinstance(row, dict)
    }
    if len(overlay) != common.EXPECTED_FLUID_OVERLAY:
        raise ValueError("T46 fluid overlay count drifted")
    return overlay


def map_item_operand(
    item: dict[str, Any],
    catalogs: t37.Catalogs,
    mte_runtime: dict[tuple[str, int], str],
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    item_id = str(item.get("item") or "")
    meta = item.get("meta")
    if item_id == identities.SOURCE_ITEM and isinstance(meta, int):
        runtime = mte_runtime.get((item_id, meta))
        errors: list[str] = []
        operand: dict[str, Any] = {
            "mapping": "blocked_unmapped",
            "source": {
                "item": item_id,
                "count": int(item.get("count") or 0),
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
        if not runtime:
            errors.append(f"unmapped T46 MTE {item_id}@{meta}")
            operand["value"] = f"{item_id}@{meta}"
            return operand, errors
        operand.update(
            {
                "mapping": "proven_equivalent",
                "value": runtime,
                "runtime_id": runtime,
                "reachable": True,
            }
        )
        return operand, errors
    return t37.map_item_operand(item, catalogs, side=side)


def map_fluid_operand(
    fluid: dict[str, Any],
    catalogs: t37.Catalogs,
    fluid_overlay: dict[str, str],
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    operand, errors = t37.map_fluid_operand(fluid, catalogs, side=side)
    if operand.get("mapping") != "blocked_unmapped":
        return operand, errors
    fluid_id = str((operand.get("source") or {}).get("fluid") or fluid.get("fluid") or "")
    mapped = fluid_overlay.get(fluid_id)
    if not mapped:
        return operand, errors
    operand.update(
        {
            "mapping": "proven_equivalent",
            "value": mapped,
            "runtime_id": mapped,
            "alias": None,
            "reachable": True,
        }
    )
    errors = [
        error
        for error in errors
        if error != f"blocked_unmapped {side} fluid {fluid_id}"
    ]
    return operand, errors


def load_ordinary_recipe_indices() -> set[int]:
    row_classification = common.load_json(common.ROW_CLASSIFICATION)
    template_denominator = common.load_json(common.TEMPLATE_DENOMINATOR)
    classes = list(row_classification.get("classes") or [])
    ordinary_index = classes.index(ORDINARY_CLASS)
    maps = list((template_denominator.get("encoding") or {}).get("maps") or [])
    if len(maps) <= common.BATH_MAP_INDEX or maps[common.BATH_MAP_INDEX] != common.SOURCE_MAP:
        raise ValueError(f"t21 encoding.maps[{common.BATH_MAP_INDEX}] must equal {common.SOURCE_MAP}")
    ordinary: set[int] = set()
    for row in row_classification.get("non_mixer_rows") or []:
        dump_index, recipe_index, class_index = row
        if dump_index != common.BATH_MAP_INDEX:
            continue
        if class_index == ordinary_index:
            ordinary.add(recipe_index)
    return ordinary


def fingerprint_payload(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        key: value
        for key, value in relation.items()
        if key not in FINGERPRINT_EXCLUDE
    }


def stable_id_for(relation: dict[str, Any]) -> str:
    digest = sha256_text(_stable_json(fingerprint_payload(relation)))[:16]
    return f"cruciblecraft:t46/{digest}"


def compile_relation(
    *,
    family_id: str,
    template_key: str,
    publication_group: str,
    recipe: dict[str, Any],
    recipe_index: int,
    shadow_order: int,
    catalogs: t37.Catalogs,
    mte_runtime: dict[tuple[str, int], str],
    fluid_overlay: dict[str, str],
) -> tuple[dict[str, Any], list[str]]:
    errors: list[str] = []
    unsupported: list[str] = []
    slot_notes: list[dict[str, Any]] = []
    if recipe.get("hidden") is True:
        unsupported.append("hidden")
    if recipe.get("fake") is True:
        unsupported.append("fake")
    extra_keys = sorted(set(recipe) - t37.CONSUMED_RECIPE_KEYS)
    if extra_keys:
        slot_notes.append({"side": "recipe", "slot": 0, "class": "extra_keys", "keys": extra_keys})
    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("inputs") or []):
        if t37._empty_item(item):
            slot_notes.append({"side": "item_input", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_item_operand(
            item, catalogs, mte_runtime, side="item_input"
        )
        errors.extend(operand_errors)
        action = t37.classify_item_action(item)
        count = int(item.get("count") or 0)
        if action["kind"] != "CONSUME":
            count = 0
        item_inputs.append(operand)
        item_input_counts.append(count)
        item_input_actions.append(action)
    item_outputs: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("outputs") or []):
        if t37._empty_item(item):
            slot_notes.append({"side": "item_output", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_item_operand(
            item, catalogs, mte_runtime, side="item_output"
        )
        errors.extend(operand_errors)
        item_outputs.append(operand)
    fluid_inputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidInputs") or []):
        if not fluid:
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, fluid_overlay, side="fluid_input"
        )
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_inputs.append(operand)
    fluid_outputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidOutputs") or []):
        if not fluid:
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, fluid_overlay, side="fluid_output"
        )
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_outputs.append(operand)
    chances = [int(value) for value in (recipe.get("chances") or [])]
    if not chances and item_outputs:
        chances = [10000] * len(item_outputs)
    elif chances and len(chances) < len(item_outputs):
        chances.extend([10000] * (len(item_outputs) - len(chances)))
    duration = int(recipe.get("duration") or 0)
    if duration <= 0:
        errors.append(f"{template_key}: duration must be positive")
    relation = {
        "family_id": family_id,
        "template_key": template_key,
        "publication_group": publication_group,
        "host": common.HOST,
        "source_map": common.SOURCE_MAP,
        "source_recipe_index": recipe_index,
        "source_revision": common.SOURCE_REVISION,
        "source_row_sha256": sha256_text(_stable_json(recipe)),
        "target_map": common.TARGET_MAP,
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
        "can_be_buffered": bool(recipe.get("canBeBuffered", True)),
        "needs_empty_output": bool(recipe.get("needsEmptyOutput")),
        "no_nbt_checks": True,
        "shadow_order": shadow_order,
        "slot_notes": slot_notes,
        "unsupported_semantics": unsupported,
        "provenance": {
            "kinds": ["SOURCE_BACKED", "SOURCE_DERIVED"],
            "fields": {
                "item_inputs": "SOURCE_DERIVED",
                "item_input_counts": "SOURCE_BACKED",
                "item_outputs": "SOURCE_DERIVED",
                "fluid_inputs": "SOURCE_DERIVED",
                "duration": "SOURCE_BACKED",
                "eut": "SOURCE_BACKED",
            },
        },
    }
    relation["stable_id"] = stable_id_for(relation)
    if unsupported:
        errors.extend(f"{template_key}: unsupported {value}" for value in unsupported)
    operands = item_inputs + item_outputs + fluid_inputs + fluid_outputs
    if any(operand.get("mapping") == "blocked_unmapped" for operand in operands):
        errors.append(f"{template_key}: blocked_unmapped operand")
    return relation, errors


def recipe_template_ids(recipes: list[dict[str, Any]]) -> dict[int, str]:
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
        template_key = f"{common.SOURCE_MAP}#{group_index:04d}"
        for recipe_index in members:
            membership[recipe_index] = template_key
    return membership


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def build_from_dump() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    if common.oracle_dirty():
        raise RuntimeError("source oracle is dirty; full replay fail closed")
    if not common.dump_present():
        raise OSError(f"missing GT6 dump map required for full replay: {common.BATH_DUMP}")
    work_set = common.load_json(WORK_SET)
    catalogs = t37.load_catalogs()
    mte_runtime = load_mte_runtime_map()
    fluid_overlay = load_fluid_overlay()
    document = common.load_json(common.BATH_DUMP)
    if document.get("nameInternal") != common.SOURCE_MAP:
        raise ValueError(f"map identity mismatch: expected {common.SOURCE_MAP}")
    recipes = list(document.get("recipes") or [])
    dump_hash = t35.sha256_file(common.BATH_DUMP)
    ordinary = load_ordinary_recipe_indices()
    membership = recipe_template_ids(recipes)
    wanted = {str(row["template_key"]): row for row in work_set.get("families") or []}
    grouped: dict[str, list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    for recipe_index in sorted(ordinary):
        template_key = membership.get(recipe_index)
        if template_key not in wanted:
            continue
        grouped[template_key].append((recipe_index, recipes[recipe_index]))
    relations: list[dict[str, Any]] = []
    errors: list[str] = []
    assigned = 0
    for row in work_set.get("families") or []:
        template_key = str(row["template_key"])
        expected = int(row.get("expanded_count") or 0)
        rows = sorted(grouped.get(template_key, []), key=lambda item: item[0])
        if len(rows) != expected:
            errors.append(f"{template_key}: expected {expected} ordinary rows, got {len(rows)}")
            continue
        for shadow_order, (recipe_index, recipe) in enumerate(rows):
            assigned += 1
            relation, row_errors = compile_relation(
                family_id=str(row["family_id"]),
                template_key=template_key,
                publication_group=str(row["publication_group"]),
                recipe=recipe,
                recipe_index=recipe_index,
                shadow_order=shadow_order,
                catalogs=catalogs,
                mte_runtime=mte_runtime,
                fluid_overlay=fluid_overlay,
            )
            errors.extend(row_errors)
            relations.append(relation)
    relations.sort(key=lambda row: (str(row["family_id"]), int(row["shadow_order"])))
    frozen = (
        not errors
        and assigned == common.EXPECTED_RELATION_COUNT
        and len(relations) == assigned
        and len({row["stable_id"] for row in relations}) == len(relations)
    )
    source = {
        "assignment": {"assigned": assigned, "missing": errors[:8]},
        "blockers": errors,
        "generated_by": "python tools/build_t46_bath_source.py --full-replay",
        "host": common.HOST,
        "owner": common.OWNER,
        "relations": relations,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_BATH_SOURCE_FROZEN" if frozen else "T46_BATH_SOURCE_BLOCKED",
        "work_set": {
            "family_count": len(work_set.get("families") or []),
            "family_ids": list(work_set.get("family_ids") or []),
            "source_rows": len(relations),
            "selection_sha256": work_set.get("selection_sha256"),
        },
    }
    mapping_counts: Counter[str] = Counter()
    for relation in relations:
        for operand in _operands(relation):
            mapping_counts[str(operand.get("mapping") or "unknown")] += 1
    review = {
        "generated_by": "python tools/build_t46_bath_source.py",
        "mapping_counts": dict(sorted(mapping_counts.items())),
        "r0_blockers": errors,
        "relation_count": len(relations),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_BATH_SOURCE_REVIEW",
    }
    compact = t35.stable_json(source)
    receipt = {
        "builder_sha256": t35.sha256_file(BUILDER),
        "compact_sha256": sha256_text(compact),
        "denominator_source_revision": common.SOURCE_REVISION,
        "dump_file_hashes": {common.relative(common.BATH_DUMP): dump_hash},
        "full_replay": True,
        "generated_by": "python tools/build_t46_bath_source.py --full-replay",
        "recipe_families_sha256": t35.sha256_file(common.RECIPE_FAMILIES),
        "schema_version": 1,
        "selection_sha256": work_set.get("selection_sha256"),
        "source_oracle_dirty": False,
        "source_oracle_path": common.ORACLE_RELATIVE,
        "source_oracle_revision": common.oracle_revision(),
        "status": "T46_BATH_SOURCE_RECEIPT",
        "work_set_sha256": t35.sha256_file(WORK_SET),
        "proof_tier": "full_replay",
        "skip_is_not_pass": True,
    }
    return source, receipt, review


def build_source_pack(
    document: dict[str, Any],
    receipt: dict[str, Any],
    review: dict[str, Any],
) -> dict[str, Any]:
    frozen = document.get("status") == "T46_BATH_SOURCE_FROZEN"
    work = document.get("work_set") or {}
    return {
        "catalog": {
            "families": work.get("family_count"),
            "relations": work.get("source_rows"),
            "selection_sha256": work.get("selection_sha256"),
        },
        "files": {
            "receipt": {"path": common.relative(RECEIPT)},
            "review": {"path": common.relative(REVIEW)},
            "source": {
                "path": common.relative(OUTPUT),
                "sha256": receipt.get("compact_sha256"),
            },
            "work_set": {"path": common.relative(WORK_SET)},
        },
        "full_replay": {
            "dump_present": common.dump_present(),
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
        "host": common.HOST,
        "owner": common.OWNER,
        "review_status": review.get("status"),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_SOURCE_PACK_FROZEN" if frozen else "T46_SOURCE_PACK_BLOCKED",
    }


def reference_only_check() -> list[str]:
    if not OUTPUT.is_file() or not RECEIPT.is_file():
        return ["missing compact source or receipt"]
    document = common.load_json(OUTPUT)
    receipt = common.load_json(RECEIPT)
    errors: list[str] = []
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != t35.stable_json(document):
        errors.append("compact source is not canonical JSON")
    if receipt.get("compact_sha256") != sha256_text(actual):
        errors.append("receipt.compact_sha256 does not match compact source")
    if document.get("status") != "T46_BATH_SOURCE_FROZEN":
        errors.append(f"compact status is {document.get('status')}")
    if document.get("blockers"):
        errors.append("compact source still has blockers")
    if receipt.get("source_oracle_dirty") is not False:
        errors.append("source_oracle_dirty must be false")
    if receipt.get("denominator_source_revision") != common.SOURCE_REVISION:
        errors.append("denominator pin drifted")
    relations = list(document.get("relations") or [])
    if len(relations) != common.EXPECTED_RELATION_COUNT:
        errors.append(
            f"T46 source relations {len(relations)} != {common.EXPECTED_RELATION_COUNT}"
        )
    for row in relations:
        missing = [field for field in REQUIRED_RELATION_FIELDS if field not in row]
        if missing:
            errors.append(f"{row.get('family_id')} missing {missing}")
        elif row.get("stable_id") != stable_id_for(row):
            errors.append(f"{row.get('family_id')} stable_id fingerprint drifted")
    return errors


def write() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    document, receipt, review = build_from_dump()
    t35.write_stable(OUTPUT, document)
    t35.write_stable(RECEIPT, receipt)
    t35.write_stable(REVIEW, review)
    t35.write_stable(SOURCE_PACK, build_source_pack(document, receipt, review))
    return document, receipt, review


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check and args.full_replay:
            errors = reference_only_check()
            expected, _receipt, _review = build_from_dump()
            if OUTPUT.read_text(encoding="utf-8") != t35.stable_json(expected):
                errors.append("compact source is stale under full replay")
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
            tier = "full replay"
        elif args.check:
            errors = reference_only_check()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
            tier = "compact"
        elif args.write or args.full_replay:
            document, _receipt, _review = write()
            tier = "full replay write"
        else:
            parser.error("choose --check, --write, and/or --full-replay")
            return 2
    except (OSError, ValueError, KeyError, json.JSONDecodeError, RuntimeError) as error:
        print(f"T46 Bath MTE source failed: {error}", file=sys.stderr)
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
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
