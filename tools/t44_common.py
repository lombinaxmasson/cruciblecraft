#!/usr/bin/env python3
"""Shared paths, denominators, and expansion helpers for the T44 storage bundle."""
from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

from tools import builder_cli
from tools import t35_common as t35

ROOT = t35.ROOT
TOOLS = t35.TOOLS
SOURCE_REVISION = t35.SOURCE_REVISION
NORMALIZATION_SCHEMA_VERSION = 1

STORAGE_SOURCE_SITES = 28
STORAGE_REGISTRATIONS = 624
LOGISTICS_SOURCE_SITES = 1
LOGISTICS_REGISTRATIONS = 1
TOTAL_REGISTRATIONS = STORAGE_REGISTRATIONS + LOGISTICS_REGISTRATIONS
T36_LIVE_MACHINE_ROWS = 85
OPENING_EXECUTION_GAP = 3076
CLOSING_EXECUTION_GAP = 3076
COMPLETION_DELTA = 0
RECLASSIFICATION_DELTA = 0
REPRESENTATIVE_MATERIAL = "steel"
EXPECTED_SELECTION_SHA256 = (
    "5c16820e514d48012c8c52f1ab1572fe5c395a3be7e77225728faddb5287b4de"
)
OWNER = "portfolio:storage/t44_storage_bundle"
HOST = "cruciblecraft:storage"

ORACLE_RELATIVE = "gt6_referencable_port_code/gregtech6_w"
ORACLE_ROOT = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
ORACLE_JAVA = ORACLE_ROOT / "src" / "main" / "java"

WORK_SET = TOOLS / "t44_storage_work_set.json"
SOURCE = TOOLS / "t44_storage_source.json"
RECEIPT = TOOLS / "t44_storage_source_receipt.json"
REVIEW = TOOLS / "t44_storage_source_review.json"
PRODUCTION_LOCK = TOOLS / "t44_storage_production_lock.json"
RUNTIME_DEPENDENCY = TOOLS / "t44_runtime_dependency_manifest.json"
CATALOG_LEDGER = TOOLS / "t44_storage_catalog.json"
EQUIVALENCE = TOOLS / "t44_storage_equivalence.json"
PLAYER_PATH = TOOLS / "t44_storage_player_path.json"
PUBLICATION_DELTA = TOOLS / "t44_storage_publication_delta.json"
LOAD_MEASUREMENTS = TOOLS / "t44_storage_load_measurements.json"
LOAD_PROJECTION = TOOLS / "t44_storage_load_projection.json"
GAMETEST_RECEIPT = TOOLS / "t44_storage_gametest_receipt.json"
GAMETEST_LOG = TOOLS / "t44_gametest.log"
GAME_TEST_JAVA = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "T44StorageGameTests.java"
)
GAME_TEST_NAMESPACE = "cruciblecraft_t44"
GAME_TEST_COMMAND = (
    ".\\gradlew.bat runGameTestServer -Pt44Storage --no-daemon"
)
T43_CENSUS_DELTA = TOOLS / "t43_census_delta.json"
T43_CARD_TOPOLOGY = TOOLS / "t43_card_topology.json"
T43_READINESS = TOOLS / "t43_readiness.json"
T36_REPAIR_READINESS = TOOLS / "t36_repair_readiness.json"
CENSUS_DELTA = TOOLS / "t44_storage_census_delta.json"
CARD_TOPOLOGY = TOOLS / "t44_card_topology.json"
READINESS = TOOLS / "t44_readiness.json"
BUNDLED_CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "storage_variants.json"
)
BUNDLED_SCHEMA = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "schema"
    / "storage_variants.schema.json"
)

