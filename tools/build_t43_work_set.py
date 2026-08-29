#!/usr/bin/env python3
"""Freeze the T43 Smelter stone work set from repaired T42 owner/blocker overlays."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as common  # noqa: E402

OUTPUT = common.WORK_SET


def build_work_set(
    *,
    owner_lock: dict[str, Any] | None = None,
    blocker: dict[str, Any] | None = None,
    families_doc: dict[str, Any] | None = None,
) -> dict[str, Any]:
    selected = common.select_t43_families(owner_lock=owner_lock, blocker=blocker)
    families_doc = families_doc if families_doc is not None else common.load_json(common.RECIPE_FAMILIES)
    if families_doc.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("t35_recipe_families source_revision drifted")
    by_id = {
        str(row.get("family_id") or ""): row
        for row in families_doc.get("families") or []
        if isinstance(row, dict)
    }
    family_ids = [str(row["family_id"]) for row in selected]
    template_keys = [str(row["template_key"]) for row in selected]
    expanded_counts: list[int] = []
    families: list[dict[str, Any]] = []
    for row in selected:
        family_id = str(row["family_id"])
        record = by_id.get(family_id)
        if record is None:
            raise ValueError(f"T43 family missing from t35 ledger: {family_id}")
        if record.get("cc_host_map") != common.HOST:
            raise ValueError(f"{family_id}: cc_host_map drifted")
        if record.get("classification") != "ordinary_optional":
            raise ValueError(f"{family_id}: classification is not ordinary_optional")
        expanded = int(record.get("expanded_count") or 0)
        if expanded != 1:
            raise ValueError(f"{family_id}: T43 does not split non-singletons")
        expanded_counts.append(expanded)
        families.append(
            {
                "catalog_group": f"cohort_{row['cohort'].lower()}",
                "cohort": row["cohort"],
                "current_owner": row["current_owner"],
                "expanded_count": expanded,
                "family_id": family_id,
                "publication_group": common.STONE_GROUP,
                "template_key": str(row["template_key"]),
            }
        )
    if len(set(family_ids)) != common.CATALOG_FAMILY_COUNT:
        raise ValueError("T43 work set has duplicate family ids")
    if len(set(template_keys)) != common.CATALOG_FAMILY_COUNT:
        raise ValueError("T43 work set has duplicate template keys")
    source_rows = sum(expanded_counts)
    if source_rows != common.CATALOG_RELATION_COUNT:
        raise ValueError(
            f"T43 expanded_count total must be {common.CATALOG_RELATION_COUNT}, got {source_rows}"
        )
    digest = common.selection_sha256(family_ids)
    distribution = common.expanded_count_distribution(expanded_counts)
    expected_distribution = {
        str(size): count for size, count in common.EXPECTED_DISTRIBUTION.items()
    }
    if distribution != expected_distribution:
        raise ValueError(f"T43 expanded_count distribution drifted: {distribution}")
    cohort_a_ids = [row["family_id"] for row in families if row["cohort"] == "A"]
    cohort_b_ids = [row["family_id"] for row in families if row["cohort"] == "B"]
    return {
        "authored_compact_family_entries": common.CATALOG_FAMILY_COUNT,
        "catalog_only": False,
        "expected_logical_relations": common.CATALOG_RELATION_COUNT,
        "expanded_count_distribution": distribution,
        "family_count": common.CATALOG_FAMILY_COUNT,
        "family_ids": family_ids,
        "families": families,
        "generated_by": "python tools/build_t43_work_set.py",
        "host": common.HOST,
        "note": (
            "407/407 is the recomputed A+B stone cohort from repaired T42 overlays. "
            "It is not a larger catalog fixture. Drying priority_intent is excluded."
        ),
        "owner": common.OWNER,
        "publication_group_count": common.PUBLICATION_GROUP_COUNT,
        "publication_groups": {
            common.STONE_GROUP: {
                "expanded_count": common.CATALOG_RELATION_COUNT,
                "family_count": common.CATALOG_FAMILY_COUNT,
                "family_ids": family_ids,
                "rule": "exact singleton compact relations",
            }
        },
        "schema_version": 1,
        "selection_rule": {
            "cohort_a": {
                "current_owner": common.COHORT_A_OWNER,
                "host": common.HOST,
                "missing_runtime_ids": "all item_inputs:gregtech:gt.stone.*",
                "second_blocker": "none; unmapped marker is allowed",
            },
            "cohort_b": {
                "current_owner": common.COHORT_B_OWNER,
                "host": common.HOST,
            },
            "exclude": [
                "hazmat/storage unmapped",
                "gt.recipe.smelter#0111 residual",
                "drying priority_intent",
            ],
            "sort": "family_id ascending",
        },
        "selection_sha256": digest,
        "source_map": common.SOURCE_MAP,
        "source_revision": common.SOURCE_REVISION,
        "source_rows": source_rows,
        "status": "T43_WORK_SET_FROZEN",
        "target_map": common.TARGET_MAP,
        "template_keys": template_keys,
        "unique_active_card": "T43",
        "cohorts": {
            "A": {
                "family_count": len(cohort_a_ids),
                "family_ids": cohort_a_ids,
            },
            "B": {
                "family_count": len(cohort_b_ids),
                "family_ids": cohort_b_ids,
            },
        },
    }


def build() -> dict[str, Any]:
    return build_work_set()


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T43 Smelter stone work set",
        OUTPUT,
        build=build_work_set,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
