#!/usr/bin/env python3
"""Extract energy-battery kinds/tiers from pinned Loader_MultiTileEntities."""
from __future__ import annotations

import json
import re
import shutil
from collections import Counter
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
    / "batteries"
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
    / "battery"
)
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"

VOLTAGES = (8, 32, 128, 512, 2048, 8192)
VOLTAGE_NAMES = ("ulv", "lv", "mv", "hv", "ev", "iv")
CABLE_MATERIALS = ("lead", "tin", "copper", "gold", "aluminium", "aluminium")
CIRCUIT_BY_OD = {
    1: "circuit_basic",
    2: "circuit_good",
    3: "circuit_advanced",
    4: "circuit_elite",
    5: "circuit_master",
    6: "circuit_ultimate",
}
CIRCUIT_RE = re.compile(r"'C',\s*OD_CIRCUITS\[(\d+)\]")

KIND_BY_SOURCE: dict[int, str] = {}
for start, kind in (
    (14000, "lead_acid_battery"),
    (14010, "alkaline_battery"),
    (14020, "nickel_cadmium_battery"),
    (14030, "lithium_cobalt_battery"),
    (14040, "lithium_manganese_battery"),
    (14500, "red_energium_crystal"),
    (14510, "cyan_energium_crystal"),
):
    count = 6 if kind.endswith("crystal") else 5
    for offset in range(count):
        KIND_BY_SOURCE[start + offset] = kind

# B-slot is IL.Battery_*_Cell_Filled (items 20000-20009). Fill is
# FluidContainerData, so battery_part:filled_cell is implemented, not a gap.
# recipe:energium_crystal_shaped stays blocked (no GT6 grid). Gem prefixes
# alias crystals via form_items. C is OD_CIRCUITS[tier].
KIND_META: dict[str, dict[str, Any]] = {
    "lead_acid_battery": {
        "energy": "EU",
        "capacity_multiplier": 2000,
        "color": 0xFF8000,
        "texture_profile": "eu/standard",
        "has_bar": True,
        "lang_en": "Lead-Acid Battery",
        "lang_zh": "铅酸电池",
        "gt6_class": "MultiTileEntityBatteryEU",
        "cell": {"item": "cruciblecraft:lead_acid_cell_filled"},
    },
    "alkaline_battery": {
        "energy": "EU",
        "capacity_multiplier": 4000,
        "color": 0x0000FF,
        "texture_profile": "eu/standard",
        "has_bar": True,
        "lang_en": "Alkaline Battery",
        "lang_zh": "碱性电池",
        "gt6_class": "MultiTileEntityBatteryEU",
        "cell": {"item": "cruciblecraft:alkaline_cell_filled"},
    },
    "nickel_cadmium_battery": {
        "energy": "EU",
        "capacity_multiplier": 4000,
        "color": 0x80FF80,
        "texture_profile": "eu/standard",
        "has_bar": True,
        "lang_en": "Nickel-Cadmium Battery",
        "lang_zh": "镍镉电池",
        "gt6_class": "MultiTileEntityBatteryEU",
        "cell": {"item": "cruciblecraft:nickel_cadmium_cell_filled"},
    },
    "lithium_cobalt_battery": {
        "energy": "EU",
        "capacity_multiplier": 64000,
        "color": 0x0000FF,
        "texture_profile": "eu/advanced",
        "has_bar": True,
        "lang_en": "Lithium-Cobalt Battery",
        "lang_zh": "锂钴电池",
        "gt6_class": "MultiTileEntityBatteryAdvEU",
        "cell": {"item": "cruciblecraft:lithium_cobalt_cell_filled"},
    },
    "lithium_manganese_battery": {
        "energy": "EU",
        "capacity_multiplier": 128000,
        "color": 0x00FF00,
        "texture_profile": "eu/advanced",
        "has_bar": True,
        "lang_en": "Lithium-Manganese Battery",
        "lang_zh": "锂锰电池",
        "gt6_class": "MultiTileEntityBatteryAdvEU",
        "cell": {"item": "cruciblecraft:lithium_manganese_cell_filled"},
    },
    "red_energium_crystal": {
        "energy": "LU",
        "capacity_multiplier": 400000,
        "color": 0xFF0000,
        "texture_profile": "lu",
        "has_bar": False,
        "lang_en": "Red Energium Crystal",
        "lang_zh": "红色能量水晶",
        "gt6_class": "MultiTileEntityBatteryLU",
        "dust": {"prefix": "dust", "material": "energium_red"},
    },
    "cyan_energium_crystal": {
        "energy": "LU",
        "capacity_multiplier": 800000,
        "color": 0x00FFFF,
        "texture_profile": "lu",
        "has_bar": False,
        "lang_en": "Cyan Energium Crystal",
        "lang_zh": "青色能量水晶",
        "gt6_class": "MultiTileEntityBatteryLU",
        "dust": {"prefix": "dust", "material": "energium_cyan"},
    },
}

