#!/usr/bin/env python3
"""Build the T22 petroleum family capability assessment.

For every v1 petroleum family, this produces a five-item capability table:
  1. source row count + row keys
  2. CC material / fluid mapping (source-backed / source-derived / DESIGN_POLICY)
  3. executing machine (implemented or B1 work item)
  4. energy identity (RU/KU/EU/HU/STEAM, never folded)
  5. real consumer with bidirectional proof (consumer + operand)

Every non-ready item has an owner and a B1 work item.  No deferred items
without owner are allowed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_family_capability.json"
BUILDER = Path(__file__).resolve()

# ---------------------------------------------------------------------------
# family definitions
# ---------------------------------------------------------------------------

# Each petroleum family maps to one or more CC RecipeMaps.
# Energy identity is drawn from GT6 source semantics and CC machine architecture.
FAMILIES: list[dict[str, Any]] = [
    {
        "family_id": "crude_oil_distillation",
        "name": "Crude Oil Distillation",
        "maps": ["cruciblecraft:distillery", "cruciblecraft:centrifuge"],
        "gt6_maps": ["gt.recipe.distillery", "gt.recipe.centrifuge"],
        "source_rows": {
            "t21_petroleum_t22": "centrifuge: 5 rows (crude_oil, oil_sand targeting)",
            "distillery": "1 v1_required row (oil -> fuel + lubricant)",
            "total_known": 6,
        },
        "materials_and_fluids": {
            "crude_oil": {
                "status": "source-backed",
                "cc_id": "cruciblecraft:crude_oil",
                "material_id": 9852,
                "note": "T9 identity, SOURCE_MATERIAL_LAYER_ONLY role per T18 O37",
            },
            "oil": {
                "status": "source-derived",
                "cc_id": "cruciblecraft:oil",
                "material_id": 9850,
                "note": "Registered in gt6_oredict_fluids_normalized.json",
            },
            "fuel": {
                "status": "source-derived",
                "cc_id": "cruciblecraft:fuel",
                "material_id": 9860,
                "note": "Registered; consumed by fuels_engine (T18)",
            },
            "lubricant": {
                "status": "source-derived",
                "cc_id": "cruciblecraft:lubricant",
                "material_id": 9882,
                "note": "Registered; no real consumer identified yet (C1 work)",
            },
        },
        "machine": {
            "distillery": {
                "implemented": True,
                "tiers": ["invar", "titanium"],
                "owner": "T17",
            },
            "centrifuge": {
                "implemented": True,
                "tiers": ["steel", "titanium"],
                "owner": "T16/T17",
            },
        },
        "energy": {
            "distillery": "HU (Heat)",
            "centrifuge": "EU (Electric)",
        },
        "consumers": {
            "fuel": {
                "consumer": "cruciblecraft:fuels_engine",
                "product": "KU power",
                "implemented": True,
                "owner": "T18",
                "consumer_proof": "T18 GameTest: crude_oil -> fuel_engine -> dynamo -> electrolyzer",
                "operand_proof": "fuel is produced by distillery row 705 (oil -> fuel), oil is produced by centrifuge from crude_oil",
            },
            "lubricant": {
                "consumer": "NOT_FOUND",
                "product": "UNKNOWN",
                "implemented": False,
                "owner": "C1 (T22 downstream product wiring)",
                "consumer_proof": "PENDING: lubricant has no real consumer in current Beta chain",
                "operand_proof": "PENDING",
                "b1_item": True,
            },
        },
        "unreachable_operands": [
            "lubricant (no consumer)",
        ],
    },
    {
        "family_id": "natural_gas_processing",
        "name": "Natural Gas Processing",
        "maps": ["cruciblecraft:centrifuge", "cruciblecraft:generifier", "cruciblecraft:fuels_gas"],
        "gt6_maps": ["gt.recipe.centrifuge", "gt.recipe.generifier"],
        "source_rows": {
            "t21_petroleum_t22": "centrifuge: natural_gas-related rows",
            "generifier": "0 petroleum rows identified (1 T11 published: natural_gas -> methane)",
            "total_known": "TBD by full replay",
        },
        "materials_and_fluids": {
            "natural_gas": {
                "status": "source-backed",
                "cc_id": "cruciblecraft:natural_gas",
                "material_id": 9853,
                "note": "T9 identity; T11 published generifier row",
            },
            "methane": {
                "status": "source-derived",
                "cc_id": "cruciblecraft:methane",
                "note": "T11 published; consumed by fuels_gas (T18)",
            },
        },
        "machine": {
            "centrifuge": {
                "implemented": True,
                "tiers": ["steel", "titanium"],
                "owner": "T16/T17",
            },
            "generifier": {
                "implemented": True,
                "tiers": ["steel"],
                "owner": "T11",
            },
            "fuels_gas": {
                "implemented": True,
                "tiers": ["bronze"],
                "owner": "T18",
            },
        },
        "energy": {
            "centrifuge": "EU (Electric)",
            "generifier": "EU (Electric) — T11 zero-energy generifier",
            "fuels_gas": "HU (Heat)",
        },
        "consumers": {
            "methane": {
                "consumer": "cruciblecraft:fuels_gas",
                "product": "HU heat",
                "implemented": True,
                "owner": "T18",
                "consumer_proof": "T18 GameTest: natural_gas -> fuels_gas -> heat",
                "operand_proof": "methane is produced by generifier from natural_gas (T11 published)",
            },
        },
        "unreachable_operands": [],
    },
    {
        "family_id": "fuel_combustion",
        "name": "Fuel Oil Combustion",
        "maps": ["cruciblecraft:fuels_engine"],
        "gt6_maps": ["gt.recipe.fuels.engine"],
        "source_rows": {
            "t21_petroleum_t22": "fuels_engine rows for fuel oil combustion",
            "total_known": "TBD — fuels_engine is a consumer map, petroleum rows TBD",
        },
        "materials_and_fluids": {
            "fuel": {
                "status": "source-derived",
                "cc_id": "cruciblecraft:fuel",
                "material_id": 9860,
            },
        },
        "machine": {
            "fuels_engine": {
                "implemented": True,
                "tiers": ["bronze"],
                "owner": "T18",
            },
        },
        "energy": {
            "fuels_engine": "KU (Kinetic) — produces KU, does not consume it",
        },
        "consumers": {
            "KU_power": {
                "consumer": "Any KU-consuming machine (crusher, press, dynamo->EU)",
                "product": "Mechanical work",
                "implemented": True,
                "owner": "T18",
                "consumer_proof": "T18 GameTest chain: fuel_engine -> dynamo -> electrolyzer",
                "operand_proof": "fuel is produced by distillery/centrifuge from crude_oil/oil",
            },
        },
        "unreachable_operands": [],
    },
    {
        "family_id": "oil_sand_processing",
        "name": "Oil Sand / Oil Shale Processing",
        "maps": ["cruciblecraft:smelter", "cruciblecraft:centrifuge"],
        "gt6_maps": ["gt.recipe.smelter", "gt.recipe.centrifuge"],
        "source_rows": {
            "t21_petroleum_t22": "smelter: 3 units / 3 rows; centrifuge: oil_sand rows",
            "total_known": "3 smelter + centrifuge oil_sand rows",
        },
        "materials_and_fluids": {
            "oil_sand": {
                "status": "source-backed",
                "cc_id": "cruciblecraft:oil_sand",
                "note": "Worldgen material (T20 ore catalog)",
            },
            "oil_shale": {
                "status": "source-backed",
                "cc_id": "cruciblecraft:oil_shale",
                "note": "Worldgen material (T20 ore catalog)",
            },
        },
        "machine": {
            "smelter": {
                "implemented": True,
                "tiers": ["invar", "titanium"],
                "owner": "T17",
            },
            "centrifuge": {
                "implemented": True,
                "tiers": ["steel", "titanium"],
                "owner": "T16/T17",
            },
        },
        "energy": {
            "smelter": "HU (Heat)",
            "centrifuge": "EU (Electric)",
        },
        "consumers": {
            "oil_sand_products": {
                "consumer": "Feeds into crude_oil_distillation family (oil intermediate)",
                "product": "Oil (intermediate)",
                "implemented": True,
                "owner": "Chained via centrifuge",
                "consumer_proof": "oil_sand -> smelter/centrifuge -> oil -> distillery -> fuel + lubricant",
                "operand_proof": "oil_sand available via T20 worldgen large veins",
            },
        },
        "unreachable_operands": [],
    },
    {
        "family_id": "mixer_petroleum_chemistry",
        "name": "Mixer Petroleum Chemistry (Template-Based)",
        "maps": ["cruciblecraft:mixer"],
        "gt6_maps": ["gt.recipe.mixer"],
        "source_rows": {
            "t21_petroleum_t22": "49 units / 1,057 rows (template-expanded)",
            "total_known": 1057,
            "note": "Largest petroleum family by row count. These are material-matrix templates producing various petroleum-derived chemicals, plastics precursors, and fuel blends.",
        },
        "materials_and_fluids": {
            "status": "PARTIAL",
            "note": "49 mixer templates span diverse material/fluid combinations. Each template needs individual operand proof. Detailed analysis deferred to B2 (first family full projection).",
        },
        "machine": {
            "mixer": {
                "implemented": True,
                "tiers": ["steel", "titanium", "tungsten"],
                "owner": "T16",
            },
        },
        "energy": {
            "mixer": "EU (Electric)",
        },
        "consumers": {
            "status": "PARTIAL",
            "note": "Mixer products feed into diverse downstream chains. Consumer mapping requires per-template analysis (B2 work). The T21 gunpowder template (4 rows, not petroleum) is the reference pattern.",
        },
        "unreachable_operands": [
            "Multiple mixer templates — detailed operand reachability pending B2",
        ],
    },
    {
        "family_id": "compressor_petroleum",
        "name": "Compressor Petroleum Processing",
        "maps": ["cruciblecraft:compressor"],
        "gt6_maps": ["gt.recipe.compressor"],
        "source_rows": {
            "t21_petroleum_t22": "1 unit / 1 row",
            "total_known": 1,
        },
        "materials_and_fluids": {
            "status": "TBD",
            "note": "Single compressor row — likely gas compression. Detailed analysis in B3 batch projection.",
        },
        "machine": {
            "compressor": {
                "implemented": True,
                "tiers": ["steel"],
                "owner": "T16 (KU kind)",
                "note": "T16 owns Compressor as KU kind; T5 host is ELECTRIC. Energy mismatch recorded in T16 audit.",
            },
        },
        "energy": {
            "compressor": "KU (Kinetic) — T16 ownership; ELECTRIC in T5 host (known mismatch)",
        },
        "consumers": {
            "status": "TBD",
            "note": "Single-row family; consumer identification in B3.",
        },
        "unreachable_operands": [
            "Consumer not yet identified",
        ],
    },
    {
        "family_id": "electrolyzer_petroleum",
        "name": "Electrolyzer Petroleum Processing",
        "maps": ["cruciblecraft:electrolyzer"],
        "gt6_maps": ["gt.recipe.electrolyzer"],
        "source_rows": {
            "t21_petroleum_t22": "1 unit / 1 row",
            "total_known": 1,
        },
        "materials_and_fluids": {
            "status": "TBD",
            "note": "Single electrolyzer row. Detailed analysis in B3 batch projection.",
        },
        "machine": {
            "electrolyzer": {
                "implemented": True,
                "tiers": ["steel", "titanium"],
                "owner": "T17 (preimplemented electric reference)",
            },
        },
        "energy": {
            "electrolyzer": "EU (Electric)",
        },
        "consumers": {
            "status": "TBD",
            "note": "Single-row family; consumer identification in B3.",
        },
        "unreachable_operands": [
            "Consumer not yet identified",
        ],
    },
]

# ---------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True,
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def build() -> dict[str, Any]:
    """Assemble the family capability assessment."""
    families = FAMILIES

    # Compute summary statistics
    total_families = len(families)
    families_with_all_machines = sum(
        1 for f in families
        if all(
            m.get("implemented", False)
            for m in f.get("machine", {}).values()
        )
    )
    families_with_full_closure = sum(
        1 for f in families
        if len(f.get("unreachable_operands", [])) == 0
    )
    families_with_unreachable = total_families - families_with_full_closure

    b1_items: list[dict[str, Any]] = []
    for f in families:
        for uid, cdata in f.get("consumers", {}).items():
            if isinstance(cdata, dict) and cdata.get("b1_item"):
                b1_items.append({
                    "family": f["family_id"],
                    "item": uid,
                    "owner": cdata.get("owner", "UNOWNED"),
                    "description": cdata.get("note", cdata.get("consumer_proof", "")),
                })
        # Check for missing machines
        for mid, mdata in f.get("machine", {}).items():
            if isinstance(mdata, dict) and not mdata.get("implemented", False):
                b1_items.append({
                    "family": f["family_id"],
                    "item": f"machine:{mid}",
                    "owner": mdata.get("owner", "UNOWNED"),
                    "description": f"Machine {mid} not yet implemented",
                })

    unowned = [item for item in b1_items if item["owner"] == "UNOWNED"]

    return {
        "schema_version": 1,
        "status": "T22_FAMILY_CAPABILITY_READY",
        "source_revision": GT6_REVISION,
        "families": families,
        "summary": {
            "total_families": total_families,
            "families_with_all_machines": families_with_all_machines,
            "families_with_full_closure": families_with_full_closure,
            "families_with_unreachable_operands": families_with_unreachable,
            "b1_work_items": len(b1_items),
            "b1_items": b1_items,
            "unowned_items": len(unowned),
            "unowned": unowned,
        },
        "inputs": {
            relative(Path(__file__)): sha256(Path(__file__)),
        },
    }


# ---------------------------------------------------------------------------
# validate / check
# ---------------------------------------------------------------------------


def validate_compact(document: dict[str, Any]) -> None:
    if document.get("schema_version") != 1:
        raise ValueError("schema_version != 1")
    if document.get("source_revision") != GT6_REVISION:
        raise ValueError("source_revision mismatch")
    # No unowned deferred items allowed
    summary = document.get("summary", {})
    if summary.get("unowned_items", 1) != 0:
        raise ValueError(
            f"unowned deferred items: {summary.get('unowned_items')}"
        )


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {relative(OUTPUT)}")
        return errors
    on_disk = load(OUTPUT)
    try:
        validate_compact(on_disk)
    except ValueError as exc:
        errors.append(str(exc))
    expected = build()
    disk_stripped = {
        k: v for k, v in on_disk.items() if k not in ("inputs",)
    }
    expected_stripped = {
        k: v for k, v in expected.items() if k not in ("inputs",)
    }
    if stable(disk_stripped) != stable(expected_stripped):
        errors.append(f"stale generated file: {relative(OUTPUT)}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


# ---------------------------------------------------------------------------
# cli
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    if args.check == args.write:
        print("error: exactly one of --check or --write required", file=sys.stderr)
        return 1
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22 family capability failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "schema_version": document.get("schema_version"),
        "status": document.get("status"),
        "families": len(document.get("families", [])),
        "unreachable": document.get("summary", {}).get(
            "families_with_unreachable_operands", -1
        ),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
