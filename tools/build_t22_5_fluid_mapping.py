#!/usr/bin/env python3
"""Build the T22.5 A2 artifact: GT6 fluid name -> CC fluid id mapping.

The mapping for the 322 normalized GT6 oredict fluids is fully DERIVED
(no hand-written entries): normalized fluids x activation policy x the
CC registered-fluid universe (three chemical gate files + material
``molten_fluid`` flags + ModFluids statics + vanilla builtins).

Fluid names referenced by T21 source rows but absent from the 322 carry
an explicit identity decision (``unknown_identity`` section).  Decisions
are class definitions, never "cannot translate": each has a rationale
and a recheck point.

Modes:
  --check --reference-only   validate the committed artifact without the
                             GT6 dump (ordinary CI)
  --write --full-replay      recompute unknown-identity row counts from
                             the GT6 dump and write the artifact

No fluid is registered here and no recipe JSON is written
(publication delta = 0).  ``build_t5_source_projection.fluid_form()``
remains the runtime derivation reference; wiring the translation layer
to consume this artifact is T23 work (see the ``consumers`` note).
"""
from __future__ import annotations

import argparse
import glob
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_5_fluid_mapping.json"
BUILDER = Path(__file__).resolve()

NORMALIZED_FLUIDS = TOOLS / "gt6_oredict_fluids_normalized.json"
ACTIVATION_POLICY = TOOLS / "gt6_material_activation_policy.json"
MATERIALS_DIR = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
GATE_FILES = [
    ROOT / "src/main/resources/data/cruciblecraft/t10_container_fluid_gate.json",
    ROOT / "src/main/resources/data/cruciblecraft/t11_hydrocarbon_fluid_gate.json",
    ROOT
    / "src/t5_chemical_generated/resources/data/cruciblecraft/"
    "t5_chemical_fluid_gate.json",
]
SOURCE_DENOMINATOR = TOOLS / "t21_source_denominator.json"
FLUID_MAP_DUMP = (
    ROOT / "gt6_dump/gt6_recipe_dump/oredict/fluid_map.json"
)

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"

BUILTIN_FLUIDS = {
    "water": "minecraft:water",
    "lava": "minecraft:lava",
    "steam": "cruciblecraft:steam",
    "creosote": "cruciblecraft:creosote",
}

DISPOSITION_VOCABULARY = {
    "mapped",
    "no_cc_fluid",
    "out_of_scope",
}

# ---------------------------------------------------------------------------
# Unknown-identity decisions (checked in; each entry is a class definition
# with rationale and recheck point, never "cannot translate")
# ---------------------------------------------------------------------------

