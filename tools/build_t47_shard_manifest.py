#!/usr/bin/env python3
"""Freeze T47 query-addressable shard membership from generated families."""
from __future__ import annotations

import hashlib
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t41_shard_router as router
from tools import t47_common as common

OUTPUT = common.SHARD_MANIFEST


def load_generated_families() -> list[dict[str, Any]]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(
            f"T47 generated family count drifted: {len(files)} != {expected}"
        )
    return [json.loads(path.read_text(encoding="utf-8")) for path in files]


def build() -> dict[str, Any]:
    families = load_generated_families()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    target_by_group: dict[str, str] = {}
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id not in common.PUBLICATION_GROUPS:
            raise ValueError(f"unexpected T47 publication_group: {group_id}")
        grouped[group_id].extend(family.get("relations") or [])
        target_by_group[group_id] = str(family.get("target_map") or "")
    groups = []
    for group_id in common.PUBLICATION_GROUPS:
        relations = grouped.get(group_id) or []
        target_map = target_by_group.get(group_id) or common.TARGET_MAP
        routed = router.route_group(target_map, group_id, relations)
        groups.append({
            "overflow_count": routed["overflow_count"],
            "overflow_shard_id": routed["overflow_shard_id"],
            "publication_group": group_id,
            "relation_count": len(routed["relations"]),
            "relations": routed["relations"],
            "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
            "shard_count": routed["shard_count"],
            "shards": routed["shards"],
            "target_map": target_map,
            "worst_shard_size": routed["worst_shard_size"],
        })
        if groups[-1]["worst_shard_size"] > router.HARD_SHARD_CEILING:
            raise ValueError(f"T47 shard exceeded hard ceiling for {group_id}")
        if groups[-1]["overflow_count"] != 0:
            raise ValueError(f"T47 overflow_count must be 0 for {group_id}")
    if router.ROUTING_SCHEMA_VERSION != "t39-shard-v1":
        raise ValueError("T47 shard router must stay on t39-shard-v1")
    if sum(group["relation_count"] for group in groups) != common.production_relation_count():
        raise ValueError("T47 shard membership drifted")
    document = {
        "group_count": len(groups),
        "groups": groups,
        "hard_ceiling": router.HARD_SHARD_CEILING,
        "schema_version": 1,
        "status": "T47_SHARD_MEMBERSHIP_FROZEN",
    }
    document["aggregate_root_sha256"] = hashlib.sha256(
        t35.stable_json(document).encode("utf-8")
    ).hexdigest()
    return document


def main(argv: list[str] | None = None) -> int:
    from tools import closeout_seal

    return common.run_managed(
        "Freeze T47 shard membership",
        OUTPUT,
        build=build,
        check=lambda: closeout_seal.live_or_sealed_errors(
            "T47",
            "shard_manifest",
            lambda: common.check_document(OUTPUT, build()),
        ),
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
