#!/usr/bin/env python3
"""Extract energy-transformer kinds/tiers from pinned Loader_MultiTileEntities."""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
LOADER = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "loaders"
    / "b"
    / "Loader_MultiTileEntities.java"
)
GT6_W_TEX = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "machines"
    / "transformers"
    / "transformer_electric"
)
DEST_TEX = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "block"
    / "machine"
    / "transformer"
    / "electric"
)
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
GT6_W_RESOURCES = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
)

VOLTAGES = (8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152)
VOLTAGE_NAMES = (
    "ulv",
    "lv",
    "mv",
    "hv",
    "ev",
    "iv",
    "luv",
    "zpm",
    "uv",
    "puv1",
)
MATERIALS = (
    "tin_alloy",
    "steel_galvanized",
    "aluminium",
    "stainless_steel",
    "chromium",
    "titanium",
    "iridium",
    "osmium_elemental",
    "trinitanium",
)
ADD_RE = re.compile(
    r'"Transformers"\s*,\s*(\d+)\s*,\s*\d+\s*,\s*aClass'
)
INPUT_RE = re.compile(r"NBT_INPUT,\s*V\[(\d+)\]")
OUTPUT_RE = re.compile(r"NBT_OUTPUT,\s*V\[(\d+)\]")
EXPECTED_PNG = 12
EXPECTED_MCMETA = 3


def dumps(document: Any) -> str:
    return json.dumps(document, indent=2, ensure_ascii=False) + "\n"


def recipe_for(index: int) -> dict[str, Any]:
    wire = "copper" if index < 3 else "annealed_copper"
    material = MATERIALS[index]
    return {
        "pattern": ["WIW", "XM ", "WIW"],
        "keys": {
            "W": {"prefix": "wire", "material": wire},
            "I": {"prefix": "double_plate", "material": "iron"},
            "X": {"prefix": "quadruple_wire", "material": wire},
            "M": {"prefix": "machine_casing", "material": material},
        },
    }


def extract_rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for line_no, raw in enumerate(LOADER.read_text(encoding="utf-8").splitlines(), 1):
        match = ADD_RE.search(raw)
        if not match:
            continue
        source_id = int(match.group(1))
        gt6_class = "MultiTileEntityTransformerElectric"
        if source_id < 10040 or source_id > 10048:
            continue
        index = source_id - 10040
        input_index = int(INPUT_RE.search(raw).group(1))
        output_index = int(OUTPUT_RE.search(raw).group(1))
        if input_index != index + 1 or output_index != index:
            raise SystemExit(
                f"{source_id} voltage pair drifted: in V[{input_index}] "
                f"out V[{output_index}]"
            )
        input_size = VOLTAGES[input_index]
        output_size = VOLTAGES[output_index]
        if input_size != output_size * 4:
            raise SystemExit(f"{source_id} multiplier drifted from 4")
        low = VOLTAGE_NAMES[output_index]
        high = VOLTAGE_NAMES[input_index]
        rows.append(
            {
                "id": f"cruciblecraft:electric_transformer_{low}_{high}",
                "kind": "cruciblecraft:electric_transformer",
                "low_voltage": low,
                "high_voltage": high,
                "voltage_index": index,
                "input_size": input_size,
                "output_size": output_size,
                "multiplier": 4,
                "capacity": input_size * 2,
                "material": MATERIALS[index],
                "energy": "EU",
                "source_id": source_id,
                "source_line": line_no,
                "gt6_class": gt6_class,
                "recipe": recipe_for(index),
            }
        )
    return rows


def write_kinds() -> None:
    document = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "kinds": [
            {
                "id": "cruciblecraft:electric_transformer",
                "runtime": "transformer",
                "energy": "EU",
                "texture_profile": "electric",
                "lang_key_en": "Transformer",
                "lang_key_zh": "变压器",
                "gt6_class": "MultiTileEntityTransformerElectric",
            }
        ],
    }
    (DATA / "energy_transformer_kinds.json").write_text(
        dumps(document), encoding="utf-8"
    )


def write_tiers(rows: list[dict[str, Any]]) -> None:
    if len(rows) != 9:
        raise SystemExit(f"expected 9 transformer rows, got {len(rows)}")
    expected = set(range(10040, 10049))
    actual = {row["source_id"] for row in rows}
    if actual != expected:
        raise SystemExit(f"source id set drifted: {sorted(actual ^ expected)}")
    document = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "tiers": rows,
    }
    (DATA / "energy_transformer_tiers.json").write_text(
        dumps(document), encoding="utf-8"
    )


def copy_art() -> None:
    if not GT6_W_TEX.is_dir():
        raise SystemExit(f"missing GT6 transformer art {GT6_W_TEX}")
    imports: list[dict[str, str]] = []
    png_count = 0
    mcmeta_count = 0
    for path in sorted(GT6_W_TEX.rglob("*")):
        if not path.is_file():
            continue
        if path.suffix not in {".png", ".mcmeta"}:
            continue
        if path.name.endswith(".png.mcmeta"):
            mcmeta_count += 1
        elif path.suffix == ".png":
            png_count += 1
        else:
            continue
        relative = path.relative_to(GT6_W_TEX)
        target = DEST_TEX / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
        imports.append(
            {
                "destination": str(
                    target.relative_to(ROOT / "src" / "main" / "resources")
                ).replace("\\", "/"),
                "gt6_source": str(path.relative_to(GT6_W_RESOURCES)).replace(
                    "\\", "/"
                ),
            }
        )
    if png_count != EXPECTED_PNG:
        raise SystemExit(f"expected {EXPECTED_PNG} transformer PNGs, copied {png_count}")
    if mcmeta_count != EXPECTED_MCMETA:
        raise SystemExit(
            f"expected {EXPECTED_MCMETA} blinking mcmeta files, copied {mcmeta_count}"
        )
    forbidden = ("longdistance", "transformer_rotation", "multiblock_casing", "conveyor")
    for row in imports:
        lowered = row["destination"].lower()
        if any(token in lowered for token in forbidden):
            raise SystemExit(f"placeholder or out-of-scope art {row['destination']}")
    manifest = {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "source_revision": SOURCE_REVISION,
        "imports": imports,
    }
    (ASSETS / "gt6_transformers_art_manifest.json").write_text(
        dumps(manifest), encoding="utf-8"
    )


def main() -> None:
    rows = extract_rows()
    write_kinds()
    write_tiers(rows)
    copy_art()
    print(f"wrote {len(rows)} transformer tiers")
    print([row["id"] for row in rows])


if __name__ == "__main__":
    main()
