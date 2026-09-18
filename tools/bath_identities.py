#!/usr/bin/env python3
"""Identity overlay helpers for the bath/identity Bath identity/form remainder wave."""
from __future__ import annotations

import re
from typing import Any

from tools import bath_remainder_identities as bath_remainder_ids
from tools import bath_identity_common as common
from tools import tool_head_prefix as thp

VANILLA_DYE_COLORS = bath_remainder_ids.VANILLA_DYE_COLORS
VANILLA_COLOR_META_ITEMS = bath_remainder_ids.VANILLA_COLOR_META_ITEMS
VANILLA_SPECIAL_META = bath_remainder_ids.VANILLA_SPECIAL_META
VANILLA_RENAMES = bath_remainder_ids.VANILLA_RENAMES

# GT prefixes that are material forms but are not in gt6_prefix_mapping.json.
PREFIX_ITEM_TO_FORM_OVERLAY: dict[str, str] = {
    "gregtech:gt.meta.crushedPurifiedTiny": "tiny_washed_crushed_ore",
    "gregtech:gt.meta.arrowGtPlastic": "arrow_gt_plastic",
    "gregtech:gt.meta.arrowGtWood": "arrow_gt_wood",
    "gregtech:gt.meta.lens": "lens",
    "gregtech:gt.meta.ingotHot": "ingot_hot",
    "gregtech:gt.meta.ingotDouble": "double_ingot",
    "gregtech:gt.meta.ingotTriple": "triple_ingot",
    "gregtech:gt.meta.ingotQuadruple": "quadruple_ingot",
    "gregtech:gt.meta.ingotQuintuple": "quintuple_ingot",
    "gregtech:gt.meta.gemExquisite": "gem_exquisite",
    "gregtech:gt.meta.gemFlawless": "gem_flawless",
    "gregtech:gt.meta.gemFlawed": "gem_flawed",
    "gregtech:gt.meta.gemChipped": "gem_chipped",
    "gregtech:gt.meta.gemLegendary": "gem_legendary",
    "gregtech:gt.meta.plateCurved": "curved_plate",
    "gregtech:gt.meta.plateTiny": "tiny_plate",
    "gregtech:gt.meta.plateGemTiny": "tiny_plate_gem",
    "gregtech:gt.meta.round": "round",
    "gregtech:gt.meta.casingSmall": "small_casing",
    "gregtech:gt.meta.chemtube": "chem_tube",
    "gregtech:gt.meta.scrapGt": "scrap",
    "gregtech:gt.meta.chain": "chain",
    "gregtech:gt.meta.billet": "billet",
    "gregtech:gt.meta.chunkGt": "chunk",
    "gregtech:gt.meta.minecartWheels": "minecart_wheels",
    "gregtech:gt.meta.storage.plate": "storage_plate",
    "gregtech:gt.meta.storage.ingot": "storage_ingot",
}

CIRCUIT_ITEM = "gregapi:gt.integrated_circuit"
PROGRAMMED_CIRCUIT = "cruciblecraft:programmed_circuit"

VANILLA_DYE_ITEMS = {
    0: "minecraft:ink_sac",
    1: "minecraft:red_dye",
    2: "minecraft:green_dye",
    3: "minecraft:cocoa_beans",
    4: "minecraft:lapis_lazuli",
    5: "minecraft:purple_dye",
    6: "minecraft:cyan_dye",
    7: "minecraft:light_gray_dye",
    8: "minecraft:gray_dye",
    9: "minecraft:pink_dye",
    10: "minecraft:lime_dye",
    11: "minecraft:yellow_dye",
    12: "minecraft:light_blue_dye",
    13: "minecraft:magenta_dye",
    14: "minecraft:orange_dye",
    15: "minecraft:bone_meal",
}

VANILLA_WILDCARD_TAGS = {
    "minecraft:stained_glass": "cruciblecraft:bath_identity_stained_glass",
    "minecraft:stained_glass_pane": "cruciblecraft:bath_identity_stained_glass_panes",
    "minecraft:stained_hardened_clay": "cruciblecraft:bath_identity_stained_terracotta",
}

