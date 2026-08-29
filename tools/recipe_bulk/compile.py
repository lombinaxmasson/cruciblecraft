#!/usr/bin/env python3
"""Compile locked families into compact authored documents. Lock is the only selector."""
from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path
from typing import Any

from tools import t35_common as t35
from tools import t45_common as common
from tools.recipe_bulk.emit import emit_family, family_filename
from tools.recipe_bulk.templates import expand_family


def _load_spec() -> dict[str, Any]:
    if not common.COMPILE_SPEC.is_file():
        raise FileNotFoundError("t45_recipe_compile_spec.json is required for compile")
    spec = common.load_json(common.COMPILE_SPEC)
    lock = common.load_production_lock()
    lock_hash = spec.get("production_lock_sha256")
    if lock_hash != common.production_lock_sha256():
        raise ValueError("compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("compile spec selection_sha256 drifted from lock")
    return spec


def planned_documents() -> list[tuple[Path, dict[str, Any]]]:
    _load_spec()
    lock = common.load_production_lock()
    source = common.load_json(common.SOURCE)
    by_template = {
        str(row["template_key"]): row for row in source.get("relations") or []
    }
    planned: list[tuple[Path, dict[str, Any]]] = []
    seen_consume: dict[str, str] = {}
    for row in (lock.get("production") or {}).get("families") or []:
        template_key = str(row["template_key"])
        relation = by_template.get(template_key)
        if relation is None:
            raise ValueError(f"locked family missing source relation: {template_key}")
        expanded = expand_family(
            family_id=str(row["family_id"]),
            relations=[relation],
            template_kind="exact_singleton",
        )
        if relation.get("parameterized"):
            raise ValueError(f"{template_key}: parameterized source must not compile")
        host = str(row["host"])
        group = str(row["publication_group"])
        target_map = str(common.HOST_CONFIG[host]["target_map"])
        document = emit_family(
            expanded["relations"][0],
            row,
            target_map=target_map,
            publication_group=group,
        )
        consume = json.dumps(
            document["relations"][0]["item_inputs"], sort_keys=True
        )
        previous = seen_consume.get(f"{target_map}:{consume}")
        if previous:
            raise ValueError(
                f"consume-identity collision on {target_map}: {previous} vs {template_key}"
            )
        seen_consume[f"{target_map}:{consume}"] = template_key
        host_name = host.split(":", 1)[-1]
        path = common.GENERATED_ROOT / host_name / family_filename(template_key)
        planned.append((path, document))
    return planned


def write_tree(planned: list[tuple[Path, dict[str, Any]]], root: Path) -> None:
    if root.exists():
        for path in root.rglob("gt_recipe_*.json"):
            path.unlink()
    for path, document in planned:
        rel = path.relative_to(common.GENERATED_ROOT)
        dest = root / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        t35.write_stable(dest, document)


def compile_report(planned: list[tuple[Path, dict[str, Any]]]) -> dict[str, Any]:
    lock = common.load_production_lock()
    by_group: dict[str, int] = defaultdict(int)
    for _path, document in planned:
        by_group[str(document["publication_group"])] += 1
    authored = common.OPENING_AUTHORED_ENTRIES + len(planned)
    return {
        "authored_entries_closing": authored,
        "authored_entries_opening": common.OPENING_AUTHORED_ENTRIES,
        "authored_hard_ceiling": common.T14_HARD_AUTHORED,
        "authored_under_ceiling": authored < common.T14_HARD_AUTHORED,
        "bundling": "v1_one_family_one_document",
        "family_count": len(planned),
        "generated_by": "python tools/build_recipe_bulk.py compile",
        "groups": dict(sorted(by_group.items())),
        "production_lock_sha256": common.production_lock_sha256(),
        "relation_count": len(planned),
        "schema_version": 1,
        "selection_sha256": lock["production"]["selection_sha256"],
        "status": "T45_COMPILE_REPORT",
        "t43_rewritten": False,
    }


def compile() -> dict[str, Any]:
    planned = planned_documents()
    return {
        "fixture": {str(path.relative_to(common.GENERATED_ROOT)): doc for path, doc in planned},
        "planned": planned,
        "report": compile_report(planned),
        "recipes": {str(path): doc for path, doc in planned},
    }
