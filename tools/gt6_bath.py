#!/usr/bin/env python3
"""Bath host card: local GT6 cube + bathing-pot art, source-exact crafts."""
from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census
from tools import io_common as io

SLUG = "machines/bath"
ROOT = io.ROOT
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
GT6_ART = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_TEX = (
    GT6_ART
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "machines"
)
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
CUBE_DEST = ASSETS / "textures" / "block" / "machine" / "bath"
POT_DEST = ASSETS / "textures" / "block" / "gt6_import" / "mte" / "bathing_pot"
WOOD_DEST = ASSETS / "textures" / "block" / "gt6_import" / "mte" / "bathing_pot_wood"
MANIFEST = ASSETS / "gt6_bath_art_manifest.json"
DELIVERY = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_delivery.json"
KINDS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_kinds.json"
ACQUISITION = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "mte_inplace_acquisition.json"
)
CUBE_LAYERS = ("colored", "overlay", "overlay_active", "overlay_running")
CUBE_FACES = ("front", "back", "left", "right", "top", "bottom")
POT_NAMES = (
    "insides",
    "sides",
    "top",
    "bottom",
    "tablebottom",
    "tableside",
)


def _gt6_rel(path: Path) -> str:
    return path.relative_to(GT6_ART).as_posix()


def _copy_png(src: Path, dest: Path, imports: list[dict[str, str]]) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)
    meta = src.with_name(src.name + ".mcmeta")
    if meta.is_file():
        shutil.copy2(meta, dest.with_name(dest.name + ".mcmeta"))
    imports.append(
        {
            "destination": dest.relative_to(ASSETS.parent).as_posix(),
            "gt6_source": _gt6_rel(src),
            "source": "gt6_referencable_port_code/gregtech6_w",
        }
    )


def import_art() -> list[dict[str, str]]:
    if not GT6_TEX.is_dir():
        raise FileNotFoundError(f"missing local GT6 art tree: {GT6_TEX}")
    imports: list[dict[str, str]] = []
    cube = GT6_TEX / "basicmachines" / "bath"
    for layer in CUBE_LAYERS:
        for face in CUBE_FACES:
            src = cube / layer / f"{face}.png"
            if not src.is_file():
                continue
            _copy_png(src, CUBE_DEST / layer / f"{face}.png", imports)
    pot = GT6_TEX / "tools" / "bathing_pot"
    wood = GT6_TEX / "tools" / "bathing_pot_wood"
    for name in POT_NAMES:
        for layer in ("colored", "overlay"):
            src = pot / layer / f"{name}.png"
            if src.is_file():
                _copy_png(src, POT_DEST / layer / f"{name}.png", imports)
            src_wood = wood / layer / f"{name}.png"
            if src_wood.is_file():
                _copy_png(src_wood, WOOD_DEST / layer / f"{name}.png", imports)
    MANIFEST.write_text(
        json.dumps(
            {
                "imports": imports,
                "source": "gt6_referencable_port_code/gregtech6_w",
                "source_present": True,
                "source_revision": SOURCE_REVISION,
            },
            indent=2,
            ensure_ascii=False,
        )
        + "\n",
        encoding="utf-8",
    )
    return imports


def _rewrite_pot_model(source: Path, dest: Path, folder: str) -> None:
    text = source.read_text(encoding="utf-8")
    text = text.replace(
        "cruciblecraft:block/machine/bath/",
        f"cruciblecraft:block/gt6_import/mte/{folder}/",
    )
    dest.write_text(text, encoding="utf-8")


def _shift_elements(elements: list[dict], dy: float) -> list[dict]:
    shifted = []
    for element in elements:
        row = json.loads(json.dumps(element))
        row["from"][1] += dy
        row["to"][1] += dy
        shifted.append(row)
    return shifted


