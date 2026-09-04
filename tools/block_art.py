"""SOURCE_BACKED static block icons from gregtech6_w.

Replaces vanilla stand-ins for block/object / remainder / stone / leftover semantic
block identities. Catalog JSON stays sealed. Models and PNGs are authority.
"""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import census_common as census

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION
GT6_W_BLOCKS = (
    ROOT
    / "gt6_referencable_port_code/gregtech6_w/src/main/resources/assets/gregtech"
    / "textures/blocks"
)
TEXTURE_ROOT = ROOT / "src/main/resources/assets/cruciblecraft/textures/block/gt6"
MODEL_GEN = ROOT / "src/generated/resources/assets/cruciblecraft/models"
ITEM_MODEL_MAIN = ROOT / "src/main/resources/assets/cruciblecraft/models/item"
BUNDLED_INDEX = (
    ROOT / "src/main/resources/data/cruciblecraft/block_art_index.json"
)
MANIFEST = TOOLS / "block_art_manifest.json"
STATUS = "BLOCK_ART_MANIFEST"
EXPECTED_BIND_COUNT = 1076

STONE_VARIANT = (
    "stone",
    "cobble",
    "cobble_mossy",
    "bricks",
    "bricks_cracked",
    "bricks_mossy",
    "bricks_chiseled",
    "smooth",
    "bricks_reinforced",
    "bricks_redstone",
    "tiles",
    "small_tiles",
    "small_bricks",
    "windmill_tiles_a",
    "windmill_tiles_b",
    "square_bricks",
)
LOG_WOOD = {
    "gt.block.log.1": ("dry", "rotten", "mossy", "frozen"),
    "gt.block.log.a": ("rubber", "maple", "willow", "bluemahoe"),
    "gt.block.log.b": ("hazel", "cinnamon", "coconut", "rainbowood"),
    "gt.block.log.c": ("bluespruce", "bluespruce", "bluespruce", "bluespruce"),
}
BEAM_WOOD = {
    "gt.block.beam.1": ("oak", "spruce", "birch", "jungle"),
    "gt.block.beam.2": ("acacia", "darkoak", "rubberwood", "wood"),
    "gt.block.beam.3": ("greatwood", "silverwood", "skyroot", "darkwood"),
    "gt.block.beam.a": ("rubber", "maple", "willow", "bluemahoe"),
    "gt.block.beam.b": ("hazel", "cinnamon", "coconut", "rainbowood"),
    "gt.block.beam.c": ("bluespruce", "bluespruce", "bluespruce", "bluespruce"),
}
PLANKS = (
    "planks_rubber",
    "planks_maple",
    "planks_willow",
    "planks_bluemahoe",
    "planks_hazel",
    "planks_cinnamon",
    "planks_coconut",
    "planks_rainbowood",
    "planks_compressed",
    "planks_wood",
    "planks_treated",
    "crate",
    "planks_dry",
    "planks_rotten",
    "planks_mossy",
    "planks_frozen",
)
CROP_BALE = ("rye", "oat", "barley", "rice")
GRASS_BALE = (
    ("grass_top", "grass_side"),
    ("grass_top_dry", "grass_side_dry"),
    ("grass_top_moldy", "grass_side_moldy"),
    ("grass_top_rotten", "grass_side_rotten"),
)
GRASS_BLOCK = ("medium", "light", "dark", "normal", "yellow", "brown")
DIGGABLE = (
    "mud",
    "clay_brown",
    "turf",
    "clay_red",
    "clay_yellow",
    "clay_blue",
    "clay_white",
)
SANDS = (
    "sand_magnetite",
    "sand_basalt_magnetite",
    "sand_granite_magnetite",
)
GLOWTUS = (
    "black",
    "red",
    "green",
    "brown",
    "blue",
    "purple",
    "cyan",
    "light_gray",
    "gray",
    "pink",
    "lime",
    "yellow",
    "light_blue",
    "magenta",
    "orange",
    "white",
)
BAR_METAL = {
    "adamantium": "metallic",
    "brass": "metallic",
    "steel": "metallic",
    "titanium": "metallic",
    "tungstensteel": "metallic",
}
RAIL_METAL = {
    "adamantium": "adamantium",
    "aluminium": "aluminium",
    "bronze": "bronze",
    "magnalium": "magnalium",
    "stainlesssteel": "stainlesssteel",
    "steel": "steel",
    "titanium": "titanium",
    "tungsten": "tungsten",
    "tungstensteel": "tungstensteel",
    "tungstencarbide": "tungstencarbide",
}


