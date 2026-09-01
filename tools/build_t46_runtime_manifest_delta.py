#!/usr/bin/env python3
"""Freeze the T46 compact-runtime group delta after publication policy exists."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common

OUTPUT = common.RUNTIME_DELTA


def build() -> dict[str, Any]:
    strategy = common.production_strategy()
    if strategy["blocked"]:
        return {
            "generated_by": "python tools/build_t46_runtime_manifest_delta.py",
            "groups": [],
            "note": (
                "T46 runtime group delta. Filled after publication group membership "
                "is frozen and the production winner is derived."
            ),
            "order": 46,
            "schema_version": 1,
            "status": "T46_RUNTIME_MANIFEST_DELTA",
            "wave_id": "T46",
        }
    group_manifest = t35.load_json(common.PUBLICATION_GROUP_MANIFEST)
    row = next(
        (
            item
            for item in group_manifest.get("groups") or []
            if item.get("publication_group") == common.PUBLICATION_GROUP
        ),
        None,
    )
    if row is None:
        raise ValueError("T46 publication group manifest is missing t46_bath_mte")
    winner = strategy["group_winners"]["bath_mte"]
    partition = common.partition_for_winner(winner, "bath_mte")
    policy_path = common.PUBLICATION_POLICY_DATAPACK_FILES[common.PUBLICATION_GROUP]
    if not policy_path.is_file():
        raise ValueError("T46 datapack publication policy is missing")
    policy = t35.load_json(policy_path)
    families = int(row["family_count"])
    relations = int(row["relation_count"])
    group = {
        "authored_family_count": families,
        "authored_relation_count": relations,
        "cache_ceiling": int(partition["lazy_cache_ceiling_rows"] or 0),
        "eager_stable_ids": list(policy.get("eager_stable_ids") or []),
        "effective_family_count": families,
        "effective_relation_count": relations,
        "membership_root_sha256": row["membership_root_sha256"],
        "policy_resource": common.relative(policy_path),
        "policy_type": winner,
        "publication_group": common.PUBLICATION_GROUP,
        "target_map": common.TARGET_MAP,
        "wave_id": "T46",
        "winner": winner,
    }
    return {
        "generated_by": "python tools/build_t46_runtime_manifest_delta.py",
        "groups": [group],
        "note": (
            "T46 runtime group delta for cruciblecraft:t46_bath_mte. Historical "
            "twelve-group v1 membership roots stay unchanged."
        ),
        "order": 46,
        "schema_version": 1,
        "status": "T46_RUNTIME_MANIFEST_DELTA",
        "wave_id": "T46",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze T46 compact runtime manifest delta",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