FAMILY_COUNTS = {
    "bookshelf": {"source_sites": 4, "expanded": 301},
    "bottle_crate": {"source_sites": 2, "expanded": 301},
    "mass_storage_barrel": {"source_sites": 13, "expanded": 13},
    "mass_storage_box": {"source_sites": 4, "expanded": 4},
    "locker": {"source_sites": 2, "expanded": 2},
    "drawer": {"source_sites": 1, "expanded": 1},
    "mass_storage_standard": {"source_sites": 1, "expanded": 1},
    "storage_inserter": {"source_sites": 1, "expanded": 1},
}
LOGISTICS_FAMILY = "mass_storage_logistics"
STORAGE_CENSUS_IDS = tuple(
    f"exclusion/{family}" for family in FAMILY_COUNTS
)
LOGISTICS_CENSUS_ID = "exclusion/mass_storage_logistics"

BARRELS = (
    {"legacy_id": "6998", "capacity": 5000, "visibility": "source_visible",
     "english": "Wooden Item Barrel (Cheap)", "chinese": "木制物品桶（廉价）",
     "wood": "any_wood", "acquisition_profile": "cheap_barrel"},
    {"legacy_id": "6992", "capacity": 5000, "visibility": "source_visible",
     "english": "Wooden Item Barrel (Cheap)", "chinese": "木制物品桶（廉价）",
     "wood": "any_wood", "acquisition_profile": "cheap_barrel"},
    {"legacy_id": "6991", "capacity": 5000, "visibility": "source_visible",
     "english": "Wooden Item Barrel (Cheap)", "chinese": "木制物品桶（廉价）",
     "wood": "any_wood", "acquisition_profile": "cheap_barrel"},
    {"legacy_id": "6990", "capacity": 5000, "visibility": "source_visible",
     "english": "Wooden Item Barrel (Cheap)", "chinese": "木制物品桶（廉价）",
     "wood": "any_wood", "acquisition_profile": "cheap_barrel"},
    {"legacy_id": "6999", "capacity": 10000, "visibility": "source_visible",
     "english": "Wooden Item Barrel", "chinese": "木制物品桶",
     "wood": "wood_treated", "acquisition_profile": "treated_barrel"},
    {"legacy_id": "6983", "capacity": 10000, "visibility": "source_hidden",
     "english": "Skyroot Item Barrel", "chinese": "天根木物品桶",
     "wood": "skyroot", "acquisition_profile": "source_hidden"},
    {"legacy_id": "6989", "capacity": 5000, "visibility": "source_hidden",
     "english": "Weedwood Item Barrel", "chinese": "杂草木物品桶",
     "wood": "weedwood", "acquisition_profile": "source_hidden"},
    {"legacy_id": "6988", "capacity": 10000, "visibility": "source_hidden",
     "english": "Livingwood Item Barrel", "chinese": "活木物品桶",
     "wood": "livingwood", "acquisition_profile": "source_hidden"},
    {"legacy_id": "6987", "capacity": 10000, "visibility": "source_hidden",
     "english": "Dreamwood Item Barrel", "chinese": "梦境木物品桶",
     "wood": "dreamwood", "acquisition_profile": "source_hidden"},
    {"legacy_id": "6986", "capacity": 10000, "visibility": "source_hidden",
     "english": "Shimmerwood Item Barrel", "chinese": "闪光木物品桶",
     "wood": "shimmerwood", "acquisition_profile": "source_hidden"},
    {"legacy_id": "6997", "capacity": 10000, "visibility": "source_hidden",
     "english": "Ironwood Item Barrel", "chinese": "铁木物品桶",
     "wood": "ironwood", "acquisition_profile": "source_hidden"},
    {"legacy_id": "6985", "capacity": 10000, "visibility": "source_hidden",
     "english": "Greatwood Item Barrel", "chinese": "巨木物品桶",
     "wood": "greatwood", "acquisition_profile": "source_hidden"},
    {"legacy_id": "6984", "capacity": 10000, "visibility": "source_hidden",
     "english": "Silverwood Item Barrel", "chinese": "银木物品桶",
     "wood": "silverwood", "acquisition_profile": "source_hidden"},
)
BOXES = (
    {"legacy_id": "6993", "capacity": 128, "english": "Plastic Storage Box (128)",
     "chinese": "塑料储物箱（128）"},
    {"legacy_id": "6994", "capacity": 256, "english": "Plastic Storage Box (256)",
     "chinese": "塑料储物箱（256）"},
    {"legacy_id": "6995", "capacity": 512, "english": "Plastic Storage Box (512)",
     "chinese": "塑料储物箱（512）"},
    {"legacy_id": "6996", "capacity": 1024, "english": "Plastic Storage Box (1024)",
     "chinese": "塑料储物箱（1024）"},
)

