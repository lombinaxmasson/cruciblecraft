#!/usr/bin/env python3
"""Gate the sifter byproduct tiny_dust pairs and prove their rows publish.

The census form-open card gated washed_crushed_ore and left the same 125
materials' tiny_dust byproduct openable. This card writes that remainder
into the gate. tiny_dust is already a public-16 prefix; the public list
does not change.
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
WASHED = (
    ROOT / "tools" / "waves" / "registry" / "census-form-open" / "required_forms.json"
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
SIFTER = ROOT / "tools" / "waves" / "recipe" / "gt6-prefix-regular-bulk" / "sifter"
GATE_SECTION = "sifter_byproduct_tiny_dust_required_forms"
PREP_STATUS = "PREP_SIFTER_BYPRODUCT_TINY_DUST"
LANDED_STATUS = "LANDED_SIFTER_BYPRODUCT_TINY_DUST"
HOSTS = frozenset({"gravel", "sand", "red_sand", "mud"})
UNLOCKED_ROWS = 500


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


def container_pairs() -> set[tuple[str, str]]:
    return {
        (material, form)
        for material, forms in pair_map(io.load_json(CONTAINER)).items()
        for form in forms
    }


def washed_materials() -> set[str]:
    required = pair_map(io.load_json(WASHED))
    unexpected = {
        material: sorted(forms - {"washed_crushed_ore"})
        for material, forms in required.items()
        if forms != {"washed_crushed_ore"}
    }
    if unexpected:
        raise SystemExit(
            "census form-open materials are no longer washed_crushed_ore only: "
            f"{unexpected}"
        )
    return set(required)


def _blocked_pair(reason: str) -> tuple[str, str]:
    prefix = "unregistered material form "
    if not reason.startswith(prefix) or " from " not in reason:
        raise SystemExit(f"sifter blocked reason is not a material form: {reason}")
    token = reason[len(prefix) : reason.index(" from ")]
    material, form = token.split(":", 1)
    return material, form


def unlocked_rows(materials: set[str]) -> list[str]:
    blocked = io.load_json(SIFTER / "blocked.json")
    hashes: list[str] = []
    for row in blocked.get("rows") or []:
        material, form = _blocked_pair(str(row["reason"]))
        if material not in materials or form not in {"washed_crushed_ore", "tiny_dust"}:
            raise SystemExit(
                "sifter blocked row is outside the tiny_dust remainder: "
                f"{row['reason']}"
            )
        hashes.append(str(row["source_row_sha256"]))
    if len(hashes) != UNLOCKED_ROWS or len(set(hashes)) != UNLOCKED_ROWS:
        raise SystemExit(
            f"expected {UNLOCKED_ROWS} distinct sifter remainder rows, "
            f"found {len(hashes)} ({len(set(hashes))} distinct)"
        )
    if int(blocked.get("source_rows") or 0) != 2877:
        raise SystemExit(
            f"sifter source row count drifted: {blocked.get('source_rows')}"
        )
    return hashes


def openable_tiny_dust() -> tuple[dict[str, set[str]], dict[str, int]]:
    census = io.load_json(CENSUS)
    metadata_only = metadata_only_ids()
    washed = washed_materials()
    standard: dict[str, set[str]] = {}
    affected: dict[str, int] = {}
    for row in census.get("openable") or []:
        if str(row["form"]) != "tiny_dust":
            continue
        material = str(row["material"])
        if material in metadata_only:
            raise SystemExit(f"metadata_only material still demands tiny_dust: {material}")
        standard.setdefault(material, set()).add("tiny_dust")
        for map_name, count in (row.get("maps") or {}).items():
            affected[str(map_name)] = affected.get(str(map_name), 0) + int(count)
    if set(standard) != washed:
        missing = sorted(washed - set(standard))[:8]
        extra = sorted(set(standard) - washed)[:8]
        raise SystemExit(
            "tiny_dust openable materials drifted from the washed crushed ore "
            f"gate: missing {missing} extra {extra}"
        )
    if affected != {"gt.recipe.sifter": UNLOCKED_ROWS}:
        raise SystemExit(f"tiny_dust demand is not the 500 sifter rows: {affected}")
    return standard, affected


def assert_factual(materials: set[str]) -> None:
    sys.path.insert(0, str(ROOT / "tools"))
    import gt6_l3_materials  # noqa: WPS433

    plan = io.load_json(gt6_l3_materials.OUT)
    index = io.load_json(MATERIALS / "index.json")
    documents: dict[str, dict[str, Any]] = {}
    for filename in index:
        document = io.load_json(MATERIALS / filename)
        documents[str(document["id"])] = document
    missing = [
        material
        for material in sorted(materials)
        if "tiny_dust"
        not in gt6_l3_materials.resolve_material_forms(documents[material], plan)
    ]
    if missing:
        raise SystemExit(
            "tiny_dust is not a factual dust-flag form for " + ", ".join(missing[:8])
        )


def freeze_document() -> dict[str, Any]:
    standard, affected = openable_tiny_dust()
    assert_factual(set(standard))
    census = io.load_json(CENSUS)
    required_forms = {
        material: sorted(forms) for material, forms in sorted(standard.items())
    }
    hashes = unlocked_rows(set(standard))
    return {
        "affected_maps": dict(sorted(affected.items())),
        "counts": {
            "census_openable_at_freeze": int(census["counts"]["openable"]),
            "required_form_pairs": sum(map(len, required_forms.values())),
            "required_materials": len(required_forms),
            "sifter_source_rows": 2877,
            "unlocked_sifter_rows": len(hashes),
        },
        "generated_by": "tools/waves/recipe/sifter-byproduct-tiny-dust/build.py",
        "note": (
            "Census openable tiny_dust on the 125 anti materials whose "
            "washed_crushed_ore the census form-open card already gated. "
            "tiny_dust stays a public-16 prefix. Unlocks the 500 sifter rows "
            "that output crushedPurified plus dustTiny on gravel, sand, "
            "red sand, and mud."
        ),
        "required_forms": required_forms,
        "source_census": io.relative(CENSUS),
        "source_revision": io.SOURCE_REVISION,
        "status": PREP_STATUS,
        "unlocked_source_row_sha256": hashes,
        "washed_gate": io.relative(WASHED),
    }


def _gate_section(gate: dict[str, Any]) -> dict[str, list[str]]:
    section = gate.get(GATE_SECTION) or {}
    return {str(material): list(forms) for material, forms in section.items()}


def _outputs(relation: dict[str, Any], form: str) -> list[str]:
    return [
        str(operand.get("material") or "")
        for operand in relation.get("item_outputs") or []
        if str(operand.get("form") or "") == form
    ]


def check_published(document: dict[str, Any], materials: set[str]) -> None:
    expected = list(document["unlocked_source_row_sha256"])
    if len(expected) != UNLOCKED_ROWS:
        raise SystemExit("frozen unlocked row list drifted")
    blocked = io.load_json(SIFTER / "blocked.json")
    if blocked.get("rows"):
        raise SystemExit(
            f"sifter still blocks {len(blocked['rows'])} rows after tiny_dust"
        )
    if int(blocked.get("source_rows") or 0) != int(document["counts"]["sifter_source_rows"]):
        raise SystemExit("sifter source row count changed")
    proof = io.load_json(SIFTER / "coverage_proof.json")
    if int(proof.get("published_rows") or 0) != int(blocked["source_rows"]):
        raise SystemExit(
            "sifter published rows do not cover the source map: "
            f"{proof.get('published_rows')} != {blocked.get('source_rows')}"
        )
    source = io.load_json(SIFTER / "source.json")
    by_hash = {
        str(relation["source_row_sha256"]): relation
        for relation in source.get("relations") or []
    }
    missing = [digest for digest in expected if digest not in by_hash]
    if missing:
        raise SystemExit(f"{len(missing)} unlocked sifter rows were not published")
    per_material: dict[str, int] = {}
    for digest in expected:
        relation = by_hash[digest]
        inputs = list(relation.get("item_inputs") or [])
        if len(inputs) != 1 or str(inputs[0].get("runtime_id") or "") != (
            "cruciblecraft:gt_hosted_ore"
        ):
            raise SystemExit(f"{digest} input is not a hosted ore")
        components = inputs[0].get("_components") or {}
        material = str(components.get("cruciblecraft:ore_material") or "")
        host = str((components.get("minecraft:block_state") or {}).get("host") or "")
        if material not in materials or host not in HOSTS:
            raise SystemExit(f"{digest} host {material}/{host} is outside the 125")
        washed = _outputs(relation, "washed_crushed_ore")
        tiny = _outputs(relation, "tiny_dust")
        if washed != [material, material] or tiny != [material, material, material]:
            raise SystemExit(
                f"{digest} outputs are not two washed ores and three tiny dusts "
                f"of {material}: washed={washed} tiny={tiny}"
            )
        chances = [int(value) for value in relation.get("output_chances") or []]
        if chances != [10000, 10000, 1500, 1000, 500]:
            raise SystemExit(f"{digest} chances drifted: {chances}")
        per_material[material] = per_material.get(material, 0) + 1
    uneven = {
        material: count
        for material, count in per_material.items()
        if count != 4
    }
    if set(per_material) != materials or uneven:
        raise SystemExit(f"unlocked rows are not 4 hosts per material: {uneven}")


def check_landed(document: dict[str, Any]) -> None:
    required = pair_map(document)
    if document["counts"]["required_form_pairs"] != sum(map(len, required.values())):
        raise SystemExit("required form pair count drifted")
    if document["counts"]["unlocked_sifter_rows"] != UNLOCKED_ROWS:
        raise SystemExit("unlocked sifter row count drifted")
    forms = {form for values in required.values() for form in values}
    if forms != {"tiny_dust"}:
        raise SystemExit(f"landed forms must be tiny_dust only, found {sorted(forms)}")
    if set(required) != washed_materials():
        raise SystemExit("landed materials drifted from the washed crushed ore gate")
    metadata_only = metadata_only_ids()
    overlap = sorted(set(required) & metadata_only)
    if overlap:
        raise SystemExit(f"metadata-only materials entered the tiny_dust gate: {overlap}")
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
        raise SystemExit(f"gate materials missing tiny_dust: {missing}")
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
        raise SystemExit(f"landed tiny_dust pairs are still census openable: {leaked}")
    containers = container_pairs()
    if still_open != containers:
        missing_open = sorted(containers - still_open)[:8]
        extra_open = sorted(still_open - containers)[:8]
        raise SystemExit(
            "census openable after tiny_dust must be the metadata-only "
            f"container prep; missing {missing_open} extra {extra_open}"
        )
    check_published(document, set(required))


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
            f"rows={document['counts']['unlocked_sifter_rows']}"
        )
        return 0
    if args.mark_landed:
        document = mark_landed()
        print(
            "Landed "
            f"{document['counts']['required_form_pairs']} tiny_dust pairs and "
            f"{document['counts']['unlocked_sifter_rows']} sifter rows"
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
        print("sifter byproduct tiny_dust prep artifact is current")
        return 0
    if status == LANDED_STATUS:
        check_landed(document)
        print("sifter byproduct tiny_dust landing artifact is current")
        return 0
    raise SystemExit(f"unexpected status {status}")


if __name__ == "__main__":
    raise SystemExit(main())
