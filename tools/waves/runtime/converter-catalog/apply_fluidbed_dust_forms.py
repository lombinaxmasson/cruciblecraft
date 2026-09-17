#!/usr/bin/env python3
"""Copy GT6 powder art, import fluidbed fuels, and gate the real dust forms."""
from __future__ import annotations

import hashlib
import json
import shutil
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import extract_energy_converter_catalog as extract
from tools import io_common as io
from tools import material_form_authority as authority

WAVE = Path(__file__).resolve().parent
SECTION = "converter_catalog_fluidbed_required_forms"
OWNER = "energy/converter-catalog"
REQUIRED_REL = "tools/waves/runtime/converter-catalog/required_forms.json"
ART_MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_dust_forms_art_manifest.json"
)
GT6_W = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
)
DEST_TEX = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "item"
    / "material"
)
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
ART_COPIES = (
    (
        "assets/gregtech/textures/items/materialicons/powder/dustdiv72.png",
        DEST_TEX / "dust_div72.png",
    ),
    (
        "assets/gregtech/textures/items/materialicons/powder/dustdiv72_overlay.png",
        DEST_TEX / "dust_div72_overlay.png",
    ),
    (
        "assets/gregtech/textures/blocks/materialicons/powder/blockdust.png",
        DEST_TEX / "storage_dust.png",
    ),
    (
        "assets/gregtech/textures/blocks/materialicons/powder/blockdust_overlay.png",
        DEST_TEX / "storage_dust_overlay.png",
    ),
)


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def copy_art() -> None:
    imports: list[dict[str, str]] = []
    DEST_TEX.mkdir(parents=True, exist_ok=True)
    for source_rel, destination in ART_COPIES:
        source = GT6_W / source_rel
        if not source.is_file():
            raise FileNotFoundError(source)
        shutil.copy2(source, destination)
        imports.append(
            {
                "destination": str(
                    destination.relative_to(ROOT / "src" / "main" / "resources")
                ).replace("\\", "/"),
                "gt6_source": source_rel,
                "source": "gt6_referencable_port_code/gregtech6_w",
            }
        )
    io.write_stable(
        ART_MANIFEST,
        {
            "imports": imports,
            "schema_version": 1,
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": io.SOURCE_REVISION,
        },
    )


def required_forms() -> dict[str, list[str]]:
    dump = io.load_json(extract.FLUIDBED_DUMP)
    xref = io.load_json(extract.CROSS_REF)
    materials = xref["material_id_to_cc"]
    needed: dict[str, set[str]] = defaultdict(set)
    for recipe in dump.get("recipes") or []:
        for stack in (recipe.get("inputs") or []) + (recipe.get("outputs") or []):
            mapped = extract.map_item(stack, materials)
            if mapped is None:
                raise ValueError(f"unmapped fluidbed operand: {stack}")
            item = mapped["item"]
            material, prefix = item.split(":", 1)[1].split("/", 1)
            needed[material].add(prefix)
    return {material: sorted(forms) for material, forms in sorted(needed.items())}


def write_required_forms(forms: dict[str, list[str]]) -> Path:
    path = WAVE / "required_forms.json"
    io.write_stable(
        path,
        {
            "counts": {
                "required_form_pairs": sum(len(row) for row in forms.values()),
                "required_materials": len(forms),
            },
            "generated_by": "runtime/converter-catalog fluidbed dust forms",
            "note": (
                "Card builder writes required forms only. "
                "material_registration_gate.json is produced by applying this overlay."
            ),
            "required_forms": forms,
            "schema_version": 1,
            "source_revision": io.SOURCE_REVISION,
            "status": "SEMANTIC_REQUIRED_FORMS",
            "wave_slug": "runtime/converter-catalog",
        },
    )
    return path


def patch_authority() -> None:
    document = authority.load_authority()
    payload = {
        "extra_factual_forms": ["dust_div72", "storage_dust"],
        "field": "required_forms",
        "gate_section": SECTION,
        "id": SECTION,
        "java_runtime_visible": True,
        "owner": OWNER,
        "path": REQUIRED_REL,
        "required_factual_prereqs": {
            "dust_div72": ["dust"],
            "storage_dust": ["dust"],
        },
    }
    sources = list(document.get("sources") or [])
    replaced = False
    for index, source in enumerate(sources):
        if source.get("id") == SECTION:
            sources[index] = payload
            replaced = True
            break
    if not replaced:
        sources.append(payload)
    sections = list(document.get("java_overlay_sections") or [])
    if SECTION not in sections:
        sections.append(SECTION)
    document["sources"] = sources
    document["java_overlay_sections"] = sections
    io.write_stable(authority.OUTPUT, document)
    authority.write()


def patch_gate(forms: dict[str, list[str]], required_path: Path) -> dict[str, int]:
    gate = io.load_json(GATE)
    overlay = {
        material: list(prefixes) for material, prefixes in forms.items()
    }
    added = 0
    materials = gate.get("materials") or {}
    for material, prefixes in forms.items():
        current = list(materials.get(material) or [])
        merged = sorted(set(current) | set(prefixes))
        added += len(merged) - len(current)
        materials[material] = merged
    gate["materials"] = materials
    gate[SECTION] = overlay
    sections = list(gate.get("java_overlay_sections") or [])
    if SECTION not in sections:
        sections.append(SECTION)
    gate["java_overlay_sections"] = sections
    sources = dict(gate.get("sources") or {})
    sources[SECTION] = {
        "classification": "semantic_ordinary_runtime_required",
        "field": "required_forms",
        "path": REQUIRED_REL,
        "sha256": sha256_file(required_path),
    }
    gate["sources"] = sources
    counts = dict(gate.get("counts") or {})
    registered = sum(len(row) for row in materials.values())
    counts["materials"] = len(materials)
    counts["registered_forms"] = registered
    gate["counts"] = counts
    io.write_stable(GATE, gate)
    return {"added_form_pairs": added, "registered_forms": registered}


def main() -> int:
    copy_art()
    imported = extract.import_fluidbed()
    forms = required_forms()
    required_path = write_required_forms(forms)
    patch_authority()
    gate_stats = patch_gate(forms, required_path)
    print(f"imported {imported} fluidbed recipes")
    print(
        "required_forms "
        f"{sum(len(row) for row in forms.values())} pairs / {len(forms)} materials"
    )
    print(json.dumps(gate_stats, sort_keys=True))
    storage = sorted(
        material for material, prefixes in forms.items() if "storage_dust" in prefixes
    )
    div72 = sorted(
        material for material, prefixes in forms.items() if "dust_div72" in prefixes
    )
    print("storage_dust", ",".join(storage[:8]))
    print("dust_div72", ",".join(div72[:8]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
