#!/usr/bin/env python3
"""Freeze T43 publication-group membership from generated compact families."""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
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
    family_ids: list[str] = []
    stable_ids: list[str] = []
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id != common.STONE_GROUP:
            raise ValueError(f"unexpected T43 publication_group: {group_id}")
        family_ids.append(str(family["family_id"]))
        for relation in family.get("relations") or []:
            stable_ids.append(str(relation["stable_id"]))
    family_ids = sorted(family_ids)
    stable_ids = sorted(stable_ids)
    if len(family_ids) != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError("T43 publication group family count drifted")
    if len(stable_ids) != common.PRODUCTION_RELATION_COUNT:
        raise ValueError("T43 publication group relation count drifted")
    groups = [
        {
            "family_count": len(family_ids),
            "family_ids": family_ids,
            "publication_group": common.STONE_GROUP,
            "relation_count": len(stable_ids),
            "stable_ids": stable_ids,
            "target_map": common.TARGET_MAP,
        }
    ]
    document = {
        "group_count": 1,
        "groups": groups,
        "schema_version": 1,
        "status": "T43_PRODUCTION_PUBLICATION_GROUPS_FROZEN",
        "target_map": common.TARGET_MAP,
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
    args = common.parse_write_check(
        "Freeze T43 publication-group membership.", argv
    )
    document = build()
    if args.check:
        errors = common.check_document(common.PUBLICATION_GROUP_MANIFEST, document)
        if errors:
            print("T43 publication-group manifest is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T43 publication-group manifest is current.")
        return 0
    t35.write_stable(common.PUBLICATION_GROUP_MANIFEST, document)
    print("Wrote T43 publication-group manifest (407 families).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