def _icon(name: str) -> str:
    return f"iconsets/{name}.png"


def _strip_item(source_item: str) -> tuple[str, int | None]:
    item = str(source_item).removeprefix("gregtech:")
    slab = None
    if ".slab." in item:
        item, _, suffix = item.rpartition(".slab.")
        slab = int(suffix)
    if item.endswith(".fireproof"):
        item = item[: -len(".fireproof")]
    return item, slab


def cc_texture(rel: str) -> str:
    return "cruciblecraft:block/gt6/" + rel[:-4]


def source_png(rel: str) -> Path:
    return GT6_W_BLOCKS / rel


def dest_png(rel: str) -> Path:
    return TEXTURE_ROOT / rel


def resolve(source_item: str, meta: int) -> dict[str, Any] | None:
    item, slab = _strip_item(source_item)
    wood = meta & 3

    if item == "gt.block.asphalt":
        return _cube(_icon("asphalt"), tint="dye", slab=slab is not None)
    if item == "gt.block.concrete":
        return _cube(_icon("concrete"), tint="dye", slab=slab is not None)
    if item == "gt.block.concrete.reinforced":
        return _cube(_icon("concrete_reinforced"), tint="dye", slab=slab is not None)
    if item == "gt.block.cfoam":
        return _cube(_icon("cfoam_hardened"), tint="dye", slab=slab is not None)
    if item == "gt.block.cfoam.fresh":
        return _cube(_icon("cfoam_fresh"), tint="dye", slab=slab is not None)
    if item in {"gt.block.planks", "gt.block.planks2"}:
        name = PLANKS[meta & 15] if item == "gt.block.planks" else "planks_bluespruce"
        return _cube(_icon(name), slab=slab is not None)
    if item in LOG_WOOD:
        kind = LOG_WOOD[item][wood]
        return _column(_icon(f"log_top_{kind}"), _icon(f"log_side_{kind}"))
    if item in BEAM_WOOD:
        kind = BEAM_WOOD[item][wood]
        return _column(_icon(f"beam_top_{kind}"), _icon(f"beam_side_{kind}"))
    if item == "gt.block.bale.crop":
        crop = CROP_BALE[wood]
        return _column(_icon(f"{crop}_top"), _icon(f"{crop}_side"))
    if item == "gt.block.bale.grass":
        top, side = GRASS_BALE[wood]
        return _column(_icon(top), _icon(side))
    if item == "gt.block.grass":
        tone = GRASS_BLOCK[meta % 6]
        return _column(
            _icon(f"grassblock_top_{tone}"),
            _icon(f"grassblock_side_{tone}"),
        )
    if item.startswith("gt.stone."):
        variant = STONE_VARIANT[meta & 15]
        return _cube(f"stones/{item}/{variant}.png", slab=slab is not None)
    if item == "gt.block.diggable":
        return _cube(_icon(DIGGABLE[min(meta, len(DIGGABLE) - 1)]))
    if item == "gt.block.sands":
        return _cube(_icon(SANDS[min(meta, len(SANDS) - 1)]))
    if item == "gt.block.lilypad.glowtus":
        return {
            "model": "generated",
            "sources": [_icon(f"glowtus_{GLOWTUS[meta & 15]}")],
        }
    if item.startswith("gt.block.bars."):
        metal = item.rsplit(".", 1)[-1]
        folder = BAR_METAL.get(metal, "metallic")
        return {
            "model": "generated",
            "sources": [f"materialicons/{folder}/blocksolid.png"],
        }
    if item.startswith("gt.block.rail."):
        metal = item.rsplit(".", 1)[-1]
        mapped = RAIL_METAL.get(metal, metal)
        kind = "detector" if ".detector." in item else "straight"
        return {
            "model": "generated",
            "sources": [_icon(f"rail_{kind}_{mapped}")],
        }
    if item.startswith("gt.block.spikes."):
        return _cube("materialicons/metallic/blocksolid.png")
    return None


