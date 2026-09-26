#!/usr/bin/env python3
"""Live R0 dump replay for semantic ordinary-closure waves."""
from __future__ import annotations

import hashlib
import json
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from gt6_recipe_templates import _stable_json
from tools import build_assembler_source as assembler
from tools import census_common as census
from tools import owner_partition_common as owner
from tools import smelter_stone_common as smelter_stone
from tools import block_object_common as block_object
from tools import bath_remainder_identities as bath_remainder_ids
from tools import bath_identity_common as bath_identity
from tools import bath_identities as identities
from tools import tool_head_prefix as thp
from tools.recipe_bulk.ordinary_r0 import (
    publication_group_for,
    remaining_owner_rows,
    remaining_summary,
    selection_sha256,
)
from tools.recipe_bulk.waves import recipe_wave

SOURCE_REVISION = census.SOURCE_REVISION
ORDINARY_CLASS = "ordinary_optional"
FINGERPRINT_EXCLUDE = frozenset(
    {
        "source_recipe_index",
        "source_row_sha256",
        "stable_id",
        "slot_notes",
        "unsupported_semantics",
        "shadow_order",
        "publication_group",
        "host",
        "owner",
        "cohort",
    }
)
EXTRA_PREFIX_ITEM_TO_FORM: dict[str, str] = {
    "gregtech:gt.meta.dustDiv72": "dust_div72",
    "gregtech:gt.meta.boule": "boule",
    "gregtech:gt.meta.railGt": "rail_gt",
    "gregtech:gt.meta.plantGtBerry": "plant_gt_berry",
    "gregtech:gt.meta.plantGtBlossom": "plant_gt_blossom",
    "gregtech:gt.meta.plantGtFiber": "plant_gt_fiber",
    "gregtech:gt.meta.plantGtTwig": "plant_gt_twig",
    "gregtech:gt.meta.plantGtWart": "plant_gt_wart",
}
OPTIONAL_PLANT_FORMS = frozenset(
    {
        "plant_gt_berry",
        "plant_gt_blossom",
        "plant_gt_fiber",
        "plant_gt_twig",
        "plant_gt_wart",
    }
)
SMELTER_UNPROVABLE = "gt.recipe.smelter#0111"
SMELTER_RECYCLING = ("gt.recipe.smelter#1829", "gt.recipe.smelter#1884")
ORDINARY_VANILLA_SPECIAL: dict[tuple[str, int], str] = {
    ("minecraft:coal", 0): "minecraft:coal",
    ("minecraft:coal", 1): "minecraft:charcoal",
    ("minecraft:anvil", 0): "minecraft:anvil",
    ("minecraft:anvil", 1): "minecraft:chipped_anvil",
    ("minecraft:anvil", 2): "minecraft:damaged_anvil",
    ("minecraft:dirt", 1): "minecraft:coarse_dirt",
    ("minecraft:dirt", 2): "minecraft:podzol",
    ("minecraft:stone_slab", 0): "minecraft:smooth_stone_slab",
    ("minecraft:stone_slab", 1): "minecraft:sandstone_slab",
    ("minecraft:stone_slab", 2): "minecraft:petrified_oak_slab",
    ("minecraft:stone_slab", 3): "minecraft:cobblestone_slab",
    ("minecraft:stone_slab", 4): "minecraft:brick_slab",
    ("minecraft:stone_slab", 5): "minecraft:stone_brick_slab",
    ("minecraft:stone_slab", 6): "minecraft:nether_brick_slab",
    ("minecraft:stone_slab", 7): "minecraft:quartz_slab",
    ("minecraft:stone_slab", 9): "minecraft:sandstone_slab",
    ("minecraft:stone_slab", 10): "minecraft:petrified_oak_slab",
    ("minecraft:stone_slab", 12): "minecraft:brick_slab",
    ("minecraft:stone_slab", 14): "minecraft:nether_brick_slab",
    ("minecraft:stone_slab", 15): "minecraft:quartz_slab",
    ("minecraft:stonebrick", 0): "minecraft:stone_bricks",
    ("minecraft:stonebrick", 1): "minecraft:mossy_stone_bricks",
    ("minecraft:stonebrick", 2): "minecraft:cracked_stone_bricks",
    ("minecraft:stonebrick", 3): "minecraft:chiseled_stone_bricks",
    ("minecraft:stone_slab", 8): "minecraft:smooth_stone_slab",
    ("minecraft:stone_slab", 11): "minecraft:smooth_stone_slab",
    ("minecraft:stone_slab", 13): "minecraft:smooth_stone_slab",
    ("minecraft:double_stone_slab", 0): "minecraft:smooth_stone",
    ("minecraft:double_stone_slab", 3): "minecraft:cobblestone",
    ("minecraft:double_stone_slab", 5): "minecraft:stone_bricks",
    ("minecraft:double_stone_slab", 8): "minecraft:smooth_stone",
    ("minecraft:double_stone_slab", 11): "minecraft:smooth_stone",
    ("minecraft:double_stone_slab", 13): "minecraft:smooth_stone",
    ("minecraft:tallgrass", 0): "minecraft:dead_bush",
    ("minecraft:tallgrass", 1): "minecraft:short_grass",
    ("minecraft:tallgrass", 2): "minecraft:fern",
    ("minecraft:fish", 0): "minecraft:cod",
    ("minecraft:fish", 1): "minecraft:salmon",
    ("minecraft:fish", 2): "minecraft:tropical_fish",
    ("minecraft:fish", 3): "minecraft:pufferfish",
    ("minecraft:cooked_fished", 0): "minecraft:cooked_cod",
    ("minecraft:cooked_fished", 1): "minecraft:cooked_salmon",
    ("minecraft:double_plant", 0): "minecraft:sunflower",
    ("minecraft:double_plant", 1): "minecraft:lilac",
    ("minecraft:double_plant", 2): "minecraft:tall_grass",
    ("minecraft:double_plant", 3): "minecraft:large_fern",
    ("minecraft:double_plant", 4): "minecraft:rose_bush",
    ("minecraft:double_plant", 5): "minecraft:peony",
    ("minecraft:wooden_slab", 0): "minecraft:oak_slab",
    ("minecraft:wooden_slab", 1): "minecraft:spruce_slab",
    ("minecraft:wooden_slab", 2): "minecraft:birch_slab",
    ("minecraft:wooden_slab", 3): "minecraft:jungle_slab",
    ("minecraft:wooden_slab", 4): "minecraft:acacia_slab",
    ("minecraft:wooden_slab", 5): "minecraft:dark_oak_slab",
    ("minecraft:wooden_slab", 8): "minecraft:oak_slab",
    ("minecraft:wooden_slab", 9): "minecraft:spruce_slab",
    ("minecraft:wooden_slab", 10): "minecraft:birch_slab",
    ("minecraft:wooden_slab", 11): "minecraft:jungle_slab",
    ("minecraft:wooden_slab", 12): "minecraft:acacia_slab",
    ("minecraft:wooden_slab", 13): "minecraft:dark_oak_slab",
    ("minecraft:planks", 0): "minecraft:oak_planks",
    ("minecraft:planks", 1): "minecraft:spruce_planks",
    ("minecraft:planks", 2): "minecraft:birch_planks",
    ("minecraft:planks", 3): "minecraft:jungle_planks",
    ("minecraft:planks", 4): "minecraft:acacia_planks",
    ("minecraft:planks", 5): "minecraft:dark_oak_planks",
    ("minecraft:brick_block", 0): "minecraft:bricks",
    ("minecraft:double_stone_slab", 1): "minecraft:sandstone",
    ("minecraft:double_stone_slab", 2): "minecraft:petrified_oak_slab",
    ("minecraft:double_stone_slab", 4): "minecraft:bricks",
    ("minecraft:double_stone_slab", 6): "minecraft:nether_bricks",
    ("minecraft:double_stone_slab", 7): "minecraft:quartz_block",
    ("minecraft:double_stone_slab", 9): "minecraft:sandstone",
    ("minecraft:double_stone_slab", 10): "minecraft:petrified_oak_slab",
    ("minecraft:double_stone_slab", 12): "minecraft:bricks",
    ("minecraft:double_stone_slab", 14): "minecraft:nether_bricks",
    ("minecraft:double_stone_slab", 15): "minecraft:quartz_block",
    ("minecraft:skull", 0): "minecraft:skeleton_skull",
    ("minecraft:skull", 1): "minecraft:wither_skeleton_skull",
    ("minecraft:skull", 2): "minecraft:zombie_head",
    ("minecraft:skull", 3): "minecraft:player_head",
    ("minecraft:skull", 4): "minecraft:creeper_head",
    ("minecraft:dye", 0): "minecraft:ink_sac",
    ("minecraft:dye", 1): "minecraft:red_dye",
    ("minecraft:dye", 2): "minecraft:green_dye",
    ("minecraft:dye", 3): "minecraft:cocoa_beans",
    ("minecraft:dye", 4): "minecraft:lapis_lazuli",
    ("minecraft:dye", 5): "minecraft:purple_dye",
    ("minecraft:dye", 6): "minecraft:cyan_dye",
    ("minecraft:dye", 7): "minecraft:light_gray_dye",
    ("minecraft:dye", 8): "minecraft:gray_dye",
    ("minecraft:dye", 9): "minecraft:pink_dye",
    ("minecraft:dye", 10): "minecraft:lime_dye",
    ("minecraft:dye", 11): "minecraft:yellow_dye",
    ("minecraft:dye", 12): "minecraft:light_blue_dye",
    ("minecraft:dye", 13): "minecraft:magenta_dye",
    ("minecraft:dye", 14): "minecraft:orange_dye",
    ("minecraft:dye", 15): "minecraft:bone_meal",
}
# 1.7.10 / 1.12 ids that 1.21.1 does not register. Leaving them as
# exact_runtime_id makes RegistryOps drop item_inputs while counts stay,
# and GTRecipe dies with inputs=0, counts=1.
UNSPLIT_VANILLA_ITEM_IDS = frozenset(
    {
        "minecraft:bed",
        "minecraft:boat",
        "minecraft:brick_block",
        "minecraft:cooked_fished",
        "minecraft:double_plant",
        "minecraft:double_stone_slab",
        "minecraft:fence_gate",
        "minecraft:fish",
        "minecraft:sapling",
        "minecraft:sign",
        "minecraft:skull",
        "minecraft:tallgrass",
        "minecraft:wooden_slab",
    }
)
ORDINARY_VANILLA_RENAMES: dict[str, str] = {
    "minecraft:lit_furnace": "minecraft:furnace",
    "minecraft:melon_block": "minecraft:melon",
    "minecraft:netherbrick": "minecraft:nether_brick",
    "minecraft:web": "minecraft:cobweb",
}
LEGACY_VANILLA_META_RENAMES: dict[tuple[str, int], str] = {
    ("minecraft:log", 0): "minecraft:oak_log",
    ("minecraft:log", 1): "minecraft:spruce_log",
    ("minecraft:log", 2): "minecraft:birch_log",
    ("minecraft:log", 3): "minecraft:jungle_log",
    ("minecraft:log2", 0): "minecraft:acacia_log",
    ("minecraft:log2", 1): "minecraft:dark_oak_log",
}
ORDINARY_VANILLA_WILDCARDS: dict[str, tuple[str, str]] = {
    "minecraft:log": ("cruciblecraft:gt6_legacy_log", "minecraft:oak_log"),
    "minecraft:log2": ("cruciblecraft:gt6_legacy_log2", "minecraft:acacia_log"),
    "minecraft:wool": ("minecraft:wool", "minecraft:white_wool"),
    "minecraft:wooden_slab": ("minecraft:wooden_slabs", "minecraft:oak_slab"),
    "minecraft:bed": ("minecraft:beds", "minecraft:white_bed"),
    "minecraft:boat": ("minecraft:boats", "minecraft:oak_boat"),
    "minecraft:fence_gate": ("minecraft:fence_gates", "minecraft:oak_fence_gate"),
    "minecraft:sapling": ("minecraft:saplings", "minecraft:oak_sapling"),
    "minecraft:sign": ("minecraft:signs", "minecraft:oak_sign"),
}