# GT6 PX_P[n]/PX_N[m] collision boxes as pixel coords 0-16.
EU_BOX = {
    8: [5, 0, 5, 11, 8, 11],
    32: [5, 0, 5, 11, 11, 11],
    128: [4, 0, 4, 12, 11, 12],
    512: [3, 0, 3, 13, 11, 13],
    2048: [2, 0, 2, 14, 13, 14],
}
LU_BOX = {
    8: [6, 0, 6, 10, 4, 10],
    32: [5, 0, 5, 11, 6, 11],
    128: [4, 0, 4, 12, 8, 12],
    512: [3, 0, 3, 13, 10, 13],
    2048: [2, 0, 2, 14, 12, 14],
    8192: [1, 0, 1, 15, 14, 15],
}
EU_SCALE = {8: 4, 32: 7, 128: 7, 512: 7, 2048: 9}

ADD_RE = re.compile(
    r'"Batteries"\s*,\s*(\d+)\s*,\s*\d+\s*,\s*(MultiTileEntityBattery\w+)\s*\.class'
)
INPUT_RE = re.compile(r"NBT_INPUT,\s*V\[(\d+)\]")
CAPACITY_RE = re.compile(r"NBT_CAPACITY,\s*V\[\d+\]\s*\*\s*(\d+)")
ENERGY_RE = re.compile(r"NBT_ENERGY_ACCEPTED,\s*TD\.Energy\.(\w+)")


def dumps(document: Any) -> str:
    return json.dumps(document, indent=2, ensure_ascii=False) + "\n"


def voxel(energy: str, voltage: int) -> list[int]:
    table = LU_BOX if energy == "LU" else EU_BOX
    return list(table[voltage])


def display_scale(energy: str, voltage: int) -> int:
    return 127 if energy == "LU" else EU_SCALE[voltage]


def parse_eu_recipe(rest: str, kind: str, tier: int) -> dict[str, Any]:
    make = rest.find("UT.NBT.make(")
    if make < 0:
        raise SystemExit(f"missing NBT.make in battery row for {kind}")
    i = make + len("UT.NBT.make(")
    depth = 1
    while i < len(rest) and depth:
        if rest[i] == "(":
            depth += 1
        elif rest[i] == ")":
            depth -= 1
        i += 1
    tail = rest[i:]
    strings = re.findall(r'"([^"]*)"', tail)
    patterns = [row for row in strings if 1 <= len(row) <= 3]
    if not patterns:
        raise SystemExit(f"missing shaped pattern for {kind}")
    if len(patterns) > 3:
        patterns = patterns[:3]
    cleaned = [
        "".join(" " if ch in "whdxf" else ch for ch in row) for row in patterns
    ]
    keys: dict[str, Any] = {}
    letters = {ch for row in cleaned for ch in row if ch != " "}
    meta = KIND_META[kind]
    if "P" in letters:
        keys["P"] = {"prefix": "plate", "material": "battery_alloy"}
    if "B" in letters:
        keys["B"] = dict(meta["cell"])
    if "W" in letters:
        keys["W"] = {"prefix": "cable", "material": CABLE_MATERIALS[tier]}
    if "C" in letters:
        circuit = CIRCUIT_RE.search(tail)
        if circuit is None:
            raise SystemExit(f"{kind} has C but no OD_CIRCUITS[tier]")
        od = int(circuit.group(1))
        path = CIRCUIT_BY_OD.get(od)
        if path is None:
            raise SystemExit(f"{kind} uses unknown OD_CIRCUITS[{od}]")
        keys["C"] = {"item": f"cruciblecraft:{path}"}
    missing = letters - set(keys)
    if missing:
        raise SystemExit(f"{kind} recipe has unmapped keys {missing}")
    return {"pattern": cleaned, "keys": keys}


def lu_recipe(kind: str, tier: int) -> dict[str, Any]:
    dust = KIND_META[kind]["dust"]
    return {
        "pattern": [" D ", "DWD", " D "],
        "keys": {
            "D": dict(dust),
            "W": {"prefix": "cable", "material": CABLE_MATERIALS[tier]},
        },
    }


