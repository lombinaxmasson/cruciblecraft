#!/usr/bin/env python3
"""Freeze T42 remaining ordinary families and source-pack identity."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t39_common as t39
from tools import t40_common as t40
from tools import t41_common as t41
from tools import t42_common as common

OUTPUT = common.PARTITION_FREEZE
CATALOG = common.REMAINING_CATALOG
PACK = common.SOURCE_PACK_MANIFEST


def build_catalog() -> dict[str, Any]:
    return common.remaining_catalog_document()


def build_pack(catalog: dict[str, Any]) -> dict[str, Any]:
    return {
        "catalog": {
            "families": catalog["family_count"],
            "ordinary_source_rows": catalog["ordinary_source_rows"],
        },
        "dump_required_for_full_replay": True,
        "files": {
            "freeze": {"path": "tools/t42_partition_freeze.json"},
            "remaining_catalog": {"path": "tools/t42_remaining_catalog.json"},
        },
        "full_replay": {
            "dump_present": common.dump_present(),
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
        "generated_by": "python tools/build_t42_partition_freeze.py",
        "note": (
            "T42 is a partition card. This pack pins remaining-family identity "
            "only; it is not a production work-set and does not publish recipes."
        ),
        "owner": common.OWNER,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_SOURCE_PACK_FROZEN",
    }


def build_freeze(catalog: dict[str, Any]) -> dict[str, Any]:
    t41_census = common.load_json(t41.CENSUS_DELTA)
    remaining = t41_census.get("remaining_ordinary") or {}
    if int(remaining.get("remaining_ordinary_families") or 0) != common.OPENING_EXECUTION_GAP:
        raise ValueError("T41 remaining_ordinary_families is not 5305")
    t41_ready = common.load_json(t41.READINESS)
    if t41_ready.get("status") != "T41_READY":
        raise ValueError("T42 requires tools/t41_readiness.json#status = T41_READY")
    if t41_ready.get("failed_gates"):
        raise ValueError("T42 requires t41_readiness.failed_gates=[]")
    roots = common.file_sha256_map(common.SEMANTIC_ROOT_PATHS)
    return {
        "combinatorial_remaining": catalog["combinatorial_remaining_family_ids"],
        "exclude_count": catalog["exclude_count"],
        "exclude_family_ids_root": catalog["exclude_family_ids_root"],
        "frozen_counts": {
            "opening_execution_gap": common.OPENING_EXECUTION_GAP,
            "t35_families": common.EXPECTED_T35_FAMILIES,
            "t35_ordinary_optional_rows": common.ORDINARY_OPTIONAL_ROWS,
            "t39_nuclear_reclassified": len(t39.phase_deferred_family_ids()),
            "t39_production": len(t39.production_family_ids()),
            "t40_production": len(t40.production_family_ids()),
            "t41_production": len(t41.production_family_ids()),
        },
        "generated_by": "python tools/build_t42_partition_freeze.py",
        "immutable_history": {
            "t35_files_rewritten": False,
            "t37_files_rewritten": False,
            "t38_files_rewritten": False,
            "t39_files_rewritten": False,
            "t40_files_rewritten": False,
            "t41_files_rewritten": False,
        },
        "note": (
            "T42 opening freeze. Bucket counts other than 5305 are R2 outputs. "
            "T40/T41 combinatorial families stay inside the remaining catalog."
        ),
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "owns_families": 0,
        "remaining_catalog_family_count": catalog["family_count"],
        "remaining_catalog_root": common.sha256_stable(
            {
                "by_host": catalog["by_host"],
                "family_ids": [row["family_id"] for row in catalog["families"]],
                "membership_roots": [
                    row["source_relation_membership_root"]
                    for row in catalog["families"]
                ],
            }
        ),
        "schema_version": 1,
        "semantic_roots": roots,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_PARTITION_FREEZE",
        "t41_remaining_opening": common.OPENING_EXECUTION_GAP,
        "t41_status": t41_ready.get("status"),
    }


def build() -> dict[str, Any]:
    catalog = build_catalog()
    return {
        "catalog": catalog,
        "freeze": build_freeze(catalog),
        "pack": build_pack(catalog),
    }


def write() -> None:
    documents = build()
    t35.write_stable(CATALOG, documents["catalog"])
    t35.write_stable(PACK, documents["pack"])
    t35.write_stable(OUTPUT, documents["freeze"])


def check() -> list[str]:
    documents = build()
    errors: list[str] = []
    errors.extend(common.check_document(CATALOG, documents["catalog"]))
    errors.extend(common.check_document(PACK, documents["pack"]))
    errors.extend(common.check_document(OUTPUT, documents["freeze"]))
    if documents["catalog"]["family_count"] != common.OPENING_EXECUTION_GAP:
        errors.append("remaining catalog family_count != 5305")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Freeze T42 remaining ordinary families", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print(
                "Wrote T42 partition freeze, remaining catalog, and source-pack "
                f"manifest ({common.OPENING_EXECUTION_GAP} families)."
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 partition freeze is current.")
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"T42 partition freeze failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
