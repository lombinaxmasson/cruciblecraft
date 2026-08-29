#!/usr/bin/env python3
"""Freeze the T41 production lock for 292 Assembler singleton families."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t41_common as common  # noqa: E402

OUTPUT = common.PRODUCTION_LOCK
SUPPORT = common.PLAYER_PATH_SUPPORT


def _source_relations() -> list[dict[str, Any]]:
    document = t35.load_json(common.SOURCE)
    relations = document.get("relations") or []
    if len(relations) != common.CATALOG_RELATION_COUNT:
        raise ValueError("T41 frozen catalog source is not 1531 relations")
    return list(relations)


def _catalog_rows() -> dict[str, dict[str, Any]]:
    document = t35.load_json(common.WORK_SET)
    return {
        str(row["template_key"]): row
        for row in document.get("families") or []
    }


def _support_route_keys() -> list[str]:
    if not SUPPORT.is_file():
        return []
    document = t35.load_json(SUPPORT)
    keys = []
    for route in document.get("routes") or []:
        keys.append(f"{route['source_map']}#{route['source_recipe']}")
    return sorted(set(keys))


def build() -> dict[str, Any]:
    relations = _source_relations()
    catalog = _catalog_rows()
    by_template: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in relations:
        by_template[str(relation["template_key"])].append(relation)
    production_families: list[dict[str, Any]] = []
    stable_ids: list[str] = []
    for template_key in sorted(catalog):
        if template_key in common.COMBINATORIAL_TEMPLATE_KEYS:
            continue
        rows = by_template.get(template_key) or []
        if len(rows) != 1:
            raise ValueError(f"T41 production family {template_key} is not a singleton")
        relation = rows[0]
        for operand, count, action in zip(
            relation.get("item_inputs") or [],
            relation.get("item_input_counts") or [],
            relation.get("item_input_actions") or [],
        ):
            kind = str((action or {}).get("kind") or "").lower()
            if kind == "consume" and int(count or 0) > 0:
                common.assert_runtime_id(operand.get("runtime_id"), consume=True)
        family_id = str(catalog[template_key]["family_id"])
        group = relation.get("publication_group") or common.publication_group_for_relation(relation)
        if group not in common.PUBLICATION_GROUPS:
            raise ValueError(f"T41 family {template_key} has invalid group {group}")
        hex16 = str(relation["stable_id"]).rsplit("/", 1)[-1]
        runtime_id = f"cruciblecraft:t41/{hex16}"
        production_families.append({
            "expanded_count": 1,
            "family_id": family_id,
            "publication_group": group,
            "stable_id": runtime_id,
            "template_key": template_key,
        })
        stable_ids.append(runtime_id)
    family_ids = [row["family_id"] for row in production_families]
    template_keys = [row["template_key"] for row in production_families]
    digest = common.selection_sha256(family_ids)
    if digest != common.EXPECTED_PRODUCTION_SELECTION_SHA256:
        raise ValueError(
            "T41 production lock refuses to shrink or expand the 292/292 set: "
            f"{digest}"
        )
    if len(production_families) != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError("T41 production lock is not 292 families")
    deferred = []
    for template_key in common.COMBINATORIAL_TEMPLATE_KEYS:
        rows = by_template.get(template_key) or []
        deferred.append({
            "expanded_count": len(rows),
            "family_id": catalog[template_key]["family_id"],
            "future_owner": "later:assembler_combinatorial",
            "reason": "combinatorial_player_path_unproven",
            "stable_ids": [],
            "template_key": template_key,
        })
    route_keys = _support_route_keys()
    return {
        "candidate_snapshot": {
            "path": common.relative(common.CANDIDATE_SELECTION)
            if common.CANDIDATE_SELECTION.is_file()
            else None,
            "selection_sha256": digest,
        },
        "catalog_fixture": {
            "families": common.CATALOG_FAMILY_COUNT,
            "relations": common.CATALOG_RELATION_COUNT,
            "selection_sha256": common.EXPECTED_SELECTION_SHA256,
            "status": "CATALOG_TEST_FIXTURE",
        },
        "generated_by": "python tools/build_t41_production_lock.py",
        "note": (
            "This file is the production authority. 294/1531 is catalog fixture "
            "only. Combinatorial #0000/#0001 stay BLOCKED and do not deduct gap."
        ),
        "phase_deferred": deferred,
        "production": {
            "family_count": len(production_families),
            "family_ids": family_ids,
            "families": production_families,
            "relation_count": len(production_families),
            "selection_sha256": digest,
            "stable_ids": stable_ids,
            "template_keys": template_keys,
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T41_PRODUCTION_LOCKED",
        "support": {
            "route_count": len(route_keys),
            "route_keys": route_keys,
        },
    }


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Freeze the T41 Assembler production lock", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        document = build()
        if args.check:
            errors = common.check_document(OUTPUT, document)
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("T41 production lock is current.")
            return 0
        t35.write_stable(OUTPUT, document)
        print(
            f"Wrote T41 production lock ({document['production']['family_count']} families)."
        )
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"T41 production lock failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
