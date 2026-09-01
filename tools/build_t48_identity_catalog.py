#!/usr/bin/env python3
"""Freeze T48 identity catalog for tool_head, multiitem, and leftover objects."""
from __future__ import annotations

import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t48_common as common
from tools import t48_identities as identities

OUTPUT = common.IDENTITY_CATALOG
BUNDLED = common.BUNDLED_IDENTITY_CATALOG
ITEM_MODEL_ROOT = ROOT / "src/main/resources/assets/cruciblecraft/models/item"
TAG_ROOT = ROOT / "src/main/resources/data/cruciblecraft/tags/item"


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return list(relation.get("item_inputs") or []) + list(
        relation.get("item_outputs") or []
    )


def remaining_unmapped_allowed(operand: dict[str, Any]) -> bool:
    return identities.operand_runtime_closed(operand)


def lock_family_ids() -> set[str]:
    if not common.SOURCE.is_file():
        return set()
    source = common.load_json(common.SOURCE)
    if source.get("status") != "T48_BATH_SOURCE_FROZEN":
        return set()
    work_set = common.load_json(common.WORK_SET)
    blocker = common.load_json(common.T42_BLOCKER)
    recycling_ids = {
        str(row["family_id"])
        for row in blocker.get("families") or []
        if row.get("recycling_candidate")
    }
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source.get("relations") or []:
        by_family[str(relation["family_id"])].append(relation)
    selected: set[str] = set()
    for row in work_set.get("families") or []:
        family_id = str(row["family_id"])
        if family_id in recycling_ids:
            continue
        relations = by_family.get(family_id) or []
        if not relations:
            continue
        if all(
            remaining_unmapped_allowed(operand)
            for relation in relations
            for operand in (
                list(relation.get("item_inputs") or [])
                + list(relation.get("item_outputs") or [])
                + list(relation.get("fluid_inputs") or [])
                + list(relation.get("fluid_outputs") or [])
            )
        ):
            selected.add(family_id)
    return selected


def _catalog_kinds(item_id: str) -> str | None:
    kind = identities.classify_source_item(item_id)
    if kind in {"tool_head", "multiitem"}:
        return kind
    if kind == "other" and item_id != identities.CIRCUIT_ITEM:
        return "object"
    if kind == "gt_prefix" and identities.is_tool_head_item(item_id):
        return "tool_head"
    return None


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    relations = identities.load_t47_work_set_relations()
    reused_aliases = identities.load_reused_aliases()
    wanted: dict[tuple[str, int], dict[str, Any]] = {}
    family_ids: set[str] = set()
    reused = 0
    reused_rows: list[dict[str, Any]] = []
    seen_reused: set[tuple[str, int]] = set()
    for relation in relations:
        family_id = str(relation.get("family_id") or "")
        for operand in _operands(relation):
            source_row = operand.get("source") or {}
            item = str(source_row.get("item") or "")
            if not item:
                continue
            meta = source_row.get("meta")
            meta_key = int(meta) if isinstance(meta, int) else 0
            alias = reused_aliases.get((item, meta_key))
            if alias:
                reused += 1
                if (item, meta_key) not in seen_reused:
                    seen_reused.add((item, meta_key))
                    reused_rows.append(
                        {
                            "meta": meta_key,
                            "runtime_id": alias,
                            "source_item": item,
                        }
                    )
                continue
            kind = _catalog_kinds(item)
            if kind is None:
                continue
            key = (item, meta_key)
            row = wanted.setdefault(
                key,
                {
                    "display": str(source_row.get("displayName") or ""),
                    "family_ids": set(),
                    "input_count": 0,
                    "kind": kind,
                    "output_count": 0,
                },
            )
            row["family_ids"].add(family_id)
            if operand in (relation.get("item_inputs") or []):
                row["input_count"] += 1
            else:
                row["output_count"] += 1
            if not row["display"] and source_row.get("displayName"):
                row["display"] = str(source_row["displayName"])
    identities_out: list[dict[str, Any]] = []
    kind_counts: Counter[str] = Counter()
    family_ids = set()
    for (item, meta), counts in sorted(wanted.items()):
        kind = str(counts["kind"])
        runtime = identities.runtime_id_for(kind, item, meta)
        common.assert_runtime_id(runtime, consume=False)
        family_ids.update(counts["family_ids"])
        kind_counts[kind] += 1
        identities_out.append(
            {
                "acquisition_authority": "T48",
                "behavior": kind,
                "chinese_name": identities.chinese_name(item, meta, counts["display"]),
                "display_requirements": {
                    "distinguishable": True,
                    "holdable": True,
                    "model": "item/generated",
                },
                "english_name": identities.english_name(item, meta, counts["display"]),
                "input_count": counts["input_count"],
                "kind": kind,
                "mapping_class": "exact_item",
                "meta": meta,
                "occurrence_count": counts["input_count"] + counts["output_count"],
                "output_count": counts["output_count"],
                "registry_kind": "item",
                "registry_path": identities.registry_path_for(kind, item, meta),
                "runtime_id": runtime,
                "source_evidence": "tools/t47_bath_source.json#T48_work_set",
                "source_item": item,
                "texture": "minecraft:item/iron_ingot",
            }
        )
    return {
        "covered_family_count": len(family_ids),
        "generated_by": "python tools/build_t48_identity_catalog.py",
        "identities": identities_out,
        "identity_count": len(identities_out),
        "kind_counts": dict(sorted(kind_counts.items())),
        "lock_family_count": len(family_ids),
        "note": (
            "Tool heads and unproven multiitems are distinct holdable items. "
            "T39 food aliases are reused, not re-registered. Prefix forms stay "
            "out of this catalog."
        ),
        "reused_alias_count": len(reused_rows),
        "reused_aliases": reused_rows,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T48_IDENTITY_CATALOG",
        "variant_count": len(identities_out),
        "work_set_family_count": len(work_set.get("families") or []),
    }


def _write_item_models(document: dict[str, Any]) -> None:
    for identity in document.get("identities") or []:
        path = ITEM_MODEL_ROOT / f"{identity['registry_path']}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            t35.stable_json(
                {
                    "parent": "minecraft:item/generated",
                    "textures": {
                        "layer0": str(identity.get("texture") or "minecraft:item/iron_ingot")
                    },
                }
            ),
            encoding="utf-8",
            newline="\n",
        )


def _write_wildcard_tags() -> None:
    TAG_ROOT.mkdir(parents=True, exist_ok=True)
    for source_item, tag_id in identities.VANILLA_WILDCARD_TAGS.items():
        values = identities.stained_color_items(source_item)
        filename = tag_id.split(":", 1)[1] + ".json"
        (TAG_ROOT / filename).write_text(
            t35.stable_json({"replace": False, "values": values}),
            encoding="utf-8",
            newline="\n",
        )


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    _write_item_models(document)
    _write_wildcard_tags()
    return document


def check() -> list[str]:
    errors = common.check_document(OUTPUT, build())
    if BUNDLED.is_file():
        bundled = common.load_json(BUNDLED)
        committed = common.load_json(OUTPUT)
        if bundled != committed:
            errors.append("bundled T48 identity catalog drifted from tools catalog")
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T48 Bath identity catalog",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