@dataclass(frozen=True)
class WorkFamily:
    family_id: str
    template_key: str
    record: dict[str, Any]
    owner: str
    relation_count: int


def wave_root(slug: str) -> Path:
    return census.TOOLS / "waves" / slug


def source_map_for(host: str) -> str:
    return "gt.recipe." + host.split(":", 1)[-1]


def dump_path_for(host: str) -> Path:
    return owner.DUMP_MAPS / f"{source_map_for(host)}.json"


def dump_exists(host: str) -> bool:
    return dump_path_for(host).is_file()


def load_work(host: str) -> list[WorkFamily]:
    families_doc = census.load_json(census.RECIPE_FAMILIES)
    if families_doc.get("source_revision") != SOURCE_REVISION:
        raise ValueError("t35_recipe_families source_revision drifted")
    by_id = {
        str(row.get("family_id") or ""): row
        for row in families_doc.get("families") or []
        if isinstance(row, dict)
    }
    work: list[WorkFamily] = []
    for row in remaining_owner_rows(host):
        family_id = str(row["family_id"])
        record = by_id.get(family_id)
        if record is None:
            raise ValueError(f"remaining family missing from t35 ledger: {family_id}")
        template_key = str(record.get("template_key") or row.get("template_key") or "")
        if template_key != str(row.get("template_key") or template_key):
            raise ValueError(f"{family_id}: owner lock template drifted")
        work.append(
            WorkFamily(
                family_id=family_id,
                template_key=template_key,
                record=record,
                owner=str(row.get("current_owner") or ""),
                relation_count=int(row.get("relation_count") or record.get("expanded_count") or 0),
            )
        )
    return work


_BASE_CATALOGS: assembler.Catalogs | None = None
_RUNTIME_MAPS: dict[str, Any] | None = None


def load_catalogs(required_forms: dict[str, set[str]] | None = None) -> assembler.Catalogs:
    global _BASE_CATALOGS
    if _BASE_CATALOGS is None:
        _BASE_CATALOGS = assembler.load_catalogs()
    catalogs = _BASE_CATALOGS
    prefix = dict(catalogs.prefix_item_to_form)
    prefix.update(identities.PREFIX_ITEM_TO_FORM_OVERLAY)
    prefix.update(EXTRA_PREFIX_ITEM_TO_FORM)
    registered = {key: set(value) for key, value in catalogs.registered_forms.items()}
    if required_forms:
        for material, forms in required_forms.items():
            registered.setdefault(material, set()).update(
                form for form in forms if form not in OPTIONAL_PLANT_FORMS
            )
    prefix_tags = dict(catalogs.prefix_tags)
    for form in list(identities.PREFIX_FORM_UNITS) + list(EXTRA_PREFIX_ITEM_TO_FORM.values()):
        if form not in prefix_tags:
            prefix_tags[form] = ("c", f"{form}s")
    return assembler.Catalogs(
        prefix_item_to_form=prefix,
        material_id_to_cc=catalogs.material_id_to_cc,
        registered_forms=registered,
        form_items=catalogs.form_items,
        prefix_tags=prefix_tags,
        fluid_to_cc=catalogs.fluid_to_cc,
        reachable=catalogs.reachable,
    )


def load_stone_runtime() -> dict[tuple[str, int], str]:
    catalog = census.load_json(smelter_stone.STONE_CATALOG)
    mapped: dict[tuple[str, int], str] = {}
    for identity in catalog.get("identities") or []:
        source_item = str(identity["source_item"])
        for variant in identity.get("variants") or []:
            mapped[(source_item, int(variant["meta"]))] = str(variant["runtime_id"])
    return mapped


def load_mte_runtime() -> dict[tuple[str, int], str]:
    mapped = identities.load_mte_runtime_map()
    smelter = census.TOOLS / "smelter_mte_identity_catalog.json"
    if smelter.is_file():
        catalog = census.load_json(smelter)
        for identity in catalog.get("identities") or []:
            mapped[(str(identity["source_item"]), int(identity["meta"]))] = str(
                identity["runtime_id"]
            )
    overlay = (
        census.TOOLS
        / "waves"
        / "content"
        / "electric-wire-cable-mte-fold"
        / "operand_runtime_map.json"
    )
    if overlay.is_file():
        document = census.load_json(overlay)
        for row in document.get("mappings") or []:
            mapped[
                ("gregtech:gt.multitileentity", int(row["meta"]))
            ] = str(row["runtime_id"])
    sanding_overlay = (
        census.TOOLS
        / "waves"
        / "machines"
        / "sanding"
        / "mte_runtime_overlay.json"
    )
    if sanding_overlay.is_file():
        document = census.load_json(sanding_overlay)
        for row in document.get("mappings") or []:
            mapped[
                (
                    str(row.get("source_item") or "gregtech:gt.multitileentity"),
                    int(row["meta"]),
                )
            ] = str(row["runtime_id"])
    implosion_overlay = (
        census.TOOLS
        / "waves"
        / "machines"
        / "implosion-compressor"
        / "mte_runtime_overlay.json"
    )
    if implosion_overlay.is_file():
        document = census.load_json(implosion_overlay)
        for row in document.get("mappings") or []:
            mapped[
                (
                    str(row.get("source_item") or "gregtech:gt.multitileentity"),
                    int(row["meta"]),
                )
            ] = str(row["runtime_id"])
    return mapped