def _cube(rel: str, *, tint: str | None = None, slab: bool = False) -> dict[str, Any]:
    payload = {"model": "slab" if slab else "cube_all", "sources": [rel]}
    if tint:
        payload["tint"] = tint
    return payload


def _column(end: str, side: str) -> dict[str, Any]:
    return {"model": "column", "sources": [end, side]}


def catalog_rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    seen: set[str] = set()

    def add(catalog: str, runtime: str, registry: str, source: str, meta: int) -> None:
        if runtime in seen:
            return
        bind = resolve(source, meta)
        if bind is None:
            return
        seen.add(runtime)
        rows.append(
            {
                "catalog": catalog,
                "meta": meta,
                "model": bind["model"],
                "registry_path": registry,
                "runtime_id": runtime,
                "source_item": source,
                "sources": list(bind["sources"]),
                "tint": bind.get("tint"),
            }
        )

    block_cat = census.load_json(
        ROOT / "src/main/resources/data/cruciblecraft/gt_block_object_catalog.json"
    )
    for identity in block_cat.get("identities") or []:
        add(
            "block_object",
            str(identity["runtime_id"]),
            str(identity["registry_path"]),
            str(identity["source_item"]),
            int(identity["meta"]),
        )
    remainder = census.load_json(
        ROOT
        / "src/main/resources/data/cruciblecraft/bath_remainder_identity_catalog.json"
    )
    for identity in remainder.get("identities") or []:
        add(
            "bath_remainder",
            str(identity["runtime_id"]),
            str(identity["registry_path"]),
            str(identity["source_item"]),
            int(identity["meta"]),
        )
    stones = census.load_json(
        ROOT / "src/main/resources/data/cruciblecraft/gt_stone_catalog.json"
    )
    for identity in stones.get("identities") or []:
        source = str(identity["source_item"])
        for variant in identity.get("variants") or []:
            add(
                "stone",
                str(variant["runtime_id"]),
                str(variant["registry_path"]),
                source,
                int(variant["meta"]),
            )
    semantic = census.load_json(
        ROOT / "src/main/resources/data/cruciblecraft/semantic_object_catalog.json"
    )
    for identity in semantic.get("identities") or []:
        if str(identity.get("kind") or "") != "block":
            continue
        add(
            "semantic_block",
            str(identity["runtime_id"]),
            str(identity["registry_path"]),
            str(identity["source_item"]),
            int(identity["meta"]),
        )
    rows.sort(key=lambda row: str(row["runtime_id"]))
    return rows


def _copy_rel(rel: str) -> str:
    source = source_png(rel)
    if not source.is_file():
        raise ValueError(f"missing SOURCE_BACKED block texture: {rel}")
    dest = dest_png(rel)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(source.read_bytes())
    return census.relative(dest)