WILDCARD_TAG_REPRESENTATIVE = {
    "minecraft:stained_glass": "minecraft:white_stained_glass",
    "minecraft:stained_glass_pane": "minecraft:white_stained_glass_pane",
    "minecraft:stained_hardened_clay": "minecraft:white_terracotta",
}

PREFIX_FORM_UNITS: dict[str, int] = {
    "tiny_washed_crushed_ore": 16,
    "arrow_gt_plastic": 144,
    "arrow_gt_wood": 144,
    "lens": 144,
    "gem_exquisite": 576,
    "gem_flawless": 288,
    "gem_flawed": 72,
    "gem_chipped": 36,
    "gem_legendary": 1296,
    "curved_plate": 144,
    "tiny_plate": 16,
    "tiny_plate_gem": 16,
    "round": 18,
    "small_casing": 72,
    "chem_tube": 144,
    "scrap": 144,
    "chain": 144,
    "billet": 144,
    "chunk": 144,
    "minecart_wheels": 144,
    "storage_plate": 1296,
    "storage_ingot": 1296,
    "quadruple_ingot": 576,
    "quintuple_ingot": 720,
}

PREFIX_ITEM_TO_FORM_OVERLAY.update(thp.prefix_item_to_form_overlay())
PREFIX_FORM_UNITS.update(
    {
        thp.cc_prefix_id(gt_prefix): int(units)
        for gt_prefix, (units, _authority) in thp.load_gt_prefix_units().items()
    }
)

EXISTING_PREFIX_FORMS = {
    "block",
    "double_ingot",
    "gem",
    "ingot",
    "ingot_hot",
    "nugget",
    "plate",
    "plate_gem",
    "tiny_centrifuged_crushed_ore",
    "triple_ingot",
}

