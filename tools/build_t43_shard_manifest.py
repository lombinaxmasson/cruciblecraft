#!/usr/bin/env python3
"""Freeze T43 query-addressable shard membership from generated families."""
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

from tools import t35_common as t35  # noqa: E402
from tools import t41_shard_router as router  # noqa: E402
from tools import t43_common as common  # noqa: E402


def load_generated_families() -> list[dict[str, Any]]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(
            f"T43 generated family count drifted: {len(files)} != {expected}"
        )
    return [json.loads(path.read_text(encoding="utf-8")) for path in files]


def build() -> dict[str, Any]:
    families = load_generated_families()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id != common.STONE_GROUP:
            raise ValueError(f"unexpected T43 publication_group: {group_id}")
        if str(family.get("target_map") or "") != common.TARGET_MAP:
            raise ValueError(f"T43 generated target_map drifted: {family.get('family_id')}")
        grouped[group_id].extend(family.get("relations") or [])

    relations = grouped.get(common.STONE_GROUP) or []
    routed = router.route_group(common.TARGET_MAP, common.STONE_GROUP, relations)
    groups = [{
        "overflow_count": routed["overflow_count"],
        "overflow_shard_id": routed["overflow_shard_id"],
        "publication_group": common.STONE_GROUP,
        "relation_count": len(routed["relations"]),
        "relations": routed["relations"],
        "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
        "shard_count": routed["shard_count"],
        "shards": routed["shards"],
        "target_map": common.TARGET_MAP,
        "worst_shard_size": routed["worst_shard_size"],
    }]
    if len(routed["relations"]) != common.PRODUCTION_RELATION_COUNT:
        raise ValueError(
            f"T43 shard membership drifted: {len(routed['relations'])} != "
            f"{common.PRODUCTION_RELATION_COUNT}"
        )
    if router.ROUTING_SCHEMA_VERSION != "t39-shard-v1":
        raise ValueError("T43 shard router must stay on t39-shard-v1")
    if groups[0]["worst_shard_size"] > router.HARD_SHARD_CEILING:
        raise ValueError("T43 shard exceeded hard ceiling")
    if groups[0]["overflow_count"] > router.HARD_SHARD_CEILING:
        raise ValueError("T43 overflow shard exceeded hard ceiling")
    document = {
        "group_count": 1,
        "groups": groups,
        "hard_ceiling": router.HARD_SHARD_CEILING,
        "schema_version": 1,
        "status": "T43_SHARD_MEMBERSHIP_FROZEN",
        "target_map": common.TARGET_MAP,
    }
    document["aggregate_root_sha256"] = hashlib.sha256(
        t35.stable_json(document).encode("utf-8")
    ).hexdigest()
    return document


def main(argv: list[str] | None = None) -> int:
    args = common.parse_write_check("Freeze T43 shard membership.", argv)
    document = build()
    if args.check:
        errors = common.check_document(common.SHARD_MANIFEST, document)
        if errors:
            print("T43 shard manifest is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T43 shard manifest is current.")
        return 0
    t35.write_stable(common.SHARD_MANIFEST, document)
    print(
        "Wrote T43 shard manifest "
        f"(worst={document['groups'][0]['worst_shard_size']}, "
        f"overflow={document['groups'][0]['overflow_count']})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