def _write_json(path: Path, document: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def _model_id(registry_path: str) -> str:
    return f"cruciblecraft:{registry_path}"


def item_layer0(identity: dict[str, Any]) -> str | None:
    if str(identity.get("kind") or "") != "block":
        return None
    source = str(identity.get("source_item") or "")
    meta = identity.get("meta")
    if not source or not isinstance(meta, int):
        return None
    bind = resolve(source, meta)
    if bind is None:
        return None
    return cc_texture(bind["sources"][0])


def identity_layer0(identity: dict[str, Any]) -> str:
    from tools import multiitem_art as multiitem

    sourced = item_layer0(identity)
    if sourced:
        return sourced
    return multiitem.model_layer0(identity)


def write_models(row: dict[str, Any]) -> None:
    sources = [cc_texture(rel) for rel in row["sources"]]
    registry = str(row["registry_path"])
    model = str(row["model"])
    tinted = row.get("tint") == "dye"
    if str(row.get("catalog") or "") == "semantic_block" or model == "generated":
        _write_json(
            ITEM_MODEL_MAIN / f"{registry}.json",
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": sources[0]},
            },
        )
        return
    stem = MODEL_GEN / registry
    if model == "cube_all":
        parent = (
            "cruciblecraft:block/tinted_cube_all"
            if tinted
            else "minecraft:block/cube_all"
        )
        _write_json(
            Path(str(stem) + ".json"),
            {"parent": parent, "textures": {"all": sources[0]}},
        )
        _write_json(
            ROOT
            / "src/generated/resources/assets/cruciblecraft/models/item"
            / f"{registry}.json",
            {"parent": _model_id(registry)},
        )
        return
    if model == "slab":
        parent_bottom = (
            "cruciblecraft:block/tinted_slab" if tinted else "minecraft:block/slab"
        )
        parent_top = (
            "cruciblecraft:block/tinted_slab_top"
            if tinted
            else "minecraft:block/slab_top"
        )
        textures = {"bottom": sources[0], "side": sources[0], "top": sources[0]}
        _write_json(
            Path(str(stem) + "_bottom.json"),
            {"parent": parent_bottom, "textures": textures},
        )
        _write_json(
            Path(str(stem) + "_top.json"),
            {"parent": parent_top, "textures": textures},
        )
        _write_json(
            Path(str(stem) + "_double.json"),
            {
                "parent": (
                    "cruciblecraft:block/tinted_cube_all"
                    if tinted
                    else "minecraft:block/cube_all"
                ),
                "textures": {"all": sources[0]},
            },
        )
        _write_json(
            ROOT
            / "src/generated/resources/assets/cruciblecraft/models/item"
            / f"{registry}.json",
            {"parent": _model_id(registry + "_bottom")},
        )
        return
    if model == "column":
        textures = {"end": sources[0], "side": sources[1]}
        _write_json(
            Path(str(stem) + ".json"),
            {"parent": "minecraft:block/cube_column", "textures": textures},
        )
        _write_json(
            Path(str(stem) + "_horizontal.json"),
            {
                "parent": "minecraft:block/cube_column_horizontal",
                "textures": textures,
            },
        )
        _write_json(
            ROOT
            / "src/generated/resources/assets/cruciblecraft/models/item"
            / f"{registry}.json",
            {"parent": _model_id(registry)},
        )
        return
    raise ValueError(f"unsupported block art model {model} for {registry}")


def generated_item_model(registry_path: str) -> Path:
    return (
        ROOT
        / "src/generated/resources/assets/cruciblecraft/models/item"
        / f"{registry_path}.json"
    )


def model_paths(row: dict[str, Any]) -> list[Path]:
    registry = str(row["registry_path"])
    model = str(row["model"])
    if str(row.get("catalog") or "") == "semantic_block" or model == "generated":
        return [ITEM_MODEL_MAIN / f"{registry}.json"]
    stem = MODEL_GEN / registry
    item = generated_item_model(registry)
    if model == "slab":
        return [
            Path(str(stem) + "_bottom.json"),
            Path(str(stem) + "_top.json"),
            Path(str(stem) + "_double.json"),
            item,
        ]
    if model == "column":
        return [Path(str(stem) + ".json"), Path(str(stem) + "_horizontal.json"), item]
    return [Path(str(stem) + ".json"), item]


