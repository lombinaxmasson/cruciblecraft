#!/usr/bin/env python3
"""Project the T43 GT stone source-object catalog from the frozen work set."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as common  # noqa: E402

OUTPUT = common.STONE_CATALOG
BUNDLED = common.BUNDLED_STONE_CATALOG


def _decode_operands(snapshot: dict[str, Any], family_ids: set[str]) -> dict[str, set[int]]:
    dictionaries = snapshot.get("dictionaries") or {}
    items = list(dictionaries.get("items") or [])
    metas_by_item: dict[str, set[int]] = defaultdict(set)
    for family in snapshot.get("families") or []:
        if str(family.get("family_id") or "") not in family_ids:
            continue
        for relation in family.get("relations") or []:
            for operand in relation.get("operands") or []:
                item_index = operand[1]
                meta = operand[3]
                if item_index is None:
                    continue
                item_id = items[item_index]
                if common.is_stone_item(item_id) and isinstance(meta, int):
                    metas_by_item[item_id].add(meta)
    return metas_by_item


def _identity_row(source_item: str, metas: list[int]) -> dict[str, Any]:
    parsed = common.parse_stone_identity(source_item)
    stone = parsed["stone"]
    variants = [
        {
            "meta": meta,
            "registry_path": common.registry_path(source_item, meta),
            "runtime_id": common.runtime_id(source_item, meta),
        }
        for meta in metas
    ]
    return {
        "chinese_name": common.STONE_CHINESE[stone],
        "english_name": common.STONE_ENGLISH[stone],
        "kind": parsed["kind"],
        "slab_variant": parsed["slab_variant"],
        "source_item": source_item,
        "source_location": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.smelter.json",
        "source_revision": common.SOURCE_REVISION,
        "stone": stone,
        "texture": common.STONE_TEXTURES[stone],
        "texture_class": "DESIGN_POLICY",
        "variant_count": len(variants),
        "variants": variants,
    }


def build_catalog(
    *,
    work_set: dict[str, Any] | None = None,
    snapshot: dict[str, Any] | None = None,
) -> dict[str, Any]:
    work_set = work_set if work_set is not None else common.load_json(common.WORK_SET)
    if work_set.get("selection_sha256") != common.EXPECTED_SELECTION_SHA256:
        raise ValueError("T43 work set selection_sha256 drifted")
    family_ids = {str(value) for value in work_set.get("family_ids") or []}
    snapshot = snapshot if snapshot is not None else common.load_json(common.T42_SNAPSHOT)
    if snapshot.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("T42 family operand snapshot source_revision drifted")
    metas_by_item = _decode_operands(snapshot, family_ids)
    if len(metas_by_item) != common.STONE_IDENTITY_COUNT:
        raise ValueError(
            f"T43 stone catalog must contain {common.STONE_IDENTITY_COUNT} source identities, "
            f"got {len(metas_by_item)}"
        )
    identities = [
        _identity_row(item_id, sorted(metas_by_item[item_id]))
        for item_id in sorted(metas_by_item)
    ]
    variant_count = sum(int(row["variant_count"]) for row in identities)
    full_count = sum(1 for row in identities if row["kind"] == "full")
    slab_count = sum(1 for row in identities if row["kind"] == "slab")
    if full_count != 17 or slab_count != 102:
        raise ValueError(
            f"T43 catalog identity split drifted: full={full_count} slab={slab_count}"
        )
    if any(row["kind"] == "slab" and row["slab_variant"] is None for row in identities):
        raise ValueError("T43 collapsed a slab identity")
    runtime_ids = [variant["runtime_id"] for row in identities for variant in row["variants"]]
    if len(runtime_ids) != len(set(runtime_ids)):
        raise ValueError("T43 catalog runtime ids collide")
    bundled = {
        "identities": identities,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T43_STONE_CATALOG",
        "variant_count": variant_count,
    }
    return {
        "acquisition": {
            "kind": "worldgen_drop",
            "note": (
                "B1 support is a bounded overworld scatter of every locked catalog "
                "variant. Creative tabs and GameTest injection are not acquisition."
            ),
            "owner": common.OWNER,
        },
        "bundled_path": common.relative(BUNDLED),
        "full_identity_count": full_count,
        "generated_by": "python tools/build_t43_stone_catalog.py",
        "identities": identities,
        "identity_count": len(identities),
        "schema_version": 1,
        "slab_identity_count": slab_count,
        "source_revision": common.SOURCE_REVISION,
        "status": "T43_STONE_CATALOG_FROZEN",
        "variant_count": variant_count,
        "work_set_selection_sha256": work_set["selection_sha256"],
        "bundled": bundled,
    }


def write() -> None:
    document = build_catalog()
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document["bundled"])


def build() -> dict[str, Any]:
    return build_catalog()


def check() -> list[str]:
    document = build_catalog()
    errors = common.check_document(OUTPUT, document)
    if not BUNDLED.is_file():
        errors.append(f"missing bundled catalog {common.relative(BUNDLED)}")
        return errors
    bundled_errors = t35.check_generated_document(BUNDLED, document["bundled"])
    errors.extend(bundled_errors)
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T43 GT stone source-object catalog",
        OUTPUT,
        build=build_catalog,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
