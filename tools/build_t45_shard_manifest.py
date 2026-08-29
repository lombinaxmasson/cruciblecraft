#!/usr/bin/env python3
"""Freeze T45 query-addressable shard membership from generated families."""
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
from tools import t45_common as common


def load_generated_families() -> list[dict[str, Any]]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(
            f"T45 generated family count drifted: {len(files)} != {expected}"
        )
    return [json.loads(path.read_text(encoding="utf-8")) for path in files]


def build() -> dict[str, Any]:
    families = load_generated_families()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    target_by_group: dict[str, str] = {}
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id not in common.PUBLICATION_GROUPS:
            raise ValueError(f"unexpected T45 publication_group: {group_id}")
        grouped[group_id].extend(family.get("relations") or [])
        target_by_group[group_id] = str(family.get("target_map") or "")
    groups = []
    for group_id in common.PUBLICATION_GROUPS:
        relations = grouped.get(group_id) or []
        target_map = target_by_group[group_id]
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
            raise ValueError(f"T45 shard exceeded hard ceiling for {group_id}")
        if groups[-1]["overflow_count"] > router.HARD_SHARD_CEILING:
            raise ValueError(f"T45 overflow shard exceeded hard ceiling for {group_id}")
    if router.ROUTING_SCHEMA_VERSION != "t39-shard-v1":
        raise ValueError("T45 shard router must stay on t39-shard-v1")
    if sum(group["relation_count"] for group in groups) != common.production_relation_count():
        raise ValueError("T45 shard membership drifted")
    document = {
        "group_count": len(groups),
        "groups": groups,
        "hard_ceiling": router.HARD_SHARD_CEILING,
        "schema_version": 1,
        "status": "T45_SHARD_MEMBERSHIP_FROZEN",
    }
    document["aggregate_root_sha256"] = hashlib.sha256(
        t35.stable_json(document).encode("utf-8")
    ).hexdigest()
    return document


def main(argv: list[str] | None = None) -> int:
    args = common.parse_write_check("Freeze T45 shard membership.", argv)
    document = build()
    if args.check:
        errors = common.check_document(common.SHARD_MANIFEST, document)
        if errors:
            print("T45 shard manifest is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T45 shard manifest is current.")
        return 0
    t35.write_stable(common.SHARD_MANIFEST, document)
    print(
        "Wrote T45 shard manifest "
        f"(groups={document['group_count']})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
