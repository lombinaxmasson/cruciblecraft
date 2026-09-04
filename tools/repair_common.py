#!/usr/bin/env python3
"""Shared paths and vocabulary for T36 machine-target builders."""
from __future__ import annotations

import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import census_common as census

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

MACHINE_TARGET = TOOLS / "repair_machine_target.json"
CENSUS_DELTA = TOOLS / "repair_census_delta.json"
READINESS = TOOLS / "repair_machine_readiness.json"
MACHINE_KINDS = TOOLS / "machine_tree_denominators" / "machine_kinds.json"
ENERGY_IDENTITIES = TOOLS / "machine_tree_denominators" / "energy_identities.json"
MACHINE_POLICY = TOOLS / "machine_tree_machine_energy_policy.json"
RECIPE_FAMILIES = census.RECIPE_FAMILIES
MACHINE_PLAYABILITY = TOOLS / "machine_machine_playability.json"
MACHINE_TIERS = census.MACHINE_TIERS
MOD_BLOCKS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModBlocks.java"
)
RM_JAVA = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregapi"
    / "data"
    / "RM.java"
)
MT_JAVA = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregapi"
    / "data"
    / "MT.java"
)

VARIANT_SEMANTICS = ("material", "eu_voltage", "tu_host")
HOST_STATUSES = (
    "host_exact",
    "host_targeted_by_t36",
    "host_requires_declared_future_system",
    "product_excluded",
)
REQUIRED_TU_HOSTS = ("autoclave", "bath", "coagulator", "generifier")
RECIPE_HOST_KIND_ALIASES = {
    "roaster": "RM.Roasting",
}
KIND_ID_SLUG = {
    "bronze_crusher": "crusher",
}
ENERGY_TO_JAVA = {
    "RU": "KINETIC_ROTATION",
    "KU": "KINETIC_PUSH",
    "HU": "HEAT",
    "EU": "ELECTRIC",
    "TU": "TIME",
    "TIME": "TIME",
}
ENERGY_TO_TRACK = census.ENERGY_TRACKS
ENERGY_TO_SEMANTICS = {
    "RU": "material",
    "KU": "material",
    "HU": "material",
    "EU": "eu_voltage",
    "TU": "tu_host",
    "TIME": "tu_host",
}
SYMBOL_TO_MATERIAL = {
    "ANY.Wood": "wood",
    "ANY.Stone": "stone",
    "ANY.Steel": "steel",
    "ANY.W": "tungsten",
    "ANY.Cu": "copper",
    "Bronze": "bronze",
    "Ti": "titanium",
    "TungstenSteel": "tungstensteel",
    "Ir": "iridium",
    "Os": "osmium",
    "Invar": "invar",
    "TungstenCarbide": "tungsten_carbide",
    "TinAlloy": "tin_alloy",
    "SteelGalvanized": "steel_galvanized",
    "Al": "aluminium",
    "StainlessSteel": "stainless_steel",
    "Cr": "chromium",
    "Trinitanium": "trinitanium",
    "Trinaquadalloy": "trinaquadalloy",
    "Neutronium": "neutronium",
    "Sn": "tin",
    "Pb": "lead",
    "Electrum": "electrum",
    "EnderiumBase": "enderium_base",
    "Enderium": "enderium",
}


OPENING_VARIANT_IDS = frozenset({
    "cruciblecraft:centrifuge",
    "cruciblecraft:steel_centrifuge",
    "cruciblecraft:titanium_centrifuge",
    "cruciblecraft:sifter",
    "cruciblecraft:steel_sifter",
    "cruciblecraft:titanium_sifter",
    "cruciblecraft:electrolyzer",
    "cruciblecraft:aluminium_electrolyzer",
    "cruciblecraft:stainless_steel_electrolyzer",
    "cruciblecraft:lathe",
    "cruciblecraft:steel_lathe",
    "cruciblecraft:titanium_lathe",
    "cruciblecraft:rollingmill",
    "cruciblecraft:steel_rollingmill",
    "cruciblecraft:titanium_rollingmill",
    "cruciblecraft:wiremill",
    "cruciblecraft:steel_wiremill",
    "cruciblecraft:titanium_wiremill",
    "cruciblecraft:shredder",
    "cruciblecraft:steel_shredder",
    "cruciblecraft:titanium_shredder",
    "cruciblecraft:press",
    "cruciblecraft:steel_press",
    "cruciblecraft:titanium_press",
    "cruciblecraft:distillery",
    "cruciblecraft:invar_distillery",
    "cruciblecraft:titanium_distillery",
    "cruciblecraft:drying",
    "cruciblecraft:invar_drying",
    "cruciblecraft:titanium_drying",
    "cruciblecraft:smelter",
    "cruciblecraft:invar_smelter",
    "cruciblecraft:titanium_smelter",
})


def relative(path: Path) -> str:
    return census.relative(path)


def opening_ids() -> frozenset[str]:
    return OPENING_VARIANT_IDS


def opening_variants(catalog: dict) -> list[dict]:
    rows = [
        row
        for row in catalog.get("variants") or []
        if isinstance(row, dict) and row.get("id") in OPENING_VARIANT_IDS
    ]
    if len(rows) != 33:
        raise ValueError(
            f"opening 33 ids must remain in the live catalog, got {len(rows)}"
        )
    return rows


def accept_catalog_schema(document: dict, owner: str) -> None:
    if document.get("schemaVersion") not in {2, 3}:
        raise ValueError(f"{owner}: machine tier catalog header drifted")
    if document.get("source", {}).get("revision") != SOURCE_REVISION:
        raise ValueError(f"{owner}: machine tier catalog revision drifted")
