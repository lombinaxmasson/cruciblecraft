#!/usr/bin/env python3
"""Shared paths and freeze helpers for the T36-Repair extensibility gate."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import builder_cli
from tools import census_common as census
from tools import repair_common as repair

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION
JAVA = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
)
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
GENERATED_MACHINE_RECIPES = (
    ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "machines"
)

REPAIR_FREEZE = TOOLS / "repair_pre_freeze.json"
REPAIR_INVENTORY = TOOLS / "repair_second_list_inventory.json"
REPAIR_READINESS = TOOLS / "repair_readiness.json"
MACHINE_KINDS = DATA / "machine_kinds.json"
MACHINE_CASINGS = DATA / "machine_casings.json"
DEVICE_MATERIALS = DATA / "device_materials.json"
ACQUISITION_OVERRIDES = DATA / "machine_acquisition.json"
SMELTER_STONE_READINESS = TOOLS / "smelter_stone_readiness.json"
SMELTER_STONE_TOPOLOGY = TOOLS / "smelter_stone_card_topology.json"
SMELTER_STONE_CLOSING_GAP = 3076
OVERLAY_DIR = (
    ROOT
    / "src"
    / "test"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "repair_overlay"
)
OVERLAY_TEST = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "machine"
    / "processing"
    / "MachineRepairExtensibilityTest.java"
)
FIXTURE_VARIANT = "cruciblecraft:invar_lathe"
FIXTURE_CASING = "cruciblecraft:iron_machine_casing"
PRODUCTION_JAVA = (
    "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java",
    "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
    "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
    "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java",
    "src/main/java/com/masson/cruciblecraft/machine/MachineMaterialRules.java",
    "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java",
)

FREEZE_FILES = (
    "src/main/resources/data/cruciblecraft/machine_tiers.json",
    "tools/t36_machine_target.json",
    "tools/repair_census_delta.json",
    "tools/t36_readiness.json",
    "tools/smelter_stone_readiness.json",
    "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java",
    "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
    "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
    "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java",
    "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java",
    "src/main/java/com/masson/cruciblecraft/machine/MachineMaterialRules.java",
    "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java",
    "src/main/resources/data/cruciblecraft/recipe/bronze_crusher.json",
)

KIND_TABLE: tuple[tuple[str, str, str, str, str | None], ...] = (
    ("assembler", "machine_generic", "装配机", "Assembler", None),
    ("autoclave", "machine_generic", "高压釜", "Autoclave", None),
    ("bath", "machine_generic", "洗矿浴池", "Ore Washing Bath", None),
    ("bender", "machine_generic", "折弯机", "Bender", None),
    ("bronze_crusher", "ku_machine_kinetic", "破碎机", "Crusher", None),
    ("centrifuge", "centrifuge", "离心机", "Centrifuge", None),
    ("coagulator", "machine_generic", "凝固机", "Coagulator", None),
    ("compressor", "compressor", "压缩机", "Compressor", None),
    ("cutter", "ku_machine_kinetic", "切割机", "Cutter", None),
    ("distillery", "ru_machine_heat", "蒸馏机", "Distillery", "t17"),
    ("drying", "ru_machine_heat", "干燥机", "Drying Machine", "t17"),
    ("electrolyzer", "electrolyzer", "电解机", "Electrolyzer", None),
    ("extruder", "ru_machine_heat", "挤压机", "Extruder", None),
    ("generifier", "machine_generic", "通化机", "Generifier", None),
    ("lathe", "ku_machine_kinetic", "车床", "Lathe", "t16"),
    ("mixer", "centrifuge", "混合机", "Mixer", None),
    ("mortar", "machine_generic", "动力研钵", "Powered Mortar", None),
    ("press", "ku_machine_kinetic", "压机", "Press", "t16"),
    ("roaster", "ru_machine_heat", "焙烧炉", "Roaster", None),
    ("rollbender", "ku_machine_kinetic", "卷板机", "Roll Bender", None),
    ("rollingmill", "ku_machine_kinetic", "轧机", "Rolling Mill", "t16"),
    ("shredder", "ku_machine_kinetic", "粉碎机", "Shredder", "t16"),
    ("sifter", "sifter", "筛选机", "Sifter", None),
    ("sluice", "centrifuge", "溜槽", "Sluice", None),
    ("smelter", "ru_machine_heat", "熔炼炉", "Smelter", "t17"),
    ("welder", "machine_generic", "焊机", "Welder", None),
    ("wiremill", "ku_machine_kinetic", "线材轧机", "Wire Mill", "t16"),
)

MATERIAL_LANG = {
    "aluminium": {"zh": "铝制", "en": "Aluminium"},
    "bronze": {"zh": "青铜", "en": "Bronze"},
    "chromium": {"zh": "铬制", "en": "Chromium"},
    "invar": {"zh": "殷钢", "en": "Invar"},
    "stainless_steel": {"zh": "不锈钢", "en": "Stainless Steel"},
    "steel": {"zh": "钢制", "en": "Steel"},
    "steel_galvanized": {"zh": "镀锌钢", "en": "Steel Galvanized"},
    "titanium": {"zh": "钛制", "en": "Titanium"},
    "tungsten_carbide": {"zh": "碳化钨", "en": "Tungsten Carbide"},
    "tungstensteel": {"zh": "钨钢", "en": "Tungstensteel"},
}

CASING_ROWS = (
    ("bronze_double_machine_casing", "bronze", "kinetic_double", True, None),
    ("steel_double_machine_casing", "steel", "kinetic_double", True, None),
    ("titanium_double_machine_casing", "titanium", "kinetic_double", True, None),
    ("tungstensteel_double_machine_casing", "tungstensteel", "kinetic_double", True, None),
    ("invar_double_machine_casing", "invar", "kinetic_double", True, None),
    ("tungsten_carbide_double_machine_casing", "tungsten_carbide", "kinetic_double", True, None),
    ("steel_galvanized_machine_casing", "steel_galvanized", "eu_single", False, "tin"),
    ("aluminium_machine_casing", "aluminium", "eu_single", False, "copper"),
    ("stainless_steel_machine_casing", "stainless_steel", "eu_single", False, "gold"),
    ("chromium_machine_casing", "chromium", "eu_single", False, "aluminium"),
    ("titanium_machine_casing", "titanium", "eu_single", False, "tungstensteel"),
)

DISTILLERY_WIRE = (
    ("steel", "constantan", "double_wire"),
    ("invar", "kanthal", "quadruple_wire"),
    ("titanium", "nichrome", "octuple_wire"),
    ("tungsten_carbide", "tungstensteel", "octuple_wire"),
)

GENERIC_TEMPLATE_OVERRIDES = (
    "mixer",
    "sluice",
    "compressor",
    "cutter",
    "extruder",
    "rollbender",
)

STEEL_CASING_OVERRIDES = (
    "invar_smelter",
    "invar_drying",
    "invar_distillery",
)

DEVICE_FREEZE = {
    "crucible": {
        "default_material": "ceramic",
        "materials": {
            "ceramic": {"processing_tier": 2, "max_temperature": 2227.0},
            "bronze": {"processing_tier": 2, "max_temperature": 1255.0},
            "steel": {"processing_tier": 3, "max_temperature": 2284.0},
        },
    },
    "anvil": {
        "default_material": "stone",
        "materials": {
            "stone": {"processing_tier": 0, "durability": 10000},
            "bronze": {"processing_tier": 1, "durability": 1000000},
            "iron": {"processing_tier": 2, "durability": 5000000},
            "steel": {"processing_tier": 3, "durability": 10000000},
        },
    },
    "hammer": {
        "default_material": "iron",
        "materials": {
            "bronze": {"processing_tier": 1, "durability": 44800},
            "iron": {"processing_tier": 2, "durability": 48000},
            "steel": {"processing_tier": 3, "durability": 51200},
        },
    },
}


def relative(path: Path) -> str:
    return census.relative(path)


def load_json(path: Path) -> Any:
    return census.load_json(path)


def parse_managed(description: str, argv: list[str] | None = None):
    return builder_cli.parse_managed(description, argv)


def handle_rebind(args: Any, output: Path) -> bool:
    if not getattr(args, "rebind_currentness_only", False):
        return False
    from tools import currentness

    currentness.rebind_sidecar(output)
    print(f"rebound currentness sidecar for {relative(output)}")
    return True


def file_sha256_map(paths: tuple[str, ...]) -> dict[str, str]:
    hashes: dict[str, str] = {}
    missing: list[str] = []
    for relative_path in paths:
        path = ROOT / relative_path
        if not path.is_file():
            missing.append(relative_path)
            continue
        hashes[relative_path] = census.sha256_file(path)
    if missing:
        raise ValueError("missing freeze files: " + ", ".join(missing))
    return hashes


def catalog_variant_ids() -> list[str]:
    catalog = load_json(repair.MACHINE_TIERS)
    ids = [
        row["id"]
        for row in catalog.get("variants") or []
        if isinstance(row, dict) and isinstance(row.get("id"), str)
    ]
    if len(ids) != 85:
        raise ValueError(f"live machine_tiers.json must remain 85 rows, got {len(ids)}")
    return ids


def recipe_path_for(variant_id: str) -> Path:
    path = variant_id.split(":", 1)[1]
    if path == "bronze_crusher":
        return (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "recipe"
            / "bronze_crusher.json"
        )
    return GENERATED_MACHINE_RECIPES / f"{path}.json"


def classify_template(recipe: dict[str, Any]) -> str:
    pattern = tuple(recipe.get("pattern") or [])
    if pattern == ("CCC", "CFC", "CCC"):
        return "machine_generic"
    if pattern == ("G ", "SC", "G "):
        return "centrifuge"
    if pattern == ("W W", "RCR", "S S"):
        return "sifter"
    if pattern == ("SMS", "W W"):
        return "electrolyzer"
    if pattern == ("PSP", "PCP", " R "):
        return "compressor"
    if pattern in {
        ("TDS", " CG"),
        ("G ", "C ", "G "),
        ("SGS", " C "),
        ("GDG", " C "),
        ("RS", "PC", "P "),
    }:
        return "ku_machine_kinetic"
    if pattern in {
        ("GPG", "WMW", " C "),
        (" P ", "BMB", "BCB"),
        (" U ", "PMP", "BCB"),
        (" P ", "PMP", "BCB"),
        ("PPP", "WMW", " C "),
    }:
        return "ru_machine_heat"
    if pattern == ("PRP", "PAP", "PRP"):
        return "skip_generic_crusher"
    raise ValueError(f"unclassified acquisition pattern {pattern}")


def ingredient_ids(recipe: dict[str, Any]) -> list[str]:
    found: set[str] = set()
    for value in (recipe.get("key") or {}).values():
        if not isinstance(value, dict):
            continue
        item = value.get("item")
        tag = value.get("tag")
        if isinstance(item, str):
            found.add(item)
        if isinstance(tag, str):
            found.add("#" + tag)
    return sorted(found)


def casing_item_from_recipe(recipe: dict[str, Any]) -> str | None:
    for value in (recipe.get("key") or {}).values():
        if not isinstance(value, dict):
            continue
        item = value.get("item")
        if isinstance(item, str) and item.endswith("_machine_casing"):
            return item
    return None


def acquisition_snapshot() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for variant_id in catalog_variant_ids():
        path = recipe_path_for(variant_id)
        if not path.is_file():
            raise ValueError(f"missing acquisition recipe for {variant_id}: {relative(path)}")
        recipe = load_json(path)
        result = ((recipe.get("result") or {}).get("id")) or variant_id
        recipe_id = (
            "cruciblecraft:bronze_crusher"
            if path.name == "bronze_crusher.json"
            else "cruciblecraft:machines/" + variant_id.split(":", 1)[1]
        )
        rows.append(
            {
                "variant_id": variant_id,
                "recipe_id": recipe_id,
                "recipe_path": relative(path),
                "result": result,
                "template": classify_template(recipe),
                "pattern": list(recipe.get("pattern") or []),
                "ingredients": ingredient_ids(recipe),
                "casing_item": casing_item_from_recipe(recipe),
            }
        )
    return rows


def smelter_stone_gap(document: dict[str, Any] | None = None) -> int:
    if document is None:
        document = load_json(SMELTER_STONE_READINESS)
    evidence = document.get("evidence") or {}
    storage_opening = document.get("storage_opening") or {}
    return int(
        storage_opening.get("remaining_recipe_gap")
        or document.get("remaining_recipe_gap")
        or evidence.get("remaining_recipe_gap")
        or -1
    )


def smelter_stone_ready() -> bool:
    if not SMELTER_STONE_READINESS.is_file():
        return False
    document = load_json(SMELTER_STONE_READINESS)
    return (
        document.get("status") == "SMELTER_STONE_READY"
        and list(document.get("failed_gates") or []) == []
        and smelter_stone_gap(document) == SMELTER_STONE_CLOSING_GAP
    )


def t36_ready_preserved() -> bool:
    if not repair.READINESS.is_file():
        return False
    document = load_json(repair.READINESS)
    return document.get("status") == "T36_READY"


def live_variant_count() -> int:
    return len(catalog_variant_ids())


def storage_unassigned() -> bool:
    if not SMELTER_STONE_TOPOLOGY.is_file():
        return False
    topology = load_json(SMELTER_STONE_TOPOLOGY)
    return (
        topology.get("next_issue_id") == "storage/lock"
        and topology.get("preassigned_host") is False
        and topology.get("preassigned_family_ids") is False
        and topology.get("unique_active_card") is None
        and topology.get("unique_active_content_card") is None
    )


def acquisition_matches_freeze() -> bool:
    if not REPAIR_FREEZE.is_file():
        return False
    freeze = load_json(REPAIR_FREEZE)
    return freeze.get("acquisition_snapshot") == acquisition_snapshot()


def fixture_absent_from_production() -> bool:
    tiers = repair.MACHINE_TIERS.read_text(encoding="utf-8") if repair.MACHINE_TIERS.is_file() else ""
    casings = MACHINE_CASINGS.read_text(encoding="utf-8") if MACHINE_CASINGS.is_file() else ""
    devices = DEVICE_MATERIALS.read_text(encoding="utf-8") if DEVICE_MATERIALS.is_file() else ""
    if FIXTURE_VARIANT in tiers or FIXTURE_CASING in casings:
        return False
    if '"material_id": "iron"' in devices.split('"crucible"', 1)[-1].split('"anvil"', 1)[0]:
        return False
    production_overlay = (
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "repair_overlay"
    )
    if production_overlay.exists():
        return False
    for relative_path in PRODUCTION_JAVA:
        text = read_text(relative_path)
        if "invar_lathe" in text or "iron_machine_casing" in text:
            return False
    return True


def overlay_proofs() -> dict[str, bool]:
    tiers = OVERLAY_DIR / "machine_tiers.json"
    casings = OVERLAY_DIR / "machine_casings.json"
    devices = OVERLAY_DIR / "device_materials.json"
    test = OVERLAY_TEST.is_file()
    l1 = False
    l2 = False
    device = False
    if tiers.is_file():
        text = tiers.read_text(encoding="utf-8")
        l1 = FIXTURE_VARIANT in text and "cruciblecraft:lathe" in text
    if casings.is_file():
        text = casings.read_text(encoding="utf-8")
        l2 = FIXTURE_CASING in text and "cruciblecraft:iron" in text
    if devices.is_file():
        text = devices.read_text(encoding="utf-8")
        device = '"material_id": "iron"' in text and "creative_visible" in text
    return {
        "overlay_l1_lathe_invar": l1 and test,
        "overlay_l2_iron_casing": l2 and test,
        "overlay_device_iron_crucible": device and test,
        "fixture_absent_from_production": fixture_absent_from_production(),
    }


def read_text(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def count_matches(text: str, pattern: str) -> int:
    return len(re.findall(pattern, text, flags=re.MULTILINE))


def second_list_rows() -> list[dict[str, Any]]:
    recipe = read_text(
        "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java"
    )
    lang = read_text(
        "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java"
    )
    variants = read_text(
        "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java"
    )
    blocks = read_text(
        "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java"
    )
    items = read_text(
        "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
    )
    rules = read_text(
        "src/main/java/com/masson/cruciblecraft/machine/MachineMaterialRules.java"
    )
    tabs = read_text(
        "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java"
    )
    kinds_json = MACHINE_KINDS.is_file()
    casings_json = MACHINE_CASINGS.is_file()
    devices_json = DEVICE_MATERIALS.is_file()
    acquisition_json = ACQUISITION_OVERRIDES.is_file()

    authored = "java.util.Set<String> authored = java.util.Set.of(" in recipe
    casing_for = "private static Item casingFor(" in recipe
    cable_switch = "private static String electrolyzerCableMaterial(" in recipe
    wire_switch = "private static String distilleryWireMaterial(" in recipe
    kind_switch = 'switch (kind) {' in recipe and "No source-backed acquisition" in recipe
    material_zh = "private static String materialZh(" in lang
    kind_zh = "private static String kindZh(" in lang
    opening = "OPENING_VARIANT_IDS" in variants
    t16 = bool(re.search(r"T16_SELECTED_KINDS\s*=\s*(?:List|Set)\.of", variants))
    t17 = bool(re.search(r"T17_SELECTED_KINDS\s*=\s*(?:List|Set)\.of", variants))
    crucible_set = 'CRUCIBLE_MATERIALS = Set.of("ceramic"' in rules
    anvil_set = "ANVIL_MATERIALS = Set.of(" in rules
    hammer_set = "HAMMER_MATERIALS = Set.of(" in rules
    crucible_tab = 'machineVariant(ModItems.CRUCIBLE.get(), "ceramic")' in tabs
    anvil_tab = 'machineVariant(ModItems.ANVIL.get(), "stone")' in tabs
    alias_blocks = count_matches(blocks, r'tieredProcessing\("')
    alias_items = count_matches(items, r'tieredProcessingItem\("')
    casing_constants = count_matches(items, r"_MACHINE_CASING =")

    rows = [
        _row(
            "authored_acquisition_set",
            "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
            authored,
            True,
            "l1",
            None,
        ),
        _row(
            "kind_acquisition_switch",
            "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
            kind_switch,
            True,
            "l1",
            None,
        ),
        _row(
            "casingFor_switch",
            "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
            casing_for,
            True,
            "l2",
            None,
        ),
        _row(
            "electrolyzer_cable_switch",
            "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
            cable_switch,
            True,
            "l2",
            None,
        ),
        _row(
            "distillery_wire_switch",
            "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java",
            wire_switch,
            True,
            "l1",
            None,
        ),
        _row(
            "language_materialZh",
            "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java",
            material_zh,
            True,
            "l1",
            None,
        ),
        _row(
            "language_kindZh",
            "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java",
            kind_zh,
            True,
            "l1",
            None,
        ),
        _row(
            "opening_variant_ids_java",
            "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java",
            opening,
            True,
            "l1",
            None,
        ),
        _row(
            "ku_machine_selected_kinds_java",
            "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java",
            t16,
            True,
            "l1",
            None,
        ),
        _row(
            "ru_machine_selected_kinds_java",
            "src/main/java/com/masson/cruciblecraft/registry/ModMachineVariants.java",
            t17,
            True,
            "l1",
            None,
        ),
        _row(
            "device_crucible_allowlist",
            "src/main/java/com/masson/cruciblecraft/machine/MachineMaterialRules.java",
            crucible_set,
            True,
            "device",
            None,
        ),
        _row(
            "device_anvil_allowlist",
            "src/main/java/com/masson/cruciblecraft/machine/MachineMaterialRules.java",
            anvil_set,
            True,
            "device",
            None,
        ),
        _row(
            "device_hammer_allowlist",
            "src/main/java/com/masson/cruciblecraft/machine/MachineMaterialRules.java",
            hammer_set,
            True,
            "device",
            None,
        ),
        _row(
            "creative_tab_crucible_constants",
            "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java",
            crucible_tab,
            True,
            "device",
            None,
        ),
        _row(
            "creative_tab_anvil_constants",
            "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java",
            anvil_tab,
            True,
            "device",
            None,
        ),
        _row(
            "tiered_processing_block_aliases",
            "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java",
            alias_blocks > 0,
            False,
            "alias",
            None,
            count=alias_blocks,
        ),
        _row(
            "tiered_processing_item_aliases",
            "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
            alias_items > 0,
            False,
            "alias",
            None,
            count=alias_items,
        ),
        _row(
            "casing_item_aliases",
            "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
            casing_constants > 0,
            False,
            "alias",
            None,
            count=casing_constants,
        ),
        _row(
            "processing_kind_behavior",
            "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
            True,
            False,
            "later",
            "later:kind_behavior",
        ),
        _row(
            "machine_kinds_catalog",
            "src/main/resources/data/cruciblecraft/machine_kinds.json",
            not kinds_json,
            True,
            "l1",
            None,
        ),
        _row(
            "machine_casings_catalog",
            "src/main/resources/data/cruciblecraft/machine_casings.json",
            not casings_json,
            True,
            "l2",
            None,
        ),
        _row(
            "device_materials_catalog",
            "src/main/resources/data/cruciblecraft/device_materials.json",
            not devices_json,
            True,
            "device",
            None,
        ),
        _row(
            "acquisition_overrides_catalog",
            "src/main/resources/data/cruciblecraft/machine_acquisition.json",
            not acquisition_json,
            True,
            "l1",
            None,
        ),
    ]
    return rows


def _row(
    list_id: str,
    path: str,
    present: bool,
    blocks_new_row: bool,
    axis: str,
    later: str | None,
    count: int | None = None,
) -> dict[str, Any]:
    status = "present" if present else "cleared"
    if later:
        status = "later"
        blocks_new_row = False
    if not present:
        blocks_new_row = False
        axis_status = "cleared"
    else:
        axis_status = axis
    return {
        "id": list_id,
        "path": path,
        "status": status,
        "count": 1 if count is None and present else (0 if count is None else count),
        "blocks_new_row": blocks_new_row,
        "axis": axis_status if later is None else "later",
        "later": later,
    }


def blocking_rows(rows: list[dict[str, Any]] | None = None) -> list[dict[str, Any]]:
    if rows is None:
        rows = second_list_rows()
    return [row for row in rows if row.get("blocks_new_row")]


def production_catalogs() -> dict[str, Any]:
    kinds = []
    for kind_id, template, zh, en, group in KIND_TABLE:
        row = {
            "id": f"cruciblecraft:{kind_id}",
            "acquisition_template": template,
            "lang_key_zh": zh,
            "lang_key_en": en,
        }
        if group:
            row["display_group"] = group
        kinds.append(row)
    casings = []
    for item_id, material, family, doubled, cable in CASING_ROWS:
        row = {
            "id": f"cruciblecraft:{item_id}",
            "material": f"cruciblecraft:{material}",
            "energy_family": family,
            "doubled": doubled,
            "creative_visible": True,
            "lang_key_zh": MATERIAL_LANG[material]["zh"]
            + ("双层机器外壳" if doubled else "机器外壳"),
            "lang_key_en": MATERIAL_LANG[material]["en"]
            + (" Double Machine Casing" if doubled else " Machine Casing"),
        }
        if cable:
            row["electrolyzer_cable_material"] = cable
        casings.append(row)
    overrides = [
        {
            "id": f"cruciblecraft:{path}",
            "acquisition_template": "machine_generic",
        }
        for path in GENERIC_TEMPLATE_OVERRIDES
    ]
    overrides.extend(
        {
            "id": f"cruciblecraft:{path}",
            "casing_item": "cruciblecraft:steel_double_machine_casing",
        }
        for path in STEEL_CASING_OVERRIDES
    )
    return {
        "machine_kinds": {
            "$schema": "schema/machine_kinds.schema.json",
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "kinds": kinds,
            "material_lang": {
                material: {"lang_key_zh": names["zh"], "lang_key_en": names["en"]}
                for material, names in MATERIAL_LANG.items()
            },
        },
        "machine_casings": {
            "$schema": "schema/machine_casings.schema.json",
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "casings": casings,
            "machine_material_extras": [
                {
                    "material": f"cruciblecraft:{material}",
                    "distillery_wire_material": wire,
                    "distillery_wire_prefix": prefix,
                }
                for material, wire, prefix in DISTILLERY_WIRE
            ],
        },
        "device_materials": {
            "$schema": "schema/device_materials.schema.json",
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "devices": {
                "crucible": {
                    "default_material": "ceramic",
                    "materials": [
                        {
                            "material_id": material,
                            "processing_tier": values["processing_tier"],
                            "creative_visible": True,
                        }
                        for material, values in DEVICE_FREEZE["crucible"][
                            "materials"
                        ].items()
                    ],
                },
                "anvil": {
                    "default_material": "stone",
                    "materials": [
                        {
                            "material_id": material,
                            "processing_tier": values["processing_tier"],
                            "durability": values["durability"],
                            "creative_visible": True,
                        }
                        for material, values in DEVICE_FREEZE["anvil"][
                            "materials"
                        ].items()
                    ],
                },
                "hammer": {
                    "default_material": "iron",
                    "materials": [
                        {
                            "material_id": material,
                            "processing_tier": values["processing_tier"],
                            "durability": values["durability"],
                            "creative_visible": False,
                        }
                        for material, values in DEVICE_FREEZE["hammer"][
                            "materials"
                        ].items()
                    ],
                },
            },
        },
        "machine_acquisition": {
            "$schema": "schema/machine_acquisition.schema.json",
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "opening_variant_ids": sorted(repair.OPENING_VARIANT_IDS),
            "overrides": overrides,
        },
    }


def write_json(path: Path, document: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