ORACLE_FILES = (
    "gregtech/loaders/b/Loader_MultiTileEntities.java",
    "gregapi/tileentity/inventories/MultiTileEntityBookShelf.java",
    "gregtech/tileentity/inventories/MultiTileEntityBottleCrate.java",
    "gregtech/tileentity/inventories/MultiTileEntityDrawerQuad.java",
    "gregtech/tileentity/inventories/MultiTileEntityLocker.java",
    "gregtech/tileentity/inventories/MultiTileEntityLockerCharging.java",
    "gregapi/tileentity/inventories/MultiTileEntityMassStorage.java",
    "gregtech/tileentity/inventories/MultiTileEntityMassStorageBarrel.java",
    "gregtech/tileentity/inventories/MultiTileEntityMassStorageBox.java",
    "gregtech/tileentity/inventories/MultiTileEntityMassStorageStandard.java",
    "gregtech/tileentity/inventories/MultiTileEntityMassStorageLogistics.java",
    "gregtech/tileentity/inventories/MultiTileEntityStorageInserter.java",
)

T14_HARD = {
    "datapack_authored_entries": 6600,
    "eager_publication_rows": 21000,
    "lazy_logical_rows": 56000,
    "lazy_cache_ceiling_rows": 4096,
    "sync_bytes": 67108864,
    "server_reload_ms": 10000,
    "server_index_ms": 1000,
    "client_reload_ms": 10000,
    "client_index_ms": 3000,
    "retained_memory_bytes": 536870912,
    "allocation_bytes": 536870912,
    "lookup_p95_ns": 2000000,
    "lookup_candidate_count": 128,
}


def relative(path: Path) -> str:
    return t35.relative(path)


def load_json(path: Path) -> Any:
    return t35.load_json(path)


def parse_managed(description: str, argv: list[str] | None = None):
    return builder_cli.parse_managed(description, argv)


def check_document(path: Path, document: dict[str, Any]) -> list[str]:
    return t35.check_generated_document(path, document)


def handle_rebind(args: Any, output: Path) -> bool:
    if not getattr(args, "rebind_currentness_only", False):
        return False
    from tools import currentness

    currentness.rebind_sidecar(output)
    print(f"rebound currentness sidecar for {relative(output)}")
    return True


def run_managed(
    description: str,
    output: Path,
    *,
    build,
    write=None,
    check=None,
    argv: list[str] | None = None,
) -> int:
    args = parse_managed(description, argv)
    if handle_rebind(args, output):
        return 0
    if args.write:
        if write is not None:
            write()
        else:
            t35.write_stable(output, build())
        print(f"Wrote {relative(output)}")
        return 0
    errors = check() if check is not None else check_document(output, build())
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"{relative(output)} is current")
    return 0


def selection_payload(runtime_ids: list[str]) -> bytes:
    return "".join(f"{item}\n" for item in runtime_ids).encode("utf-8")


def selection_sha256(runtime_ids: list[str]) -> str:
    return hashlib.sha256(selection_payload(runtime_ids)).hexdigest()


def reclaim_storage_sites() -> list[dict[str, Any]]:
    document = load_json(t35.EXCLUSION_RECLAIM)
    rows = []
    for site in document.get("source_sites") or []:
        family = str(site.get("canonical_family") or "")
        if family in FAMILY_COUNTS or family == LOGISTICS_FAMILY:
            rows.append(site)
    rows.sort(key=lambda row: (
        1 if row["canonical_family"] == LOGISTICS_FAMILY else 0,
        str(row["canonical_family"]),
        str(row["site_key"]),
    ))
    return rows