def extract_rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for line_no, raw in enumerate(LOADER.read_text(encoding="utf-8").splitlines(), 1):
        match = ADD_RE.search(raw)
        if not match:
            continue
        source_id = int(match.group(1))
        gt6_class = match.group(2)
        if source_id not in KIND_BY_SOURCE:
            continue
        kind = KIND_BY_SOURCE[source_id]
        meta = KIND_META[kind]
        voltage_index = int(INPUT_RE.search(raw).group(1))
        multiplier = int(CAPACITY_RE.search(raw).group(1))
        energy = ENERGY_RE.search(raw).group(1)
        if energy != meta["energy"]:
            raise SystemExit(f"{source_id} energy {energy} != {meta['energy']}")
        if multiplier != meta["capacity_multiplier"]:
            raise SystemExit(
                f"{source_id} multiplier {multiplier} != {meta['capacity_multiplier']}"
            )
        voltage = VOLTAGES[voltage_index]
        voltage_name = VOLTAGE_NAMES[voltage_index]
        recipe = (
            lu_recipe(kind, voltage_index)
            if energy == "LU"
            else parse_eu_recipe(raw, kind, voltage_index)
        )
        rows.append(
            {
                "id": f"cruciblecraft:{kind}_{voltage_name}",
                "kind": f"cruciblecraft:{kind}",
                "voltage": voltage_name,
                "voltage_index": voltage_index,
                "input_size": voltage,
                "capacity": voltage * multiplier,
                "energy": energy,
                "source_id": source_id,
                "source_line": line_no,
                "gt6_class": gt6_class,
                "voxel": voxel(energy, voltage),
                "display_scale_max": display_scale(energy, voltage),
                "recipe": recipe,
            }
        )
    return rows


def write_kinds() -> None:
    kinds = []
    for kind_id, meta in KIND_META.items():
        kinds.append(
            {
                "id": f"cruciblecraft:{kind_id}",
                "runtime": "battery",
                "energy": meta["energy"],
                "capacity_multiplier": meta["capacity_multiplier"],
                "color": meta["color"],
                "texture_profile": meta["texture_profile"],
                "has_bar": meta["has_bar"],
                "lang_key_en": meta["lang_en"],
                "lang_key_zh": meta["lang_zh"],
                "gt6_class": meta["gt6_class"],
            }
        )
    document = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "kinds": kinds,
    }
    (DATA / "energy_battery_kinds.json").write_text(dumps(document), encoding="utf-8")


def write_tiers(rows: list[dict[str, Any]]) -> None:
    if len(rows) != 37:
        counts = Counter(row["kind"] for row in rows)
        raise SystemExit(f"expected 37 battery rows, got {len(rows)}: {counts}")
    expected_ids = {14000 + n for n in range(5)}
    expected_ids |= {14010 + n for n in range(5)}
    expected_ids |= {14020 + n for n in range(5)}
    expected_ids |= {14030 + n for n in range(5)}
    expected_ids |= {14040 + n for n in range(5)}
    expected_ids |= {14500 + n for n in range(6)}
    expected_ids |= {14510 + n for n in range(6)}
    actual = {row["source_id"] for row in rows}
    if actual != expected_ids:
        raise SystemExit(f"source id set drifted: {sorted(actual ^ expected_ids)}")
    document = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "tiers": rows,
    }
    (DATA / "energy_battery_tiers.json").write_text(dumps(document), encoding="utf-8")


def copy_art() -> None:
    if not GT6_W_TEX.is_dir():
        raise SystemExit(f"missing GT6 battery art {GT6_W_TEX}")
    imports: list[dict[str, str]] = []
    for path in sorted(GT6_W_TEX.rglob("*.png")):
        relative = path.relative_to(GT6_W_TEX)
        target = DEST_TEX / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
        imports.append(
            {
                "destination": str(
                    target.relative_to(ROOT / "src" / "main" / "resources")
                ).replace("\\", "/"),
                "gt6_source": str(
                    path.relative_to(
                        ROOT
                        / "gt6_referencable_port_code"
                        / "gregtech6_w"
                        / "src"
                        / "main"
                        / "resources"
                    )
                ).replace("\\", "/"),
            }
        )
    if len(imports) != 58:
        raise SystemExit(f"expected 58 battery PNGs, copied {len(imports)}")
    manifest = {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "source_revision": SOURCE_REVISION,
        "imports": imports,
    }
    (ASSETS / "gt6_batteries_art_manifest.json").write_text(
        dumps(manifest), encoding="utf-8"
    )


def main() -> None:
    rows = extract_rows()
    write_kinds()
    write_tiers(rows)
    copy_art()
    counts = Counter(row["kind"].split(":")[-1] for row in rows)
    print(f"wrote {len(rows)} battery tiers")
    print(dict(counts))


if __name__ == "__main__":
    main()