def load_block_runtime() -> dict[tuple[str, int], str]:
    return identities.load_block_runtime_map()


def load_semantic_object_overlay() -> dict[tuple[str, int | None], dict[str, Any]]:
    mapped: dict[tuple[str, int | None], dict[str, Any]] = {}
    bundled = (
        census.ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "semantic_object_catalog.json"
    )
    if bundled.is_file():
        document = census.load_json(bundled)
        for identity in document.get("identities") or []:
            source_item = str(identity.get("source_item") or "")
            meta = identity.get("meta")
            key = (source_item, int(meta) if isinstance(meta, int) else None)
            mapped[key] = dict(identity)
    for slug in (
        "smelter/ordinary-closure",
        "mixer/ordinary-closure",
        "drying/ordinary-closure",
        "electrolyzer/ordinary-closure",
        "centrifuge/ordinary-closure",
        "autoclave/ordinary-closure",
        "compressor/ordinary-closure",
    ):
        path = wave_root(slug) / "object_catalog.json"
        if not path.is_file():
            continue
        document = census.load_json(path)
        for identity in document.get("identities") or []:
            source_item = str(identity.get("source_item") or "")
            meta = identity.get("meta")
            key = (source_item, int(meta) if isinstance(meta, int) else None)
            mapped[key] = identity
    return mapped


def load_item_overlay() -> dict[tuple[str, int | None], dict[str, Any]]:
    overlay = dict(bath_remainder_ids.load_bath_remainder_item_overlay())
    overlay.update(identities.load_bath_identity_item_overlay())
    overlay.update(load_semantic_object_overlay())
    return overlay


def object_kind_for(item_id: str) -> str | None:
    kind = identities.classify_source_item(item_id)
    if kind in {"tool_head", "multiitem"}:
        return kind
    if item_id.startswith("gregtech:gt.armor."):
        return "armor"
    if kind == "block":
        return "block"
    return None


def object_runtime(item_id: str, meta: int, kind: str) -> str:
    mapped_kind = "tool_head" if kind == "tool_head" else (
        "multiitem" if kind == "multiitem" else "object"
    )
    return identities.runtime_id_for(mapped_kind, item_id, meta)


_LEDGER_FLUID_OVERLAY: dict[str, str] | None = None


_LEDGER_ITEM_RUNTIME: dict[tuple[str, int], str] | None = None


def load_ledger_item_runtime() -> dict[tuple[str, int], str]:
    global _LEDGER_ITEM_RUNTIME
    if _LEDGER_ITEM_RUNTIME is not None:
        return _LEDGER_ITEM_RUNTIME
    path = census.TOOLS / "global_build_identity_ledger.v2.json"
    mapped: dict[tuple[str, int], str] = {}
    if path.is_file():
        for row in census.load_json(path).get("records") or []:
            key = str(row.get("source_key") or "")
            if "|" in key:
                key = key.split("|", 1)[1]
            target = str(row.get("target_identity") or "")
            if not target.startswith(("minecraft:", "cruciblecraft:")):
                continue
            for prefix in ("block:", "item:", "stone:"):
                if not key.startswith(prefix):
                    continue
                rest = key[len(prefix) :]
                if "@" not in rest:
                    continue
                item, meta = rest.rsplit("@", 1)
                if meta.isdigit():
                    mapped[(item, int(meta))] = target
                break
    _LEDGER_ITEM_RUNTIME = mapped
    return mapped


def proven_item_runtime(
    item_id: str,
    meta: int | None,
    *,
    stone_runtime: dict[tuple[str, int], str] | None = None,
    mte_runtime: dict[tuple[str, int], str] | None = None,
    block_runtime: dict[tuple[str, int], str] | None = None,
) -> str | None:
    if not isinstance(meta, int):
        return None
    key = (item_id, meta)
    if mte_runtime and key in mte_runtime:
        return mte_runtime[key]
    if block_runtime and key in block_runtime:
        return block_runtime[key]
    if stone_runtime and key in stone_runtime:
        return stone_runtime[key]
    return load_ledger_item_runtime().get(key)


def load_ledger_fluid_overlay() -> dict[str, str]:
    global _LEDGER_FLUID_OVERLAY
    if _LEDGER_FLUID_OVERLAY is not None:
        return _LEDGER_FLUID_OVERLAY
    path = census.TOOLS / "global_build_identity_ledger.v2.json"
    overlay: dict[str, str] = {}
    if path.is_file():
        for row in census.load_json(path).get("records") or []:
            key = str(row.get("source_key") or "")
            target = str(row.get("target_identity") or "")
            if key.startswith("fluid:") and target:
                overlay[key[len("fluid:") :]] = target
    _LEDGER_FLUID_OVERLAY = overlay
    return overlay


def load_fluid_overlay() -> dict[str, str]:
    overlay = dict(bath_remainder_ids.load_fluid_base_overlay())
    overlay.update(identities.load_bath_identity_fluid_overlay())
    for slug in (
        "smelter/ordinary-closure",
        "mixer/ordinary-closure",
        "drying/ordinary-closure",
        "electrolyzer/ordinary-closure",
        "centrifuge/ordinary-closure",
        "autoclave/ordinary-closure",
        "compressor/ordinary-closure",
    ):
        path = wave_root(slug) / "fluid_mapping.json"
        if not path.is_file():
            continue
        document = census.load_json(path)
        for row in document.get("mapping") or []:
            source_fluid = str(row.get("source_fluid") or "")
            runtime = str(row.get("cc_fluid_id") or "")
            if source_fluid and runtime:
                overlay[source_fluid] = runtime
                overlay[source_fluid.split(":", 1)[-1]] = runtime
    overlay.update(load_ledger_fluid_overlay())
    return overlay


def _empty_operand(item: dict[str, Any]) -> dict[str, Any]:
    return {
        "mapping": "blocked_unmapped",
        "source": {
            "item": str(item.get("item") or ""),
            "count": int(item.get("count") or 0),
            "meta": item.get("meta"),
            "displayName": item.get("displayName"),
        },
        "value": None,
        "runtime_id": None,
        "material": None,
        "form": None,
        "tag": None,
        "alias": None,
        "reachable": None,
        "slot_class": "occupied",
    }


def extruder_shape_runtime(meta: int) -> str | None:
    cached = getattr(extruder_shape_runtime, "by_meta", None)
    if cached is None:
        from tools.gt6_resolve import extruder_shapes

        cached = {
            int(row["meta"]): str(row["cc_item"])
            for row in extruder_shapes().values()
            if isinstance(row.get("meta"), int)
            and row.get("live")
            and row.get("cc_item")
        }
        extruder_shape_runtime.by_meta = cached
    return cached.get(meta)


def _assert_runtime(runtime: str) -> str:
    if not runtime.startswith(("minecraft:", "cruciblecraft:")):
        raise ValueError(f"runtime id must be minecraft: or cruciblecraft:: {runtime}")
    return runtime


_STONE_VARIANT = {
    0: "stone",
    1: "cobble",
    2: "mossy_cobble",
    3: "bricks",
    4: "cracked_bricks",
    5: "mossy_bricks",
    6: "chiseled",
    7: "smooth",
    8: "reinforced_bricks",
    9: "redstoned_bricks",
    10: "tiles",
    11: "small_tiles",
    12: "small_bricks",
    13: "windmill_tiles_a",
    14: "windmill_tiles_b",
    15: "square_bricks",
}
_SLAB_FACE = ("down", "up", "north", "south", "west", "east")
_STONE_SLUG = {
    "granite.black": "granite_black",
    "granite.red": "granite_red",
    "prismarine.dark": "prismarine_dark",
    "prismarine.light": "prismarine",
}
_GLASS_COLORS = (
    ("light blue", "light_blue"),
    ("light gray", "light_gray"),
    ("light grey", "light_gray"),
    ("dark gray", "gray"),
    ("white", "white"),
    ("orange", "orange"),
    ("magenta", "magenta"),
    ("yellow", "yellow"),
    ("lime", "lime"),
    ("pink", "pink"),
    ("gray", "gray"),
    ("grey", "gray"),
    ("cyan", "cyan"),
    ("purple", "purple"),
    ("blue", "blue"),
    ("brown", "brown"),
    ("green", "green"),
    ("red", "red"),
    ("black", "black"),
)


def _live_registry_ids() -> set[str]:
    cached = getattr(_live_registry_ids, "value", None)
    if cached is not None:
        return cached
    gate = census.load_json(
        census.ROOT
        / "src"
        / "main"
        / "resources"
        / "census"
        / "runtime_registry_gate.json"
    )
    categories = gate.get("categories") or {}
    cached = set(categories.get("items") or []) | set(categories.get("blocks") or [])
    _live_registry_ids.value = cached
    return cached


