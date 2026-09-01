#!/usr/bin/env python3
"""Build T10 generic-cell fluid registration and closure ledgers."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MATERIALS = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
T5_GATE = (
    ROOT
    / "src/t5_chemical_generated/resources/data/cruciblecraft"
    / "t5_chemical_fluid_gate.json"
)
T10_FLUID_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "container_fluid_gate.json"
)
CELL_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "cell_content_gate.json"
)
READINESS = ROOT / "tools/t10_container_readiness.json"

REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
CONTAINER_TAG = "ITEMGENERATOR.CONTAINERS"
FLUID_TAG = "ITEMGENERATOR.CONTAINERS_FLUID"
GAS_TAG = "ITEMGENERATOR.CONTAINERS_GAS"
RESERVED = {
    "creosote": ("cruciblecraft:creosote", "fluid"),
    "steam": ("cruciblecraft:steam", "gas"),
}
EXPECTED_DENYLIST = {
    "aerotheum",
    "anti_gravitonium",
    "anti_mac_guffium",
    "cryotheum",
    "gravitonium",
    "honey",
    "honeydew",
    "ice",
    "mac_guffium",
    "methane_ice",
    "milk",
    "nitro_carbon",
    "petrotheum",
    "pyrotheum",
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def digest(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def material_documents() -> dict[str, dict[str, Any]]:
    documents = {}
    for filename in load(MATERIALS / "index.json"):
        document = load(MATERIALS / filename)
        documents[document["id"]] = document
    return documents


def domains(
    documents: dict[str, dict[str, Any]],
) -> tuple[set[str], set[str], set[str]]:
    by_tag: dict[str, set[str]] = {
        CONTAINER_TAG: set(),
        FLUID_TAG: set(),
        GAS_TAG: set(),
    }
    for material_id, document in documents.items():
        tags = set(
            (document.get("gt6_metadata") or {})
            .get("generation_tags") or []
        )
        for tag in by_tag:
            if tag in tags:
                by_tag[tag].add(material_id)
    fluid = by_tag[FLUID_TAG]
    gas = by_tag[GAS_TAG]
    if fluid & gas:
        raise ValueError(
            "T10 fluid/gas container domains overlap: "
            + ", ".join(sorted(fluid & gas))
        )
    denied = by_tag[CONTAINER_TAG] - fluid - gas
    if (
        len(fluid) != 61
        or len(gas) != 48
        or denied != EXPECTED_DENYLIST
        or len(fluid | gas | denied) != 123
    ):
        raise ValueError(
            "T10 container domain denominator drifted: "
            f"fluid={len(fluid)}, gas={len(gas)}, "
            f"denied={sorted(denied)}, "
            f"union={len(fluid | gas | denied)}"
        )
    return fluid, gas, denied


def fluid_row(
    material_id: str,
    kind: str,
    document: dict[str, Any],
) -> dict[str, Any]:
    density_value = max(
        1,
        round(abs(float(document["thermal"]["density"])) * 1_000),
    )
    state = "gas" if kind == "gas" else "liquid"
    return {
        "color": document["color"].upper(),
        "density": -density_value if state == "gas" else density_value,
        "id": material_id,
        "material": material_id,
        "source": {
            "path": (
                "src/main/resources/data/cruciblecraft/materials/"
                f"{material_id}.json"
            ),
            "reason": (
                "T10 container generation-tag domain requires a stable "
                "non-placeable runtime fluid identity"
            ),
            "repository": "GregTech6/gregtech6",
            "revision": REVISION,
        },
        "state": state,
        "temperature_kelvin": 300,
        "viscosity": 200 if state == "gas" else 1_000,
        "world_placeable": False,
    }


def build() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    documents = material_documents()
    fluid, gas, denied = domains(documents)
    t5_rows = load(T5_GATE)["fluids"]
    t5_by_material = {row["material"]: row for row in t5_rows}
    if len(t5_by_material) != len(t5_rows):
        raise ValueError("T5 chemical gate has duplicate material identities")

    domain_kinds = {
        **{material: "fluid" for material in fluid},
        **{material: "gas" for material in gas},
    }
    new_materials = sorted(
        set(domain_kinds) - set(t5_by_material) - set(RESERVED)
    )
    if len(new_materials) != 93:
        raise ValueError(
            f"T10 chemical-fluid addition count drifted: {len(new_materials)}"
        )
    t10_rows = [
        fluid_row(material, domain_kinds[material], documents[material])
        for material in new_materials
    ]
    occupied_ids = {row["id"] for row in t5_rows}
    collisions = occupied_ids & {row["id"] for row in t10_rows}
    if collisions:
        raise ValueError(
            "T10 chemical fluid ids collide with T5: "
            + ", ".join(sorted(collisions))
        )
    fluid_gate = {
        "schema_version": 1,
        "fluids": t10_rows,
    }

    runtime_id_by_material = {
        material: f"cruciblecraft:{row['id']}"
        for material, row in t5_by_material.items()
    }
    runtime_id_by_material.update({
        row["material"]: f"cruciblecraft:{row['id']}"
        for row in t10_rows
    })
    runtime_id_by_material.update({
        material: values[0]
        for material, values in RESERVED.items()
    })
    missing_runtime = sorted(
        set(domain_kinds) - set(runtime_id_by_material)
    )
    if missing_runtime:
        raise ValueError(
            "T10 cell domain lacks runtime fluids: "
            + ", ".join(missing_runtime)
        )
    cell_rows = [
        {
            "id": runtime_id_by_material[material],
            "kind": domain_kinds[material],
            "material": material,
        }
        for material in sorted(domain_kinds)
    ]
    if len(cell_rows) != 109:
        raise ValueError("T10 cell gate must contain 109 identities")
    cell_gate = {
        "schema_version": 1,
        "fluids": cell_rows,
    }
    fluid_content = stable_json(fluid_gate)
    cell_content = stable_json(cell_gate)
    readiness = {
        "schema_version": 1,
        "status": "READY",
        "adr": {
            "path": "T10d-容器身份与边界ADR.md",
            "cell_items": ["cruciblecraft:fluid_cell", "cruciblecraft:gas_cell"],
            "capacity_mb": 1_000,
            "empty_stack_limit": 64,
            "filled_stack_limit": 1,
            "component_ingredient_index": False,
        },
        "counts": {
            "container_memberships": 204,
            "union_materials": 123,
            "fluid_domain": len(fluid),
            "gas_domain": len(gas),
            "containers_only_denied": len(denied),
            "existing_t5_domain_fluids": len(
                set(domain_kinds) & set(t5_by_material)
            ),
            "reserved_domain_fluids": len(
                set(domain_kinds) & set(RESERVED)
            ),
            "new_t10_chemical_fluids": len(t10_rows),
            "cell_gate_entries": len(cell_rows),
        },
        "domains": {
            "fluid": sorted(fluid),
            "gas": sorted(gas),
            "containers_only_denied": sorted(denied),
        },
        "reserved_runtime_fluids": {
            material: values[0]
            for material, values in sorted(RESERVED.items())
        },
        "outputs": {
            "chemical_fluid_gate": {
                "path": T10_FLUID_GATE.relative_to(ROOT).as_posix(),
                "sha256": digest(fluid_content),
            },
            "cell_content_gate": {
                "path": CELL_GATE.relative_to(ROOT).as_posix(),
                "sha256": digest(cell_content),
            },
        },
    }
    return fluid_gate, cell_gate, readiness


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write == args.check:
        parser.error("exactly one of --write or --check is required")
    fluid_gate, cell_gate, readiness = build()
    outputs = {
        T10_FLUID_GATE: stable_json(fluid_gate),
        CELL_GATE: stable_json(cell_gate),
        READINESS: stable_json(readiness),
    }
    stale = [
        path.relative_to(ROOT).as_posix()
        for path, content in outputs.items()
        if not path.is_file()
        or path.read_text(encoding="utf-8") != content
    ]
    if args.write:
        for path, content in outputs.items():
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8", newline="\n")
        print(stable_json({
            "status": "WRITTEN",
            "stale_outputs": stale,
        }), end="")
        return 0
    print(stable_json({
        "status": "CURRENT" if not stale else "STALE",
        "stale_outputs": stale,
    }), end="")
    return 0 if not stale else 1


if __name__ == "__main__":
    raise SystemExit(main())
