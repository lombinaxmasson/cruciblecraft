#!/usr/bin/env python3
"""Record source-to-runtime semantic diffs for T44 storage profiles."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.EQUIVALENCE

PROFILES = (
    {
        "behavior_profile": "bookshelf",
        "source_class": "MultiTileEntityBookShelf",
        "positive": [
            "book, written_book, writable_book, enchanted_book insert",
            "enchant power grows with occupied valid slots",
            "contents persist across save/reload",
        ],
        "negative": [
            "non-book items are rejected",
            "breaking without loot keep_contents drops contents as items",
        ],
        "runtime_host": "BookshelfBlockEntity",
    },
    {
        "behavior_profile": "bottle_crate",
        "source_class": "MultiTileEntityBottleCrate",
        "positive": ["glass bottle and potion insert into 9 slots"],
        "negative": ["non-bottle items are rejected"],
        "runtime_host": "BottleCrateBlockEntity",
    },
    {
        "behavior_profile": "drawer",
        "source_class": "MultiTileEntityDrawerQuad",
        "positive": [
            "four compartments of 36 slots (144 total)",
            "horizontal facing is stored on the blockstate",
        ],
        "negative": ["compartment index outside 0..3 is rejected"],
        "runtime_host": "DrawerBlockEntity",
    },
    {
        "behavior_profile": "locker",
        "source_class": "MultiTileEntityLocker",
        "positive": ["front-face use swaps FEET/LEGS/CHEST/HEAD with four slots"],
        "negative": ["non-front faces do not swap armor"],
        "runtime_host": "LockerBlockEntity",
    },
    {
        "behavior_profile": "locker_charging",
        "source_class": "MultiTileEntityLockerCharging",
        "positive": [
            "retains locker swap lineage",
            "EnergyType.ELECTRIC insert charges slotted items with electric_charge",
        ],
        "negative": ["HEAT/KINETIC insert is rejected"],
        "runtime_host": "LockerBlockEntity",
    },
    {
        "behavior_profile": "mass_storage",
        "source_class": "MultiTileEntityMassStorage",
        "positive": [
            "single item type lock",
            "capacity from source NBT_CAPACITY",
            "filter retained until emptied and reset",
        ],
        "negative": [
            "mismatched item type is rejected",
            "insert beyond capacity is overflow-rejected",
        ],
        "runtime_host": "MassStorageBlockEntity",
    },
    {
        "behavior_profile": "mass_storage_logistics",
        "source_class": "MultiTileEntityMassStorageLogistics",
        "positive": [
            "same bulk store as standard",
            "exposes ILogisticsStorage with semi-filtered item priority",
        ],
        "negative": ["standard mass storage does not expose ILogisticsStorage"],
        "runtime_host": "MassStorageBlockEntity",
    },
    {
        "behavior_profile": "storage_inserter",
        "source_class": "MultiTileEntityStorageInserter",
        "positive": [
            "player use scans downward columns then 50-radius horizontal mass storage",
            "inserts matching player items into scanned mass storage",
        ],
        "negative": [
            "does not tick-transfer",
            "does not scan chests or hoppers",
        ],
        "runtime_host": "StorageInserterBlockEntity",
    },
)


def build() -> dict[str, Any]:
    variants = common.expand_variants()
    return {
        "generated_by": "python tools/build_t44_storage_equivalence.py",
        "schema_version": 1,
        "status": "T44_STORAGE_EQUIVALENCE_READY",
        "source_revision": common.SOURCE_REVISION,
        "fidelity": "source_backed",
        "profiles": list(PROFILES),
        "registration_count": len(variants),
        "storage_registrations": common.STORAGE_REGISTRATIONS,
        "logistics_registrations": common.LOGISTICS_REGISTRATIONS,
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write T44 storage equivalence evidence",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