def _if_live(runtime: str | None) -> str | None:
    if runtime and runtime in _live_registry_ids():
        return runtime
    return None


def _stone_block_runtime(item_id: str, meta: int) -> str | None:
    tail = item_id.removeprefix("gregtech:gt.stone.")
    face = None
    if ".slab." in tail:
        tail, slab_s = tail.rsplit(".slab.", 1)
        if not slab_s.isdigit():
            return None
        face = _SLAB_FACE[int(slab_s) % 6]
    variant = _STONE_VARIANT.get(meta)
    if variant is None:
        return None
    stone = _STONE_SLUG.get(tail, tail.replace(".", "_"))
    path = f"cruciblecraft:{stone}/{variant}"
    if face:
        path = f"{path}/slab_{face}"
    return _if_live(path)


def _glass_block_runtime(item_id: str, display: str) -> str | None:
    if not item_id.startswith("gregtech:gt.block.glass"):
        return None
    family = "glow_glass" if ".glow" in item_id else "glass"
    face = None
    if ".slab." in item_id:
        slab_s = item_id.rsplit(".slab.", 1)[-1]
        if not slab_s.isdigit():
            return None
        face = _SLAB_FACE[int(slab_s) % 6]
    lowered = display.lower()
    color = next((slug for word, slug in _GLASS_COLORS if word in lowered), None)
    if color is None:
        return None
    path = f"cruciblecraft:{family}/{color}"
    if face:
        path = f"{path}/slab_{face}"
    return _if_live(path)


def _technological_part_runtime(meta: int) -> str | None:
    cached = getattr(_technological_part_runtime, "by_meta", None)
    if cached is None:
        document = census.load_json(
            census.ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "technological_parts.json"
        )
        cached = {
            int(row["source_id"]): str(row["id"])
            for row in document.get("parts") or []
            if isinstance(row.get("source_id"), int) and row.get("id")
        }
        _technological_part_runtime.by_meta = cached
    return _if_live(cached.get(meta))


def _energium_runtime(meta: int) -> str | None:
    cached = getattr(_energium_runtime, "by_meta", None)
    if cached is None:
        document = census.load_json(
            census.ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "energy_battery_tiers.json"
        )
        cached = {}
        for row in document.get("tiers") or document.get("batteries") or []:
            source = row.get("source_id")
            runtime = row.get("id")
            if isinstance(source, int) and runtime:
                cached[int(source)] = str(runtime)
        if not cached:
            for row in document.get("entries") or []:
                source = row.get("source_id")
                runtime = row.get("id")
                if isinstance(source, int) and runtime:
                    cached[int(source)] = str(runtime)
        _energium_runtime.by_meta = cached
    return _if_live(cached.get(meta))


def _pipe_runtime(meta: int) -> str | None:
    cached = getattr(_pipe_runtime, "by_meta", None)
    if cached is None:
        from tools import gt6_pipes
        from tools.gt6_pipe_cable_baseline import (
            FLUID_GAUGES,
            ITEM_GAUGES,
            PIPE_SOURCE,
            _cc_material,
        )

        source = gt6_pipes.load(PIPE_SOURCE)

        def material_for(symbol: str) -> str | None:
            from tools import gt6_resolve

            resolved = gt6_resolve.resolve(f"MT.{symbol}").get("cc_material")
            slugged = _cc_material(symbol)
            forms = [
                form
                for _spec, _offset, form, _diameter, _extra in (
                    *FLUID_GAUGES,
                    *ITEM_GAUGES,
                )
                if form
            ]
            for candidate in (resolved, slugged):
                if not isinstance(candidate, str) or not candidate:
                    continue
                if any(
                    _if_live(f"cruciblecraft:{candidate}/{form}") for form in forms
                ):
                    return candidate
            return None

        mapped: dict[int, str] = {}
        for row in source["direct_addFluidPipes_calls"]:
            material = material_for(str(row["source_symbol"]))
            if not material:
                continue
            for _spec, offset, form, _diameter, _tanks in FLUID_GAUGES:
                if not form:
                    continue
                runtime = _if_live(f"cruciblecraft:{material}/{form}")
                if runtime:
                    mapped[int(row["base_id"]) + int(offset)] = runtime
        for row in source["direct_addItemPipes_calls"]:
            material = material_for(str(row["source_symbol"]))
            if not material:
                continue
            for _spec, offset, form, _diameter, _restrictive in ITEM_GAUGES:
                if not form:
                    continue
                runtime = _if_live(f"cruciblecraft:{material}/{form}")
                if runtime:
                    mapped[int(row["base_id"]) + int(offset)] = runtime
        cached = mapped
        _pipe_runtime.by_meta = cached
    return cached.get(meta)


# GT6 ore.broken / ore.normal stone hosts. The material meta is the ore,
# the item path is the stone. Sediment hosts are not unique overworld cubes.
_ORE_STONE_HOSTS = {
    "andesite": "andesite",
    "basalt": "basalt",
    "blackgranite": "granite_black",
    "blueschist": "blueschist",
    "default": "stone",
    "diorite": "diorite",
    "endstone": "end_stone",
    "granite": "granite",
    "gravel": "gravel",
    "greenschist": "greenschist",
    "kimberlite": "kimberlite",
    "komatiite": "komatiite",
    "limestone": "limestone",
    "marble": "marble",
    "mud": "mud",
    "netherrack": "netherrack",
    "prismarine.dark": "prismarine_dark",
    "prismarine.light": "prismarine",
    "quartzite": "quartzite",
    "redgranite": "granite_red",
    "redsand": "red_sand",
    "sand": "sand",
    "sandstone": "sandstone",
    "shale": "shale",
    "slate": "slate",
}
_ORE_HOST_RUNTIME = {
    "broken": "cruciblecraft:gt_broken_ore",
    "normal": "cruciblecraft:gt_hosted_ore",
}


def _ore_stone_operand(
    item: dict[str, Any],
    catalogs: assembler.Catalogs,
) -> dict[str, Any] | None:
    item_id = str(item.get("item") or "")
    prefix = "gregtech:gt.meta.ore."
    if not item_id.startswith(prefix):
        return None
    kind, _, stone = item_id[len(prefix):].partition(".")
    runtime = _ORE_HOST_RUNTIME.get(kind)
    host = _ORE_STONE_HOSTS.get(stone)
    meta = item.get("meta")
    if runtime is None or host is None or not isinstance(meta, int):
        return None
    material = catalogs.material_id_to_cc.get(meta)
    if not material:
        return None
    operand = _empty_operand(item)
    operand.update(
        {
            "mapping": "proven_equivalent",
            "value": runtime,
            "runtime_id": _assert_runtime(runtime),
            "reachable": True,
            "material": material,
            "kind": "ore_host",
            "_components": {
                "cruciblecraft:ore_material": material,
                "minecraft:block_state": {"host": host},
            },
        }
    )
    return operand