def assert_reclaim_counts(sites: list[dict[str, Any]]) -> None:
    storage = [row for row in sites if row["canonical_family"] != LOGISTICS_FAMILY]
    logistics = [row for row in sites if row["canonical_family"] == LOGISTICS_FAMILY]
    if len(storage) != STORAGE_SOURCE_SITES:
        raise ValueError(f"storage source sites {len(storage)} != {STORAGE_SOURCE_SITES}")
    if len(logistics) != LOGISTICS_SOURCE_SITES:
        raise ValueError("logistics source sites drifted")
    by_family: dict[str, tuple[int, int]] = {}
    for row in storage:
        family = str(row["canonical_family"])
        sites_n, expanded = by_family.get(family, (0, 0))
        by_family[family] = (sites_n + 1, expanded + int(row["multiplicity"]))
    for family, expected in FAMILY_COUNTS.items():
        got = by_family.get(family)
        if got != (expected["source_sites"], expected["expanded"]):
            raise ValueError(f"{family} reclaim counts drifted: {got}")
    if int(logistics[0]["multiplicity"]) != LOGISTICS_REGISTRATIONS:
        raise ValueError("logistics multiplicity drifted")


def runtime_id(path: str) -> str:
    return f"cruciblecraft:{path}"


def _base_row(
    *,
    family: str,
    site: dict[str, Any],
    expansion_key: str,
    path: str,
    behavior_profile: str,
    capacity_profile: str,
    inventory_profile: str,
    blockstate_profile: str,
    model_profile: str,
    visibility: str,
    acquisition_profile: str,
    english: str,
    chinese: str,
    slots: int,
    capacity: int,
    counts_toward_storage_624: bool,
    plank_index: int | None = None,
    charging: bool = False,
    logistics: bool = False,
    source_hidden_reason: str | None = None,
) -> dict[str, Any]:
    return {
        "runtime_id": runtime_id(path),
        "source_site_id": site["site_key"],
        "source_legacy_id": site["source_id_expression"],
        "family": family,
        "expansion_key": expansion_key,
        "behavior_profile": behavior_profile,
        "capacity_profile": capacity_profile,
        "inventory_profile": inventory_profile,
        "blockstate_profile": blockstate_profile,
        "model_profile": model_profile,
        "loot_profile": "drop_self_keep_contents",
        "visibility": visibility,
        "acquisition_profile": acquisition_profile,
        "counts_toward_storage_624": counts_toward_storage_624,
        "english": english,
        "chinese": chinese,
        "slots": slots,
        "capacity": capacity,
        "plank_index": plank_index,
        "charging": charging,
        "logistics": logistics,
        "representative_material": REPRESENTATIVE_MATERIAL,
        "source_revision": SOURCE_REVISION,
        "source_path": site["source_identity"]["source_path"],
        "source_hidden_reason": source_hidden_reason,
    }