UNKNOWN_IDENTITY_DECISIONS: dict[str, dict[str, str]] = {
    "potion.mineralwater": {
        "class": "out_of_scope_g10_brewing",
        "rationale": "GT6 potion-brewing base fluid; CC has no brewing "
        "system and potions are G10 (out of scope for v1).",
        "recheck_point": "CC adds a brewing system with fluid consumers.",
    },
    "potion.dressing": {
        "class": "out_of_scope_g10_brewing",
        "rationale": "GT6 brewing byproduct; same as potion.mineralwater.",
        "recheck_point": "CC adds a brewing system.",
    },
    "spectral_dew": {
        "class": "out_of_scope_magic_compat",
        "rationale": "GT6 magic-compat fluid with no CC identity; no CC "
        "consumer or producer route.",
        "recheck_point": "CC introduces a magic system consuming it.",
    },
    "fieryblood": {
        "class": "out_of_scope_g10_nether_mobs",
        "rationale": "GT6 nether mob-drop fluid; mob drops are G10.",
        "recheck_point": "CC adds nether mob processing.",
    },
    "fierytears": {
        "class": "out_of_scope_g10_nether_mobs",
        "rationale": "GT6 nether mob-drop fluid; mob drops are G10.",
        "recheck_point": "CC adds nether mob processing.",
    },
    "enderair": {
        "class": "out_of_scope_g10_dimension",
        "rationale": "End dimension atmosphere fluid; no CC consumer.",
        "recheck_point": "CC adds End dimension content.",
    },
    "netherair": {
        "class": "out_of_scope_g10_dimension",
        "rationale": "Nether dimension atmosphere fluid; no CC consumer.",
        "recheck_point": "CC adds Nether dimension content.",
    },
    "soulsandoil": {
        "class": "out_of_scope_g10_dimension",
        "rationale": "Soul sand processing fluid; nether content is G10.",
        "recheck_point": "CC adds nether processing.",
    },
    "applevinegar": {
        "class": "out_of_scope_g10_food",
        "rationale": "G10 food-processing fluid; no CC consumer.",
        "recheck_point": "CC adds a food chain.",
    },
    "vinegar": {
        "class": "out_of_scope_g10_food",
        "rationale": "G10 food-processing fluid; no CC consumer.",
        "recheck_point": "CC adds a food chain.",
    },
    "canevinegar": {
        "class": "out_of_scope_g10_food",
        "rationale": "G10 food-processing fluid; no CC consumer.",
        "recheck_point": "CC adds a food chain.",
    },
    "ricevinegar": {
        "class": "out_of_scope_g10_food",
        "rationale": "G10 food-processing fluid; no CC consumer.",
        "recheck_point": "CC adds a food chain.",
    },
    "ketchup": {
        "class": "out_of_scope_g10_food",
        "rationale": "G10 food-processing fluid; no CC consumer.",
        "recheck_point": "CC adds a food chain.",
    },
    "bbqsauce": {
        "class": "out_of_scope_g10_food",
        "rationale": "G10 food-processing fluid; no CC consumer.",
        "recheck_point": "CC adds a food chain.",
    },
    "coconutmilk": {
        "class": "out_of_scope_g10_food",
        "rationale": "G10 food-processing fluid; no CC consumer.",
        "recheck_point": "CC adds a food chain.",
    },
    "liquid_light_oil": {
        "class": "covered_by_petroleum_identity",
        "rationale": "GT6 distillation fraction covered by the T11/T22 "
        "petroleum identity (crude_oil chain); O-37 keeps GT6 fluid "
        "names unbound from CC materials.",
        "recheck_point": "T22.5 B1 reclassifies its rows under the "
        "petroleum vocabulary.",
    },
    "liquid_medium_oil": {
        "class": "covered_by_petroleum_identity",
        "rationale": "Same as liquid_light_oil.",
        "recheck_point": "Same as liquid_light_oil.",
    },
    "liquid_heavy_oil": {
        "class": "covered_by_petroleum_identity",
        "rationale": "Same as liquid_light_oil.",
        "recheck_point": "Same as liquid_light_oil.",
    },
    "liquid_extra_heavy_oil": {
        "class": "covered_by_petroleum_identity",
        "rationale": "Same as liquid_light_oil.",
        "recheck_point": "Same as liquid_light_oil.",
    },
}

# cfoam.{color}: GT6 colored construction-foam variants of the CC-ACTIVE
# construction_foam material.  CC registers one uncolored fluid; the
# color channel is a GT6 cosmetic layer.
for _color in (
    "white", "red", "lightgray", "gray", "black", "green", "lime",
    "pink", "yellow", "brown", "lightblue", "orange",
):
    UNKNOWN_IDENTITY_DECISIONS[f"cfoam.{_color}"] = {
        "class": "equivalence_construction_foam_color",
        "rationale": "GT6 colored variant of the CC-ACTIVE "
        "construction_foam material (see ic2constructionfoam). CC has "
        "one uncolored fluid; the color channel is GT6 cosmetic.",
        "recheck_point": "CC adds colored construction foam.",
    }

# dye.watermixed.{color} and dye.chemical.{color}: GT6 dye dilution and
# chemical dye fluids; dyes are cosmetics (G10).  The color sets below
# are exactly the names the GT6-dump scan observed.
for _color in ("blue", "cyan", "brown", "yellow"):
    UNKNOWN_IDENTITY_DECISIONS[f"dye.watermixed.{_color}"] = {
        "class": "out_of_scope_g10_dyeing",
        "rationale": "GT6 water-diluted dye fluid; dyeing is G10.",
        "recheck_point": "CC adds a dye system.",
    }
