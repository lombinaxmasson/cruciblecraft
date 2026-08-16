#!/usr/bin/env python3
"""Build the T22.5 B2 artifact: disposition of the remaining CC fluid
gaps (A2 no_cc_fluid materials — 15 materials / 18 fluid names, which
corrects the plan §0.4 count of 13).

Disposition per gap material:
  disposition  register | no_registration
  status       no_cc_consumer | cross_mod | registered

Evidence: for every gap fluid name, the B1 class distribution of the
rows that touch it.  Zero v1_required rows touching a gap fluid means
no CC v1 consumer — 登记不注册 (record, do not register).

If a v1 consumer exists, the registration path is a chemical gate file
entry — NEVER ``materials/*.json`` (twelve builders pin the material
tree hash).  This phase is expected to register nothing.

Modes:
  --check   validate the committed artifact (no dump)
  --write --full-replay   scan the dump for gap-fluid row evidence
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_5_fluid_gap_disposition.json"
BUILDER = Path(__file__).resolve()

A2_ARTIFACT = TOOLS / "t22_5_fluid_mapping.json"
B1_ARTIFACT = TOOLS / "t22_5_row_classification.json"
MATERIALS_DIR = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
MIXER_INDEX = TOOLS / "gt6_mixer_templates_index.json"
LEDGER_2 = TOOLS / "t21_template_denominator.json"

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def material_tree_digest() -> str:
    rows = [
        (path.relative_to(ROOT).as_posix(), _sha256(path))
        for path in sorted(MATERIALS_DIR.glob("*.json"))
    ]
    return hashlib.sha256(
        json.dumps(rows, sort_keys=True).encode("utf-8")
    ).hexdigest()


def _template_fluids(template: dict[str, Any]) -> set[str]:
    skeleton = template.get("skeleton") or {}
    names = set()
    for side in ("fluidInputs", "fluidOutputs"):
        for stack in skeleton.get(side) or []:
            if isinstance(stack, dict) and isinstance(
                stack.get("fluid"), str
            ):
                names.add(stack["fluid"])
    return names


def build_from_dump() -> dict[str, Any]:
    a2 = _load(A2_ARTIFACT)
    b1 = _load(B1_ARTIFACT)
    mixer_index = _load(MIXER_INDEX)
    ledger_2 = _load(LEDGER_2)

    gaps = [
        row for row in a2["mapping"]
        if row["disposition"] == "no_cc_fluid"
    ]
    gap_names = {row["fluid"] for row in gaps}
    gap_materials = {row["cc_material"] for row in gaps}
    by_material: dict[str, list[str]] = {}
    for row in gaps:
        by_material.setdefault(row["cc_material"], []).append(row["fluid"])

    class_index = {i: c for i, c in enumerate(b1["classes"])}
    maps = ledger_2["encoding"]["maps"]
    dump_cache: dict[int, list[Any]] = {}
    evidence: dict[str, dict[str, int]] = {
        name: {c: 0 for c in b1["classes"]} for name in gap_names
    }

    for row in b1["non_mixer_rows"]:
        map_index, recipe_index, klass_i = row
        klass = class_index[klass_i]
        map_name = maps[map_index]
        if map_index not in dump_cache:
            document = _load(DUMP / "maps" / f"{map_name}.json")
            dump_cache[map_index] = document.get("recipes") or []
        recipe = dump_cache[map_index][recipe_index]
        names = set()
        for side in ("fluidInputs", "fluidOutputs"):
            for stack in recipe.get(side) or []:
                if isinstance(stack, dict) and isinstance(
                    stack.get("fluid"), str
                ):
                    names.add(stack["fluid"])
        for name in names & gap_names:
            evidence[name][klass] += 1

    for template_row in b1["mixer_templates"]:
        template = mixer_index["templates"][
            template_row["template_index"]
        ]
        names = _template_fluids(template)
        for name in names & gap_names:
            evidence[name][template_row["class"]] += template_row["rows"]

    records = []
    for material in sorted(gap_materials):
        names = by_material[material]
        v1_rows = sum(
            evidence[name].get("v1_required", 0) for name in names
        )
        records.append(
            {
                "material": material,
                "fluid_names": names,
                "disposition": (
                    "register" if v1_rows else "no_registration"
                ),
                "status": "no_cc_consumer" if not v1_rows else "pending",
                "v1_rows_touching": v1_rows,
                "rationale": (
                    "No CC fluid form on any registration path and no "
                    "v1 consumer among the B1-classified rows touching "
                    "it. Recorded, not registered (T22.5 registers no "
                    "fluids)."
                )
                if not v1_rows
                else "v1 consumer exists — gate-file registration "
                "required with a real consumer/producer route.",
                "evidence_by_fluid": {
                    name: evidence[name] for name in names
                },
            }
        )

    return {
        "schema_version": 1,
        "status": "T22_5_FLUID_GAP_DISPOSITION_READY",
        "source_revision": GT6_REVISION,
        "scope_correction": (
            "The plan §0.4 listed 13 gap materials / 16 names. The A2 "
            "derivation finds 15 materials / 18 names: concrete, glass, "
            "mercury and mac_guffium were not in the plan's list; the "
            "'molten hsla' space-variant resolves to hslasteel, which "
            "DOES have a molten_fluid form and is therefore not a gap."
        ),
        "registration_policy": (
            "If disposition == register: append an entry to a chemical "
            "gate file per the ChemicalFluidRegistrationGate schema; "
            "source.reason must point at a real consumer or producer "
            "route. materials/*.json must never be modified."
        ),
        "records": records,
        "counts": {
            "materials": len(records),
            "fluid_names": len(gap_names),
            "register": sum(
                1 for r in records if r["disposition"] == "register"
            ),
            "no_registration": sum(
                1 for r in records
                if r["disposition"] == "no_registration"
            ),
        },
        "material_tree": {
            "path": _relative(MATERIALS_DIR),
            "sha256": material_tree_digest(),
            "note": "materials/*.json must remain byte-identical",
        },
        "inputs": {
            _relative(A2_ARTIFACT): _sha256(A2_ARTIFACT),
            _relative(B1_ARTIFACT): _sha256(B1_ARTIFACT),
            _relative(BUILDER): _sha256(BUILDER),
        },
    }


def write() -> dict[str, Any]:
    document = build_from_dump()
    OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")
    return document


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status") != "T22_5_FLUID_GAP_DISPOSITION_READY":
        errors.append("status != T22_5_FLUID_GAP_DISPOSITION_READY")
    counts = on_disk.get("counts") or {}
    records = on_disk.get("records") or []
    if counts.get("materials") != len(records):
        errors.append("counts.materials != len(records)")
    for record in records:
        if record["disposition"] not in ("register", "no_registration"):
            errors.append(
                f"{record['material']}: invalid disposition "
                f"{record['disposition']}"
            )
        if record.get("fluid_names") and record.get("rationale"):
            pass
        else:
            errors.append(f"{record.get('material')}: incomplete record")
    tree = on_disk.get("material_tree") or {}
    if tree.get("sha256") != material_tree_digest():
        errors.append("materials/*.json tree digest drifted")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        elif args.write:
            if not args.full_replay:
                raise ValueError("--write requires --full-replay")
            document = write()
        else:
            parser.error("choose --check or --write")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22.5 fluid gap disposition failed: {error}",
              file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                "schema_version": document.get("schema_version"),
                "status": document.get("status"),
                "counts": document.get("counts"),
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