PREFIX_FORM_DOCS: dict[str, dict[str, Any]] = {
    "tiny_washed_crushed_ore": {
        "aliases": ["crushedpurifiedtiny"],
        "capabilities": [
            "cruciblecraft:ore",
            "cruciblecraft:washed",
            "cruciblecraft:tiny",
            "cruciblecraft:smeltable",
        ],
        "generation_flag": "cruciblecraft:generates_tiny_washed_crushed_ore",
        "tag_directory": "tiny_washed_crushed_ores",
    },
    "arrow_gt_plastic": {
        "aliases": ["arrowgtplastic"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_arrow_gt_plastic",
        "tag_directory": "arrow_gt_plastics",
    },
    "arrow_gt_wood": {
        "aliases": ["arrowgtwood"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_arrow_gt_wood",
        "tag_directory": "arrow_gt_woods",
    },
    "lens": {
        "aliases": ["lens"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_lens",
        "tag_directory": "lenses",
    },
    "gem_exquisite": {
        "aliases": ["gemexquisite"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_gem_exquisite",
        "tag_directory": "exquisite_gems",
    },
    "gem_flawless": {
        "aliases": ["gemflawless"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_gem_flawless",
        "tag_directory": "flawless_gems",
    },
    "gem_flawed": {
        "aliases": ["gemflawed"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_gem_flawed",
        "tag_directory": "flawed_gems",
    },
    "gem_chipped": {
        "aliases": ["gemchipped"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_gem_chipped",
        "tag_directory": "chipped_gems",
    },
    "gem_legendary": {
        "aliases": ["gemlegendary"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_gem_legendary",
        "tag_directory": "legendary_gems",
    },
    "curved_plate": {
        "aliases": ["platecurved"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_curved_plate",
        "tag_directory": "curved_plates",
    },
    "tiny_plate": {
        "aliases": ["platetiny"],
        "capabilities": ["cruciblecraft:tiny"],
        "generation_flag": "cruciblecraft:generates_tiny_plate",
        "tag_directory": "tiny_plates",
    },
    "tiny_plate_gem": {
        "aliases": ["plategemtiny"],
        "capabilities": ["cruciblecraft:tiny"],
        "generation_flag": "cruciblecraft:generates_tiny_plate_gem",
        "tag_directory": "tiny_plate_gems",
    },
    "round": {
        "aliases": ["round"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_round",
        "tag_directory": "rounds",
    },
    "small_casing": {
        "aliases": ["casingsmall"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_small_casing",
        "tag_directory": "small_casings",
    },
    "chem_tube": {
        "aliases": ["chemtube"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_chem_tube",
        "tag_directory": "chem_tubes",
    },
    "scrap": {
        "aliases": ["scrapgt"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_scrap",
        "tag_directory": "scraps",
    },
    "chain": {
        "aliases": ["chain"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_chain",
        "tag_directory": "chains",
    },
    "billet": {
        "aliases": ["billet"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_billet",
        "tag_directory": "billets",
    },
    "chunk": {
        "aliases": ["chunkgt"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_chunk",
        "tag_directory": "chunks",
    },
    "minecart_wheels": {
        "aliases": ["minecartwheels"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_minecart_wheels",
        "tag_directory": "minecart_wheels",
    },
    "storage_plate": {
        "aliases": ["storageplate"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_storage_plate",
        "tag_directory": "storage_plates",
    },
    "storage_ingot": {
        "aliases": ["storageingot"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_storage_ingot",
        "tag_directory": "storage_ingots",
    },
    "quadruple_ingot": {
        "aliases": ["ingotquadruple"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_quadruple_ingot",
        "tag_directory": "quadruple_ingots",
    },
    "quintuple_ingot": {
        "aliases": ["ingotquintuple"],
        "capabilities": [],
        "generation_flag": "cruciblecraft:generates_quintuple_ingot",
        "tag_directory": "quintuple_ingots",
    },
}

PREFIX_FORM_TEXTURE: dict[str, str] = {
    "tiny_washed_crushed_ore": "cruciblecraft:item/material/tiny_crushed_ore",
    "arrow_gt_plastic": "cruciblecraft:item/material/arrow_gt_plastic",
    "arrow_gt_wood": "cruciblecraft:item/material/arrow_gt_wood",
    "plant_gt_berry": "cruciblecraft:item/material/plant_gt_berry",
    "plant_gt_blossom": "cruciblecraft:item/material/plant_gt_blossom",
    "plant_gt_fiber": "cruciblecraft:item/material/plant_gt_fiber",
    "plant_gt_twig": "cruciblecraft:item/material/plant_gt_twig",
    "plant_gt_wart": "cruciblecraft:item/material/plant_gt_wart",
    "rail_gt": "cruciblecraft:item/material/rail_gt",
    "lens": "minecraft:item/glass",
    "gem_exquisite": "minecraft:item/diamond",
    "gem_flawless": "minecraft:item/diamond",
    "gem_flawed": "minecraft:item/emerald",
    "gem_chipped": "minecraft:item/emerald",
    "gem_legendary": "minecraft:item/nether_star",
    "curved_plate": "cruciblecraft:item/material/plate",
    "tiny_plate": "cruciblecraft:item/material/plate",
    "tiny_plate_gem": "cruciblecraft:item/material/plate_gem",
    "round": "minecraft:item/iron_nugget",
    "small_casing": "minecraft:item/iron_ingot",
    "chem_tube": "minecraft:item/glass_bottle",
    "scrap": "cruciblecraft:item/material/scrap",
    "chain": "minecraft:item/string",
    "billet": "minecraft:item/iron_ingot",
    "chunk": "minecraft:item/raw_iron",
    "minecart_wheels": "minecraft:item/minecart",
    "storage_plate": "minecraft:item/iron_ingot",
    "storage_ingot": "minecraft:item/iron_ingot",
    "quadruple_ingot": "minecraft:item/iron_ingot",
    "quintuple_ingot": "minecraft:item/iron_ingot",
}

_SLUG_RE = re.compile(r"[^a-z0-9]+")


def classify_source_item(item_id: str) -> str:
    return bath_remainder_ids.classify_source_item(item_id)


def load_reused_aliases() -> dict[tuple[str, int], str]:
    from tools import centrifuge_common as centrifuge

    aliases = dict(centrifuge.FIXTURE_ONLY_LOSSY_ITEM_ALIASES)
    aliases.update(centrifuge.LEGACY_VANILLA_ITEMS)
    return {
        key: value
        for key, value in aliases.items()
        if isinstance(key, tuple) and len(key) == 2
    }


def vanilla_meta_runtime(item_id: str, meta: Any) -> str | None:
    mapped = bath_remainder_ids.vanilla_meta_runtime(item_id, meta)
    if mapped:
        return mapped
    if item_id == "minecraft:dye" and isinstance(meta, int):
        return VANILLA_DYE_ITEMS.get(meta)
    return None


def vanilla_wildcard_meta(item_id: str, meta: Any) -> bool:
    return bath_remainder_ids.vanilla_wildcard_meta(item_id, meta)


def load_mte_runtime_map() -> dict[tuple[str, int], str]:
    return bath_remainder_ids.load_mte_runtime_map()


def load_block_runtime_map() -> dict[tuple[str, int], str]:
    return bath_remainder_ids.load_block_runtime_map()


def load_fluid_base_overlay() -> dict[str, str]:
    overlay = bath_remainder_ids.load_fluid_base_overlay()
    overlay.update(bath_remainder_ids.load_bath_remainder_fluid_overlay())
    return overlay


def load_bath_identity_fluid_overlay() -> dict[str, str]:
    if not common.FLUID_MAPPING.is_file():
        return {}
    mapping = common.load_json(common.FLUID_MAPPING)
    return {
        str(row["source_fluid"]): str(row["cc_fluid_id"])
        for row in mapping.get("mapping") or []
        if isinstance(row, dict) and row.get("source_fluid") and row.get("cc_fluid_id")
    }


def load_bath_identity_item_overlay() -> dict[tuple[str, int | None], dict[str, Any]]:
    if not common.IDENTITY_CATALOG.is_file():
        return {}
    catalog = common.load_json(common.IDENTITY_CATALOG)
    mapped: dict[tuple[str, int | None], dict[str, Any]] = {}
    for identity in catalog.get("identities") or []:
        source_item = str(identity.get("source_item") or "")
        meta = identity.get("meta")
        key = (source_item, int(meta) if isinstance(meta, int) else None)
        row = dict(identity)
        if isinstance(meta, int):
            rewritten = thp.mapped_runtime(source_item, meta)
            if rewritten:
                row["runtime_id"] = rewritten
                row["registry_path"] = rewritten.split(":", 1)[-1]
        mapped[key] = row
    return mapped


def overlay_prefix_form(item_id: str) -> str | None:
    return PREFIX_ITEM_TO_FORM_OVERLAY.get(str(item_id))


def slug_source_item(item_id: str) -> str:
    item = str(item_id)
    if item.startswith("gregtech:gt.meta."):
        item = item[len("gregtech:gt.meta.") :]
    elif item.startswith("gregtech:gt.multiitem."):
        item = "multiitem_" + item[len("gregtech:gt.multiitem.") :]
    elif item.startswith("gregtech:"):
        item = item[len("gregtech:") :]
    return _SLUG_RE.sub("_", item.lower()).strip("_") or "item"


def runtime_id_for(kind: str, source_item: str, meta: int) -> str:
    if kind == "tool_head":
        mapped = thp.mapped_runtime(source_item, meta)
        if mapped:
            return mapped
        remainder = {
            (str(row.get("source_item") or ""), int(row["meta"]))
            for row in (thp.load_remap().get("remainder") or [])
            if isinstance(row.get("meta"), int)
        }
        if thp.load_remap().get("mapped") and (source_item, int(meta)) not in remainder:
            raise ValueError(
                f"mapped tool-head remap is closed; refusing unique item for "
                f"{source_item}@{meta}"
            )
    from tools import catalog_modern_ids as modern

    try:
        return modern.runtime_id_for(source_item, meta)
    except ValueError:
        raise ValueError(
            f"no modern catalog id for {kind} {source_item}@{meta}"
        ) from None


def registry_path_for(kind: str, source_item: str, meta: int) -> str:
    runtime = runtime_id_for(kind, source_item, meta)
    return runtime.split(":", 1)[1]


def is_tool_head_item(item_id: str) -> bool:
    return classify_source_item(item_id) == "tool_head"


def is_multiitem_item(item_id: str) -> bool:
    return classify_source_item(item_id) == "multiitem"


def english_name(source_item: str, meta: int, display: str | None = None) -> str:
    text = str(display or "").strip()
    if text:
        return text
    slug = slug_source_item(source_item).replace("_", " ")
    return f"{slug} m{meta}"


def chinese_name(source_item: str, meta: int, display: str | None = None) -> str:
    return english_name(source_item, meta, display)


def operand_runtime_closed(operand: dict[str, Any]) -> bool:
    runtime = str(operand.get("runtime_id") or "")
    mapping = str(operand.get("mapping") or "")
    if mapping == "blocked_unmapped" or not runtime:
        return False
    if runtime.startswith(("gregtech:", "gregapi:", "fixed:")):
        return False
    return runtime.startswith("minecraft:") or runtime.startswith("cruciblecraft:")


def work_set_family_ids() -> set[str]:
    return set(common.load_json(common.WORK_SET).get("family_ids") or [])


def iter_work_set_relations(source: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    wanted = work_set_family_ids()
    document = source
    if document is None:
        if common.SOURCE.is_file():
            document = common.load_json(common.SOURCE)
            if document.get("status") != "BATH_IDENTITY_BATH_SOURCE_FROZEN":
                document = common.load_json(common.bath_remainder.SOURCE)
        else:
            document = common.load_json(common.bath_remainder.SOURCE)
    return [
        relation
        for relation in document.get("relations") or []
        if str(relation.get("family_id") or "") in wanted
    ]


def load_bath_remainder_work_set_relations() -> list[dict[str, Any]]:
    wanted = work_set_family_ids()
    document = common.load_json(common.bath_remainder.SOURCE)
    return [
        relation
        for relation in document.get("relations") or []
        if str(relation.get("family_id") or "") in wanted
    ]


def prefix_document(form: str) -> dict[str, Any]:
    meta = dict(PREFIX_FORM_DOCS.get(form) or {})
    units = int(PREFIX_FORM_UNITS.get(form) or 144)
    texture = PREFIX_FORM_TEXTURE.get(form, "minecraft:item/iron_ingot")
    return {
        "aliases": list(meta.get("aliases") or [form.replace("_", "")]),
        "capabilities": list(meta.get("capabilities") or []),
        "generation_flag": str(
            meta.get("generation_flag") or f"cruciblecraft:generates_{form}"
        ),
        "id": f"cruciblecraft:{form}",
        "model_template": "minecraft:item/generated",
        "model_texture": texture,
        "serialized_path": form,
        "tag_directory": str(meta.get("tag_directory") or f"{form}s"),
        "tag_namespace": "c",
        "units": units,
    }


def extra_factual_forms() -> list[str]:
    forms = set(PREFIX_ITEM_TO_FORM_OVERLAY.values())
    forms.update(
        {
            "block",
            "nugget",
            "plate_gem",
            "tiny_centrifuged_crushed_ore",
        }
    )
    return sorted(forms)


def vanilla_wildcard_tag(item_id: str) -> str | None:
    return VANILLA_WILDCARD_TAGS.get(str(item_id))


def vanilla_wildcard_representative(item_id: str) -> str | None:
    return WILDCARD_TAG_REPRESENTATIVE.get(str(item_id))


def stained_color_items(kind: str) -> list[str]:
    if kind == "minecraft:stained_glass":
        template = "minecraft:{color}_stained_glass"
    elif kind == "minecraft:stained_glass_pane":
        template = "minecraft:{color}_stained_glass_pane"
    elif kind == "minecraft:stained_hardened_clay":
        template = "minecraft:{color}_terracotta"
    else:
        return []
    return [template.format(color=color) for color in VANILLA_DYE_COLORS]
