#!/usr/bin/env python3
"""Freeze T43 Smelter compact source from classified GT6 rows.

Modes:
  --full-replay           rebuild source, receipt, and review from the GT6 dump
  --check                 validate committed source and receipt without the dump
  --check --full-replay   rebuild from the dump and compare to the committed source
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
from tools import t43_common as common  # noqa: E402

BUILDER = Path(__file__).resolve()
SCHEMA = common.SCHEMA
OUTPUT = common.SOURCE
RECEIPT = common.RECEIPT
REVIEW = common.REVIEW
SOURCE_PACK = common.SOURCE_PACK
WORK_SET = common.WORK_SET
STONE_CATALOG = common.STONE_CATALOG
SMELTER_DUMP = common.SMELTER_DUMP
SOURCE_REVISION = common.SOURCE_REVISION
HOST = common.HOST
SOURCE_MAP = common.SOURCE_MAP
TARGET_MAP = common.TARGET_MAP
OWNER = common.OWNER
CATALOG_FAMILY_COUNT = common.CATALOG_FAMILY_COUNT
CATALOG_RELATION_COUNT = common.CATALOG_RELATION_COUNT
TEMPLATE_RE = re.compile(r"^gt\.recipe\.smelter#(\d{4})$")
FAMILY_SUFFIX_RE = re.compile(r"/gt\.recipe\.smelter#(\d{4})$")
ORDINARY_CLASS = "ordinary_optional"
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

Catalogs = t37.Catalogs
load_catalogs = t37.load_catalogs
classify_item_action = t37.classify_item_action


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
    return (path or SMELTER_DUMP).is_file()


def load_stone_runtime_map(catalog: dict[str, Any] | None = None) -> dict[tuple[str, int], str]:
    catalog = catalog if catalog is not None else load_json(STONE_CATALOG)
    mapped: dict[tuple[str, int], str] = {}
    for identity in catalog.get("identities") or []:
        source_item = str(identity["source_item"])
        for variant in identity.get("variants") or []:
            mapped[(source_item, int(variant["meta"]))] = str(variant["runtime_id"])
    if len(mapped) != catalog.get("variant_count"):
        raise ValueError("T43 stone catalog variant map drifted")
    return mapped


def map_item_operand(
    item: dict[str, Any],
    catalogs: Catalogs,
    stone_runtime: dict[tuple[str, int], str],
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    item_id = str(item.get("item") or "")
    meta = item.get("meta")
    if common.is_stone_item(item_id) and isinstance(meta, int):
        runtime = stone_runtime.get((item_id, meta))
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
            "t43_class": "SOURCE_BACKED",
        }
        if not runtime:
            errors.append(f"unmapped T43 stone {item_id}@{meta}")
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
    operand, errors = t37.map_item_operand(item, catalogs, side=side)
    if operand.get("runtime_id"):
        operand["t43_class"] = "SOURCE_DERIVED"
    return operand, errors


def load_work_set(
    work_set: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
) -> list[WorkFamily]:
    work_set = work_set if work_set is not None else load_json(WORK_SET)
    families_doc = families_doc if families_doc is not None else load_json(common.RECIPE_FAMILIES)
    if families_doc.get("source_revision") != SOURCE_REVISION:
        raise ValueError("t35_recipe_families source_revision drifted")
    if work_set.get("selection_sha256") != common.EXPECTED_SELECTION_SHA256:
        raise ValueError("t43_work_set selection_sha256 drifted")
    family_ids = [str(value) for value in (work_set.get("family_ids") or [])]
    by_id = {
        str(row.get("family_id") or ""): row
        for row in families_doc.get("families") or []
        if isinstance(row, dict)
    }
    work: list[WorkFamily] = []
    for family_id in family_ids:
        record = by_id.get(family_id)
        if record is None:
            raise ValueError(f"T43 work set family missing from t35 ledger: {family_id}")
        template_key = str(record.get("template_key") or "")
        if TEMPLATE_RE.fullmatch(template_key) is None:
            raise ValueError(f"{family_id}: unexpected template_key {template_key}")
        suffix = FAMILY_SUFFIX_RE.search(family_id)
        if suffix is None or f"{SOURCE_MAP}#{suffix.group(1)}" != template_key:
            raise ValueError(f"{family_id}: family suffix does not match template_key")
        work.append(WorkFamily(family_id=family_id, template_key=template_key, record=record))
    if len(work) != CATALOG_FAMILY_COUNT:
        raise ValueError(f"T43 work set must contain {CATALOG_FAMILY_COUNT} families")
    return work


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
        template_key = f"{SOURCE_MAP}#{group_index:04d}"
        for recipe_index in members:
            membership[recipe_index] = template_key
    return membership


def load_smelter_recipes(path: Path | None = None) -> tuple[list[dict[str, Any]], str]:
    dump_path = path or SMELTER_DUMP
    if not dump_path.is_file():
        display = relative(dump_path) if dump_path.is_relative_to(ROOT) else dump_path.as_posix()
        raise OSError("missing GT6 dump map required for full replay: " + display)
    document = load_json(dump_path)
    if document.get("nameInternal") != SOURCE_MAP:
        raise ValueError(f"map identity mismatch: expected {SOURCE_MAP}")
    recipes = list(document.get("recipes") or [])
    if not recipes:
        raise ValueError("Smelter dump recipe list is empty")
    return recipes, sha256_file(dump_path)


def load_ordinary_recipe_indices(
    row_classification: dict[str, Any] | None = None,
    template_denominator: dict[str, Any] | None = None,
) -> set[int]:
    row_classification = (
        row_classification
        if row_classification is not None
        else load_json(common.ROW_CLASSIFICATION)
    )
    template_denominator = (
        template_denominator
        if template_denominator is not None
        else load_json(common.TEMPLATE_DENOMINATOR)
    )
    classes = list(row_classification.get("classes") or [])
    ordinary_index = classes.index(ORDINARY_CLASS)
    maps = list((template_denominator.get("encoding") or {}).get("maps") or [])
    if len(maps) <= common.SMELTER_MAP_INDEX or maps[common.SMELTER_MAP_INDEX] != SOURCE_MAP:
        raise ValueError(
            f"t21 encoding.maps[{common.SMELTER_MAP_INDEX}] must equal {SOURCE_MAP}"
        )
    ordinary: set[int] = set()
    for row in row_classification.get("non_mixer_rows") or []:
        map_index, recipe_index, class_index = row
        if map_index != common.SMELTER_MAP_INDEX:
            continue
        if class_index == ordinary_index:
            ordinary.add(recipe_index)
    return ordinary


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
    return f"cruciblecraft:t43/{digest}"


def compile_relation(
    *,
    work: WorkFamily,
    recipe: dict[str, Any],
    recipe_index: int,
    shadow_order: int,
    catalogs: Catalogs,
    stone_runtime: dict[tuple[str, int], str],
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
    needs_empty_output = bool(recipe.get("needsEmptyOutput"))
    no_nbt_checks = True
    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("inputs") or []):
        if t37._empty_item(item):
            slot_notes.append({"side": "item_input", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_item_operand(
            item, catalogs, stone_runtime, side="item_input"
        )
        errors.extend(operand_errors)
        action = classify_item_action(item)
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
            item, catalogs, stone_runtime, side="item_output"
        )
        errors.extend(operand_errors)
        item_outputs.append(operand)
    fluid_inputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidInputs") or []):
        if not fluid:
            continue
        operand, operand_errors = t37.map_fluid_operand(fluid, catalogs, side="fluid_input")
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_inputs.append(operand)
    fluid_outputs: list[dict[str, Any]] = []
    for slot, fluid in enumerate(recipe.get("fluidOutputs") or []):
        if not fluid:
            continue
        operand, operand_errors = t37.map_fluid_operand(fluid, catalogs, side="fluid_output")
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
        errors.append(f"{work.template_key}: duration must be positive")
    can_be_buffered = bool(recipe.get("canBeBuffered", True))
    relation = {
        "family_id": work.family_id,
        "template_key": work.template_key,
        "publication_group": common.STONE_GROUP,
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
            "kinds": ["SOURCE_BACKED", "SOURCE_DERIVED"],
            "fields": {
                "item_inputs": "SOURCE_DERIVED",
                "item_input_counts": "SOURCE_BACKED",
                "duration": "SOURCE_BACKED",
                "eut": "SOURCE_BACKED",
                "special_value": "SOURCE_BACKED",
                "can_be_buffered": "SOURCE_BACKED",
            },
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
    membership = recipe_template_ids(recipes)
    wanted = {item.template_key for item in work}
    grouped: dict[str, list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    for recipe_index in sorted(ordinary_indices):
        template_key = membership.get(recipe_index)
        if template_key not in wanted:
            continue
        grouped[template_key].append((recipe_index, recipes[recipe_index]))
    expected = {item.template_key: int(item.record["expanded_count"]) for item in work}
    missing = [
        f"{item.template_key}: expected {expected[item.template_key]}, got {len(grouped[item.template_key])}"
        for item in work
        if len(grouped[item.template_key]) != expected[item.template_key]
    ]
    assigned = sum(len(rows) for rows in grouped.values())
    return dict(grouped), {
        "assigned": assigned,
        "missing": missing,
        "extra": [],
        "duplicate": [],
    }


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def compile_relations(
    recipes: list[dict[str, Any]],
    work: list[WorkFamily],
    catalogs: Catalogs,
    ordinary_indices: set[int],
    stone_runtime: dict[tuple[str, int], str],
) -> tuple[list[dict[str, Any]], dict[str, Any], list[str]]:
    assigned, report = assign_work_rows(recipes, work, ordinary_indices)
    errors: list[str] = []
    if report["assigned"] != CATALOG_RELATION_COUNT:
        errors.append(
            f"assignment assigned={report['assigned']}, expected {CATALOG_RELATION_COUNT}"
        )
    if report["missing"]:
        errors.append("assignment missing: " + "; ".join(report["missing"][:8]))
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
                stone_runtime=stone_runtime,
            )
            errors.extend(row_errors)
            relations.append(relation)
    return relations, report, errors


def build_review(document: dict[str, Any]) -> dict[str, Any]:
    relations = list(document.get("relations") or [])
    mapping_counts: Counter[str] = Counter()
    for relation in relations:
        for operand in _operands(relation):
            mapping_counts[str(operand.get("mapping") or "unknown")] += 1
    blockers = list(document.get("blockers") or [])
    return {
        "generated_by": "python tools/build_t43_smelter_source.py",
        "mapping_counts": dict(sorted(mapping_counts.items())),
        "r0_blockers": blockers,
        "relation_count": len(relations),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "T43_SMELTER_SOURCE_REVIEW",
    }


def build_document(
    relations: list[dict[str, Any]],
    work: list[WorkFamily],
    report: dict[str, Any],
    errors: list[str],
) -> dict[str, Any]:
    blocked = any(
        operand.get("mapping") == "blocked_unmapped"
        for relation in relations
        for operand in _operands(relation)
    )
    unsupported = any(relation.get("unsupported_semantics") for relation in relations)
    frozen = (
        not errors
        and len(work) == CATALOG_FAMILY_COUNT
        and len(relations) == CATALOG_RELATION_COUNT
        and report["assigned"] == CATALOG_RELATION_COUNT
        and not report["missing"]
        and not blocked
        and not unsupported
        and len({relation["stable_id"] for relation in relations}) == CATALOG_RELATION_COUNT
    )
    return {
        "assignment": report,
        "blockers": errors,
        "generated_by": "python tools/build_t43_smelter_source.py --full-replay",
        "host": HOST,
        "owner": OWNER,
        "relations": relations,
        "schema_version": 1,
        "source_map": SOURCE_MAP,
        "source_revision": SOURCE_REVISION,
        "status": "T43_SMELTER_SOURCE_FROZEN" if frozen else "T43_SMELTER_SOURCE_BLOCKED",
        "target_map": TARGET_MAP,
        "work_set": {
            "family_count": len(work),
            "family_ids": [item.family_id for item in work],
            "source_rows": len(relations),
            "template_keys": [item.template_key for item in work],
        },
    }


def build_receipt(
    document: dict[str, Any],
    *,
    dump_sha256: str,
    dump_path: Path,
) -> dict[str, Any]:
    compact = t35.stable_json(document)
    receipt = {
        "builder_sha256": sha256_file(BUILDER),
        "compact_sha256": sha256_text(compact),
        "dump": {"path": relative(dump_path), "sha256": dump_sha256},
        "full_replay": True,
        "generated_by": "python tools/build_t43_smelter_source.py --full-replay",
        "recipe_families_sha256": sha256_file(common.RECIPE_FAMILIES),
        "schema_sha256": sha256_file(SCHEMA) if SCHEMA.is_file() else None,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "T43_SMELTER_SOURCE_RECEIPT",
        "work_set_sha256": sha256_file(WORK_SET),
        "proof_tier": "full_replay",
        "skip_is_not_pass": True,
    }
    return receipt


def build_source_pack(
    document: dict[str, Any],
    receipt: dict[str, Any],
    review: dict[str, Any],
) -> dict[str, Any]:
    frozen = document.get("status") == "T43_SMELTER_SOURCE_FROZEN"
    return {
        "catalog": {
            "families": CATALOG_FAMILY_COUNT,
            "relations": CATALOG_RELATION_COUNT,
            "selection_sha256": common.EXPECTED_SELECTION_SHA256,
        },
        "files": {
            "receipt": {"path": relative(RECEIPT)},
            "review": {"path": relative(REVIEW)},
            "source": {
                "path": relative(OUTPUT),
                "sha256": receipt.get("compact_sha256"),
            },
            "work_set": {"path": relative(WORK_SET), "sha256": receipt.get("work_set_sha256")},
        },
        "full_replay": {
            "dump_present": dump_exists(),
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
        "host": HOST,
        "owner": OWNER,
        "review_status": review.get("status"),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "T43_SOURCE_PACK_FROZEN" if frozen else "T43_SOURCE_PACK_BLOCKED",
    }


def build_from_dump(*, dump_path: Path | None = None) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], str]:
    work = load_work_set()
    recipes, dump_sha256 = load_smelter_recipes(dump_path)
    ordinary_indices = load_ordinary_recipe_indices()
    catalogs = load_catalogs()
    stone_runtime = load_stone_runtime_map()
    relations, report, errors = compile_relations(
        recipes, work, catalogs, ordinary_indices, stone_runtime
    )
    document = build_document(relations, work, report, errors)
    review = build_review(document)
    receipt = build_receipt(
        document, dump_sha256=dump_sha256, dump_path=dump_path or SMELTER_DUMP
    )
    return document, receipt, review, dump_sha256


def validate_committed(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if document.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if document.get("source_revision") != SOURCE_REVISION:
        errors.append("source_revision drifted")
    relations = list(document.get("relations") or [])
    if len(relations) != CATALOG_RELATION_COUNT:
        errors.append(f"relations length {len(relations)} != {CATALOG_RELATION_COUNT}")
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if len(stable_ids) != len(set(stable_ids)):
        errors.append("stable ids are not unique")
    for row in relations:
        missing_fields = [field for field in REQUIRED_RELATION_FIELDS if field not in row]
        if missing_fields:
            errors.append(f"{row.get('family_id')} missing {missing_fields}")
            continue
        if not str(row.get("stable_id") or "").startswith("cruciblecraft:t43/"):
            errors.append(f"{row.get('family_id')} stable_id namespace drifted")
        elif row.get("stable_id") != stable_id_for(row):
            errors.append(f"{row.get('family_id')} stable_id fingerprint drifted")
        if any(operand.get("mapping") == "blocked_unmapped" for operand in _operands(row)):
            errors.append(f"{row.get('family_id')} has blocked_unmapped operand")
    return errors


def reference_only_check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing compact source: {relative(OUTPUT)}"]
    if not RECEIPT.is_file():
        return [f"missing receipt: {relative(RECEIPT)}"]
    document = load_json(OUTPUT)
    receipt = load_json(RECEIPT)
    errors: list[str] = []
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != t35.stable_json(document):
        errors.append(f"{relative(OUTPUT)} is not canonical JSON")
    if receipt.get("compact_sha256") != sha256_text(actual):
        errors.append("receipt.compact_sha256 does not match compact source")
    errors.extend(validate_committed(document))
    if document.get("status") != "T43_SMELTER_SOURCE_FROZEN":
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


def write() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    document, receipt, review, _digest = build_from_dump()
    t35.write_stable(OUTPUT, document)
    t35.write_stable(RECEIPT, receipt)
    t35.write_stable(REVIEW, review)
    t35.write_stable(SOURCE_PACK, build_source_pack(document, receipt, review))
    return document, receipt, review


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
            parser.error("choose --check and/or --full-replay")
            return 2
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T43 smelter source failed: {error}", file=sys.stderr)
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
    return 0 if document.get("status") == "T43_SMELTER_SOURCE_FROZEN" else 1


if __name__ == "__main__":
    raise SystemExit(main())
