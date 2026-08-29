#!/usr/bin/env python3
"""Freeze the reviewed T40 production subset without auto-resigning it."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import build_t40_production_selection as candidate_builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t40_common as t40  # noqa: E402

OUTPUT = t40.PRODUCTION_LOCK
SUPPORT = t40.TOOLS / "t40_player_path_support.json"

# Reviewed 2026-08-26. These families are family-atomic on T21 with
# unproven_lossy_alias=0 and no extra support. Combinatorial #0000/#0001
# stay BLOCKED. Changing this tuple is a production re-sign.
LOCKED_TEMPLATE_KEYS = (
    "gt.recipe.electrolyzer#0002",
    "gt.recipe.electrolyzer#0005",
    "gt.recipe.electrolyzer#0031",
    "gt.recipe.electrolyzer#0041",
    "gt.recipe.electrolyzer#0053",
    "gt.recipe.electrolyzer#0056",
    "gt.recipe.electrolyzer#0060",
    "gt.recipe.electrolyzer#0067",
    "gt.recipe.electrolyzer#0079",
    "gt.recipe.electrolyzer#0090",
    "gt.recipe.electrolyzer#0098",
    "gt.recipe.electrolyzer#0101",
    "gt.recipe.electrolyzer#0103",
)

PHASE_DEFERRED = (
    (
        "gt.recipe.electrolyzer#0000",
        "combinatorial_player_path_unproven",
        "later:electrolyzer_combinatorial",
    ),
    (
        "gt.recipe.electrolyzer#0001",
        "combinatorial_player_path_unproven",
        "later:electrolyzer_combinatorial",
    ),
)


def _digest(value: Any) -> str:
    payload = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def _source_relations() -> list[dict[str, Any]]:
    document = t35.load_json(t40.SOURCE)
    relations = document.get("relations") or []
    if len(relations) != t40.CATALOG_RELATION_COUNT:
        raise ValueError("T40 frozen catalog source is not 151 relations")
    return list(relations)


def _catalog_rows() -> dict[str, dict[str, Any]]:
    document = t35.load_json(t40.WORK_SET)
    return {
        str(row["template_key"]): row
        for row in document.get("families") or []
    }


def _candidate() -> dict[str, Any]:
    document = candidate_builder.build()
    if document.get("status") != "T40_PRODUCTION_CANDIDATE":
        raise ValueError("T40 production candidate is not reviewable")
    candidate = document.get("candidate") or {}
    if tuple(candidate.get("template_keys") or []) != LOCKED_TEMPLATE_KEYS:
        raise ValueError(
            "T40 candidate drifted from the reviewed production lock seed: "
            f"{tuple(candidate.get('template_keys') or [])}"
        )
    return document


def _support_contract() -> dict[str, Any]:
    if not SUPPORT.is_file():
        return {
            "route_count": 0,
            "route_keys": [],
            "routes_sha256": _digest([]),
        }
    document = t35.load_json(SUPPORT)
    routes = document.get("routes") or []
    route_keys = sorted(
        f"{row['source_map']}#{int(row['source_recipe'])}" for row in routes
    )
    if len(route_keys) != len(set(route_keys)):
        raise ValueError("T40 support route_keys are not unique")
    return {
        "route_count": len(route_keys),
        "route_keys": route_keys,
        "routes_sha256": _digest(routes),
    }


def build() -> dict[str, Any]:
    candidate_document = _candidate()
    candidate = candidate_document["candidate"]
    catalog = _catalog_rows()
    by_template: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in _source_relations():
        by_template[str(relation["template_key"])].append(relation)

    families: list[dict[str, Any]] = []
    stable_ids: list[str] = []
    for template_key in LOCKED_TEMPLATE_KEYS:
        row = catalog.get(template_key)
        relations = by_template.get(template_key) or []
        if row is None or len(relations) != int(row["expanded_count"]):
            raise ValueError(f"T40 lock source drifted for {template_key}")
        blockers = t40.family_fidelity_blockers(relations)
        if blockers:
            raise ValueError(f"T40 locked family has fidelity blockers: {template_key}")
        family = {
            "expanded_count": int(row["expanded_count"]),
            "family_id": str(row["family_id"]),
            "publication_group": str(row["publication_group"]),
            "stable_ids": sorted(str(value["stable_id"]) for value in relations),
            "template_key": template_key,
        }
        families.append(family)
        stable_ids.extend(family["stable_ids"])

    family_ids = [row["family_id"] for row in families]
    relation_count = sum(row["expanded_count"] for row in families)
    if relation_count != 22:
        raise ValueError(
            f"T40 reviewed production lock must contain 22 relations, got {relation_count}"
        )
    if t40.selection_sha256(family_ids) != candidate["selection_sha256"]:
        raise ValueError("T40 reviewed candidate hash drifted")

    deferred = []
    for template_key, reason, future_owner in PHASE_DEFERRED:
        row = catalog.get(template_key)
        relations = by_template.get(template_key) or []
        if row is None or not relations:
            raise ValueError(f"T40 deferred combinatorial family drifted: {template_key}")
        deferred.append({
            "expanded_count": int(row["expanded_count"]),
            "family_id": str(row["family_id"]),
            "future_owner": future_owner,
            "reason": reason,
            "stable_ids": sorted(str(value["stable_id"]) for value in relations),
            "template_key": template_key,
        })

    return {
        "schema_version": 1,
        "status": "T40_PRODUCTION_LOCKED",
        "source_revision": t40.SOURCE_REVISION,
        "catalog_fixture": {
            "families": t40.CATALOG_FAMILY_COUNT,
            "relations": t40.CATALOG_RELATION_COUNT,
            "selection_sha256": t40.EXPECTED_SELECTION_SHA256,
            "status": "CATALOG_TEST_FIXTURE",
        },
        "candidate_snapshot": {
            "path": t40.relative(t40.CANDIDATE_SELECTION),
            "selection_sha256": candidate["selection_sha256"],
            "sha256": _digest(candidate_document),
        },
        "production": {
            "families": families,
            "family_count": len(families),
            "family_ids": family_ids,
            "relation_count": relation_count,
            "selection_sha256": t40.selection_sha256(family_ids),
            "stable_ids": sorted(stable_ids),
            "template_keys": list(LOCKED_TEMPLATE_KEYS),
        },
        "support": _support_contract(),
        "phase_deferred": deferred,
        "note": (
            "This file is the production authority. 61/151 is catalog fixture "
            "only. Combinatorial #0000/#0001 stay BLOCKED. Rewriting an "
            "existing lock requires explicit --approve-resign."
        ),
    }


def check() -> list[str]:
    return t40.check_document(OUTPUT, build())


def write(*, approve_resign: bool) -> None:
    if OUTPUT.is_file() and not approve_resign:
        raise ValueError(
            "refusing to replace existing T40 production lock without --approve-resign"
        )
    t35.write_stable(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--approve-resign", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        if args.write:
            write(approve_resign=args.approve_resign)
        else:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T40 production lock failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
