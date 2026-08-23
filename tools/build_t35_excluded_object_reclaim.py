#!/usr/bin/env python3
"""Build and check T35 exclusion source-site reclaim.

Consumes ``tools/t13_denominators/machine_kinds.json`` exclusions and
maps every excluded registration call site to exactly one deterministic
canonical family while preserving lineage fields needed by census.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
BUILDER = Path(__file__).resolve()
MACHINE_KINDS = TOOLS / "t13_denominators" / "machine_kinds.json"
OUTPUT = TOOLS / "t35_excluded_object_reclaim.json"

SOURCE_REVISION = common.SOURCE_REVISION

STORAGE_BEHAVIOR_FAMILIES: dict[str, str] = {
    "MultiTileEntityBookShelf": "bookshelf",
    "MultiTileEntityBottleCrate": "bottle_crate",
    "MultiTileEntityMassStorageBarrel": "mass_storage_barrel",
    "MultiTileEntityMassStorageBox": "mass_storage_box",
    "MultiTileEntityLocker": "locker",
    "MultiTileEntityLockerCharging": "locker",
    "MultiTileEntityDrawerQuad": "drawer",
    "MultiTileEntityMassStorageStandard": "mass_storage_standard",
    "MultiTileEntityStorageInserter": "storage_inserter",
    "MultiTileEntityMassStorageLogistics": "mass_storage_logistics",
}

CATEGORY_FAMILIES: dict[str, str] = {
    "Chests": "chest",
    "Safes": "safe",
    "Fluid Containers": "fluid_container",
    "Hoppers": "hopper",
    "Sorting": "sorting",
    "Pumps": "pump",
}

STORAGE_CORE_FAMILIES = (
    "bookshelf",
    "bottle_crate",
    "mass_storage_barrel",
    "mass_storage_box",
    "locker",
    "drawer",
    "mass_storage_standard",
    "storage_inserter",
)

EXPECTED = {
    "source_sites": 763,
    "expanded_multiplicity": 1701,
    "storage_source_sites": 28,
    "storage_expanded_multiplicity": 624,
    "mass_storage_logistics_source_sites": 1,
    "mass_storage_logistics_expanded_multiplicity": 1,
}

STORAGE_FAMILY_COUNTS = {
    "bookshelf": (4, 301),
    "bottle_crate": (2, 301),
    "mass_storage_barrel": (13, 13),
    "mass_storage_box": (4, 4),
    "locker": (2, 2),
    "drawer": (1, 1),
    "mass_storage_standard": (1, 1),
    "storage_inserter": (1, 1),
}


def _camel_to_snake(name: str) -> str:
    first = re.sub("(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub("([a-z0-9])([A-Z])", r"\1_\2", first).lower()


def _slug_category(category: str) -> str:
    return category.lower().replace(" ", "_").replace("-", "_")


def canonical_family(row: dict[str, Any]) -> str:
    behavior = str(row.get("behavior_class") or "")
    category = str(row.get("category") or "")
    reason = str(row.get("exclusion_reason") or "")

    mapped = STORAGE_BEHAVIOR_FAMILIES.get(behavior)
    if mapped is not None:
        return mapped

    mapped = CATEGORY_FAMILIES.get(category)
    if mapped is not None:
        return mapped

    if "Tank" in behavior:
        return "tank"

    if reason == "compatibility_wrapper":
        return f"compat/{_camel_to_snake(behavior)}"

    if reason == "multiblock_owned_by_t13d":
        return f"multiblock/{_camel_to_snake(behavior)}"

    if reason == "reactor_part_not_machine_behavior":
        return f"reactor/{_camel_to_snake(behavior)}"

    return f"{_slug_category(category)}/{_camel_to_snake(behavior)}"


def _site_key(row: dict[str, Any]) -> str:
    identity = row.get("source_identity") or {}
    key = identity.get("normalized_row_key")
    if not isinstance(key, str) or not key.strip():
        raise ValueError("exclusion row missing source_identity.normalized_row_key")
    return key


def _site_record(row: dict[str, Any]) -> dict[str, Any]:
    identity = row.get("source_identity")
    if not isinstance(identity, dict):
        raise ValueError("exclusion row missing source_identity")
    record = {
        "site_key": _site_key(row),
        "category": row.get("category"),
        "behavior_class": row.get("behavior_class"),
        "display_expression": row.get("display_expression"),
        "source_id_expression": row.get("source_id_expression"),
        "multiplicity": row.get("multiplicity"),
        "source_identity": dict(identity),
        "historical_exclusion_reason": row.get("exclusion_reason"),
        "canonical_family": canonical_family(row),
    }
    source_line = row.get("source_line")
    if source_line is not None:
        record["source_line"] = source_line
    return record


def build() -> dict[str, Any]:
    document = common.load_json(MACHINE_KINDS)
    exclusions = document.get("exclusions")
    if not isinstance(exclusions, list):
        raise ValueError("machine_kinds.exclusions must be a list")

    sites: list[dict[str, Any]] = []
    seen_keys: set[str] = set()
    family_sites: Counter[str] = Counter()
    family_multiplicity: Counter[str] = Counter()
    family_reasons: dict[str, set[str]] = defaultdict(set)
    family_categories: dict[str, set[str]] = defaultdict(set)
    category_sites: Counter[str] = Counter()
    category_multiplicity: Counter[str] = Counter()

    for row in exclusions:
        if not isinstance(row, dict):
            raise ValueError("exclusion row must be an object")
        site = _site_record(row)
        key = site["site_key"]
        if key in seen_keys:
            raise ValueError(f"duplicate exclusion source site: {key}")
        seen_keys.add(key)
        sites.append(site)

        family = site["canonical_family"]
        multiplicity = int(site["multiplicity"])
        family_sites[family] += 1
        family_multiplicity[family] += multiplicity
        family_reasons[family].add(str(site["historical_exclusion_reason"]))
        family_categories[family].add(str(site["category"]))
        category = str(site["category"])
        category_sites[category] += 1
        category_multiplicity[category] += multiplicity

    sites.sort(key=lambda item: item["site_key"])

    source_sites = len(sites)
    expanded = sum(int(item["multiplicity"]) for item in sites)
    if source_sites != EXPECTED["source_sites"]:
        raise ValueError(
            f"source_sites={source_sites}, expected {EXPECTED['source_sites']}"
        )
    if expanded != EXPECTED["expanded_multiplicity"]:
        raise ValueError(
            f"expanded_multiplicity={expanded}, "
            f"expected {EXPECTED['expanded_multiplicity']}"
        )

    storage_sites = [
        item for item in sites if item["category"] == "Storage"
    ]
    storage_site_count = len(storage_sites)
    storage_multiplicity = sum(int(item["multiplicity"]) for item in storage_sites)
    if storage_site_count != EXPECTED["storage_source_sites"]:
        raise ValueError(
            f"storage_source_sites={storage_site_count}, "
            f"expected {EXPECTED['storage_source_sites']}"
        )
    if storage_multiplicity != EXPECTED["storage_expanded_multiplicity"]:
        raise ValueError(
            f"storage_expanded_multiplicity={storage_multiplicity}, "
            f"expected {EXPECTED['storage_expanded_multiplicity']}"
        )

    logistics = [
        item
        for item in sites
        if item["canonical_family"] == "mass_storage_logistics"
    ]
    logistics_sites = len(logistics)
    logistics_multiplicity = sum(int(item["multiplicity"]) for item in logistics)
    if logistics_sites != EXPECTED["mass_storage_logistics_source_sites"]:
        raise ValueError("mass_storage_logistics source site count drifted")
    if logistics_multiplicity != EXPECTED["mass_storage_logistics_expanded_multiplicity"]:
        raise ValueError("mass_storage_logistics multiplicity drifted")

    storage_families = {
        name: {
            "source_sites": family_sites[name],
            "expanded_multiplicity": family_multiplicity[name],
        }
        for name in STORAGE_CORE_FAMILIES
    }
    for name, (sites_expected, mult_expected) in STORAGE_FAMILY_COUNTS.items():
        actual = storage_families[name]
        if actual["source_sites"] != sites_expected:
            raise ValueError(
                f"{name} source_sites={actual['source_sites']} != {sites_expected}"
            )
        if actual["expanded_multiplicity"] != mult_expected:
            raise ValueError(
                f"{name} expanded={actual['expanded_multiplicity']} "
                f"!= {mult_expected}"
            )

    family_summaries = [
        {
            "canonical_family": family,
            "categories": sorted(family_categories[family]),
            "source_sites": family_sites[family],
            "expanded_multiplicity": family_multiplicity[family],
            "historical_exclusion_reasons": sorted(family_reasons[family]),
        }
        for family in sorted(family_sites)
    ]
    category_summaries = [
        {
            "category": category,
            "source_sites": category_sites[category],
            "expanded_multiplicity": category_multiplicity[category],
        }
        for category in sorted(category_sites)
    ]

    return {
        "schema_version": 1,
        "status": "T35_EXCLUSION_RECLAIM_READY",
        "source_revision": SOURCE_REVISION,
        "generated_by": "python tools/build_t35_excluded_object_reclaim.py",
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(MACHINE_KINDS): common.sha256_file(MACHINE_KINDS),
            }
        },
        "counts": {
            "source_sites": source_sites,
            "expanded_multiplicity": expanded,
            "unmapped": 0,
            "duplicate_family_membership": 0,
            "storage_source_sites": storage_site_count,
            "storage_expanded_multiplicity": storage_multiplicity,
            "mass_storage_logistics_source_sites": logistics_sites,
            "mass_storage_logistics_expanded_multiplicity": logistics_multiplicity,
            "canonical_families": len(family_sites),
        },
        "storage_families": storage_families,
        "mass_storage_logistics": {
            "canonical_family": "mass_storage_logistics",
            "category": "Logistics",
            "source_sites": logistics_sites,
            "expanded_multiplicity": logistics_multiplicity,
            "counts_toward_storage_624": False,
        },
        "family_summaries": family_summaries,
        "category_summaries": category_summaries,
        "source_sites": sites,
        "source_hashes": {
            common.relative(MACHINE_KINDS): common.sha256_file(MACHINE_KINDS),
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {common.relative(OUTPUT)}")
        return errors
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(f"{common.relative(OUTPUT)} is stale")
        return errors

    document = common.load_json(OUTPUT)
    counts = document.get("counts") or {}
    for key, value in EXPECTED.items():
        if counts.get(key) != value:
            errors.append(f"counts.{key} != {value} (got {counts.get(key)})")
    if counts.get("unmapped") != 0:
        errors.append("counts.unmapped != 0")
    if counts.get("duplicate_family_membership") != 0:
        errors.append("counts.duplicate_family_membership != 0")

    sites = document.get("source_sites") or []
    if len(sites) != EXPECTED["source_sites"]:
        errors.append("source_sites length mismatch")
    keys = [item.get("site_key") for item in sites]
    if len(keys) != len(set(keys)):
        errors.append("duplicate source site keys on disk")

    families = [item.get("canonical_family") for item in sites]
    if len(families) != len(sites):
        errors.append("missing canonical_family on a source site")

    storage_rows = [item for item in sites if item.get("category") == "Storage"]
    storage_family_set = {item["canonical_family"] for item in storage_rows}
    if storage_family_set != set(STORAGE_CORE_FAMILIES):
        errors.append(
            f"storage family set mismatch: {sorted(storage_family_set)}"
        )

    on_disk_hashes = document.get("source_hashes") or {}
    if on_disk_hashes.get(common.relative(MACHINE_KINDS)) != common.sha256_file(
        MACHINE_KINDS
    ):
        errors.append("source_hashes.machine_kinds is stale")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    selected = [flag for flag in (args.write, args.check) if flag]
    if len(selected) != 1:
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
        else:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 exclusion reclaim failed: {error}", file=sys.stderr)
        return 1
    print(json.dumps({"counts": document.get("counts")}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
