#!/usr/bin/env python3
"""In-place GT6 MTE runtime: dummy modern ids become live BlockItems.

Shared across sequential unique-active family children. Does not rewrite R0
or the connector baseline ledger. Source-exact obtain lives on
content/gt6-mte-inplace-acquisition; remaining D0 gaps stay blocked.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import catalog_modern_ids as modern
from tools import census_common as census
from tools import io_common as io

GT6_REVISION = io.SOURCE_REVISION
STATUS = "MTE_INPLACE_RUNTIME_READY"
DATA = census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
DATA_CATALOG = DATA / "smelter_mte_identity_catalog.json"
TOOLS_CATALOG = census.TOOLS / "smelter_mte_identity_catalog.json"
INPLACE_CATALOG = DATA / "mte_inplace_catalog.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
LEDGER = (
    census.TOOLS
    / "waves"
    / "content"
    / "gt6-pipe-cable-baseline"
    / "identity_resolution_ledger.json"
)
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
ITEM_MODELS = ASSETS / "models" / "item"
BLOCK_MODELS = ASSETS / "models" / "block"
BLOCKSTATES = ASSETS / "blockstates"
LOOT = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "loot_table"
    / "blocks"
)
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_TEX = Path("src/main/resources/assets/gregtech/textures/blocks")
DEST_ROOT = "assets/cruciblecraft/textures/block/gt6_import/mte"
CORE_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleCraftGameTests.java"
)
EMPTY_SRC = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_fluid_pipe_runtime"
    / "structure"
    / "empty.nbt"
)
ARCHIVE = {
    "dummy_item": "unregister CatalogNamedItem at the same modern id",
    "dummy_model": "replace iron_ingot item model with GT6-imported block model",
    "world_stacks": (
        "same registry path becomes the BlockItem; no NeoForge alias"
    ),
    "recipes": "operands keep the dummy modern id, now a live BlockItem",
    "obtain": "explicitly_blocked until a later acquisition child",
}

CLASS_KIND = (
    ("FluidCapNozzle", "CAP_NOZZLE"),
    ("FluidNozzle", "NOZZLE"),
    ("FluidFunnel", "FUNNEL"),
    ("FluidTap", "TAP"),
    ("Faucet", "FAUCET"),
    ("MultiTileEntityBridge", "TANK_BRIDGE"),
    ("MultiTileEntityExtender", "TANK_EXTENDER"),
    ("TurbineSteam", "STEAM_TURBINE"),
    ("Battery", "BATTERY_BOX"),
    ("PanelWood", "WOOD_PANEL"),
    ("Rope", "ROPE"),
    ("AdvancedCraftingTable", "CRAFTING_TABLE"),
    ("BookShelf", "BOOKSHELF"),
    ("BottleCrate", "BOTTLE_CRATE"),
    ("DrawerQuad", "DRAWER"),
    ("Locker", "LOCKER"),
    ("MassStorageBarrel", "BARREL"),
    ("MassStorageStandard", "MASS_STORAGE"),
    ("SafeKeyLocked", "SAFE"),
    ("SafeMechanical", "SAFE"),
    ("Scaffold", "SCAFFOLD"),
    ("MultiTileEntityChest", "CHEST"),
    ("MultiTileEntityAxle", "AXLE"),
    ("GearBox", "GEARBOX"),
    ("EngineRotation", "ROTATION_ENGINE"),
    ("TransformerRotation", "ROTATION_TRANSFORMER"),
)

DOMAINS: dict[str, dict[str, Any]] = {
    "attachments": {
        "slug": "content/gt6-mte-fluid-attachments-runtime",
        "family": "fluid_attachment",
        "expected": 33,
        "tests": [
            "stainlessTapIsLiveAttachment",
            "stoneFaucetIsLiveBlockNotCover",
            "stoneFaucetPoursIntoTankBelow",
        ],
        "plan_stem": "GT6流体附件runtime详细计划.md",
        "game_tests": "MteFluidAttachmentsRuntimeGameTests.java",
        "collision_reason": "in-place fluid attachment BlockItem",
        "depends_on": [
            "registry/catalog-modern-ids",
            "content/gt6-fluid-pipe-runtime",
        ],
        "lock_note": (
            "33 fluid attachments are live face-placed BlockItems; obtain "
            "explicitly_blocked; not player_complete"
        ),
    },
    "extender": {
        "slug": "content/gt6-mte-extender-runtime",
        "family": "extender",
        "expected": 2,
        "tests": [
            "tankBridgeIsLiveExtender",
            "tankExtenderDelegatesFluid",
        ],
        "plan_stem": "GT6扩展器runtime详细计划.md",
        "game_tests": "MteExtenderRuntimeGameTests.java",
        "collision_reason": "in-place extender BlockItem",
        "depends_on": [
            "registry/catalog-modern-ids",
            "content/gt6-fluid-pipe-runtime",
        ],
        "lock_note": (
            "tank extender and tank bridge are live BlockItems; not covers; "
            "not player_complete"
        ),
    },
    "converter_remainder": {
        "slug": "content/gt6-mte-converter-remainder-runtime",
        "family": "energy_converter",
        "expected": 8,
        "tests": [
            "luvBatteryBoxIsNotDummy",
            "steamTurbineConsumesSteam",
        ],
        "plan_stem": "GT6能源转换器余量runtime详细计划.md",
        "game_tests": "MteConverterRemainderRuntimeGameTests.java",
        "collision_reason": "in-place converter remainder BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "6 steam turbines and 2 battery boxes are live BlockItems; not "
            "folded onto existing converter hosts; not player_complete"
        ),
    },
    "decorative": {
        "slug": "content/gt6-mte-decorative-runtime",
        "family": "decorative",
        "expected": 24,
        "tests": [
            "steelRopeIsNotVanillaLead",
            "woodPanelIsLiveBlock",
        ],
        "plan_stem": "GT6装饰件runtime详细计划.md",
        "game_tests": "MteDecorativeRuntimeGameTests.java",
        "collision_reason": "in-place decorative BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "23 wood panels and 1 rope are live BlockItems; not vanilla "
            "planks or lead; not player_complete"
        ),
    },
    "furniture_chest": {
        "slug": "content/gt6-mte-furniture-chest-runtime",
        "family": "furniture_storage",
        "class_contains": "MultiTileEntityChest",
        "expected": 101,
        "tests": ["leadChestIsLiveInventory"],
        "plan_stem": "GT6家具箱子runtime详细计划.md",
        "game_tests": "MteFurnitureChestRuntimeGameTests.java",
        "collision_reason": "in-place chest BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": "101 GT6 chests are live BlockItems; not player_complete",
    },
    "furniture_safe": {
        "slug": "content/gt6-mte-furniture-safe-runtime",
        "family": "furniture_storage",
        "class_contains": "Safe",
        "expected": 100,
        "tests": ["leadMechanicalSafeIsLiveInventory"],
        "plan_stem": "GT6家具保险箱runtime详细计划.md",
        "game_tests": "MteFurnitureSafeRuntimeGameTests.java",
        "collision_reason": "in-place safe BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": "100 GT6 safes are live BlockItems; not player_complete",
    },
    "furniture_table": {
        "slug": "content/gt6-mte-furniture-table-runtime",
        "family": "furniture_storage",
        "class_contains": "AdvancedCraftingTable",
        "expected": 50,
        "tests": ["leadCraftingTableIsLiveBlock"],
        "plan_stem": "GT6家具工作台runtime详细计划.md",
        "game_tests": "MteFurnitureTableRuntimeGameTests.java",
        "collision_reason": "in-place crafting table BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "50 advanced crafting tables are live BlockItems; not player_complete"
        ),
    },
    "furniture_scaffold": {
        "slug": "content/gt6-mte-furniture-scaffold-runtime",
        "family": "furniture_storage",
        "class_contains": "Scaffold",
        "expected": 50,
        "tests": ["leadScaffoldIsLiveBlock"],
        "plan_stem": "GT6家具脚手架runtime详细计划.md",
        "game_tests": "MteFurnitureScaffoldRuntimeGameTests.java",
        "collision_reason": "in-place scaffold BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": "50 GT6 scaffolds are live BlockItems; not player_complete",
    },
    "furniture_barrel": {
        "slug": "content/gt6-mte-furniture-barrel-runtime",
        "family": "furniture_storage",
        "class_contains": "MassStorageBarrel",
        "expected": 12,
        "tests": ["skyrootBarrelIsLiveInventory"],
        "plan_stem": "GT6家具木桶runtime详细计划.md",
        "game_tests": "MteFurnitureBarrelRuntimeGameTests.java",
        "collision_reason": "in-place barrel BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": "12 mass-storage barrels are live BlockItems; not player_complete",
    },
    "furniture_storage": {
        "slug": "content/gt6-mte-furniture-storage-runtime",
        "family": "furniture_storage",
        "class_contains": (
            "BookShelf|BottleCrate|DrawerQuad|Locker|MassStorageStandard"
        ),
        "expected": 250,
        "tests": ["leadBookshelfIsLiveInventory"],
        "plan_stem": "GT6家具储物runtime详细计划.md",
        "game_tests": "MteFurnitureStorageRuntimeGameTests.java",
        "collision_reason": "in-place storage furniture BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "bookshelf/crate/drawer/locker/mass-storage dummies are live "
            "BlockItems; storage_variants material mismatch forbade fold; "
            "not player_complete"
        ),
    },
    "drive": {
        "slug": "content/gt6-mte-drive-runtime",
        "family": "drive",
        "expected": 63,
        "tests": [
            "axleIsNotKuRotationalAxle",
            "drivePushesKineticToAdjacentDrive",
        ],
        "plan_stem": "GT6传动件runtime详细计划.md",
        "game_tests": "MteDriveRuntimeGameTests.java",
        "collision_reason": "in-place GT6 drive BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "63 GT6 axles/gearboxes/engines are live BlockItems and are not "
            "KU rotational_axle; not player_complete"
        ),
    },
    "misc_tool": {
        "slug": "content/gt6-mte-misc-tool-runtime",
        "family": "misc_tool",
        "expected": 39,
        "tests": ["gt6AnvilIsNotVanillaAnvil"],
        "plan_stem": "GT6杂项工具runtime详细计划.md",
        "game_tests": "MteMiscToolRuntimeGameTests.java",
        "collision_reason": "in-place misc tool BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "39 misc tool MTEs are live BlockItems; not vanilla anvil/"
            "grindstone; not player_complete"
        ),
    },
    "multiblock": {
        "slug": "content/gt6-mte-multiblock-runtime",
        "family": "multiblock",
        "expected": 74,
        "tests": ["gt6CokeOvenMteIsNotNamedCokeOven"],
        "plan_stem": "GT6多方块设备runtime详细计划.md",
        "game_tests": "MteMultiblockRuntimeGameTests.java",
        "collision_reason": "in-place GT6 multiblock MTE BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "74 GT6 multiblock MTEs are live BlockItems; not named CC "
            "controllers; not player_complete"
        ),
    },
    "crucible_foundry": {
        "slug": "content/gt6-mte-crucible-foundry-runtime",
        "family": "crucible_foundry",
        "expected": 85,
        "tests": ["foundryCrucibleIsNotCeramicCrucible"],
        "plan_stem": "GT6坩埚铸造runtime详细计划.md",
        "game_tests": "MteCrucibleFoundryRuntimeGameTests.java",
        "collision_reason": "in-place foundry BlockItem",
        "depends_on": ["registry/catalog-modern-ids"],
        "lock_note": (
            "85 foundry MTEs are live BlockItems; not the ceramic crucible; "
            "not player_complete"
        ),
    },
}

ISSUE: dict[str, dict[str, Any]] = {
    "converter_remainder": {
        "title": "GT6 Converter Remainder Runtime",
        "semantic_key": "mte:converter-remainder:in-place",
        "sample_ids": ["steam/turbine_bronze", "boxwood/battery_luv"],
        "reason": (
            "6 steam turbines and 2 battery boxes keep dummy modern ids as live "
            "BlockItems. They are not folded onto existing converter hosts."
        ),
    },
    "decorative": {
        "title": "GT6 Decorative Runtime",
        "semantic_key": "mte:decorative:in-place",
        "sample_ids": ["steel/rope", "panel/wood_0"],
        "reason": (
            "Wood panels and steel rope keep dummy modern ids as live BlockItems. "
            "They are not vanilla planks or lead."
        ),
        "checks": [
            {
                "name": "steelRopeIsNotVanillaLead",
                "path": "steel/rope",
                "kind": "ROPE",
                "forbid": "vanilla_lead",
            },
            {
                "name": "woodPanelIsLiveBlock",
                "path": "panel/wood_0",
                "kind": "WOOD_PANEL",
            },
        ],
    },
    "furniture_chest": {
        "title": "GT6 Furniture Chest Runtime",
        "semantic_key": "mte:furniture-chest:in-place",
        "sample_ids": ["lead/chest"],
        "reason": "GT6 chests keep dummy modern ids as live inventory BlockItems.",
        "checks": [
            {
                "name": "leadChestIsLiveInventory",
                "path": "lead/chest",
                "kind": "CHEST",
                "forbid": "inventory",
            }
        ],
    },
    "furniture_safe": {
        "title": "GT6 Furniture Safe Runtime",
        "semantic_key": "mte:furniture-safe:in-place",
        "sample_ids": ["safe/mechanical_lead_safe"],
        "reason": "GT6 safes keep dummy modern ids as live inventory BlockItems.",
        "checks": [
            {
                "name": "leadMechanicalSafeIsLiveInventory",
                "path": "safe/mechanical_lead_safe",
                "kind": "SAFE",
                "forbid": "inventory",
            }
        ],
    },
    "furniture_table": {
        "title": "GT6 Furniture Crafting Table Runtime",
        "semantic_key": "mte:furniture-table:in-place",
        "sample_ids": ["furniture/advanced_crafting_table_lead"],
        "reason": (
            "Advanced crafting tables keep dummy modern ids as live BlockItems."
        ),
        "checks": [
            {
                "name": "leadCraftingTableIsLiveBlock",
                "path": "furniture/advanced_crafting_table_lead",
                "kind": "CRAFTING_TABLE",
                "forbid": "inventory",
            }
        ],
    },
    "furniture_scaffold": {
        "title": "GT6 Furniture Scaffold Runtime",
        "semantic_key": "mte:furniture-scaffold:in-place",
        "sample_ids": ["lead/scaffold"],
        "reason": "GT6 scaffolds keep dummy modern ids as live BlockItems.",
        "checks": [
            {
                "name": "leadScaffoldIsLiveBlock",
                "path": "lead/scaffold",
                "kind": "SCAFFOLD",
            }
        ],
    },
    "furniture_barrel": {
        "title": "GT6 Furniture Barrel Runtime",
        "semantic_key": "mte:furniture-barrel:in-place",
        "sample_ids": ["skyroot/item_barrel"],
        "reason": "Mass-storage barrels keep dummy modern ids as live BlockItems.",
        "checks": [
            {
                "name": "skyrootBarrelIsLiveInventory",
                "path": "skyroot/item_barrel",
                "kind": "BARREL",
                "forbid": "inventory",
            }
        ],
    },
    "furniture_storage": {
        "title": "GT6 Furniture Storage Runtime",
        "semantic_key": "mte:furniture-storage:in-place",
        "sample_ids": ["furniture/bookshelf_lead"],
        "reason": (
            "Bookshelf/crate/drawer/locker/mass-storage dummies stay live "
            "BlockItems because storage_variants material mismatch forbade fold."
        ),
        "checks": [
            {
                "name": "leadBookshelfIsLiveInventory",
                "path": "furniture/bookshelf_lead",
                "kind": "BOOKSHELF",
                "forbid": "inventory",
            }
        ],
    },
    "drive": {
        "title": "GT6 Drive Runtime",
        "semantic_key": "mte:drive:in-place",
        "sample_ids": ["drive/small_brass_axle"],
        "reason": (
            "GT6 axles, gearboxes, and rotation engines keep dummy modern ids as "
            "live BlockItems. They are not KU rotational_axle."
        ),
        "checks": [
            {
                "name": "axleIsNotKuRotationalAxle",
                "path": "drive/small_brass_axle",
                "kind": "AXLE",
                "forbid": "ku_axle",
            },
            {
                "name": "drivePushesKineticToAdjacentDrive",
                "path": "drive/small_brass_axle",
                "kind": "AXLE",
                "forbid": "drive_push",
            },
        ],
    },
    "misc_tool": {
        "title": "GT6 Misc Tool Runtime",
        "semantic_key": "mte:misc-tool:in-place",
        "sample_ids": ["stone/anvil"],
        "reason": (
            "Misc tool MTEs keep dummy modern ids as live BlockItems. They are "
            "not vanilla or CC anvils."
        ),
        "checks": [
            {
                "name": "gt6AnvilIsNotVanillaAnvil",
                "path": "stone/anvil",
                "kind": "MISC_TOOL",
                "forbid": "anvil",
            }
        ],
    },
    "multiblock": {
        "title": "GT6 Multiblock Runtime",
        "semantic_key": "mte:multiblock:in-place",
        "sample_ids": ["coal_coke/oven"],
        "reason": (
            "GT6 multiblock MTEs keep dummy modern ids as live BlockItems. They "
            "are not named CC controllers."
        ),
        "checks": [
            {
                "name": "gt6CokeOvenMteIsNotNamedCokeOven",
                "path": "coal_coke/oven",
                "kind": "MULTIBLOCK_PART",
                "forbid": "coke_oven",
            }
        ],
    },
    "crucible_foundry": {
        "title": "GT6 Crucible Foundry Runtime",
        "semantic_key": "mte:crucible-foundry:in-place",
        "sample_ids": ["foundry/smelting_crucible_invar"],
        "reason": (
            "Foundry MTEs keep dummy modern ids as live BlockItems. They are not "
            "the ceramic crucible."
        ),
        "checks": [
            {
                "name": "foundryCrucibleIsNotCeramicCrucible",
                "path": "foundry/smelting_crucible_invar",
                "kind": "CRUCIBLE_FOUNDRY",
                "forbid": "crucible",
            }
        ],
    },
}

ART_KIND: dict[str, list[tuple[str, str]]] = {
    "FAUCET": [
        ("materialicons/metallic/blocksolid.png", "faucet.png"),
        ("materialicons/metallic/blocksolid_overlay.png", "faucet_overlay.png"),
    ],
    "TAP": [("machines/tools/tap/colored/side.png", "tap.png")],
    "FUNNEL": [("machines/tools/funnel/colored/side.png", "funnel.png")],
    "NOZZLE": [("machines/tools/nozzle/colored/side.png", "nozzle.png")],
    "CAP_NOZZLE": [("machines/tools/capnozzle/colored/side.png", "cap_nozzle.png")],
    "TANK_EXTENDER": [("machines/extenders/tank/colored/side.png", "tank_extender.png")],
    "TANK_BRIDGE": [
        ("machines/extenders/bridge_tank/colored/side.png", "tank_bridge.png")
    ],
    "STEAM_TURBINE": [
        ("machines/turbines/rotation_steam/colored/side.png", "steam_turbine.png")
    ],
    "BATTERY_BOX": [
        ("machines/batteries/eu/advanced/128/sides.png", "battery_box.png")
    ],
    "WOOD_PANEL": [("iconsets/planks_wood.png", "wood_panel.png")],
    "ROPE": [("machines/tools/rope/colored.png", "rope.png")],
    "CHEST": [
        ("model/gt.multitileentity/metalchest.colored.png", "chest.png")
    ],
    "SAFE": [("machines/safes/mechanical/colored/side.png", "safe.png")],
    "CRAFTING_TABLE": [
        ("machines/craftingtables/advanced/colored/top.png", "crafting_table.png")
    ],
    "SCAFFOLD": [("iconsets/planks_wood.png", "scaffold.png")],
    "BARREL": [("machines/massstorage/barrel/colored/side.png", "barrel.png")],
    "BOOKSHELF": [("iconsets/planks_wood.png", "bookshelf.png")],
    "BOTTLE_CRATE": [("iconsets/planks_wood.png", "bottle_crate.png")],
    "DRAWER": [("machines/drawers/quad/colored/side.png", "drawer.png")],
    "LOCKER": [("machines/lockers/normal/colored/side.png", "locker.png")],
    "MASS_STORAGE": [
        ("machines/massstorage/standard/colored/side.png", "mass_storage.png")
    ],
    "AXLE": [("iconsets/axle.png", "axle.png")],
    "GEARBOX": [("iconsets/gearbox.png", "gearbox.png")],
    "ROTATION_ENGINE": [
        ("machines/engines/kinetic_rotation/colored/side.png", "rotation_engine.png")
    ],
    "ROTATION_TRANSFORMER": [
        ("machines/transformers/transformer_rotation/colored/side.png", "rotation_transformer.png")
    ],
    "MISC_TOOL": [("machines/tools/bathing_pot/colored/sides.png", "misc_tool.png")],
    "MULTIBLOCK_PART": [
        ("machines/multiblockparts/centrifugeparts/0/colored/side.png", "multiblock_part.png")
    ],
    "CRUCIBLE_FOUNDRY": [
        ("machines/multiblockmains/crucible/colored/side.png", "crucible_foundry.png")
    ],
}


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _write_loot(path: str) -> None:
    _write_json(
        LOOT / f"{path}.json",
        {
            "type": "minecraft:block",
            "pools": [
                {
                    "bonus_rolls": 0.0,
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                    "entries": [
                        {
                            "type": "minecraft:item",
                            "name": f"cruciblecraft:{path}",
                        }
                    ],
                    "rolls": 1.0,
                }
            ],
        },
    )


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _strip_ns(value: str) -> str:
    return value.split(":", 1)[-1]


def _wave(domain: str) -> Path:
    return census.TOOLS / "waves" / "content" / DOMAINS[domain]["slug"].split("/", 1)[1]


def _pack(domain: str) -> Path:
    slug = DOMAINS[domain]["slug"].replace("/", "_").replace("-", "_")
    return (
        census.ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / f"cruciblecraft_wave_{slug}"
    )


def _game_tests(domain: str) -> Path:
    return (
        census.ROOT
        / "src"
        / "test"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "gametest"
        / str(DOMAINS[domain]["game_tests"])
    )


def _kind_for(gt6_class: str, family: str = "") -> str:
    for token, kind in CLASS_KIND:
        if token in gt6_class:
            return kind
    return {
        "misc_tool": "MISC_TOOL",
        "multiblock": "MULTIBLOCK_PART",
        "crucible_foundry": "CRUCIBLE_FOUNDRY",
        "drive": "AXLE",
        "furniture_storage": "CHEST",
        "decorative": "WOOD_PANEL",
        "energy_converter": "STEAM_TURBINE",
    }.get(family, "MISC_TOOL")


def _dummy_paths() -> set[str]:
    catalog = census.load_json(DATA_CATALOG)
    return {
        str(row["registry_path"])
        for row in catalog.get("identities") or []
        if row.get("registry_kind") == "item"
    }


def _class_matches(gt6_class: str, needle: str | None) -> bool:
    if not needle:
        return True
    if "|" in needle:
        return any(part in gt6_class for part in needle.split("|"))
    return needle in gt6_class


def _r0_family_rows(domain: str) -> list[dict[str, Any]]:
    spec = DOMAINS[domain]
    dummy = _dummy_paths()
    inplace: set[str] = set()
    if INPLACE_CATALOG.is_file():
        inplace = {
            str(row.get("registry_path") or "")
            for row in census.load_json(INPLACE_CATALOG).get("identities") or []
        }
    live = dummy | inplace
    rows = []
    for row in census.load_json(R0).get("identities") or []:
        if row.get("family") != spec["family"]:
            continue
        path = str(row.get("registry_path") or "")
        if path not in live:
            continue
        tag = str(row.get("gt6_class_or_tag") or "")
        if not _class_matches(tag, spec.get("class_contains")):
            continue
        rows.append(row)
    rows.sort(key=lambda item: int(item["meta"]))
    return rows


def build_overlay(domain: str) -> dict[str, Any]:
    spec = DOMAINS[domain]
    smelter = {
        int(row["meta"]): row
        for row in census.load_json(DATA_CATALOG).get("identities") or []
    }
    rows = []
    for row in _r0_family_rows(domain):
        path = str(row["registry_path"])
        kind = _kind_for(str(row.get("gt6_class_or_tag") or ""), spec["family"])
        catalog = smelter.get(int(row["meta"])) or {}
        english = str(
            catalog.get("english_name")
            or row.get("english_name")
            or path
        )
        chinese = str(
            catalog.get("chinese_name")
            or english
        )
        rows.append(
            {
                "meta": int(row["meta"]),
                "dummy_path": path,
                "live_block": f"cruciblecraft:{path}",
                "kind": kind,
                "family": spec["family"],
                "gt6_class": row.get("gt6_class_or_tag"),
                "english_name": english,
                "chinese_name": chinese,
                "disposition": "keep_distinct",
            }
        )
    return {
        "schema_version": 1,
        "capability_slug": spec["slug"],
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "domain": domain,
        "counts": {"in_place": len(rows)},
        "rows": rows,
    }


def topology(domain: str, unique_active: bool) -> dict[str, Any]:
    spec = DOMAINS[domain]
    return {
        "schema_version": 1,
        "wave_slug": spec["slug"],
        "unique_active_wave": spec["slug"] if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "depends_on": list(spec["depends_on"]),
    }


def readiness(domain: str, unique_active: bool) -> dict[str, Any]:
    spec = DOMAINS[domain]
    return {
        "schema_version": 1,
        "wave_slug": spec["slug"],
        "unique_active_wave": spec["slug"] if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "close_target": "runtime_ready",
        "obtain": "explicitly_blocked",
    }


def _gt6_file(relative: str) -> Path:
    rel = relative.replace("\\", "/")
    if rel.startswith("model/") or rel.startswith("gui/"):
        return GT6_W / "src/main/resources/assets/gregtech/textures" / rel
    return GT6_W / GT6_TEX / rel


def _copy_art(kinds: set[str]) -> list[dict[str, Any]]:
    manifest: list[dict[str, Any]] = []
    dest_root = census.ROOT / "src" / "main" / "resources" / DEST_ROOT
    dest_root.mkdir(parents=True, exist_ok=True)
    for kind in sorted(kinds):
        copies = ART_KIND.get(kind)
        if not copies:
            raise ValueError(f"no GT6 art mapping for kind {kind}")
        for gt6_rel, dest_name in copies:
            source = _gt6_file(gt6_rel)
            if not source.is_file():
                raise FileNotFoundError(
                    f"missing GT6 art {source} for kind {kind}"
                )
            dest = dest_root / dest_name
            dest.parent.mkdir(parents=True, exist_ok=True)
            if not dest.is_file() or dest.read_bytes() != source.read_bytes():
                shutil.copyfile(source, dest)
            manifest.append(
                {
                    "source": "gt6_referencable_port_code/gregtech6_w",
                    "gt6_source": str(source.relative_to(GT6_W)).replace("\\", "/"),
                    "destination": f"{DEST_ROOT}/{dest_name}",
                    "kind": kind,
                    "sha256": _sha256(source),
                }
            )
    return manifest


def _texture_name(kind: str) -> str:
    return ART_KIND[kind][0][1].removesuffix(".png")


def _write_models(rows: list[dict[str, Any]]) -> None:
    foundry = [row for row in rows if str(row["kind"]) == "CRUCIBLE_FOUNDRY"]
    if foundry:
        from tools import gt6_foundry_art as foundry_art

        foundry_art.write_models(foundry)
        for row in foundry:
            _write_loot(str(row["dummy_path"]))
    for row in rows:
        kind = str(row["kind"])
        if kind == "CRUCIBLE_FOUNDRY":
            continue
        path = str(row["dummy_path"])
        texture = f"cruciblecraft:block/gt6_import/mte/{_texture_name(kind)}"
        model_name = f"mte_inplace_{kind.lower()}"
        _write_json(
            BLOCK_MODELS / f"{model_name}.json",
            {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": texture},
            },
        )
        _write_json(
            BLOCKSTATES / f"{path}.json",
            {
                "variants": {
                    "facing=down": {"model": f"cruciblecraft:block/{model_name}", "x": 90},
                    "facing=east": {
                        "model": f"cruciblecraft:block/{model_name}",
                        "y": 90,
                    },
                    "facing=north": {"model": f"cruciblecraft:block/{model_name}"},
                    "facing=south": {
                        "model": f"cruciblecraft:block/{model_name}",
                        "y": 180,
                    },
                    "facing=up": {"model": f"cruciblecraft:block/{model_name}", "x": 270},
                    "facing=west": {
                        "model": f"cruciblecraft:block/{model_name}",
                        "y": 270,
                    },
                }
            },
        )
        _write_json(
            ITEM_MODELS / f"{path}.json",
            {"parent": f"cruciblecraft:block/{model_name}"},
        )
        _write_loot(path)


def _merge_inplace_catalog(rows: list[dict[str, Any]]) -> None:
    existing: list[dict[str, Any]] = []
    if INPLACE_CATALOG.is_file():
        existing = list(
            census.load_json(INPLACE_CATALOG).get("identities") or []
        )
    metas = {int(row["meta"]) for row in rows}
    kept = [row for row in existing if int(row["meta"]) not in metas]
    added = []
    for row in rows:
        added.append(
            {
                "chinese_name": row["chinese_name"],
                "english_name": row["english_name"],
                "family": row["family"],
                "gt6_class": row["gt6_class"],
                "kind": row["kind"],
                "meta": int(row["meta"]),
                "registry_path": row["dummy_path"],
                "runtime_id": f"cruciblecraft:{row['dummy_path']}",
            }
        )
    merged = kept + added
    merged.sort(key=lambda item: int(item["meta"]))
    _write_json(
        INPLACE_CATALOG,
        {
            "identities": merged,
            "schema_version": 1,
            "source_revision": GT6_REVISION,
            "status": "MTE_INPLACE_CATALOG",
        },
    )


def _patch_catalogs(rows: list[dict[str, Any]]) -> int:
    by_meta = {int(row["meta"]): row for row in rows}
    changed = 0
    for path in (DATA_CATALOG, TOOLS_CATALOG):
        document = census.load_json(path)
        identities = list(document.get("identities") or [])
        for identity in identities:
            row = by_meta.get(int(identity["meta"]))
            if row is None:
                continue
            live = row["dummy_path"]
            if identity.get("registry_kind") != "existing_item" or identity.get(
                "registry_path"
            ) != live:
                identity["registry_kind"] = "existing_item"
                identity["registry_path"] = live
                identity["runtime_id"] = f"cruciblecraft:{live}"
                changed += 1
        created = [
            identity
            for identity in identities
            if identity.get("registry_kind") == "item"
        ]
        document["new_item_count"] = len(created)
        _write_json(path, document)
    return changed


def _patch_modern_map(rows: list[dict[str, Any]], reason: str) -> int:
    document = census.load_json(modern.MAP_PATH)
    by_meta = {int(row["meta"]): row for row in rows}
    changed = 0
    for row in document.get("rows") or []:
        if str(row.get("source_item") or "") != "gregtech:gt.multitileentity":
            continue
        target = by_meta.get(int(row["meta"]))
        if target is None:
            continue
        live = target["dummy_path"]
        if row.get("registry_path") != live:
            row["registry_path"] = live
            row["runtime_id"] = f"cruciblecraft:{live}"
            row["collision_reason"] = reason
            changed += 1
        elif row.get("collision_reason") != reason:
            row["collision_reason"] = reason
            changed += 1
    document["collision_count"] = sum(
        1 for row in document.get("rows") or [] if row.get("collision_reason")
    )
    _write_json(modern.MAP_PATH, document)
    return changed


def _copy_empty_nbt(domain: str) -> None:
    if not EMPTY_SRC.is_file():
        raise FileNotFoundError(f"missing {census.relative(EMPTY_SRC)}")
    pack = _pack(domain)
    for dest in (
        pack / "structure" / "empty.nbt",
        pack / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.is_file() and dest.read_bytes() == EMPTY_SRC.read_bytes():
            continue
        shutil.copyfile(EMPTY_SRC, dest)


def write(unique_active: bool = True, domain: str = "attachments") -> dict[str, Any]:
    if domain not in DOMAINS:
        raise ValueError(f"unknown domain {domain}")
    spec = DOMAINS[domain]
    overlay = build_overlay(domain)
    if int(overlay["counts"]["in_place"]) != spec["expected"]:
        raise ValueError(
            f"{domain} in_place {overlay['counts']['in_place']} != {spec['expected']}"
        )
    wave = _wave(domain)
    wave.mkdir(parents=True, exist_ok=True)
    kinds = {str(row["kind"]) for row in overlay["rows"]}
    manifest = _copy_art(kinds)
    _write_json(wave / "runtime_overlay.json", overlay)
    _write_json(wave / "topology.json", topology(domain, unique_active))
    _write_json(wave / "readiness.json", readiness(domain, unique_active))
    _write_json(wave / "archive_strategy.json", ARCHIVE)
    _write_json(wave / "production_lock.json", {"note": spec["lock_note"]})
    _write_json(wave / "art_manifest.json", {"rows": manifest})
    _write_json(ASSETS / "gt6_mte_inplace_art_manifest.json", {"rows": manifest})
    _merge_inplace_catalog(overlay["rows"])
    folded = _patch_catalogs(overlay["rows"])
    mapped = _patch_modern_map(overlay["rows"], spec["collision_reason"])
    modern.rewrite_catalog_item_tags()
    _write_models(overlay["rows"])
    _copy_empty_nbt(domain)
    (wave / "r0_disposition_sha256.txt").write_text(
        _sha256(R0) + "\n", encoding="utf-8"
    )
    (wave / "baseline_ledger_sha256.txt").write_text(
        _sha256(LEDGER) + "\n", encoding="utf-8"
    )
    return {
        "domain": domain,
        "in_place": len(overlay["rows"]),
        "folded_identities": folded,
        "mapped_rows": mapped,
        "art_rows": len(manifest),
    }


def _check_domain(domain: str) -> list[str]:
    spec = DOMAINS[domain]
    wave = _wave(domain)
    overlay_path = wave / "runtime_overlay.json"
    errors: list[str] = []
    if not overlay_path.is_file():
        return [f"missing {census.relative(overlay_path)}"]
    live = build_overlay(domain)
    committed = census.load_json(overlay_path)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"{domain} runtime_overlay.json drifted: {drift}")
    if int((committed.get("counts") or {}).get("in_place") or 0) != spec["expected"]:
        errors.append(
            f"{domain} in_place {(committed.get('counts') or {}).get('in_place')} "
            f"!= {spec['expected']}"
        )
    by_meta = {int(row["meta"]): row for row in committed.get("rows") or []}
    live_hosts = modern.live_host_paths()
    inplace = {
        str(row.get("registry_path") or "")
        for row in census.load_json(INPLACE_CATALOG).get("identities") or []
    }
    for path in (DATA_CATALOG, TOOLS_CATALOG):
        catalog = census.load_json(path)
        catalog_by_meta = {
            int(row["meta"]): row for row in catalog.get("identities") or []
        }
        for meta, row in by_meta.items():
            identity = catalog_by_meta.get(meta)
            if identity is None:
                errors.append(f"{census.relative(path)} missing meta {meta}")
                continue
            live_path = row["dummy_path"]
            if live_path not in live_hosts:
                errors.append(f"{domain} live host {live_path} is not registered")
            if live_path not in inplace:
                errors.append(f"{domain} {live_path} missing from inplace catalog")
            if identity.get("registry_kind") != "existing_item":
                errors.append(f"{domain} meta {meta} still dummy item")
            if identity.get("registry_path") != live_path:
                errors.append(
                    f"{domain} meta {meta} catalog {identity.get('registry_path')} "
                    f"!= {live_path}"
                )
            model = ITEM_MODELS / f"{live_path}.json"
            if model.is_file() and "iron_ingot" in model.read_text(encoding="utf-8"):
                errors.append(f"{live_path} still uses iron_ingot")
    tests_path = _game_tests(domain)
    tests = tests_path.read_text(encoding="utf-8") if tests_path.is_file() else ""
    for name in spec["tests"]:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in spec["tests"]:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    pack = _pack(domain)
    if not (pack / "structure" / "empty.nbt").is_file():
        errors.append(f"missing {domain} structure/empty.nbt")
    if not (pack / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append(f"missing {domain} gametest/structure/empty.nbt")
    if (wave / "r0_disposition_sha256.txt").read_text(encoding="utf-8").strip() != _sha256(
        R0
    ):
        errors.append("R0 disposition ledger was modified")
    if (
        wave / "baseline_ledger_sha256.txt"
    ).read_text(encoding="utf-8").strip() != _sha256(LEDGER):
        errors.append("baseline identity_resolution_ledger was modified")
    return errors


def check() -> list[str]:
    errors: list[str] = []
    found = False
    for domain in DOMAINS:
        overlay = _wave(domain) / "runtime_overlay.json"
        if not overlay.is_file():
            continue
        found = True
        errors.extend(_check_domain(domain))
    if not found:
        return ["missing any MTE inplace overlay"]
    return errors


def _pack_namespace(slug: str) -> str:
    return "cruciblecraft_wave_" + slug.replace("/", "_").replace("-", "_")


def _owned_paths(domain: str, *, closed: bool) -> list[str]:
    spec = DOMAINS[domain]
    lane = "closed" if closed else "active"
    return [
        f"docs/history/card-plans/{lane}/{spec['plan_stem']}",
        "src/main/java/com/masson/cruciblecraft/content/block/MteInPlaceBlock.java",
        "src/main/java/com/masson/cruciblecraft/content/blockentity/MteInPlaceBlockEntity.java",
        "src/main/java/com/masson/cruciblecraft/content/mte/**",
        f"src/test/java/com/masson/cruciblecraft/gametest/{spec['game_tests']}",
        "src/test/java/com/masson/cruciblecraft/gametest/MteInPlaceGameTestSupport.java",
        "src/main/resources/data/cruciblecraft/mte_inplace_catalog.json",
        f"src/main/resources/data/{_pack_namespace(spec['slug'])}/**",
        f"tools/capabilities/{spec['slug']}/**",
        f"tools/waves/content/{spec['slug'].split('/', 1)[1]}/**",
        "tools/gt6_mte_inplace_runtime.py",
        "tools/build_gt6_mte_inplace_runtime.py",
        "tools/tests/test_gt6_mte_runtime.py",
    ]


def _plan_body(domain: str, *, closed: bool) -> str:
    spec = DOMAINS[domain]
    chinese = spec["plan_stem"].removesuffix(".md")
    slug = spec["slug"]
    depends = ", ".join(spec["depends_on"])
    if closed:
        lane = (
            "lane                         = closed\n"
            f"capability_slug              = {slug}\n"
            "unique_active_wave           = null\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = accepted\n"
            f"depends_on                   = {depends}\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：已关闭 `runtime_ready` / `workflow=accepted`。\n"
            "> 本文件位于 `card-plans/closed/`。"
        )
        gate = "[x]"
    else:
        lane = (
            "lane                         = unique-active\n"
            f"capability_slug              = {slug}\n"
            f"unique_active_wave           = {slug}\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = active\n"
            f"depends_on                   = {depends}\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：unique-active，目标 `runtime_ready`。\n"
            "> 本文件位于 `card-plans/active/`。"
        )
        gate = "[ ]"
    return (
        f"# {chinese}\n\n"
        f"> 计划 slug：`{slug}`\n"
        f"{status}\n"
        f"> 正式名称：{chinese}\n"
        "> 性质：在 dummy 现代 id 上原地实现独立 BlockItem。\n"
        ">\n"
        "> Java/tick 源：`gt6_code/gregtech6` @ "
        "`3703e40308c8c030763fd6297dea8b210d2a77b1`。\n"
        "> 美术源：`gt6_referencable_port_code/gregtech6_w`。\n\n"
        "```text\n"
        f"{lane}\n"
        "```\n\n"
        "不改 R0，不改连接件基线 `identity_resolution_ledger.json`。"
        "`depends_on` 只引用已有 capability slug。\n\n"
        "---\n\n"
        "## 0. 边界\n\n"
        f"- 覆盖 R0 `{spec['family']}` 本 child 的 {spec['expected']} 行。\n"
        "- dummy 现代 id 原地升级为 `MteInPlaceBlock` + `CatalogNamedBlockItem`。"
        "禁止再注册 `*_real`，禁止 alias 已有主机。\n"
        "- 获得格保持 `explicitly_blocked`。不造 stand-in 配方。\n"
        "- 贴图从本地 `gregtech6_w` 迁入 `textures/block/gt6_import/mte/`，"
        "写 art manifest。\n"
        "- 存档：同一 registry path 变成 BlockItem；无 NeoForge alias。\n\n"
        "## 1. 验收\n\n"
        f"- {gate} {spec['expected']} 行 live BlockItem\n"
        f"- {gate} 隔离 GameTest `-PwaveRecipes={slug}`\n"
        f"- {gate} `python tools/verify.py integration --profile capability-runtime`\n"
        f"- {gate} 关闭目标 `runtime_ready`\n"
    )


def _render_game_test(domain: str) -> str:
    spec = DOMAINS[domain]
    issue = ISSUE[domain]
    class_name = spec["game_tests"].removesuffix(".java")
    ns = _pack_namespace(spec["slug"])
    forbids = {row.get("forbid") for row in issue.get("checks") or []}
    imports = [
        "import com.masson.cruciblecraft.content.block.MteInPlaceBlock;",
        "import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;",
        "import com.masson.cruciblecraft.content.mte.MteInPlaceKind;",
        "import com.masson.cruciblecraft.registry.ModBlocks;",
        "",
        "import net.minecraft.gametest.framework.GameTest;",
        "import net.minecraft.gametest.framework.GameTestHelper;",
        "import net.neoforged.neoforge.gametest.GameTestHolder;",
        "import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;",
    ]
    if forbids & {"inventory", "drive_push", "crucible"}:
        imports.insert(
            1,
            "import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;",
        )
        imports.insert(5, "import net.minecraft.core.BlockPos;")
    if "drive_push" in forbids:
        imports.insert(0, "import com.masson.cruciblecraft.api.energy.EnergyType;")
        imports.insert(6, "import net.minecraft.core.Direction;")
    if forbids & {"vanilla_lead", "inventory", "anvil"}:
        imports.append("import net.minecraft.world.item.Items;")
    if "inventory" in forbids:
        imports.append("import net.minecraft.world.item.ItemStack;")
    if "anvil" in forbids:
        imports.append("import net.minecraft.world.level.block.Blocks;")
    if "crucible" in forbids:
        kind_import = "import com.masson.cruciblecraft.content.mte.MteInPlaceKind;"
        if kind_import in imports:
            imports.insert(
                imports.index(kind_import) + 1,
                "import com.masson.cruciblecraft.content.mte.MteFoundryTanks;",
            )
    methods = []
    for row in issue.get("checks") or []:
        forbid = row.get("forbid")
        timeout = 80 if forbid == "drive_push" else 40
        body = [
            "        MteInPlaceGameTestSupport.assertLive(",
            f"                helper, \"{row['path']}\", "
            f"MteInPlaceKind.{row['kind']});",
        ]
        if forbid == "vanilla_lead":
            body += [
                "        helper.assertTrue(",
                f"                MteInPlaceGameTestSupport.item(\"{row['path']}\")",
                "                        != Items.LEAD,",
                f"                \"{row['path']} aliased vanilla lead\");",
            ]
        elif forbid == "inventory":
            body += [
                "        BlockPos pos = new BlockPos(2, 2, 2);",
                "        MteInPlaceBlock block = ModBlocks.mteInPlaceBlocksById()",
                "                .get(MteInPlaceGameTestSupport.id(",
                f"                        \"{row['path']}\"))",
                "                .get();",
                "        helper.setBlock(pos, block.defaultBlockState());",
                "        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);",
                "        helper.assertTrue(",
                "                be.items().insertItem(",
                "                        0, new ItemStack(Items.APPLE), false)",
                "                        .isEmpty(),",
                f"                \"{row['path']} rejected an item\");",
            ]
        elif forbid == "ku_axle":
            body += [
                "        helper.assertTrue(",
                "                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(",
                f"                        \"{row['path']}\")).getBlock()",
                "                        != ModBlocks.ROTATIONAL_AXLE.get(),",
                f"                \"{row['path']} aliased KU rotational_axle\");",
            ]
        elif forbid == "anvil":
            body += [
                "        helper.assertTrue(",
                "                MteInPlaceGameTestSupport.item(",
                f"                        \"{row['path']}\") != Items.ANVIL",
                "                        && ((CatalogNamedBlockItem) "
                "MteInPlaceGameTestSupport.item(",
                f"                                \"{row['path']}\")).getBlock()",
                "                                != ModBlocks.ANVIL.get()",
                "                        && ((CatalogNamedBlockItem) "
                "MteInPlaceGameTestSupport.item(",
                f"                                \"{row['path']}\")).getBlock()",
                "                                != Blocks.ANVIL,",
                f"                \"{row['path']} aliased an anvil\");",
            ]
        elif forbid == "coke_oven":
            body += [
                "        helper.assertTrue(",
                "                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(",
                f"                        \"{row['path']}\")).getBlock()",
                "                        != ModBlocks.COKE_OVEN.get(),",
                f"                \"{row['path']} aliased named coke_oven\");",
            ]
        elif forbid == "crucible":
            body += [
                "        helper.assertTrue(",
                "                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(",
                f"                        \"{row['path']}\")).getBlock()",
                "                        != ModBlocks.CRUCIBLE.get(),",
                f"                \"{row['path']} aliased the ceramic crucible\");",
                "        BlockPos pos = new BlockPos(2, 2, 2);",
                "        helper.setBlock(",
                "                pos,",
                "                ModBlocks.mteInPlaceBlocksById()",
                "                        .get(MteInPlaceGameTestSupport.id(",
                f"                                \"{row['path']}\"))",
                "                        .get()",
                "                        .defaultBlockState());",
                "        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);",
                "        helper.assertTrue(",
                "                be.tank().getCapacity() == MteFoundryTanks.SMELTERY_MB,",
                "                \"foundry smeltery tank is not 16 ingots\");",
            ]
        elif forbid == "drive_push":
            body += [
                "        BlockPos sourcePos = new BlockPos(1, 2, 2);",
                "        BlockPos destPos = new BlockPos(2, 2, 2);",
                "        MteInPlaceBlock axle = ModBlocks.mteInPlaceBlocksById()",
                "                .get(MteInPlaceGameTestSupport.id(",
                f"                        \"{row['path']}\"))",
                "                .get();",
                "        helper.setBlock(",
                "                sourcePos,",
                "                axle.defaultBlockState().setValue(",
                "                        MteInPlaceBlock.FACING, Direction.EAST));",
                "        helper.setBlock(",
                "                destPos,",
                "                axle.defaultBlockState().setValue(",
                "                        MteInPlaceBlock.FACING, Direction.EAST));",
                "        MteInPlaceBlockEntity source = helper.getBlockEntity(",
                "                sourcePos);",
                "        helper.assertTrue(",
                "                source.insert(",
                "                        EnergyType.KINETIC_ROTATION,",
                "                        1L,",
                "                        128L,",
                "                        Direction.EAST,",
                "                        false) == 128L,",
                "                \"drive source rejected kinetic energy\");",
                "        helper.startSequence()",
                "                .thenExecuteAfter(2, () -> {",
                "                    MteInPlaceBlockEntity dest =",
                "                            helper.getBlockEntity(destPos);",
                "                    helper.assertTrue(",
                "                            dest.stored(",
                "                                    EnergyType.KINETIC_ROTATION) > 0L,",
                "                            \"drive did not push kinetic to neighbor\");",
                "                })",
                "                .thenSucceed();",
            ]
        if forbid != "drive_push":
            body.append("        helper.succeed();")
        methods.append(
            f"    @GameTest(template = TEMPLATE, timeoutTicks = {timeout})\n"
            f"    public static void {row['name']}(GameTestHelper helper) {{\n"
            + "\n".join(body)
            + "\n    }"
        )
    return (
        "package com.masson.cruciblecraft.gametest;\n\n"
        + "\n".join(imports)
        + "\n\n"
        f"@GameTestHolder({class_name}.NAMESPACE)\n"
        "@PrefixGameTestTemplate(false)\n"
        f"public final class {class_name} {{\n"
        f"    public static final String NAMESPACE =\n"
        f"            \"{ns}\";\n"
        '    private static final String TEMPLATE = "empty";\n\n'
        f"    private {class_name}() {{}}\n\n"
        + "\n\n".join(methods)
        + "\n}\n"
    )


def _write_game_test(domain: str) -> None:
    if domain not in ISSUE or not ISSUE[domain].get("checks"):
        return
    path = _game_tests(domain)
    payload = _render_game_test(domain)
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(payload, encoding="utf-8")


def issue_active(domain: str) -> dict[str, Any]:
    if domain not in DOMAINS or domain not in ISSUE:
        raise ValueError(f"cannot issue {domain}")
    spec = DOMAINS[domain]
    issue = ISSUE[domain]
    active_dir = census.ROOT / "docs" / "history" / "card-plans" / "active"
    for path in sorted(active_dir.glob("*.md")):
        if path.name != spec["plan_stem"]:
            raise ValueError(f"unique-active already occupied by {path.name}")
    cap_path = census.TOOLS / "capabilities" / spec["slug"] / "capability.json"
    if cap_path.is_file():
        existing = census.load_json(cap_path)
        if existing.get("workflow") == "accepted":
            raise ValueError(f"{spec['slug']} already closed")
    document = {
        "schema_version": 2,
        "slug": spec["slug"],
        "title": issue["title"],
        "maturity": "runtime_ready",
        "workflow": "active",
        "owned_paths": _owned_paths(domain, closed=False),
        "depends_on": list(spec["depends_on"]),
        "profiles": ["capability-runtime"],
        "wave_slug": spec["slug"],
        "required_test_ids": list(spec["tests"]),
        "identity_disposition": [
            {
                "semantic_key": issue["semantic_key"],
                "disposition": "keep_distinct",
                "runtime_ids": [
                    f"cruciblecraft:{path}" for path in issue["sample_ids"]
                ],
                "reason": issue["reason"],
            }
        ],
        "note": (
            f"In-place {domain} runtime. Close at runtime_ready. Source-exact "
            "obtain lives on content/gt6-mte-inplace-acquisition; remaining D0 "
            "gaps stay blocked. Do not edit R0."
        ),
    }
    _write_json(cap_path, document)
    plan = active_dir / spec["plan_stem"]
    plan.parent.mkdir(parents=True, exist_ok=True)
    plan.write_text(_plan_body(domain, closed=False), encoding="utf-8")
    _write_game_test(domain)
    return {"domain": domain, "slug": spec["slug"], "issued": True}


def prepare_close(domain: str) -> dict[str, Any]:
    spec = DOMAINS[domain]
    cap_path = census.TOOLS / "capabilities" / spec["slug"] / "capability.json"
    document = census.load_json(cap_path)
    document["owned_paths"] = _owned_paths(domain, closed=True)
    _write_json(cap_path, document)
    plan = (
        census.ROOT
        / "docs"
        / "history"
        / "card-plans"
        / "active"
        / spec["plan_stem"]
    )
    if not plan.is_file():
        raise ValueError(f"missing active plan {spec['plan_stem']}")
    plan.write_text(_plan_body(domain, closed=True), encoding="utf-8")
    return {"domain": domain, "slug": spec["slug"], "prepared_close": True}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--issue", action="store_true")
    parser.add_argument("--prepare-close", action="store_true")
    parser.add_argument("--unique-active", action="store_true")
    parser.add_argument("--no-unique-active", action="store_true")
    parser.add_argument(
        "--domain",
        choices=sorted(DOMAINS),
        default="attachments",
    )
    args = parser.parse_args(argv)
    unique_active = True
    if args.no_unique_active:
        unique_active = False
    if args.unique_active:
        unique_active = True
    if args.issue:
        print(json.dumps(issue_active(args.domain), ensure_ascii=False, indent=2))
    if args.prepare_close:
        print(json.dumps(prepare_close(args.domain), ensure_ascii=False, indent=2))
    if args.write:
        result = write(unique_active=unique_active, domain=args.domain)
        print(json.dumps(result, ensure_ascii=False, indent=2))
    if args.check:
        errors = check()
        if errors:
            for error in errors:
                print(error, file=sys.stderr)
            return 1
        print("gt6_mte_inplace_runtime: OK")
    if not any([args.write, args.check, args.issue, args.prepare_close]):
        parser.error("choose --write, --check, --issue, and/or --prepare-close")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