for _color in (
    "white", "orange", "cyan", "red", "blue", "pink", "black", "brown",
    "magenta", "green", "yellow", "lightblue", "lime", "lightgray",
    "gray", "purple",
):
    UNKNOWN_IDENTITY_DECISIONS[f"dye.chemical.{_color}"] = {
        "class": "out_of_scope_g10_dyeing",
        "rationale": "GT6 chemical dye fluid; dyeing is G10.",
        "recheck_point": "CC adds a dye system.",
    }

# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------


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


# ---------------------------------------------------------------------------
# derivation
# ---------------------------------------------------------------------------


def _registered_universe() -> tuple[dict[str, str], dict[str, str], set[str]]:
    """Return (gate material -> cc fluid id, gate id -> material,
    molten_fluid material ids)."""
    gate_materials: dict[str, str] = {}
    gate_ids: dict[str, str] = {}
    for path in GATE_FILES:
        for entry in _load(path)["fluids"]:
            gate_materials[entry["material"]] = f"cruciblecraft:{entry['id']}"
            gate_ids[entry["id"]] = entry["material"]
    molten: set[str] = set()
    for path in glob.glob(str(MATERIALS_DIR / "*.json")):
        document = _load(Path(path))
        if isinstance(document, dict) and document.get("molten_fluid"):
            molten.add(document["id"])
    return gate_materials, gate_ids, molten


def derive(policy_override: dict[str, dict[str, Any]] | None = None) -> dict[str, Any]:
    """Derive the 322-fluid mapping.  Pure function of committed inputs;
    ``policy_override`` re-maps activation-policy records by source_id for
    mutation testing."""
    records = _load(NORMALIZED_FLUIDS)["records"]
    policy = _load(ACTIVATION_POLICY)["records"]
    by_source_id: dict[int, dict[str, Any]] = {
        int(record["source_id"]): record for record in policy
    }
    if policy_override:
        for source_id, replacement in policy_override.items():
            by_source_id[source_id] = replacement
    gate_materials, gate_ids, molten = _registered_universe()

    dispositions: dict[str, list[dict[str, Any]]] = {
        "mapped": [],
        "no_cc_fluid": [],
        "out_of_scope": [],
    }
    for record in records:
        name: str = record["fluid"]
        entry: dict[str, Any] = {
            "fluid": name,
            "material_id": int(record["material_id"]),
        }
        if name in BUILTIN_FLUIDS:
            entry.update(
                {
                    "disposition": "mapped",
                    "cc_fluid_id": BUILTIN_FLUIDS[name],
                    "derivation": "builtin",
                }
            )
            dispositions["mapped"].append(entry)
            continue
        policy_record = by_source_id.get(int(record["material_id"]))
        if policy_record is None or policy_record["source_id"] == -1:
            entry.update(
                {
                    "disposition": "out_of_scope",
                    "cc_fluid_id": None,
                    "derivation": "policy_out_of_scope",
                }
            )
            dispositions["out_of_scope"].append(entry)
            continue
        cc_id = policy_record["cc_id"]
        molten_name = name.startswith("molten.") or name.startswith(
            "molten "
        )
        if molten_name:
            cc_fluid_id = f"cruciblecraft:molten_{cc_id}"
            registered = cc_id in molten or f"molten_{cc_id}" in gate_ids
            derivation = "molten_fluid" if cc_id in molten else "route_gate"
        else:
            cc_fluid_id = f"cruciblecraft:{cc_id}"
            registered = cc_id in gate_materials or cc_id in (
                "creosote",
                "steam",
            )
            derivation = "chemical_gate"
        entry.update(
            {
                "disposition": "mapped" if registered else "no_cc_fluid",
                "cc_fluid_id": cc_fluid_id if registered else None,
                "cc_material": cc_id,
                "derivation": derivation,
            }
        )
        dispositions[entry["disposition"]].append(entry)

    for bucket in dispositions.values():
        bucket.sort(key=lambda row: row["fluid"])

    return {
        "records": (
            dispositions["mapped"]
            + dispositions["no_cc_fluid"]
            + dispositions["out_of_scope"]
        ),
        "counts": {
            "total": len(records),
            "mapped": len(dispositions["mapped"]),
            "no_cc_fluid": len(dispositions["no_cc_fluid"]),
            "out_of_scope": len(dispositions["out_of_scope"]),
            "unclassified": 0,
        },
    }