def expand_variants(sites: list[dict[str, Any]] | None = None) -> list[dict[str, Any]]:
    sites = sites if sites is not None else reclaim_storage_sites()
    assert_reclaim_counts(sites)
    by_key = {row["site_key"]: row for row in sites}
    variants: list[dict[str, Any]] = []

    def site(key: str) -> dict[str, Any]:
        if key not in by_key:
            raise KeyError(key)
        return by_key[key]

    metal_shelf = site("loader:Storage|7100+aID|MultiTileEntityBookShelf")
    variants.append(_base_row(
        family="bookshelf",
        site=metal_shelf,
        expansion_key="7100",
        path="bookshelf_7100",
        behavior_profile="bookshelf",
        capacity_profile="bookshelf_28",
        inventory_profile="books_28",
        blockstate_profile="horizontal_facing",
        model_profile="bookshelf_metal",
        visibility="source_visible",
        acquisition_profile="steel_shaped",
        english="Bookshelf",
        chinese="书架",
        slots=28,
        capacity=28,
        counts_toward_storage_624=True,
    ))
    for offset, site_expr in ((7000, "i+7000"), (7900, "i+7900"), (7800, "i+7800")):
        plank_base = {7000: 0, 7900: 100, 7800: 200}[offset]
        loop_site = site(f"loader:Storage|{site_expr}|MultiTileEntityBookShelf")
        for index in range(100):
            plank_index = plank_base + index
            visible = plank_index == 0
            variants.append(_base_row(
                family="bookshelf",
                site=loop_site,
                expansion_key=str(offset + index),
                path=f"bookshelf_{offset + index}",
                behavior_profile="bookshelf",
                capacity_profile="bookshelf_28",
                inventory_profile="books_28",
                blockstate_profile="horizontal_facing",
                model_profile="bookshelf_wood",
                visibility="source_visible" if visible else "source_hidden",
                acquisition_profile="oak_planks" if visible else "source_hidden",
                english="Wooden Bookshelf",
                chinese="木制书架",
                slots=28,
                capacity=28,
                counts_toward_storage_624=True,
                plank_index=plank_index,
                source_hidden_reason=None if visible else "invalid_or_unmapped_plank",
            ))

    metal_crate = site("loader:Storage|8600+aID|MultiTileEntityBottleCrate")
    variants.append(_base_row(
        family="bottle_crate",
        site=metal_crate,
        expansion_key="8600",
        path="bottle_crate_8600",
        behavior_profile="bottle_crate",
        capacity_profile="bottle_crate_9",
        inventory_profile="bottles_9",
        blockstate_profile="horizontal_facing",
        model_profile="bottle_crate_metal",
        visibility="source_visible",
        acquisition_profile="steel_shaped",
        english="Bottlecrate",
        chinese="瓶箱",
        slots=9,
        capacity=9,
        counts_toward_storage_624=True,
    ))
    crate_loop = site("loader:Storage|i+8700|MultiTileEntityBottleCrate")
    for index in range(300):
        visible = index == 0
        variants.append(_base_row(
            family="bottle_crate",
            site=crate_loop,
            expansion_key=str(8700 + index),
            path=f"bottle_crate_{8700 + index}",
            behavior_profile="bottle_crate",
            capacity_profile="bottle_crate_9",
            inventory_profile="bottles_9",
            blockstate_profile="horizontal_facing",
            model_profile="bottle_crate_wood",
            visibility="source_visible" if visible else "source_hidden",
            acquisition_profile="oak_planks" if visible else "source_hidden",
            english="Wooden Bottlecrate",
            chinese="木制瓶箱",
            slots=9,
            capacity=9,
            counts_toward_storage_624=True,
            plank_index=index,
            source_hidden_reason=None if visible else "invalid_or_unmapped_plank",
        ))

    for barrel in BARRELS:
        barrel_site = site(
            f"loader:Storage|{barrel['legacy_id']}|MultiTileEntityMassStorageBarrel"
        )
        hidden = barrel["visibility"] == "source_hidden"
        variants.append(_base_row(
            family="mass_storage_barrel",
            site=barrel_site,
            expansion_key=barrel["legacy_id"],
            path=f"mass_storage_barrel_{barrel['legacy_id']}",
            behavior_profile="mass_storage",
            capacity_profile=f"barrel_{barrel['capacity']}",
            inventory_profile="mass_storage_single",
            blockstate_profile="horizontal_facing",
            model_profile="mass_storage_barrel",
            visibility=barrel["visibility"],
            acquisition_profile=barrel["acquisition_profile"],
            english=barrel["english"],
            chinese=barrel["chinese"],
            slots=1,
            capacity=barrel["capacity"],
            counts_toward_storage_624=True,
            source_hidden_reason="mod_wood_not_in_b0" if hidden else None,
        ))

    for box in BOXES:
        box_site = site(
            f"loader:Storage|{box['legacy_id']}|MultiTileEntityMassStorageBox"
        )
        variants.append(_base_row(
            family="mass_storage_box",
            site=box_site,
            expansion_key=box["legacy_id"],
            path=f"mass_storage_box_{box['legacy_id']}",
            behavior_profile="mass_storage",
            capacity_profile=f"box_{box['capacity']}",
            inventory_profile="mass_storage_single",
            blockstate_profile="horizontal_facing",
            model_profile="mass_storage_box",
            visibility="source_visible",
            acquisition_profile="plastic_box",
            english=box["english"],
            chinese=box["chinese"],
            slots=1,
            capacity=box["capacity"],
            counts_toward_storage_624=True,
        ))

    locker_site = site("loader:Storage|7300+aID|MultiTileEntityLocker")
    variants.append(_base_row(
        family="locker",
        site=locker_site,
        expansion_key="7300",
        path="locker_7300",
        behavior_profile="locker",
        capacity_profile="armor_4",
        inventory_profile="armor_4",
        blockstate_profile="horizontal_facing",
        model_profile="locker",
        visibility="source_visible",
        acquisition_profile="steel_shaped",
        english="Locker",
        chinese="储物柜",
        slots=4,
        capacity=4,
        counts_toward_storage_624=True,
    ))
    charging_site = site("loader:Storage|7500+aID|MultiTileEntityLockerCharging")
    variants.append(_base_row(
        family="locker",
        site=charging_site,
        expansion_key="7500",
        path="charging_locker_7500",
        behavior_profile="locker_charging",
        capacity_profile="armor_4",
        inventory_profile="armor_4",
        blockstate_profile="horizontal_facing",
        model_profile="charging_locker",
        visibility="source_visible",
        acquisition_profile="charging_locker",
        english="Charging Locker",
        chinese="充电储物柜",
        slots=4,
        capacity=4,
        counts_toward_storage_624=True,
        charging=True,
    ))

    drawer_site = site("loader:Storage|4000+aID|MultiTileEntityDrawerQuad")
    variants.append(_base_row(
        family="drawer",
        site=drawer_site,
        expansion_key="4000",
        path="drawer_4000",
        behavior_profile="drawer",
        capacity_profile="drawer_144",
        inventory_profile="drawer_4x36",
        blockstate_profile="horizontal_facing",
        model_profile="drawer",
        visibility="source_visible",
        acquisition_profile="steel_shaped",
        english="Compartment Drawer",
        chinese="分区抽屉",
        slots=144,
        capacity=144,
        counts_toward_storage_624=True,
    ))

    standard_site = site("loader:Storage|6000+aID|MultiTileEntityMassStorageStandard")
    variants.append(_base_row(
        family="mass_storage_standard",
        site=standard_site,
        expansion_key="6000",
        path="mass_storage_6000",
        behavior_profile="mass_storage",
        capacity_profile="standard_1000000",
        inventory_profile="mass_storage_single",
        blockstate_profile="horizontal_facing",
        model_profile="mass_storage_standard",
        visibility="source_visible",
        acquisition_profile="steel_shaped",
        english="Mass Storage",
        chinese="大容量仓储",
        slots=1,
        capacity=1_000_000,
        counts_toward_storage_624=True,
    ))

    inserter_site = site("loader:Storage|32751|MultiTileEntityStorageInserter")
    variants.append(_base_row(
        family="storage_inserter",
        site=inserter_site,
        expansion_key="32751",
        path="storage_inserter_32751",
        behavior_profile="storage_inserter",
        capacity_profile="inserter_scan",
        inventory_profile="none",
        blockstate_profile="horizontal_facing",
        model_profile="storage_inserter",
        visibility="source_visible",
        acquisition_profile="inserter",
        english="Storage Inserter",
        chinese="仓储投放器",
        slots=0,
        capacity=0,
        counts_toward_storage_624=True,
    ))

    logistics_site = site(
        "loader:Logistics|6200+aID|MultiTileEntityMassStorageLogistics"
    )
    variants.append(_base_row(
        family=LOGISTICS_FAMILY,
        site=logistics_site,
        expansion_key="6200",
        path="mass_storage_logistics_6200",
        behavior_profile="mass_storage_logistics",
        capacity_profile="standard_1000000",
        inventory_profile="mass_storage_single",
        blockstate_profile="horizontal_facing",
        model_profile="mass_storage_logistics",
        visibility="source_hidden",
        acquisition_profile="source_hidden",
        english="Logistics Mass Storage",
        chinese="物流大容量仓储",
        slots=1,
        capacity=1_000_000,
        counts_toward_storage_624=False,
        logistics=True,
        source_hidden_reason="logistics_cover_not_in_b0",
    ))

    validate_variants(variants)
    return variants