def write_models() -> None:
    models = ASSETS / "models" / "block"
    pot_src = models / "bath.json"
    pot_dest = models / "mte_inplace_bathing_pot.json"
    wood_dest = models / "mte_inplace_bathing_pot_wood.json"
    pot_text = pot_src.read_text(encoding="utf-8") if pot_src.is_file() else ""
    if "insides" in pot_text and "machine_cube_2_layer" not in pot_text:
        _rewrite_pot_model(pot_src, pot_dest, "bathing_pot")
        _rewrite_pot_model(pot_src, wood_dest, "bathing_pot_wood")
    elif not pot_dest.is_file() or "insides" not in pot_dest.read_text(encoding="utf-8"):
        raise FileNotFoundError("missing bathing-pot voxel source (models/block/bath.json)")
    pot = json.loads(pot_dest.read_text(encoding="utf-8"))
    wood = json.loads(wood_dest.read_text(encoding="utf-8"))
    for model, folder, dest_name in (
        (pot, "bathing_pot", "mte_inplace_bathing_pot_table.json"),
        (wood, "bathing_pot_wood", "mte_inplace_bathing_pot_wood_table.json"),
    ):
        table = json.loads(json.dumps(model))
        table["textures"]["tablebottom"] = (
            f"cruciblecraft:block/gt6_import/mte/{folder}/colored/tablebottom"
        )
        table["textures"]["tableside"] = (
            f"cruciblecraft:block/gt6_import/mte/{folder}/colored/tableside"
        )
        table["textures"]["tablebottom_overlay"] = (
            f"cruciblecraft:block/gt6_import/mte/{folder}/overlay/tablebottom"
        )
        table["textures"]["tableside_overlay"] = (
            f"cruciblecraft:block/gt6_import/mte/{folder}/overlay/tableside"
        )
        legs = []
        for x, z in ((1.0, 1.0), (13.0, 1.0), (1.0, 13.0), (13.0, 13.0)):
            legs.append(
                {
                    "from": [x, 0.0, z],
                    "to": [x + 2.0, 8.0, z + 2.0],
                    "faces": {
                        "north": {"texture": "#tableside", "tintindex": 0},
                        "south": {"texture": "#tableside", "tintindex": 0},
                        "west": {"texture": "#tableside", "tintindex": 0},
                        "east": {"texture": "#tableside", "tintindex": 0},
                        "up": {"texture": "#tablebottom", "tintindex": 0},
                        "down": {"texture": "#tablebottom", "tintindex": 0},
                    },
                }
            )
            legs.append(
                {
                    "from": [x - 0.01, -0.01, z - 0.01],
                    "to": [x + 2.01, 8.01, z + 2.01],
                    "faces": {
                        "north": {"texture": "#tableside_overlay"},
                        "south": {"texture": "#tableside_overlay"},
                        "west": {"texture": "#tableside_overlay"},
                        "east": {"texture": "#tableside_overlay"},
                        "up": {"texture": "#tablebottom_overlay"},
                        "down": {"texture": "#tablebottom_overlay"},
                    },
                }
            )
        top = [
            {
                "from": [0.0, 7.0, 0.0],
                "to": [16.0, 8.0, 16.0],
                "faces": {
                    "up": {"texture": "#tablebottom", "tintindex": 0},
                    "down": {"texture": "#tablebottom", "tintindex": 0},
                    "north": {"texture": "#tableside", "tintindex": 0},
                    "south": {"texture": "#tableside", "tintindex": 0},
                    "west": {"texture": "#tableside", "tintindex": 0},
                    "east": {"texture": "#tableside", "tintindex": 0},
                },
            }
        ]
        table["elements"] = legs + top + _shift_elements(table["elements"], 8.0)
        (models / dest_name).write_text(
            json.dumps(table, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
    if pot_src.is_file() and "insides" in pot_text and "machine_cube_2_layer" not in pot_text:
        pot_src.unlink()


def copy_empty_nbt() -> None:
    src = (
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft_wave_content_gt6_fluid_pipe_runtime"
        / "structure"
        / "empty.nbt"
    )
    pack = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft_wave_machines_bath"
    for relative in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = pack / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)


def _blockstate(model: str) -> dict:
    return {
        "variants": {
            "facing=down": {"model": model, "x": 90},
            "facing=east": {"model": model, "y": 90},
            "facing=north": {"model": model},
            "facing=south": {"model": model, "y": 180},
            "facing=up": {"model": model, "x": 270},
            "facing=west": {"model": model, "y": 270},
        }
    }


def write_blockstates() -> None:
    mapping = {
        "bathing_pot": "cruciblecraft:block/mte_inplace_bathing_pot",
        "bathing_pot_table": "cruciblecraft:block/mte_inplace_bathing_pot_table",
        "wooden_bathing_pot": "cruciblecraft:block/mte_inplace_bathing_pot_wood",
        "wooden_bathing_pot_table": "cruciblecraft:block/mte_inplace_bathing_pot_wood_table",
    }
    states = ASSETS / "blockstates" / "misc_tool"
    items = ASSETS / "models" / "item" / "misc_tool"
    for path, model in mapping.items():
        (states / f"{path}.json").write_text(
            json.dumps(_blockstate(model), indent=2) + "\n", encoding="utf-8"
        )
        (items / f"{path}.json").write_text(
            json.dumps({"parent": model}, indent=2) + "\n", encoding="utf-8"
        )


def check() -> list[str]:
    errors: list[str] = []
    if not (CUBE_DEST / "colored" / "front.png").is_file():
        errors.append("missing GT6 bath cube front.png")
    if not (POT_DEST / "colored" / "insides.png").is_file():
        errors.append("missing GT6 bathing pot insides.png")
    if not (WOOD_DEST / "colored" / "insides.png").is_file():
        errors.append("missing GT6 wooden bathing pot insides.png")
    if (ASSETS / "models" / "block" / "bath.json").is_file():
        errors.append("main bath.json must not remain; datagen owns the cube")
    generated_model = (
        ROOT
        / "src"
        / "generated"
        / "resources"
        / "assets"
        / "cruciblecraft"
        / "models"
        / "block"
        / "bath.json"
    )
    if not generated_model.is_file():
        errors.append("generated bath cube model missing")
    else:
        bath_model = generated_model.read_text(encoding="utf-8")
        if "machine_cube_2_layer" not in bath_model:
            errors.append("bath machine model is not the GT6 cube")
        if "insides" in bath_model:
            errors.append("bath machine model still uses the bathing-pot voxel")
    pot_model = (
        ASSETS / "models" / "block" / "mte_inplace_bathing_pot.json"
    ).read_text(encoding="utf-8")
    if "gt6_import/mte/bathing_pot/" not in pot_model or "insides" not in pot_model:
        errors.append("stainless bathing pot voxel is missing GT6 insides")
    wood_model = (
        ASSETS / "models" / "block" / "mte_inplace_bathing_pot_wood.json"
    ).read_text(encoding="utf-8")
    if (
        "gt6_import/mte/bathing_pot_wood/" not in wood_model
        or "insides" not in wood_model
    ):
        errors.append("wooden bathing pot voxel is missing GT6 insides")
    pack = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft_wave_machines_bath"
    if not (pack / "structure" / "empty.nbt").is_file():
        errors.append("missing machines/bath structure/empty.nbt")
    if not (pack / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing machines/bath gametest/structure/empty.nbt")
    delivery = census.load_json(DELIVERY)
    if "bath" in delivery.get("shaped_models") or []:
        errors.append("bath is still in shaped_models; machine must be a cube")
    kinds = census.load_json(KINDS)
    bath = next(row for row in kinds["kinds"] if row["id"] == "cruciblecraft:bath")
    if bath.get("acquisition_template") != "bath":
        errors.append("machine_kinds bath template is not bath")
    host = next(row for row in delivery["hosts"] if row["id"] == "cruciblecraft:bath")
    slots = host.get("slots") or {}
    if (
        slots.get("item_inputs") != 6
        or slots.get("item_outputs") != 6
        or slots.get("fluid_inputs") != 1
        or slots.get("fluid_outputs") != 3
    ):
        errors.append("machine_delivery bath slots are not 6/6/1/3")
    acquisition = census.load_json(ACQUISITION)
    paths = {row.get("path"): row for row in acquisition.get("recipes") or []}
    if "misc_tool/wooden_bathing_pot" in paths:
        errors.append("wooden bathing pot craft must stay blocked on OD.itemGlue")
    if "misc_tool/wooden_bathing_pot_table" in paths:
        errors.append("wooden bathing pot table craft must stay blocked")
    table = paths.get("misc_tool/bathing_pot_table")
    if table is None:
        errors.append("stainless bathing pot table recipe missing")
    else:
        ingredients = table.get("ingredients") or {}
        if (ingredients.get("M") or {}).get("item") != "cruciblecraft:misc_tool/bathing_pot":
            errors.append("table M is not the stainless bathing pot")
        if (ingredients.get("S") or {}).get("item") != "minecraft:brick_slab":
            errors.append("table S is not brick_slab (GT6 stone_slab meta 4)")
    manifest = census.load_json(MANIFEST)
    blob = json.dumps(manifest)
    if "minecraft:block" in blob or "multiblock_casing" in blob:
        errors.append("bath art manifest aliases a placeholder texture")
    if "gt6_referencable_port_code/gregtech6_w" not in blob:
        errors.append("bath art manifest is not sourced from local GT6")
    generated = (
        ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "machines"
        / "bath.json"
    )
    if generated.is_file():
        recipe = generated.read_text(encoding="utf-8")
        if "minecraft:copper_ingot" in recipe or "minecraft:furnace" in recipe:
            errors.append("bath acquisition is still the copper-furnace stand-in")
        if "stainless_steel" not in recipe:
            errors.append("bath acquisition is not stainless steel")
        if "CwC" not in recipe or "PMP" not in recipe:
            errors.append("bath acquisition grid is not GT6 CwC/PMP/PPP")
    table_generated = (
        ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "misc_tool"
        / "bathing_pot_table.json"
    )
    if table_generated.is_file():
        table_recipe = table_generated.read_text(encoding="utf-8")
        if "cruciblecraft:misc_tool/bathing_pot" not in table_recipe:
            errors.append("generated table recipe is missing the stainless pot")
        if "minecraft:brick_slab" not in table_recipe:
            errors.append("generated table recipe is missing brick_slab")
    wooden = (
        ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "misc_tool"
        / "wooden_bathing_pot.json"
    )
    if wooden.is_file():
        errors.append("wooden bathing pot generated craft must stay absent")
    return errors


EXPECTED_TESTS = [
    "bathHostAcquisitionIsSourceExact",
    "bathMachineUsesCubeNotPotVoxel",
    "bathingPotClickProcessesWithoutGui",
    "bathingPotManifestResolvesLocalGt6",
    "stainlessBathingPotTableHasAcquisitionRecipe",
    "woodenBathingPotStaysBlockedOnGlue",
]


if __name__ == "__main__":
    imported = import_art()
    write_models()
    write_blockstates()
    copy_empty_nbt()
    print(f"imported {len(imported)} bath textures")