def _scan_unknown_rows() -> dict[str, int]:
    """Scan all T21 source-row recipes for fluid names absent from the
    GT6 oredict fluid map.  Requires the GT6 dump (full-replay only)."""
    denominator = _load(SOURCE_DENOMINATOR)
    map_ids = denominator["source"]["maps"]
    rows = denominator["rows"]
    fluid_map = _load(FLUID_MAP_DUMP)
    known_names = set(fluid_map.keys())

    needed: dict[tuple[int, int], int] = {}
    for row in rows:
        key = (row[0], row[1])
        needed[key] = needed.get(key, 0) + 1

    cache: dict[int, list[Any]] = {}
    counts: dict[str, int] = {}
    for (map_index, recipe_index), count in needed.items():
        if map_index not in cache:
            document = _load(
                ROOT
                / "gt6_dump/gt6_recipe_dump/maps"
                / f"{map_ids[map_index]}.json"
            )
            recipes = document.get("recipes") or document.get("entries")
            cache[map_index] = recipes if isinstance(recipes, list) else []
        recipe = cache[map_index][recipe_index]

        def collect(node: Any, out: list[str]) -> None:
            if isinstance(node, dict):
                if "fluid" in node and isinstance(node.get("fluid"), str):
                    out.append(node["fluid"])
                for value in node.values():
                    collect(value, out)
            elif isinstance(node, list):
                for value in node:
                    collect(value, out)

        fluids: list[str] = []
        collect(recipe, fluids)
        for name in set(fluids):
            if name not in known_names:
                counts[name] = counts.get(name, 0) + count
    return counts


# ---------------------------------------------------------------------------
# build / check / write
# ---------------------------------------------------------------------------


def build(existing: dict[str, Any] | None = None) -> dict[str, Any]:
    """Assemble the artifact.  Unknown-identity recorded counts are
    preserved from the committed file; ``--write --full-replay`` is the
    only path that recomputes them (dump-gated)."""
    existing = existing or (_load(OUTPUT) if OUTPUT.is_file() else {})
    derivation = derive()
    unknown_existing = (existing.get("unknown_identity") or {}).get(
        "records", []
    )
    recorded_rows = {
        row["fluid"]: row["recorded_rows"] for row in unknown_existing
    }
    unknown_records = []
    for name, decision in sorted(UNKNOWN_IDENTITY_DECISIONS.items()):
        unknown_records.append(
            {
                "fluid": name,
                "recorded_rows": recorded_rows.get(name, 0),
                **decision,
            }
        )
    document = {
        "schema_version": 1,
        "status": "T22_5_FLUID_MAPPING_READY",
        "source_revision": GT6_REVISION,
        "consumers": (
            "Runtime derivation reference remains "
            "build_t5_source_projection.fluid_form(); wiring the "
            "translation layer to consume this artifact is T23 work."
        ),
        "derivation_chain": {
            "normalized_fluids": _relative(NORMALIZED_FLUIDS),
            "activation_policy": _relative(ACTIVATION_POLICY),
            "gate_files": [_relative(p) for p in GATE_FILES],
            "material_molten_flags": _relative(MATERIALS_DIR),
            "builtins": BUILTIN_FLUIDS,
        },
        "mapping": derivation["records"],
        "counts": derivation["counts"],
        "unknown_identity": {
            "definition": (
                "Fluid names referenced by T21 source rows but absent "
                "from the 322 normalized GT6 oredict fluids. Each has an "
                "explicit identity decision; recorded_rows come from the "
                "GT6-dump scan (full-replay)."
            ),
            "records": unknown_records,
            "recorded_rows_total": sum(
                row["recorded_rows"] for row in unknown_records
            ),
        },
        "fixtures": [
            {
                "gt6_fluid": "molten.brass",
                "cc_fluid_id": "cruciblecraft:molten_brass",
            },
            {
                "gt6_fluid": "molten.asphalt",
                "cc_fluid_id": "cruciblecraft:molten_asphalt",
            },
            {
                "gt6_fluid": "ic2constructionfoam",
                "cc_fluid_id": "cruciblecraft:construction_foam",
            },
            {
                "gt6_fluid": "ic2distilledwater",
                "cc_fluid_id": "cruciblecraft:water_distilled",
            },
        ],
        "publication_delta": 0,
        "inputs": {
            _relative(NORMALIZED_FLUIDS): _sha256(NORMALIZED_FLUIDS),
            _relative(ACTIVATION_POLICY): _sha256(ACTIVATION_POLICY),
            _relative(BUILDER): _sha256(BUILDER),
        },
    }
    return document


