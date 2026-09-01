#!/usr/bin/env python3
"""Emit the T49 compact-runtime group delta after the production winner."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import wave_bath_tiny_purified as common

OUTPUT = common.RUNTIME_DELTA


def _empty(note: str) -> dict[str, Any]:
    return {
        "generated_by": "python tools/build_t49_runtime_manifest_delta.py",
        "groups": [],
        "note": note,
        "order": 49,
        "schema_version": 1,
        "status": "T49_RUNTIME_MANIFEST_DELTA",
        "wave_id": "T49",
    }


def build() -> dict[str, Any]:
    if not common.PUBLICATION_GROUP_MANIFEST.is_file():
        return _empty(
            "T49 runtime group delta. Empty until publication group membership "
            "is frozen. Does not invent cache_ceiling or winner."
        )
    strategy = common.production_strategy()
    if strategy["blocked"]:
        return _empty(
            "T49 runtime group delta. Publication group manifest exists but "
            "groups stay empty until a production winner is derived. Does not "
            "invent cache_ceiling or winner."
        )
    group_manifest = t35.load_json(common.PUBLICATION_GROUP_MANIFEST)
    groups: list[dict[str, Any]] = []
    expected = set(common.production_publication_groups())
    for row in group_manifest.get("groups") or []:
        group_id = str(row["publication_group"])
        key = common.PUBLICATION_GROUP_KEYS[group_id]
        winners = strategy["group_winners"]
        winner = winners.get(group_id) or winners.get(key)
        if winner not in {"immediate", "on_demand", "hybrid"}:
            raise ValueError(f"unknown T49 winner for {group_id}: {winner}")
        policy_path = common.PUBLICATION_POLICY_DATAPACK_FILES[group_id]
        if not policy_path.is_file():
            raise ValueError(f"T49 datapack publication policy is missing for {group_id}")
        policy = t35.load_json(policy_path)
        partition = common.partition_for_winner(winner, key)
        families = int(row["family_count"])
        relations = int(row["relation_count"])
        groups.append(
            {
                "authored_family_count": families,
                "authored_relation_count": relations,
                "cache_ceiling": int(partition["lazy_cache_ceiling_rows"] or 0),
                "eager_stable_ids": list(policy.get("eager_stable_ids") or []),
                "effective_family_count": families,
                "effective_relation_count": relations,
                "membership_root_sha256": row["membership_root_sha256"],
                "policy_resource": common.relative(policy_path),
                "policy_type": winner,
                "publication_group": group_id,
                "target_map": common.TARGET_MAP,
                "wave_id": "T49",
                "winner": winner,
            }
        )
    if {row["publication_group"] for row in groups} != expected:
        raise ValueError("T49 runtime delta groups drifted from lock publication groups")
    return {
        "generated_by": "python tools/build_t49_runtime_manifest_delta.py",
        "groups": groups,
        "note": (
            "T49 runtime group delta for t49_bath_exact_multi. "
            "Historical v1/T46/T47/T48 membership roots stay unchanged."
        ),
        "order": 49,
        "schema_version": 1,
        "status": "T49_RUNTIME_MANIFEST_DELTA",
        "wave_id": "T49",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze T49 compact runtime manifest delta",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
