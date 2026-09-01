#!/usr/bin/env python3
"""Freeze T49 publication-group membership from generated compact families."""
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
from tools import wave_bath_tiny_purified as common

OUTPUT = common.PUBLICATION_GROUP_MANIFEST


def load_generated_families() -> list[dict[str, Any]]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(
            f"T49 generated family count drifted: {len(files)} != {expected}"
        )
    return [json.loads(path.read_text(encoding="utf-8")) for path in files]


def build() -> dict[str, Any]:
    families = load_generated_families()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    allowed = set(common.production_publication_groups())
    forbidden = {
        *common.FORBIDDEN_PUBLICATION_GROUPS,
    }
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id in forbidden:
            raise ValueError(f"T49 must not reuse historical group {group_id}")
        if group_id not in allowed:
            raise ValueError(f"unexpected T49 publication_group: {group_id}")
        grouped[group_id].append(family)
    groups = []
    for group_id in common.production_publication_groups():
        rows = grouped.get(group_id) or []
        if not rows:
            raise ValueError(f"T49 publication group {group_id} is empty")
        family_ids = sorted(str(row["family_id"]) for row in rows)
        stable_ids = sorted(
            str(relation["stable_id"])
            for row in rows
            for relation in row.get("relations") or []
        )
        groups.append({
            "family_count": len(family_ids),
            "family_ids": family_ids,
            "membership_root_sha256": common.membership_root_sha256(family_ids, stable_ids),
            "publication_group": group_id,
            "relation_count": len(stable_ids),
            "stable_ids": stable_ids,
            "target_map": common.TARGET_MAP,
        })
    if sum(group["family_count"] for group in groups) != common.production_family_count():
        raise ValueError("T49 publication group family count drifted")
    if sum(group["relation_count"] for group in groups) != common.production_relation_count():
        raise ValueError("T49 publication group relation count drifted")
    document = {
        "group_count": len(groups),
        "groups": groups,
        "schema_version": 1,
        "status": "T49_PRODUCTION_PUBLICATION_GROUPS_FROZEN",
        "production_lock": {
            "path": common.relative(common.PRODUCTION_LOCK),
            "selection_sha256": common.load_production_lock()["production"][
                "selection_sha256"
            ],
            "sha256": common.production_lock_sha256(),
        },
    }
    document["aggregate_root_sha256"] = hashlib.sha256(
        t35.stable_json(document).encode("utf-8")
    ).hexdigest()
    return document


def main(argv: list[str] | None = None) -> int:
    from tools import closeout_seal

    return common.run_managed(
        "Freeze T49 publication-group membership",
        OUTPUT,
        build=build,
        check=lambda: closeout_seal.live_or_sealed_errors(
            "T49",
            "publication_group_manifest",
            lambda: common.check_document(OUTPUT, build()),
        ),
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