def copy_and_write(rows: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    if not GT6_W_BLOCKS.is_dir():
        raise ValueError(
            "gregtech6_w block textures missing; "
            f"expected {census.relative(GT6_W_BLOCKS)}"
        )
    identities = []
    copied: dict[str, str] = {}
    for row in rows or catalog_rows():
        for rel in row["sources"]:
            if rel not in copied:
                copied[rel] = _copy_rel(rel)
        write_models(row)
        textures = [cc_texture(rel) for rel in row["sources"]]
        identities.append(
            {
                "catalog": row["catalog"],
                "meta": row["meta"],
                "model": row["model"],
                "registry_path": row["registry_path"],
                "runtime_id": row["runtime_id"],
                "sha256": census.sha256_file(dest_png(row["sources"][0])),
                "source_item": row["source_item"],
                "source_paths": row["sources"],
                "texture": textures[0],
                "textures": textures,
                "tint": row.get("tint"),
            }
        )
    document = {
        "evidence_class": "SOURCE_BACKED",
        "generated_by": "python tools/build_block_art.py",
        "gt6_asset_root": (
            "gt6_referencable_port_code/gregtech6_w/"
            "src/main/resources/assets/gregtech"
        ),
        "identities": identities,
        "identity_count": len(identities),
        "note": (
            "Static block/object / remainder / stone / leftover semantic block icons "
            "from gregtech6_w. Catalog texture fields stay vanilla so smelter/stone/block/object "
            "hashes do not move. Dye-tinted kinds share one PNG + GT6 DYES_INT."
        ),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": STATUS,
        "unique_png_count": len(copied),
    }
    return document


def bundled_index(document: dict[str, Any]) -> dict[str, Any]:
    textures: dict[str, dict[str, Any]] = {}
    for row in document.get("identities") or []:
        entry: dict[str, Any] = {
            "texture": row["texture"],
            "tint": row.get("tint"),
        }
        extra = list(row.get("textures") or [])
        if len(extra) > 1:
            entry["side"] = extra[1]
        textures[str(row["registry_path"])] = entry
    return {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "BLOCK_ART_INDEX",
        "textures": textures,
    }


def check_models_and_pngs(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    for row in document.get("identities") or []:
        runtime = str(row.get("runtime_id") or "")
        expected = str(row.get("texture") or "")
        registry = str(row.get("registry_path") or "")
        if expected.startswith("minecraft:"):
            errors.append(f"{runtime} still uses a vanilla placeholder")
        block_model = Path(str(MODEL_GEN / registry) + ".json")
        for path in model_paths(row):
            if not path.is_file():
                errors.append(f"missing block art model {census.relative(path)}")
                continue
            try:
                model = census.load_json(path)
            except ValueError as exc:
                errors.append(f"{census.relative(path)}: {exc}")
                continue
            parent = str(model.get("parent") or "")
            if path == block_model and parent == _model_id(registry):
                errors.append(f"self-parent stub {census.relative(path)}")
            textures = model.get("textures") or {}
            used = " ".join(str(value) for value in textures.values())
            if not textures:
                continue
            if expected and expected not in used:
                errors.append(f"{census.relative(path)} does not reference {expected}")
    return errors


def check_payload(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if document.get("status") != STATUS:
        errors.append("block art status drifted")
    identities = list(document.get("identities") or [])
    if int(document.get("identity_count") or 0) != len(identities):
        errors.append("block art identity_count drifted")
    if len(identities) != EXPECTED_BIND_COUNT:
        errors.append(
            f"block art expected {EXPECTED_BIND_COUNT} binds, got {len(identities)}"
        )
    if not identities:
        errors.append("block art index is empty")
    seen: set[str] = set()
    for row in identities:
        runtime = str(row.get("runtime_id") or "")
        if runtime in seen:
            errors.append(f"duplicate block art runtime {runtime}")
        seen.add(runtime)
        rels = list(row.get("source_paths") or [])
        if not rels:
            errors.append(f"{runtime} missing source_paths")
            continue
        dest = dest_png(str(rels[0]))
        if not dest.is_file():
            errors.append(f"missing copied block PNG: {census.relative(dest)}")
        elif census.sha256_file(dest) != str(row.get("sha256") or ""):
            errors.append(f"block PNG hash drifted: {census.relative(dest)}")
    errors.extend(check_models_and_pngs(document))
    return errors
