#!/usr/bin/env python3
"""Freeze the immutable T46 production lock after identity/fluid/B0/non-recycling close."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common

OUTPUT = common.PRODUCTION_LOCK


def _require_closed() -> None:
    catalog = common.load_json(common.MTE_CATALOG)
    if int(catalog.get("source_meta_count") or 0) != common.EXPECTED_MTE_METAS:
        raise ValueError("T46 lock requires 118 MTE identities")
    fluids = common.load_json(common.FLUID_MAPPING)
    if int((fluids.get("counts") or {}).get("mapped") or 0) != common.EXPECTED_FLUID_OVERLAY:
        raise ValueError("T46 lock requires 30 overlay fluids")
    recycling = common.load_json(common.RECYCLING_DISPOSITION)
    if recycling.get("recycling_candidate") is not False:
        raise ValueError("T46 lock requires non-recycling disposition")
    if int(recycling.get("family_count") or 0) != common.EXPECTED_FAMILY_COUNT:
        raise ValueError("T46 recycling disposition is not 803 families")
    support = common.load_json(common.PLAYER_PATH_SUPPORT)
    if int(support.get("oil_b1_count") or 0) != len(common.B0_FLUID_IDS):
        raise ValueError("T46 lock requires seven B0 oil B1 paths")
    identity = common.load_json(common.IDENTITY_DELTA)
    if not identity.get("records"):
        raise ValueError("T46 lock requires a filled identity delta")


def build() -> dict[str, Any]:
    _require_closed()
    work_set = common.load_json(common.WORK_SET)
    candidate = common.load_json(common.CANDIDATE_SELECTION)
    source = common.load_json(common.SOURCE)
    if candidate.get("accepted_count") != len(work_set.get("families") or []):
        raise ValueError("T46 candidate is not the full work set")
    if source.get("status") != "T46_BATH_SOURCE_FROZEN":
        raise ValueError("T46 production lock requires a frozen source")
    by_template: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source.get("relations") or []:
        by_template[str(relation["template_key"])].append(relation)
    production_families: list[dict[str, Any]] = []
    stable_ids: list[str] = []
    exact = 0
    multi = 0
    for row in work_set.get("families") or []:
        relations = sorted(
            by_template[str(row["template_key"])],
            key=lambda item: int(item.get("shadow_order") or 0),
        )
        expanded = int(row.get("expanded_count") or 0)
        if len(relations) != expanded:
            raise ValueError(f"{row['template_key']}: lock source cardinality drifted")
        representation = "exact" if expanded == 1 else "exact_multi"
        if representation == "exact":
            exact += 1
        else:
            multi += 1
        family_stable = [str(relation["stable_id"]) for relation in relations]
        consume = relations[0]["item_inputs"][0]
        runtime_id = common.assert_runtime_id(consume.get("runtime_id"), consume=True)
        production_families.append(
            {
                "b0_status": (
                    "typed_b0" if f"item:{runtime_id}" in common.b0_identities()
                    else "needs_b1_support"
                ),
                "b1_status": "real_support",
                "expanded_count": expanded,
                "family_id": row["family_id"],
                "host": row["host"],
                "mapped_cc_identity": runtime_id,
                "mapped_runtime_id": runtime_id,
                "mapping_class": consume.get("mapping") or "proven_equivalent",
                "publication_group": row["publication_group"],
                "representation": representation,
                "selection_sha256": candidate["selection_sha256"],
                "source_identity": (consume.get("source") or {}).get("item"),
                "source_template": row["template_key"],
                "stable_ids": family_stable,
                "template_key": row["template_key"],
            }
        )
        stable_ids.extend(family_stable)
    family_ids = [row["family_id"] for row in production_families]
    digest = common.selection_sha256(family_ids)
    if exact != common.EXPECTED_EXACT_FAMILIES or multi != common.EXPECTED_EXACT_MULTI_FAMILIES:
        raise ValueError(f"T46 lock representation drifted exact={exact} multi={multi}")
    if len(stable_ids) != common.EXPECTED_RELATION_COUNT:
        raise ValueError("T46 lock relation count drifted")
    if len(stable_ids) != len(set(stable_ids)):
        raise ValueError("T46 lock stable ids collided")
    support = common.load_json(common.PLAYER_PATH_SUPPORT)
    route_keys = sorted(
        {
            f"{route['source_map']}#{route.get('source_recipe', route.get('route_key', ''))}"
            for route in support.get("routes") or []
        }
    )
    return {
        "candidate_snapshot": {
            "path": common.relative(common.CANDIDATE_SELECTION),
            "selection_sha256": candidate["selection_sha256"],
        },
        "catalog_fixture": {
            "families": common.EXPECTED_FAMILY_COUNT,
            "relations": common.EXPECTED_RELATION_COUNT,
            "selection_sha256": digest,
            "status": "PRODUCTION_EQUALS_WORK_SET",
        },
        "generated_by": "python tools/build_t46_production_lock.py",
        "note": (
            "803/1517 is the production authority. Remaining Bath families and "
            "Mixer stay out. No combinatorial deferral."
        ),
        "phase_deferred": [],
        "production": {
            "families": production_families,
            "family_count": len(production_families),
            "family_ids": family_ids,
            "relation_count": len(stable_ids),
            "selection_sha256": digest,
            "stable_ids": stable_ids,
            "template_keys": [row["template_key"] for row in production_families],
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_PRODUCTION_LOCKED",
        "support": {
            "route_count": len(route_keys),
            "route_keys": route_keys,
        },
    }


def main(argv: list[str] | None = None) -> int:
    from tools import closeout_seal

    return common.run_managed(
        "Freeze the T46 production lock",
        OUTPUT,
        build=build,
        check=lambda: closeout_seal.live_or_sealed_errors(
            "T46",
            "production_lock",
            lambda: common.check_document(OUTPUT, build()),
        ),
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