def validate_variants(variants: list[dict[str, Any]]) -> None:
    storage = [row for row in variants if row["counts_toward_storage_624"]]
    logistics = [row for row in variants if not row["counts_toward_storage_624"]]
    if len(storage) != STORAGE_REGISTRATIONS:
        raise ValueError(f"storage registrations {len(storage)} != {STORAGE_REGISTRATIONS}")
    if len(logistics) != LOGISTICS_REGISTRATIONS:
        raise ValueError("logistics registrations drifted")
    ids = [row["runtime_id"] for row in variants]
    if len(ids) != len(set(ids)):
        raise ValueError("duplicate runtime ids")
    sites = {row["source_site_id"] for row in storage}
    if len(sites) != STORAGE_SOURCE_SITES:
        raise ValueError(f"unmapped or extra storage sites: {len(sites)}")
    by_family: dict[str, int] = {}
    for row in storage:
        by_family[row["family"]] = by_family.get(row["family"], 0) + 1
    for family, expected in FAMILY_COUNTS.items():
        if by_family.get(family) != expected["expanded"]:
            raise ValueError(
                f"{family} expanded {by_family.get(family)} != {expected['expanded']}"
            )


def variant_ids(variants: list[dict[str, Any]] | None = None) -> list[str]:
    variants = variants if variants is not None else expand_variants()
    return [row["runtime_id"] for row in variants]