def map_item_operand(
    item: dict[str, Any],
    catalogs: assembler.Catalogs,
    *,
    stone_runtime: dict[tuple[str, int], str],
    mte_runtime: dict[tuple[str, int], str],
    block_runtime: dict[tuple[str, int], str],
    item_overlay: dict[tuple[str, int | None], dict[str, Any]],
    reused_aliases: dict[tuple[str, int], str],
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    item_id = str(item.get("item") or "")
    meta = item.get("meta")
    meta_key = int(meta) if isinstance(meta, int) else None
    proven = proven_item_runtime(
        item_id,
        meta_key,
        stone_runtime=stone_runtime,
        mte_runtime=mte_runtime,
        block_runtime=block_runtime,
    )
    if proven:
        if item_id == "gregtech:gt.multiitem.technological" and isinstance(meta, int):
            part_runtime = _technological_part_runtime(meta)
            if part_runtime:
                proven = part_runtime
        operand = _empty_operand(item)
        runtime = _assert_runtime(proven)
        operand.update(
            {
                "mapping": "proven_equivalent",
                "value": runtime,
                "runtime_id": runtime,
                "reachable": True,
            }
        )
        if item_id == identities.CIRCUIT_ITEM and isinstance(meta, int):
            operand["_components"] = {
                "cruciblecraft:circuit_config": meta,
            }
            operand["slot_class"] = (
                "catalyst"
                if int(item.get("count") or 0) == 0
                else "occupied"
            )
        return operand, []
    overlay = item_overlay.get((item_id, meta_key))
    if overlay and overlay.get("runtime_id"):
        runtime = str(overlay["runtime_id"])
        runtime = thp.rewrite_published_runtime(runtime, item_id, meta_key)
        diagnostic_prefix = item_id.startswith("gregtech:gt.meta.") and (
            "/gt_object/" in f"/{runtime.split(':', 1)[-1]}"
        )
        if not diagnostic_prefix:
            operand = _empty_operand(item)
            runtime = _assert_runtime(runtime)
            operand.update(
                {
                    "mapping": str(overlay.get("mapping_class") or "proven_equivalent"),
                    "value": runtime,
                    "runtime_id": runtime,
                    "reachable": True,
                }
            )
            return operand, []
    if smelter_stone.is_stone_item(item_id) and isinstance(meta, int):
        operand = _empty_operand(item)
        runtime = stone_runtime.get((item_id, meta))
        if runtime:
            operand.update(
                {
                    "mapping": "proven_equivalent",
                    "value": runtime,
                    "runtime_id": _assert_runtime(runtime),
                    "reachable": True,
                    "smelter_stone_class": "SOURCE_BACKED",
                }
            )
            return operand, []
        native = NATIVE_ITEM_EQUIVALENTS.get((item_id, meta)) or _stone_block_runtime(
            item_id, meta
        )
        if native:
            operand.update(
                {
                    "mapping": "proven_equivalent",
                    "value": native,
                    "runtime_id": _assert_runtime(native),
                    "reachable": True,
                    "smelter_stone_class": "SOURCE_BACKED",
                }
            )
            return operand, []
        operand["value"] = f"{item_id}@{meta}"
        return operand, [f"unmapped stone {item_id}@{meta}"]
    if isinstance(meta, int) and (item_id, meta) in reused_aliases:
        operand = _empty_operand(item)
        runtime = _assert_runtime(reused_aliases[(item_id, meta)])
        operand.update(
            {
                "mapping": "proven_equivalent",
                "value": runtime,
                "runtime_id": runtime,
                "alias": runtime,
                "reachable": True,
            }
        )
        return operand, []
    if item_id == identities.CIRCUIT_ITEM:
        operand = _empty_operand(item)
        if not isinstance(meta, int):
            operand.update(
                {
                    "mapping": "blocked_unmapped",
                    "value": f"{item_id}@{meta}",
                }
            )
            return operand, [f"circuit missing integer meta {item_id}@{meta}"]
        operand["source"]["meta"] = meta
        operand["_components"] = {
            "cruciblecraft:circuit_config": meta,
        }
        operand.update(
            {
                "mapping": "proven_equivalent",
                "value": identities.PROGRAMMED_CIRCUIT,
                "runtime_id": identities.PROGRAMMED_CIRCUIT,
                "reachable": True,
                "slot_class": "catalyst" if int(item.get("count") or 0) == 0 else "occupied",
            }
        )
        return operand, []
    wildcard = None
    if identities.vanilla_wildcard_meta(item_id, meta):
        wildcard = (
            identities.vanilla_wildcard_tag(item_id),
            identities.vanilla_wildcard_representative(item_id),
        )
        if not wildcard[0] and item_id in ORDINARY_VANILLA_WILDCARDS:
            wildcard = ORDINARY_VANILLA_WILDCARDS[item_id]
    elif meta == "*" and item_id in ORDINARY_VANILLA_WILDCARDS:
        wildcard = ORDINARY_VANILLA_WILDCARDS[item_id]
    if wildcard and wildcard[0] and wildcard[1]:
        tag, representative = wildcard
        operand = _empty_operand(item)
        operand.update(
            {
                "mapping": "vanilla_wildcard_tag",
                "value": tag,
                "runtime_id": _assert_runtime(representative),
                "tag": tag,
                "reachable": True,
            }
        )
        return operand, []
    vanilla = ORDINARY_VANILLA_SPECIAL.get((item_id, int(meta) if isinstance(meta, int) else -1))
    if vanilla is None and isinstance(meta, int):
        vanilla = LEGACY_VANILLA_META_RENAMES.get((item_id, meta))
    if vanilla is None and item_id == "minecraft:brick_block":
        vanilla = "minecraft:bricks"
    if vanilla is None and item_id in ORDINARY_VANILLA_RENAMES:
        vanilla = ORDINARY_VANILLA_RENAMES[item_id]
    if vanilla is None:
        vanilla = identities.vanilla_meta_runtime(item_id, meta)
    if vanilla:
        operand = _empty_operand(item)
        operand.update(
            {
                "mapping": "exact_runtime_id",
                "value": vanilla,
                "runtime_id": _assert_runtime(vanilla),
                "reachable": True,
            }
        )
        return operand, []
    if item_id in UNSPLIT_VANILLA_ITEM_IDS:
        operand = _empty_operand(item)
        operand.update(
            {
                "mapping": "blocked_unmapped",
                "value": f"{item_id}@{meta}",
            }
        )
        return operand, [f"unsplit 1.7.10 vanilla {item_id}@{meta}"]
    if item_id == "gregtech:gt.multitileentity" and isinstance(meta, int):
        operand = _empty_operand(item)
        runtime = (
            mte_runtime.get((item_id, meta))
            or _pipe_runtime(meta)
            or _energium_runtime(meta)
        )
        if runtime:
            operand.update(
                {
                    "mapping": "proven_equivalent",
                    "value": runtime,
                    "runtime_id": _assert_runtime(runtime),
                    "reachable": True,
                    "kind": "mte",
                }
            )
            return operand, []
        operand["value"] = f"{item_id}@{meta}"
        return operand, [f"unmapped MTE {item_id}@{meta}"]
    if item_id == "gregtech:gt.multiitem.technological" and isinstance(meta, int):
        operand = _empty_operand(item)
        runtime = extruder_shape_runtime(meta) or _technological_part_runtime(meta)
        if runtime:
            operand.update(
                {
                    "mapping": "proven_equivalent",
                    "value": runtime,
                    "runtime_id": _assert_runtime(runtime),
                    "reachable": True,
                    "kind": "extruder_shape",
                }
            )
            return operand, []
        operand["value"] = f"{item_id}@{meta}"
        return operand, [f"unmapped extruder shape {item_id}@{meta}"]
    if item_id.startswith("gregtech:gt.block."):
        operand = _empty_operand(item)
        runtime = block_runtime.get((item_id, int(meta) if isinstance(meta, int) else 0))
        if runtime is None:
            runtime = _glass_block_runtime(item_id, str(item.get("displayName") or ""))
        if runtime:
            operand.update(
                {
                    "mapping": "proven_equivalent",
                    "value": runtime,
                    "runtime_id": _assert_runtime(runtime),
                    "reachable": True,
                    "kind": "block",
                }
            )
            return operand, []
    object_kind = object_kind_for(item_id)
    if object_kind:
        use_meta = meta
        if use_meta == "*" or use_meta is None:
            use_meta = 0
        if isinstance(use_meta, int):
            operand = _empty_operand(item)
            runtime = _assert_runtime(object_runtime(item_id, use_meta, object_kind))
            registered = object_kind == "multiitem" or (
                object_kind == "tool_head" and thp.is_mapped(item_id, use_meta)
            )
            if not registered:
                operand["value"] = f"{item_id}@{use_meta}"
                return operand, [
                    f"unregistered {object_kind} {item_id}@{use_meta}"
                ]
            operand.update(
                {
                    "mapping": (
                        "vanilla_wildcard_tag"
                        if meta == "*"
                        else "explicit_object_expression"
                    ),
                    "value": runtime,
                    "runtime_id": runtime,
                    "reachable": True,
                    "kind": object_kind,
                }
            )
            return operand, []
    hosted = _ore_stone_operand(item, catalogs)
    if hosted is not None:
        return hosted, []
    operand, errors = assembler.map_item_operand(item, catalogs, side=side)
    runtime = str(operand.get("runtime_id") or "")
    renamed = identities.VANILLA_RENAMES.get(runtime)
    if renamed:
        operand = dict(operand)
        operand["runtime_id"] = renamed
        if operand.get("value") == runtime:
            operand["value"] = renamed
        runtime = renamed
    if runtime:
        try:
            _assert_runtime(runtime)
        except ValueError:
            operand["mapping"] = "blocked_unmapped"
            operand["runtime_id"] = None
            errors = list(errors) + [f"unpublished runtime {side} {item_id}@{meta}"]
    elif operand.get("mapping") != "blocked_unmapped":
        operand["mapping"] = "blocked_unmapped"
        errors = list(errors) + [f"null runtime {side} {item_id}@{meta}"]
    return operand, errors


def map_fluid_operand(
    fluid: dict[str, Any],
    catalogs: assembler.Catalogs,
    fluid_overlay: dict[str, str],
    *,
    side: str,
) -> tuple[dict[str, Any], list[str]]:
    operand, errors = assembler.map_fluid_operand(fluid, catalogs, side=side)
    if operand.get("runtime_id"):
        return operand, errors
    fluid_id = str((operand.get("source") or {}).get("fluid") or fluid.get("fluid") or "")
    mapped = (
        fluid_overlay.get(fluid_id)
        or fluid_overlay.get(fluid_id.split(":", 1)[-1])
        or native_fluid_equivalent(fluid_id)
    )
    if not mapped:
        return operand, errors
    operand.update(
        {
            "mapping": "proven_equivalent",
            "value": mapped,
            "runtime_id": _assert_runtime(mapped),
            "reachable": True,
        }
    )
    errors = [error for error in errors if "blocked_unmapped" not in error]
    return operand, errors


def load_ordinary_indices(host: str) -> set[int]:
    source_map = source_map_for(host)
    if host.endswith(":mixer"):
        by_template = owner.mixer_ordinary_optional_recipe_indexes()
        indices: set[int] = set()
        for members in by_template.values():
            indices.update(members)
        return indices
    maps = list(
        (census.load_json(owner.T21_TEMPLATE_DENOMINATOR).get("encoding") or {}).get("maps") or []
    )
    map_index = maps.index(source_map)
    classes = list(census.load_json(owner.ROW_CLASSIFICATION).get("classes") or [])
    ordinary_index = classes.index(ORDINARY_CLASS)
    ordinary: set[int] = set()
    for row in census.load_json(owner.ROW_CLASSIFICATION).get("non_mixer_rows") or []:
        dump_index, recipe_index, class_index = row
        if dump_index == map_index and class_index == ordinary_index:
            ordinary.add(int(recipe_index))
    return ordinary


def fingerprint_payload(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        key: value
        for key, value in relation.items()
        if key not in FINGERPRINT_EXCLUDE
    }


def sha256_text(payload: str) -> str:
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def stable_id_for(slug: str, relation: dict[str, Any]) -> str:
    digest = sha256_text(_stable_json(fingerprint_payload(relation)))[:16]
    prefix = slug.replace("-", "_")
    return f"cruciblecraft:{prefix}/{digest}"


def compile_relation(
    *,
    work: WorkFamily,
    recipe: dict[str, Any],
    recipe_index: int,
    shadow_order: int,
    catalogs: assembler.Catalogs,
    maps: dict[str, Any],
    host: str,
    slug: str,
) -> tuple[dict[str, Any], list[str]]:
    errors: list[str] = []
    unsupported: list[str] = []
    slot_notes: list[dict[str, Any]] = []
    if recipe.get("hidden") is True:
        unsupported.append("hidden")
    if recipe.get("fake") is True:
        unsupported.append("fake")
    extra_keys = sorted(set(recipe) - assembler.CONSUMED_RECIPE_KEYS)
    if extra_keys:
        slot_notes.append({"side": "recipe", "slot": 0, "class": "extra_keys", "keys": extra_keys})
    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("inputs") or []):
        if assembler._empty_item(item):
            slot_notes.append({"side": "item_input", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_item_operand(
            item,
            catalogs,
            stone_runtime=maps["stone"],
            mte_runtime=maps["mte"],
            block_runtime=maps["block"],
            item_overlay=maps["items"],
            reused_aliases=maps["aliases"],
            side="item_input",
        )
        errors.extend(operand_errors)
        action = assembler.classify_item_action(item)
        count = int(item.get("count") or 0)
        if action["kind"] != "CONSUME":
            count = 0
        item_inputs.append(operand)
        item_input_counts.append(count)
        item_input_actions.append(action)
    item_outputs: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("outputs") or []):
        if assembler._empty_item(item):
            slot_notes.append({"side": "item_output", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = map_item_operand(
            item,
            catalogs,
            stone_runtime=maps["stone"],
            mte_runtime=maps["mte"],
            block_runtime=maps["block"],
            item_overlay=maps["items"],
            reused_aliases=maps["aliases"],
            side="item_output",
        )
        errors.extend(operand_errors)
        item_outputs.append(operand)
    fluid_inputs: list[dict[str, Any]] = []
    for fluid in recipe.get("fluidInputs") or []:
        if not fluid:
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, maps["fluids"], side="fluid_input"
        )
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_inputs.append(operand)
    fluid_outputs: list[dict[str, Any]] = []
    for fluid in recipe.get("fluidOutputs") or []:
        if not fluid:
            continue
        operand, operand_errors = map_fluid_operand(
            fluid, catalogs, maps["fluids"], side="fluid_output"
        )
        errors.extend(operand_errors)
        if operand["slot_class"] != "empty":
            fluid_outputs.append(operand)
    chances = [int(value) for value in (recipe.get("chances") or [])]
    if not chances and item_outputs:
        chances = [10000] * len(item_outputs)
    elif chances and len(chances) < len(item_outputs):
        chances.extend([10000] * (len(item_outputs) - len(chances)))
    duration = int(recipe.get("duration") or 0)
    if duration <= 0:
        errors.append(f"{work.template_key}: duration must be positive")
    group = publication_group_for(host, work.owner, int(work.record.get("expanded_count") or 1))
    if host.endswith(":mixer"):
        group = mixer_publication_group(work.owner, int(work.record.get("expanded_count") or 1), {
            "fluid_inputs": fluid_inputs,
            "fluid_outputs": fluid_outputs,
        })
    cohort = str(group).rsplit("/", 1)[-1]
    relation = {
        "cohort": cohort,
        "family_id": work.family_id,
        "host": host,
        "owner": work.owner,
        "publication_group": group,
        "source_map": source_map_for(host),
        "source_recipe_index": recipe_index,
        "source_revision": SOURCE_REVISION,
        "source_row_sha256": sha256_text(_stable_json(recipe)),
        "target_map": host,
        "template_key": work.template_key,
        "item_inputs": item_inputs,
        "item_input_counts": item_input_counts,
        "item_input_actions": item_input_actions,
        "item_outputs": item_outputs,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "output_chances": chances,
        "duration": duration,
        "eut": int(recipe.get("euPerTick") or 0),
        "special_value": int(recipe.get("specialValue") or 0),
        "can_be_buffered": bool(recipe.get("canBeBuffered", True)),
        "needs_empty_output": bool(recipe.get("needsEmptyOutput")),
        "no_nbt_checks": True,
        "shadow_order": shadow_order,
        "slot_notes": slot_notes,
        "unsupported_semantics": unsupported,
        "provenance": {
            "kinds": ["SOURCE_BACKED", "SOURCE_DERIVED"],
            "fields": {
                "item_inputs": "SOURCE_DERIVED",
                "item_input_counts": "SOURCE_BACKED",
                "duration": "SOURCE_BACKED",
                "eut": "SOURCE_BACKED",
                "special_value": "SOURCE_BACKED",
                "can_be_buffered": "SOURCE_BACKED",
            },
        },
    }
    relation["stable_id"] = stable_id_for(slug, relation)
    if unsupported:
        errors.extend(f"{work.template_key}: unsupported {value}" for value in unsupported)
    return relation, errors


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def relation_blocked(relation: dict[str, Any]) -> bool:
    if relation.get("unsupported_semantics"):
        return True
    return any(
        not operand.get("runtime_id") or operand.get("mapping") == "blocked_unmapped"
        for operand in _operands(relation)
    )


CROSS_MOD_FLUID_MARKERS = (
    "binnie.",
    "ic2",
    "forestry.",
    "extrabiomes.",
    "biomesoplenty.",
    "grc.",
)
NATIVE_FLUID_EQUIVALENTS = {
    "ic2constructionfoam": "cruciblecraft:construction_foam",
    "sluicejuice": "cruciblecraft:sluice_juice",
}
NATIVE_ITEM_EQUIVALENTS = {
    ("gregtech:gt.stone.basalt", 0): "minecraft:basalt",
}


def native_fluid_equivalent(fluid_id: str) -> str | None:
    key = fluid_id.split(":", 1)[-1]
    if key in NATIVE_FLUID_EQUIVALENTS:
        return NATIVE_FLUID_EQUIVALENTS[key]
    if key.startswith("cfoam."):
        return "cruciblecraft:construction_foam"
    return None


def mixer_cross_mod_reason(relations: list[dict[str, Any]]) -> str | None:
    unmapped: list[str] = []
    for relation in relations:
        for operand in _operands(relation):
            if operand.get("runtime_id") and operand.get("mapping") != "blocked_unmapped":
                continue
            source = operand.get("source") or {}
            fluid = str(source.get("fluid") or "")
            item = str(source.get("item") or "")
            if native_fluid_equivalent(fluid):
                continue
            if fluid:
                unmapped.append(fluid)
            elif item:
                unmapped.append(item)
    if not unmapped:
        return None
    lowered = [value.lower() for value in unmapped]
    if all(
        any(marker in value for marker in CROSS_MOD_FLUID_MARKERS)
        for value in lowered
    ):
        return (
            "source classification depends on external-mod fluids/items: "
            + ",".join(sorted(set(unmapped)))
        )
    return None


def collect_required_forms(relations: list[dict[str, Any]]) -> dict[str, set[str]]:
    required: dict[str, set[str]] = defaultdict(set)
    for relation in relations:
        for operand in _operands(relation):
            material = operand.get("material")
            form = operand.get("form")
            if material and form and str(form) not in OPTIONAL_PLANT_FORMS:
                required[str(material)].add(str(form))
    return {material: forms for material, forms in required.items()}


def optional_plant_forms_in(relations: list[dict[str, Any]]) -> list[str]:
    return sorted(
        {
            str(operand.get("form"))
            for relation in relations
            for operand in _operands(relation)
            if str(operand.get("form") or "") in OPTIONAL_PLANT_FORMS
        }
    )


def assign_rows(
    recipes: list[dict[str, Any]],
    work: list[WorkFamily],
    ordinary_indices: set[int],
    host: str,
) -> dict[str, list[tuple[int, dict[str, Any]]]]:
    wanted = {item.template_key for item in work}
    grouped: dict[str, list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    if host.endswith(":mixer"):
        for template_key, indexes in owner.mixer_ordinary_optional_recipe_indexes().items():
            if template_key not in wanted:
                continue
            for recipe_index in sorted(indexes):
                if recipe_index < len(recipes):
                    grouped[template_key].append((recipe_index, recipes[recipe_index]))
        return dict(grouped)
    membership = owner.recipe_template_ids(source_map_for(host), recipes)
    for recipe_index in sorted(ordinary_indices):
        if recipe_index >= len(recipes):
            continue
        template_key = membership.get(recipe_index)
        if template_key not in wanted:
            continue
        grouped[template_key].append((recipe_index, recipes[recipe_index]))
    return dict(grouped)


def mixer_publication_group(
    owner: str,
    expanded_count: int,
    relation: dict[str, Any],
) -> str:
    blob = " ".join(
        str((operand.get("source") or {}).get("fluid") or operand.get("runtime_id") or "")
        for operand in list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    ).lower()
    if "constructionfoam" in blob.replace("_", "") or "construction_foam" in blob:
        return "cruciblecraft:mixer/ordinary_closure/construction_foam_matrix"
    if "foam" in owner or owner == "recipe_wave/mixer":
        return "cruciblecraft:mixer/ordinary_closure/construction_foam_matrix"
    if owner.startswith("material_expression") or expanded_count > 1:
        return "cruciblecraft:mixer/ordinary_closure/material_matrix"
    return "cruciblecraft:mixer/ordinary_closure/opaque"


def drying_heat_envelope_ok(relation: dict[str, Any]) -> bool:
    if (
        len(relation.get("item_inputs") or []) > 1
        or len(relation.get("item_outputs") or []) > 1
        or len(relation.get("fluid_inputs") or []) > 1
        or len(relation.get("fluid_outputs") or []) > 1
    ):
        return False
    eut = int(relation.get("eut") or 0)
    if eut <= 0 or eut > 1024:
        return False
    for operand in list(relation.get("fluid_inputs") or []) + list(
        relation.get("fluid_outputs") or []
    ):
        amount = int((operand.get("source") or {}).get("amount") or operand.get("amount") or 0)
        if amount > 32_000:
            return False
    return True


def autoclave_envelope_ok(relation: dict[str, Any]) -> bool:
    if (
        len(relation.get("item_inputs") or []) > 2
        or len(relation.get("item_outputs") or []) > 3
        or len(relation.get("fluid_inputs") or []) > 1
        or len(relation.get("fluid_outputs") or []) > 1
    ):
        return False
    eut = int(relation.get("eut") or 0)
    if eut < 0:
        return False
    duration = int(relation.get("duration") or 0)
    if duration <= 0:
        return False
    for operand in relation.get("fluid_inputs") or []:
        amount = int((operand.get("source") or {}).get("amount") or operand.get("amount") or 0)
        if amount > 4_000_000:
            return False
    for operand in relation.get("fluid_outputs") or []:
        amount = int((operand.get("source") or {}).get("amount") or operand.get("amount") or 0)
        if amount > 512_000:
            return False
    return True


def centrifuge_panel_envelope_ok(relation: dict[str, Any]) -> bool:
    if (
        len(relation.get("item_inputs") or []) > 1
        or len(relation.get("item_outputs") or []) > 6
        or len(relation.get("fluid_inputs") or []) > 1
        or len(relation.get("fluid_outputs") or []) > 6
    ):
        return False
    eut = int(relation.get("eut") or 0)
    if eut <= 0 or eut > 4096:
        return False
    for operand in list(relation.get("fluid_inputs") or []) + list(
        relation.get("fluid_outputs") or []
    ):
        amount = int((operand.get("source") or {}).get("amount") or operand.get("amount") or 0)
        if amount > 64_000:
            return False
    return True


def smelter_bronze_envelope_ok(relation: dict[str, Any]) -> bool:
    if (
        len(relation.get("item_inputs") or []) > 1
        or len(relation.get("item_outputs") or []) > 4
        or len(relation.get("fluid_inputs") or []) > 0
        or len(relation.get("fluid_outputs") or []) > 1
    ):
        return False
    eut = int(relation.get("eut") or 0)
    if eut <= 0 or eut > 1024:
        return False
    if any(int(count) > 64 for count in (relation.get("item_input_counts") or [])):
        return False
    for output in relation.get("fluid_outputs") or []:
        amount = int(
            output.get("amount")
            or (output.get("source") or {}).get("amount")
            or 0
        )
        if amount <= 0 or amount > 8000:
            return False
    return True


def classify_family(item: WorkFamily, relations: list[dict[str, Any]]) -> dict[str, Any]:
    blocked = [relation for relation in relations if relation_blocked(relation)]
    expected = int(item.record.get("expanded_count") or item.relation_count)
    count_ok = len(relations) == expected
    if item.template_key == SMELTER_UNPROVABLE:
        return {
            "disposition": "reclassify",
            "future_owner": "later:object_expression/unprovable_residue",
            "reason": (
                "wildcard selector-tag consume with no smelter outputs; "
                "not a proven ordinary identity"
            ),
        }
    if item.owner == "recycling/evidence_needed":
        mte = any(
            str((operand.get("source") or {}).get("item") or "")
            == "gregtech:gt.multitileentity"
            for relation in relations
            for operand in _operands(relation)
        )
        if mte:
            return {
                "disposition": "reclassify",
                "future_owner": "later:recycling",
                "reason": (
                    "autoclave family is MTE dismantling without recycling runtime"
                ),
                "recheck_condition": "recycling/deferred-ordinary-runtime owns this family",
            }
    if item.template_key in SMELTER_RECYCLING:
        mte = any(
            str((operand.get("source") or {}).get("item") or "") == "gregtech:gt.multitileentity"
            for relation in relations
            for operand in _operands(relation)
        )
        if blocked and mte:
            return {
                "disposition": "reclassify",
                "future_owner": "later:recycling",
                "reason": "recovery family still depends on MTE dismantling runtime",
            }
    if item.template_key.startswith("gt.recipe.centrifuge") and any(
        not centrifuge_panel_envelope_ok(relation) for relation in relations
    ):
        return {
            "disposition": "reclassify",
            "future_owner": "later:execution_envelope/gt6_panel",
            "reason": (
                "family exceeds centrifuge envelope 1/6/1/6 with 64000 mB "
                "tanks and EUt 1-4096"
            ),
            "recheck_condition": (
                "centrifuge panel or higher-tier variant must accept this "
                "shape, tank, or EUt"
            ),
        }
    if item.template_key.startswith("gt.recipe.autoclave") and any(
        not autoclave_envelope_ok(relation) for relation in relations
    ):
        return {
            "disposition": "reclassify",
            "future_owner": "later:execution_envelope/autoclave_time",
            "reason": (
                "family exceeds autoclave envelope 2/3/1/1 with 4000000/512000 "
                "mB tanks"
            ),
            "recheck_condition": (
                "autoclave envelope or relation must match independently"
            ),
        }
    if item.template_key.startswith("gt.recipe.drying") and any(
        not drying_heat_envelope_ok(relation) for relation in relations
    ):
        return {
            "disposition": "reclassify",
            "future_owner": "later:execution_envelope/drying_heat",
            "reason": (
                "family exceeds Drying heat envelope 1/1/1/1 with 32000 mB "
                "tanks and EUt 1-1024; live-ready #0149 is not a proxy"
            ),
            "recheck_condition": "drying machine envelope or relation must match independently",
        }
    if item.template_key.startswith("gt.recipe.smelter") and any(
        not smelter_bronze_envelope_ok(relation) for relation in relations
    ):
        return {
            "disposition": "reclassify",
            "future_owner": "later:execution_envelope/t5_bronze",
            "reason": (
                "family exceeds bronze Smelter envelope 1/4/0/1 with 8000 mB "
                "output tanks and EUt 1-1024; not completed by raising the machine gate"
            ),
        }
    optional_forms = optional_plant_forms_in(relations)
    if optional_forms:
        return {
            "disposition": "reclassify",
            "future_owner": "later:agriculture/plant_form_overlay",
            "reason": (
                "family requires optional crops plant forms absent from the base "
                "material registration gate: " + ",".join(optional_forms)
            ),
            "recheck_condition": (
                "the crops addon must register the exact plant_gt_* forms before "
                "this family can return to ordinary closure"
            ),
        }
    if not count_ok:
        return {
            "disposition": "blocked",
            "future_owner": None,
            "reason": f"row count {len(relations)} != ledger {expected}",
        }
    if blocked:
        cross_mod = mixer_cross_mod_reason(relations)
        if cross_mod:
            return {
                "disposition": "reclassify",
                "future_owner": "later:cross_mod",
                "reason": cross_mod,
                "recheck_condition": (
                    "external-mod operands must have a proven in-mod identity "
                    "or a dedicated later:cross_mod owner"
                ),
            }
        return {
            "disposition": "blocked",
            "future_owner": None,
            "reason": "unmapped operands after live registries/form/fluid replay",
        }
    return {
        "disposition": "complete",
        "future_owner": None,
        "reason": "source-complete exact/exact_multi",
    }


def _family_consume_key(relations: list[dict[str, Any]]) -> str:
    rows: list[Any] = []
    for relation in sorted(
        relations,
        key=lambda row: int(row.get("shadow_order") or 0),
    ):
        def _ops(field: str) -> tuple[Any, ...]:
            packed = []
            for operand in relation.get(field) or []:
                packed.append(
                    (
                        operand.get("runtime_id"),
                        operand.get("tag"),
                        (operand.get("source") or {}).get("count"),
                        (operand.get("source") or {}).get("amount"),
                    )
                )
            return tuple(packed)

        rows.append(
            (
                _ops("item_inputs"),
                _ops("item_outputs"),
                _ops("fluid_inputs"),
                _ops("fluid_outputs"),
            )
        )
    return json.dumps(rows, sort_keys=True, separators=(",", ":"))


def load_runtime_maps() -> dict[str, Any]:
    global _RUNTIME_MAPS
    if _RUNTIME_MAPS is None:
        _RUNTIME_MAPS = {
            "stone": load_stone_runtime(),
            "mte": load_mte_runtime(),
            "block": load_block_runtime(),
            "items": load_item_overlay(),
            "aliases": identities.load_reused_aliases(),
            "fluids": load_fluid_overlay(),
        }
    return _RUNTIME_MAPS


def invalidate_runtime_maps() -> None:
    global _RUNTIME_MAPS, _LEDGER_FLUID_OVERLAY, _LEDGER_ITEM_RUNTIME
    _RUNTIME_MAPS = None
    _LEDGER_FLUID_OVERLAY = None
    _LEDGER_ITEM_RUNTIME = None


def replay(slug: str) -> dict[str, Any]:
    spec = recipe_wave(slug)
    host = spec.host
    if not dump_exists(host):
        raise OSError(f"missing GT6 dump for {host}: {dump_path_for(host)}")
    work = load_work(host)
    recipes = owner.load_map_recipes(source_map_for(host))
    ordinary = load_ordinary_indices(host)
    assigned = assign_rows(recipes, work, ordinary, host)
    maps = dict(load_runtime_maps())
    catalogs = load_catalogs()
    relations: list[dict[str, Any]] = []
    for item in work:
        rows = sorted(assigned.get(item.template_key, []), key=lambda row: row[0])
        for shadow_order, (recipe_index, recipe) in enumerate(rows):
            relation, _errors = compile_relation(
                work=item,
                recipe=recipe,
                recipe_index=recipe_index,
                shadow_order=shadow_order,
                catalogs=catalogs,
                maps=maps,
                host=host,
                slug=slug,
            )
            relations.append(relation)
    required = collect_required_forms(relations)
    if required:
        catalogs = load_catalogs(required)
        remapped: list[dict[str, Any]] = []
        for item in work:
            rows = sorted(assigned.get(item.template_key, []), key=lambda row: row[0])
            for shadow_order, (recipe_index, recipe) in enumerate(rows):
                relation, _errors = compile_relation(
                    work=item,
                    recipe=recipe,
                    recipe_index=recipe_index,
                    shadow_order=shadow_order,
                    catalogs=catalogs,
                    maps=maps,
                    host=host,
                    slug=slug,
                )
                remapped.append(relation)
        relations = remapped
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in relations:
        by_family[str(relation["family_id"])].append(relation)
    classifications: list[dict[str, Any]] = []
    production: list[dict[str, Any]] = []
    reclassified: list[dict[str, Any]] = []
    blocked: list[dict[str, Any]] = []
    for item in work:
        rows = by_family.get(item.family_id, [])
        verdict = classify_family(item, rows)
        payload = {
            "expanded_count": len(rows),
            "family_id": item.family_id,
            "owner": item.owner,
            "relation_hashes": [str(row["source_row_sha256"]) for row in rows],
            "template_key": item.template_key,
            **verdict,
        }
        classifications.append(payload)
        if verdict["disposition"] == "complete":
            production.append(item)
        elif verdict["disposition"] == "reclassify":
            reclassified.append(payload)
        else:
            blocked.append(payload)
    consume_winners: dict[str, str] = {}
    collapsed: list[WorkFamily] = []
    for item in list(production):
        key = _family_consume_key(by_family.get(item.family_id, []))
        winner = consume_winners.get(key)
        if winner is None:
            consume_winners[key] = item.template_key
            continue
        collapsed.append(item)
    if collapsed:
        production = [item for item in production if item not in collapsed]
        by_template = {row["template_key"]: row for row in classifications}
        for item in collapsed:
            payload = by_template[item.template_key]
            payload["disposition"] = "reclassify"
            payload["future_owner"] = "later:identity_mapping/collapsed_1_21"
            payload["reason"] = (
                "1.12 meta collapsed onto the same 1.21 consume identity as "
                + consume_winners[_family_consume_key(by_family.get(item.family_id, []))]
            )
            reclassified.append(payload)
    mapping_counts: Counter[str] = Counter()
    unmapped_items: Counter[str] = Counter()
    unmapped_fluids: Counter[str] = Counter()
    for relation in relations:
        for operand in _operands(relation):
            mapping_counts[str(operand.get("mapping") or "unknown")] += 1
            if operand.get("runtime_id") and operand.get("mapping") != "blocked_unmapped":
                continue
            source = operand.get("source") or {}
            if source.get("item"):
                unmapped_items[f"{source.get('item')}@{source.get('meta')}"] += 1
            if source.get("fluid"):
                unmapped_fluids[str(source.get("fluid"))] += 1
    summary = remaining_summary(host)
    production_ids = {item.family_id for item in production}
    production_relations = [
        relation
        for relation in relations
        if relation["family_id"] in production_ids
    ]
    return {
        "assigned": sum(len(rows) for rows in assigned.values()),
        "blockers": blocked,
        "classifications": classifications,
        "dump_path": census.relative(dump_path_for(host)),
        "dump_sha256": census.sha256_file(dump_path_for(host)),
        "expected": summary,
        "family_count": len(work),
        "host": host,
        "mapping_counts": dict(sorted(mapping_counts.items())),
        "production_families": [item.family_id for item in production],
        "production_relations": production_relations,
        "production_work": production,
        "reclassified": reclassified,
        "relations": relations,
        "required_forms": {
            material: sorted(forms) for material, forms in sorted(required.items())
        },
        "unmapped_fluids": dict(unmapped_fluids.most_common()),
        "unmapped_items": dict(unmapped_items.most_common()),
        "wave_slug": slug,
        "work": work,
    }


def diagnose(slug: str) -> dict[str, Any]:
    payload = replay(slug)
    complete = sum(1 for row in payload["classifications"] if row["disposition"] == "complete")
    reclassified = sum(
        1 for row in payload["classifications"] if row["disposition"] == "reclassify"
    )
    blocked = sum(1 for row in payload["classifications"] if row["disposition"] == "blocked")
    return {
        "assigned": payload["assigned"],
        "blocked_families": blocked,
        "complete_families": complete,
        "expected_families": payload["expected"]["family_count"],
        "expected_relations": payload["expected"]["relation_count"],
        "host": payload["host"],
        "mapping_counts": payload["mapping_counts"],
        "reclassified_families": reclassified,
        "required_form_pairs": sum(len(forms) for forms in payload["required_forms"].values()),
        "required_forms": payload["required_forms"],
        "unmapped_fluids": payload["unmapped_fluids"],
        "unmapped_items": payload["unmapped_items"],
        "wave_slug": slug,
        "blocked_samples": payload["blockers"][:12],
        "reclassified": payload["reclassified"],
    }
