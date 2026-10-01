#!/usr/bin/env python3
"""Land the standard-gate slice of the material-form census openable set.

The census openable set is the denominator. Metadata-only container pairs
stay on the existing container-form prep and do not enter this gate section.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io

CENSUS = (
    ROOT / "tools" / "waves" / "prep" / "material-form-demand-census" / "census.json"
)
CONTAINER = (
    ROOT
    / "tools"
    / "waves"
    / "prep"
    / "gt6-container-chem-tube-forms"
    / "required_forms.json"
)
OUTPUT = Path(__file__).resolve().parent / "required_forms.json"
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
MATERIALS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
GATE_SECTION = "census_form_open_required_forms"
PREP_STATUS = "PREP_BOUNDED_FORM_OPEN"
LANDED_STATUS = "LANDED_BOUNDED_FORM_OPEN"


def metadata_only_ids() -> set[str]:
    index = io.load_json(MATERIALS / "index.json")
    found: set[str] = set()
    for filename in index:
        document = io.load_json(MATERIALS / filename)
        if document.get("metadata_only"):
            found.add(str(document["id"]))
    return found


def pair_map(document: dict[str, Any]) -> dict[str, set[str]]:
    return {
        str(material): {str(form) for form in forms}
        for material, forms in (document.get("required_forms") or {}).items()
    }


def container_pairs() -> dict[str, set[str]]:
    return pair_map(io.load_json(CONTAINER))


def split_openable() -> tuple[dict[str, set[str]], dict[str, int]]:
    census = io.load_json(CENSUS)
    metadata_only = metadata_only_ids()
    containers = container_pairs()
    standard: dict[str, set[str]] = {}
    excluded: dict[str, set[str]] = {}
    affected: dict[str, int] = {}
    for row in census.get("openable") or []:
        material = str(row["material"])
        form = str(row["form"])
        target = excluded if material in metadata_only else standard
        target.setdefault(material, set()).add(form)
        if material not in metadata_only:
            for map_name, count in (row.get("maps") or {}).items():
                affected[str(map_name)] = affected.get(str(map_name), 0) + int(count)
    if excluded != containers:
        missing = {
            material: sorted(forms - containers.get(material, set()))
            for material, forms in excluded.items()
            if forms - containers.get(material, set())
        }
        extra = {
            material: sorted(forms - excluded.get(material, set()))
            for material, forms in containers.items()
            if forms - excluded.get(material, set())
        }
        raise SystemExit(
            "metadata-only openable pairs drifted from the container-form prep: "
            f"missing={missing} extra={extra}"
        )
    unexpected = {
        material: sorted(forms - {"washed_crushed_ore"})
        for material, forms in standard.items()
        if forms - {"washed_crushed_ore"}
    }
    if unexpected:
        raise SystemExit(
            "standard-gate openable contains forms other than washed_crushed_ore: "
            f"{unexpected}"
        )
    return standard, affected


def freeze_document() -> dict[str, Any]:
    standard, affected = split_openable()
    census = io.load_json(CENSUS)
    required_forms = {
        material: sorted(forms) for material, forms in sorted(standard.items())
    }
    return {
        "affected_maps": dict(sorted(affected.items())),
        "counts": {
            "census_openable_at_freeze": int(census["counts"]["openable"]),
            "excluded_metadata_only_pairs": sum(map(len, container_pairs().values())),
            "required_form_pairs": sum(map(len, required_forms.values())),
            "required_materials": len(required_forms),
        },
        "excluded_metadata_only": {
            "owner": io.relative(CONTAINER),
            "reason": (
                "metadata_only materials cannot enter material_registration_gate"
            ),
        },
        "generated_by": "tools/waves/registry/census-form-open/build.py",
        "note": (
            "Standard-gate slice of a fresh census openable set. Only "
            "washed_crushed_ore (GT6 crushedPurified) on non-metadata "
            "materials. Does not open generation-flag scale, "
            "gated_unresolved, or metadata-only chem tubes and empty arrows."
        ),
        "required_forms": required_forms,
        "source_census": io.relative(CENSUS),
        "source_revision": io.SOURCE_REVISION,
        "status": PREP_STATUS,
    }


def _gate_section(gate: dict[str, Any]) -> dict[str, list[str]]:
    section = gate.get(GATE_SECTION) or {}
    return {str(material): list(forms) for material, forms in section.items()}


def check_landed(document: dict[str, Any]) -> None:
    required = pair_map(document)
    if document["counts"]["required_form_pairs"] != sum(map(len, required.values())):
        raise SystemExit("required form pair count drifted")
    forms = {form for values in required.values() for form in values}
    if forms != {"washed_crushed_ore"}:
        raise SystemExit(
            "landed forms must be washed_crushed_ore only, "
            f"found {sorted(forms)}"
        )
    metadata_only = metadata_only_ids()
    overlap = sorted(set(required) & metadata_only)
    if overlap:
        raise SystemExit(f"metadata-only materials entered the gate slice: {overlap}")
    gate = io.load_json(GATE)
    section = pair_map({"required_forms": _gate_section(gate)})
    if section != required:
        raise SystemExit(f"{GATE_SECTION} does not match the frozen required forms")
    materials = gate.get("materials") or {}
    missing = {
        material: sorted(forms - set(materials.get(material) or []))
        for material, forms in required.items()
        if not forms <= set(materials.get(material) or [])
    }
    if missing:
        raise SystemExit(f"gate materials missing landed forms: {missing}")
    census = io.load_json(CENSUS)
    still_open = {
        (str(row["material"]), str(row["form"]))
        for row in census.get("openable") or []
    }
    landed_pairs = {
        (material, form) for material, forms in required.items() for form in forms
    }
    leaked = sorted(still_open & landed_pairs)
    if leaked:
        raise SystemExit(f"landed pairs are still census openable: {leaked}")
    containers = {
        (material, form)
        for material, forms in container_pairs().items()
        for form in forms
    }
    # Sifter rows emit crushedPurified plus tiny byproduct dust. This card
    # left tiny_dust openable. The sifter byproduct card gates that remainder;
    # afterwards openable is only the metadata-only container prep.
    byproduct = {(material, "tiny_dust") for material in required}
    if byproduct <= still_open:
        expected_open = containers | byproduct
    elif byproduct.isdisjoint(still_open):
        expected_open = containers
    else:
        raise SystemExit(
            "tiny_dust byproduct is only partly still openable: "
            f"{sorted(byproduct & still_open)[:8]}"
        )
    if still_open != expected_open:
        missing = sorted(expected_open - still_open)[:8]
        extra = sorted(still_open - expected_open)[:8]
        raise SystemExit(
            "census openable after this card must be the metadata-only "
            "container prep, plus tiny_dust on the landed materials until "
            f"the sifter byproduct card gates them; missing {missing} extra {extra}"
        )


def mark_landed() -> dict[str, Any]:
    if not OUTPUT.is_file():
        raise SystemExit(f"missing {io.relative(OUTPUT)}; run --write first")
    document = io.load_json(OUTPUT)
    if document.get("status") not in {PREP_STATUS, LANDED_STATUS}:
        raise SystemExit(f"unexpected status {document.get('status')}")
    document["status"] = LANDED_STATUS
    check_landed(document)
    io.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--mark-landed", action="store_true")
    args = parser.parse_args(argv)
    selected = sum(bool(flag) for flag in (args.write, args.check, args.mark_landed))
    if selected != 1:
        parser.error("choose exactly one of --write, --check, or --mark-landed")
    if args.write:
        document = freeze_document()
        io.write_stable(OUTPUT, document)
        print(
            "Wrote "
            f"{io.relative(OUTPUT)} "
            f"pairs={document['counts']['required_form_pairs']} "
            f"excluded={document['counts']['excluded_metadata_only_pairs']}"
        )
        return 0
    if args.mark_landed:
        document = mark_landed()
        print(
            "Landed "
            f"{document['counts']['required_form_pairs']} standard-gate pairs"
        )
        return 0
    if not OUTPUT.is_file():
        raise SystemExit(f"missing {io.relative(OUTPUT)}")
    document = io.load_json(OUTPUT)
    status = document.get("status")
    if status == PREP_STATUS:
        expected = freeze_document()
        if document != expected:
            raise SystemExit(f"{io.relative(OUTPUT)} drifted; run --write")
        print("census form-open prep artifact is current")
        return 0
    if status == LANDED_STATUS:
        check_landed(document)
        print("census form-open landing artifact is current")
        return 0
    raise SystemExit(f"unexpected status {status}")


if __name__ == "__main__":
    raise SystemExit(main())
