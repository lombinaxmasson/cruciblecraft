#!/usr/bin/env python3
"""Freeze T48 Bath remainder compact source from classified GT6 rows.

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
from tools import t48_common as common  # noqa: E402
from tools import t48_identities as identities  # noqa: E402

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


def _empty_operand(item: dict[str, Any]) -> dict[str, Any]:
    return {
        "mapping": "blocked_unmapped",
        "source": {
            "item": str(item.get("item") or ""),
            "count": int(item.get("count") or 0),
            "meta": item.get("meta"),
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


def load_t48_catalogs() -> t37.Catalogs:
    catalogs = t37.load_catalogs()
    prefix = dict(catalogs.prefix_item_to_form)
    prefix.update(identities.PREFIX_ITEM_TO_FORM_OVERLAY)
    registered = {key: set(value) for key, value in catalogs.registered_forms.items()}
    if common.REQUIRED_FORMS.is_file():
        forms_doc = common.load_json(common.REQUIRED_FORMS)
        for material, forms in (forms_doc.get("required_forms") or {}).items():
            registered.setdefault(str(material), set()).update(
                str(form) for form in forms
            )
    prefix_tags = dict(catalogs.prefix_tags)
    for form in identities.PREFIX_FORM_UNITS:
        if form not in prefix_tags:
            prefix_tags[form] = ("c", f"{form}s")
    return t37.Catalogs(
        prefix_item_to_form=prefix,
        material_id_to_cc=catalogs.material_id_to_cc,
        registered_forms=registered,
        form_items=catalogs.form_items,
        prefix_tags=prefix_tags,
        fluid_to_cc=catalogs.fluid_to_cc,
        reachable=catalogs.reachable,
    )


def _close_runtime(operand: dict[str, Any], errors: list[str], *, side: str) -> list[str]:
    runtime = operand.get("runtime_id")
    if runtime:
        common.assert_runtime_id(runtime, consume=False)
        return errors
    item = str((operand.get("source") or {}).get("item") or "")
    meta = (operand.get("source") or {}).get("meta")
    errors = list(errors)
    errors.append(f"null runtime {side} {item}@{meta}")
    operand["mapping"] = "blocked_unmapped"
    operand["runtime_id"] = None
    return errors


def map_item_operand(
    item: dict[str, Any],
    catalogs: t37.Catalogs,
    t48_items: dict[tuple[str, int | None], dict[str, Any]],
    reused_aliases: dict[tuple[str, int], str],
    mte_runtime: dict[tuple[str, int], str],
    block_runtime: dict[tuple[str, int], str],
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    item_id = str(item.get("item") or "")
    meta = item.get("meta")
    meta_key = int(meta) if isinstance(meta, int) else None
    overlay = t48_items.get((item_id, meta_key))
    if overlay and overlay.get("runtime_id"):
        operand = _empty_operand(item)
        runtime = str(overlay["runtime_id"])
        common.assert_runtime_id(runtime, consume=False)
        operand.update(
            {
                "mapping": str(overlay.get("mapping_class") or "proven_equivalent"),
                "value": runtime,
                "runtime_id": runtime,
                "reachable": True,
                "kind": overlay.get("kind"),
            }
        )
        return operand, []
    if isinstance(meta, int) and (item_id, meta) in reused_aliases:
        operand = _empty_operand(item)
        runtime = reused_aliases[(item_id, meta)]
        common.assert_runtime_id(runtime, consume=False)
        operand.update(
            {
                "mapping": "proven_equivalent",
                "value": runtime,
                "runtime_id": runtime,
                "alias": runtime,
                "reachable": True,
            }
        )
        return operand, []
    if item_id == identities.CIRCUIT_ITEM:
        operand = _empty_operand(item)
        operand.update(
            {
                "mapping": "proven_equivalent",
                "value": identities.PROGRAMMED_CIRCUIT,
                "runtime_id": identities.PROGRAMMED_CIRCUIT,
                "reachable": True,
                "slot_class": "catalyst" if int(item.get("count") or 0) == 0 else "occupied",
            }
        )
        return operand, []
    if identities.vanilla_wildcard_meta(item_id, meta):
        tag = identities.vanilla_wildcard_tag(item_id)
        representative = identities.vanilla_wildcard_representative(item_id)
        if tag and representative:
            operand = _empty_operand(item)
            common.assert_runtime_id(representative, consume=False)
            operand.update(
                {
                    "mapping": "vanilla_wildcard_tag",
                    "value": tag,
                    "runtime_id": representative,
                    "tag": tag,
                    "reachable": True,
                }
            )
            return operand, []
        operand = _empty_operand(item)
        operand["value"] = f"{item_id}@*"
        return operand, [f"lossy wildcard vanilla meta {item_id}"]
    vanilla = identities.vanilla_meta_runtime(item_id, meta)
    if vanilla:
        operand = _empty_operand(item)
        common.assert_runtime_id(vanilla, consume=False)
        operand.update(
            {
                "mapping": "exact_runtime_id",
                "value": vanilla,
                "runtime_id": vanilla,
                "reachable": True,
            }
        )
        return operand, []
    if item_id == "gregtech:gt.multitileentity" and isinstance(meta, int):
        operand = _empty_operand(item)
        runtime = mte_runtime.get((item_id, meta))
        if runtime:
            operand.update(
                {
                    "mapping": "proven_equivalent",
                    "value": runtime,
                    "runtime_id": runtime,
                    "reachable": True,
                    "kind": "mte",
                }
            )
            return operand, []
        operand["value"] = f"{item_id}@{meta}"
        return operand, [f"unmapped remainder MTE {item_id}@{meta}"]
    if item_id.startswith("gregtech:gt.block."):
        operand = _empty_operand(item)
        meta_key_block = int(meta) if isinstance(meta, int) else 0
        runtime = block_runtime.get((item_id, meta_key_block))
        if runtime:
            operand.update(
                {
                    "mapping": "proven_equivalent",
                    "value": runtime,
                    "runtime_id": runtime,
                    "reachable": True,
                    "kind": "block",
                }
            )
            return operand, []
        operand["value"] = f"{item_id}@{meta}"
        return operand, [f"unmapped remainder block {item_id}@{meta}"]
    operand, errors = t37.map_item_operand(item, catalogs, side=side)
    runtime = str(operand.get("runtime_id") or "")
    renamed = identities.VANILLA_RENAMES.get(runtime)
    if renamed:
        operand = dict(operand)
        operand["runtime_id"] = renamed
        if operand.get("value") == runtime:
            operand["value"] = renamed
    return operand, _close_runtime(operand, errors, side=side)


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
    mapped = fluid_overlay.get(fluid_id) or fluid_overlay.get(fluid_id.split(":", 1)[-1])
    if not mapped:
        return operand, errors
    common.assert_runtime_id(mapped, consume=False)
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
    return f"cruciblecraft:t48/{digest}"


def compile_relation(
    *,
    family_id: str,
    template_key: str,
    publication_group: str,
    recipe: dict[str, Any],
    recipe_index: int,
    shadow_order: int,
    catalogs: t37.Catalogs,
    t48_items: dict[tuple[str, int | None], dict[str, Any]],
    reused_aliases: dict[tuple[str, int], str],
    mte_runtime: dict[tuple[str, int], str],
    block_runtime: dict[tuple[str, int], str],
    fluid_overlay: dict[str, str],
) -> tuple[dict[str, Any], list[str], list[str]]:
    reconstruction: list[str] = []
    mapping: list[str] = []
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
            item,
            catalogs,
            t48_items,
            reused_aliases,
            mte_runtime,
            block_runtime,
            side="item_input",
        )
        mapping.extend(operand_errors)
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
            item,
            catalogs,
            t48_items,
            reused_aliases,
            mte_runtime,
            block_runtime,
            side="item_output",
        )
        mapping.extend(operand_errors)
        item_outputs.append(operand)
    fluid_inputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidInputs") or []):
        if not fluid:
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, fluid_overlay, side="fluid_input"
        )
        mapping.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_inputs.append(operand)
    fluid_outputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidOutputs") or []):
        if not fluid:
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, fluid_overlay, side="fluid_output"
        )
        mapping.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_outputs.append(operand)
    chances = [int(value) for value in (recipe.get("chances") or [])]
    if not chances and item_outputs:
        chances = [10000] * len(item_outputs)
    elif chances and len(chances) < len(item_outputs):
        chances.extend([10000] * (len(item_outputs) - len(chances)))
    duration = int(recipe.get("duration") or 0)
    if duration <= 0:
        reconstruction.append(f"{template_key}: duration must be positive")
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
        reconstruction.extend(f"{template_key}: unsupported {value}" for value in unsupported)
    return relation, reconstruction, mapping


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
    catalogs = load_t48_catalogs()
    t48_items = identities.load_t48_item_overlay()
    reused_aliases = identities.load_reused_aliases()
    mte_runtime = identities.load_mte_runtime_map()
    block_runtime = identities.load_block_runtime_map()
    fluid_overlay = identities.load_fluid_base_overlay()
    fluid_overlay.update(identities.load_t48_fluid_overlay())
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
    reconstruction: list[str] = []
    mapping: list[str] = []
    assigned = 0
    for row in work_set.get("families") or []:
        template_key = str(row["template_key"])
        expected = int(row.get("expanded_count") or 0)
        rows = sorted(grouped.get(template_key, []), key=lambda item: item[0])
        if len(rows) != expected:
            reconstruction.append(
                f"{template_key}: expected {expected} ordinary rows, got {len(rows)}"
            )
            continue
        for shadow_order, (recipe_index, recipe) in enumerate(rows):
            assigned += 1
            relation, row_reconstruction, row_mapping = compile_relation(
                family_id=str(row["family_id"]),
                template_key=template_key,
                publication_group=str(row["publication_group"]),
                recipe=recipe,
                recipe_index=recipe_index,
                shadow_order=shadow_order,
                catalogs=catalogs,
                t48_items=t48_items,
                reused_aliases=reused_aliases,
                mte_runtime=mte_runtime,
                block_runtime=block_runtime,
                fluid_overlay=fluid_overlay,
            )
            reconstruction.extend(row_reconstruction)
            mapping.extend(row_mapping)
            relations.append(relation)
    relations.sort(key=lambda row: (str(row["family_id"]), int(row["shadow_order"])))
    frozen = (
        not reconstruction
        and assigned == common.CANDIDATE_RELATION_COUNT
        and len(relations) == assigned
        and len({row["stable_id"] for row in relations}) == len(relations)
    )
    source = {
        "assignment": {"assigned": assigned, "missing": reconstruction[:8]},
        "blockers": reconstruction,
        "generated_by": "python tools/build_t48_bath_source.py --full-replay",
        "host": common.HOST,
        "mapping_blockers": mapping[:32],
        "owner": common.OWNER,
        "relations": relations,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T48_BATH_SOURCE_FROZEN" if frozen else "T48_BATH_SOURCE_BLOCKED",
        "work_set": {
            "family_count": len(work_set.get("families") or []),
            "family_ids": list(work_set.get("family_ids") or []),
            "source_rows": len(relations),
            "selection_sha256": work_set.get("selection_sha256"),
        },
    }
    mapping_counts: Counter[str] = Counter()
    unmapped_items: Counter[str] = Counter()
    unmapped_fluids: Counter[str] = Counter()
    for relation in relations:
        for operand in _operands(relation):
            mapping_counts[str(operand.get("mapping") or "unknown")] += 1
            if operand.get("mapping") == "blocked_unmapped" or not operand.get("runtime_id"):
                source_row = operand.get("source") or {}
                if source_row.get("item"):
                    unmapped_items[f"{source_row.get('item')}@{source_row.get('meta')}"] += 1
                if source_row.get("fluid"):
                    unmapped_fluids[str(source_row.get("fluid"))] += 1
    review = {
        "generated_by": "python tools/build_t48_bath_source.py",
        "mapping_counts": dict(sorted(mapping_counts.items())),
        "r0_reconstruction_blockers": reconstruction,
        "relation_count": len(relations),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T48_BATH_SOURCE_REVIEW",
        "unmapped_fluid_count": len(unmapped_fluids),
        "unmapped_fluids": [
            {"count": count, "source_fluid": fluid}
            for fluid, count in unmapped_fluids.most_common(64)
        ],
        "unmapped_item_count": len(unmapped_items),
        "unmapped_items": [
            {"count": count, "source_item": item}
            for item, count in unmapped_items.most_common(64)
        ],
    }
    compact = t35.stable_json(source)
    receipt = {
        "builder_sha256": t35.sha256_file(BUILDER),
        "compact_sha256": sha256_text(compact),
        "denominator_source_revision": common.SOURCE_REVISION,
        "dump_file_hashes": {common.relative(common.BATH_DUMP): dump_hash},
        "full_replay": True,
        "generated_by": "python tools/build_t48_bath_source.py --full-replay",
        "recipe_families_sha256": t35.sha256_file(common.RECIPE_FAMILIES),
        "schema_version": 1,
        "selection_sha256": work_set.get("selection_sha256"),
        "source_oracle_dirty": False,
        "source_oracle_path": common.ORACLE_RELATIVE,
        "source_oracle_revision": common.oracle_revision(),
        "status": "T48_BATH_SOURCE_RECEIPT",
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
    frozen = document.get("status") == "T48_BATH_SOURCE_FROZEN"
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
        "status": "T48_SOURCE_PACK_FROZEN" if frozen else "T48_SOURCE_PACK_BLOCKED",
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
    if document.get("status") != "T48_BATH_SOURCE_FROZEN":
        errors.append(f"compact status is {document.get('status')}")
    if document.get("blockers"):
        errors.append("compact source still has reconstruction blockers")
    if receipt.get("source_oracle_dirty") is not False:
        errors.append("source_oracle_dirty must be false")
    if receipt.get("denominator_source_revision") != common.SOURCE_REVISION:
        errors.append("denominator pin drifted")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("receipt skip_is_not_pass must be true")
    relations = list(document.get("relations") or [])
    if len(relations) != common.CANDIDATE_RELATION_COUNT:
        errors.append(
            f"T48 source relations {len(relations)} != {common.CANDIDATE_RELATION_COUNT}"
        )
    for row in relations[:64]:
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
        print(f"T48 Bath remainder source failed: {error}", file=sys.stderr)
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