def oracle_revision() -> str | None:
    if not (ORACLE_ROOT / ".git").exists() and not (ORACLE_ROOT / ".git").is_file():
        git_dir = ORACLE_ROOT / ".git"
        if not git_dir.exists():
            return None
    try:
        completed = subprocess.run(
            ["git", "-C", str(ORACLE_ROOT), "rev-parse", "HEAD"],
            check=True,
            capture_output=True,
            text=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return None
    return completed.stdout.strip()


def oracle_dirty() -> bool:
    try:
        completed = subprocess.run(
            ["git", "-C", str(ORACLE_ROOT), "status", "--porcelain"],
            check=True,
            capture_output=True,
            text=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return True
    return bool(completed.stdout.strip())


def oracle_file_hashes() -> dict[str, str]:
    hashes: dict[str, str] = {}
    for relative_path in ORACLE_FILES:
        path = ORACLE_JAVA / relative_path
        if not path.is_file():
            raise FileNotFoundError(relative_path)
        hashes[relative_path] = t35.sha256_file(path)
    return hashes


def discovered_gametest_ids() -> list[str]:
    if not GAME_TEST_JAVA.is_file():
        return []
    text = GAME_TEST_JAVA.read_text(encoding="utf-8")
    names = re.findall(
        r"@GameTest\b[\s\S]*?public static void ([a-zA-Z0-9_]+)\(",
        text,
    )
    return sorted(name.lower() for name in names)


def parse_gametest_log(text: str, required: int) -> dict[str, Any]:
    match = re.search(r"All (\d+) required tests passed", text)
    if match:
        passed = int(match.group(1))
        return {
            "failed": 0,
            "passed": passed,
            "required_tests": passed,
            "status": "PASS" if passed == required else "FAIL",
        }
    failed_match = re.search(r"(\d+) required tests? failed", text)
    complete_match = re.search(r"(\d+)\s+GAME TESTS COMPLETE", text)
    failed = int(failed_match.group(1)) if failed_match else required
    total = int(complete_match.group(1)) if complete_match else required
    passed = max(0, total - failed)
    return {
        "failed": failed,
        "passed": passed,
        "required_tests": required,
        "status": "FAIL",
    }
