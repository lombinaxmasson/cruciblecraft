#!/usr/bin/env python3
"""Project the T44 production lock into the bundled storage_variants catalog."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t44_common as common  # noqa: E402

OUTPUT = common.CATALOG_LEDGER
BUNDLED = common.BUNDLED_CATALOG


def bundled_catalog(variants: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    variants = variants if variants is not None else common.expand_variants()
    common.validate_variants(variants)
    return {
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "storage_count": common.STORAGE_REGISTRATIONS,
        "logistics_count": common.LOGISTICS_REGISTRATIONS,
        "variants": variants,
    }


def build() -> dict[str, Any]:
    variants = common.expand_variants()
    return {
        "generated_by": "python tools/build_t44_storage_catalog.py",
        "schema_version": 1,
        "status": "T44_STORAGE_CATALOG_FROZEN",
        "source_revision": common.SOURCE_REVISION,
        "bundled_catalog": common.relative(BUNDLED),
        "storage_count": common.STORAGE_REGISTRATIONS,
        "logistics_count": common.LOGISTICS_REGISTRATIONS,
        "source_visible": sum(
            1 for row in variants if row["visibility"] == "source_visible"
        ),
        "source_hidden": sum(
            1 for row in variants if row["visibility"] == "source_hidden"
        ),
        "selection_sha256": common.selection_sha256(common.variant_ids(variants)),
        "bundled_sha256": t35.sha256_file(BUNDLED) if BUNDLED.is_file() else None,
        "family_counts": {
            family: expected["expanded"]
            for family, expected in common.FAMILY_COUNTS.items()
        },
        "logistics_family": {
            "family": common.LOGISTICS_FAMILY,
            "expanded": common.LOGISTICS_REGISTRATIONS,
            "counts_toward_storage_624": False,
        },
    }


def write() -> None:
    variants = common.expand_variants()
    t35.write_stable(BUNDLED, bundled_catalog(variants))
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    errors = common.check_document(OUTPUT, build())
    expected = t35.stable_json(bundled_catalog())
    if not BUNDLED.is_file():
        errors.append(f"MISSING: {common.relative(BUNDLED)}")
    elif BUNDLED.read_text(encoding="utf-8") != expected:
        errors.append(f"SEMANTIC_DRIFT: {common.relative(BUNDLED)} is stale")
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Project the T44 storage catalog",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