def write_with_replay() -> dict[str, Any]:
    """Full-replay write: recompute unknown-identity recorded counts
    from the GT6 dump, then write."""
    if not FLUID_MAP_DUMP.is_file():
        raise OSError(
            "gt6_dump/gt6_recipe_dump/oredict/fluid_map.json is required "
            "for --write --full-replay"
        )
    counts = _scan_unknown_rows()
    existing = {}
    document = build(existing)
    for row in document["unknown_identity"]["records"]:
        row["recorded_rows"] = counts.get(row["fluid"], 0)
    document["unknown_identity"]["recorded_rows_total"] = sum(
        row["recorded_rows"]
        for row in document["unknown_identity"]["records"]
    )
    scanned_names = set(counts)
    decision_names = set(UNKNOWN_IDENTITY_DECISIONS)
    if scanned_names != decision_names:
        raise ValueError(
            "unknown-identity decision list mismatch: "
            f"undecided={sorted(scanned_names - decision_names)}, "
            f"stale={sorted(decision_names - scanned_names)}"
        )
    OUTPUT.write_bytes(_stable(document).encode("utf-8"))
    return document


def check() -> list[str]:
    """Validate the committed artifact without the GT6 dump."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load(OUTPUT)
    expected = build(on_disk)
    derivation = derive()
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status") != "T22_5_FLUID_MAPPING_READY":
        errors.append("status != T22_5_FLUID_MAPPING_READY")
    if on_disk.get("counts") != derivation["counts"]:
        errors.append("mapping counts do not match the fresh derivation")
    if on_disk.get("mapping") != derivation["records"]:
        errors.append("mapping records do not match the fresh derivation")
    if on_disk.get("publication_delta") != 0:
        errors.append("publication_delta != 0")
    unknown = on_disk.get("unknown_identity") or {}
    decision_names = set(UNKNOWN_IDENTITY_DECISIONS)
    unknown_names = {row.get("fluid") for row in unknown.get("records", [])}
    if unknown_names != decision_names:
        errors.append(
            "unknown_identity records do not match the checked-in "
            f"decisions: missing={sorted(decision_names - unknown_names)}, "
            f"extra={sorted(unknown_names - decision_names)}"
        )
    for row in unknown.get("records", []):
        if row["fluid"] in {r["fluid"] for r in derivation["records"]}:
            errors.append(
                f"unknown_identity {row['fluid']} is in the 322; "
                "decision must move into the mapping"
            )
        if not isinstance(row.get("recorded_rows"), int) or row[
            "recorded_rows"
        ] < 0:
            errors.append(
                f"unknown_identity {row['fluid']} recorded_rows invalid"
            )
    if _stable(on_disk) != _stable(expected):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
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
            document = write_with_replay()
        else:
            parser.error("choose --check or --write")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22.5 fluid mapping failed: {error}", file=sys.stderr)
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
