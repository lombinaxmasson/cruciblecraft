#!/usr/bin/env python3
"""Freeze the reviewed T39 production subset without auto-resigning it."""
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

from tools import build_t39_production_selection as candidate_builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t39_common as t39  # noqa: E402

OUTPUT = t39.PRODUCTION_LOCK
SUPPORT = t39.TOOLS / "t39_player_path_support.json"

# Reviewed 2026-08-26. Changing this tuple is a production re-sign, not a
# routine regeneration.
LOCKED_TEMPLATE_KEYS = (
    "gt.recipe.centrifuge#0008",
    "gt.recipe.centrifuge#0011",
    "gt.recipe.centrifuge#0047",
    "gt.recipe.centrifuge#0055",
    "gt.recipe.centrifuge#0056",
    "gt.recipe.centrifuge#0057",
    "gt.recipe.centrifuge#0060",
    "gt.recipe.centrifuge#0067",
    "gt.recipe.centrifuge#0068",
    "gt.recipe.centrifuge#0073",
    "gt.recipe.centrifuge#0080",
    "gt.recipe.centrifuge#0112",
    "gt.recipe.centrifuge#0113",
    "gt.recipe.centrifuge#0145",
    "gt.recipe.centrifuge#0147",
    "gt.recipe.centrifuge#0168",
    "gt.recipe.centrifuge#0170",
    "gt.recipe.centrifuge#0171",
    "gt.recipe.centrifuge#0206",
    "gt.recipe.centrifuge#0215",
    "gt.recipe.centrifuge#0216",
    "gt.recipe.centrifuge#0217",
)

PHASE_DEFERRED = (
    ("gt.recipe.centrifuge#0169", 9441, "MultiTileEntityReactorRodProduct"),
    ("gt.recipe.centrifuge#0176", 9319, "MultiTileEntityReactorRodDepleted"),
    ("gt.recipe.centrifuge#0180", 9329, "MultiTileEntityReactorRodDepleted"),
    ("gt.recipe.centrifuge#0185", 9339, "MultiTileEntityReactorRodDepleted"),
    ("gt.recipe.centrifuge#0188", 9349, "MultiTileEntityReactorRodDepleted"),
    ("gt.recipe.centrifuge#0190", 9360, "MultiTileEntityReactorRodDepleted"),
    ("gt.recipe.centrifuge#0191", 9361, "MultiTileEntityReactorRodDepleted"),
)


def _digest(value: Any) -> str:
    payload = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def _source_relations() -> list[dict[str, Any]]:
    document = t35.load_json(t39.SOURCE)
    relations = document.get("relations") or []
    if len(relations) != t39.CATALOG_RELATION_COUNT:
        raise ValueError("T39 frozen catalog source is not 250 relations")
    return list(relations)


def _catalog_rows() -> dict[str, dict[str, Any]]:
    document = t35.load_json(t39.WORK_SET)
    return {
        str(row["template_key"]): row
        for row in document.get("families") or []
    }


def _candidate() -> dict[str, Any]:
    document = candidate_builder.build()
    if document.get("status") != "T39_PRODUCTION_CANDIDATE":
        raise ValueError("T39 production candidate is not reviewable")
    candidate = document.get("candidate") or {}
    if tuple(candidate.get("template_keys") or []) != LOCKED_TEMPLATE_KEYS:
        raise ValueError(
            "T39 candidate drifted from the reviewed production lock seed"
        )
    return document


def _support_contract() -> dict[str, Any]:
    document = t35.load_json(SUPPORT)
    routes = document.get("routes") or []
    route_keys = sorted(
        f"{row['source_map']}#{int(row['source_recipe'])}" for row in routes
    )
    if len(route_keys) != 34 or len(route_keys) != len(set(route_keys)):
        raise ValueError("T39 reviewed support slice must contain 34 unique routes")
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
            raise ValueError(f"T39 lock source drifted for {template_key}")
        blockers = t39.family_fidelity_blockers(relations)
        if blockers:
            raise ValueError(f"T39 locked family has fidelity blockers: {template_key}")
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
    if relation_count != 32:
        raise ValueError("T39 reviewed production lock must contain 32 relations")
    if t39.selection_sha256(family_ids) != candidate["selection_sha256"]:
        raise ValueError("T39 reviewed candidate hash drifted")

    deferred = []
    for template_key, meta, behavior_class in PHASE_DEFERRED:
        row = catalog.get(template_key)
        relations = by_template.get(template_key) or []
        if row is None or len(relations) != 1:
            raise ValueError(f"T39 deferred fuel-rod family drifted: {template_key}")
        source_objects = [
            {
                "behavior_class": behavior_class,
                "item": "gregtech:gt.multitileentity",
                "meta": meta,
            }
        ]
        deferred.append({
            "family_id": str(row["family_id"]),
            "future_owner": "post_1x:nuclear",
            "reason": "stateful_reactor_rod_not_registered",
            "source_objects": source_objects,
            "stable_ids": [str(relations[0]["stable_id"])],
            "template_key": template_key,
        })

    return {
        "schema_version": 1,
        "status": "T39_PRODUCTION_LOCKED",
        "source_revision": t39.SOURCE_REVISION,
        "catalog_fixture": {
            "families": t39.CATALOG_FAMILY_COUNT,
            "relations": t39.CATALOG_RELATION_COUNT,
            "selection_sha256": t39.EXPECTED_SELECTION_SHA256,
            "status": "WITHDRAWN_TEST_FIXTURE",
        },
        "candidate_snapshot": {
            "path": t39.relative(t39.CANDIDATE_SELECTION),
            "selection_sha256": candidate["selection_sha256"],
            "sha256": _digest(candidate_document),
        },
        "production": {
            "families": families,
            "family_count": len(families),
            "family_ids": family_ids,
            "relation_count": relation_count,
            "selection_sha256": t39.selection_sha256(family_ids),
            "stable_ids": sorted(stable_ids),
            "template_keys": list(LOCKED_TEMPLATE_KEYS),
        },
        "support": _support_contract(),
        "phase_deferred": deferred,
        "note": (
            "This file is the production authority. Candidate selection is "
            "diagnostic; 157/250 is fixture-only. Rewriting an existing lock "
            "requires explicit --approve-resign."
        ),
    }


def check() -> list[str]:
    return t39.check_document(OUTPUT, build())


def write(*, approve_resign: bool) -> None:
    if OUTPUT.is_file() and not approve_resign:
        raise ValueError(
            "refusing to replace existing T39 production lock without --approve-resign"
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
        print(f"T39 production lock failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
