#!/usr/bin/env python3
"""Project the committed T8 readiness ledger into live material definitions."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MATERIALS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials"
)
READINESS = ROOT / "tools" / "pipe_readiness.json"

FLUID_FORMS = {
    "tiny": ("pipeTiny", "cruciblecraft:generates_tiny_fluid_pipe"),
    "small": ("pipeSmall", "cruciblecraft:generates_small_fluid_pipe"),
    "normal": ("pipeMedium", "cruciblecraft:generates_fluid_pipe"),
    "large": ("pipeLarge", "cruciblecraft:generates_large_fluid_pipe"),
    "huge": ("pipeHuge", "cruciblecraft:generates_huge_fluid_pipe"),
}
ITEM_FORMS = {
    "normal": ("pipeMedium", "cruciblecraft:generates_item_pipe"),
    "large": ("pipeLarge", "cruciblecraft:generates_large_item_pipe"),
    "huge": ("pipeHuge", "cruciblecraft:generates_huge_item_pipe"),
}
COMBO_FLUID_FLAGS = {
    "cruciblecraft:generates_quadruple_fluid_pipe",
    "cruciblecraft:generates_nonuple_fluid_pipe",
}
RESTRICTIVE_ITEM_FLAGS = {
    "cruciblecraft:generates_restrictive_item_pipe",
    "cruciblecraft:generates_large_restrictive_item_pipe",
    "cruciblecraft:generates_huge_restrictive_item_pipe",
}
ALL_PIPE_FLAGS = {
    flag for _, flag in (*FLUID_FORMS.values(), *ITEM_FORMS.values())
} | COMBO_FLUID_FLAGS | RESTRICTIVE_ITEM_FLAGS


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def expected_projection() -> dict[str, dict[str, Any]]:
    readiness = load(READINESS)
    if readiness.get("status") != "READY":
        raise ValueError("T8 readiness ledger is not READY")
    projection: dict[str, dict[str, Any]] = {}
    for row in readiness["fluid_domain"]["material_catalog"]:
        fluid = {}
        flags = set()
        for gauge, values in row["specifications_by_gauge"].items():
            specification, flag = FLUID_FORMS[gauge]
            flags.add(flag)
            fluid[specification] = {
                "capacity_mb": int(values["capacity_mb"]),
                "max_temperature_kelvin": int(
                    values["max_temperature_kelvin"]
                ),
                "gas_proof": bool(values["gas_proof"]),
                "acid_proof": bool(values["acid_proof"]),
                "plasma_proof": bool(values["plasma_proof"]),
                "magic_proof": bool(values["magic_proof"]),
                "contact_damage": bool(values["contact_damage"]),
                "flammable": bool(values["flammable"]),
                "recipe": bool(values["recipe"]),
                "blocking": bool(values["blocking"]),
            }
        projection.setdefault(
            row["material"],
            {
                "fluid_by_specification": {},
                "item_by_specification": {},
                "generation_flags": set(),
            },
        )
        projection[row["material"]]["fluid_by_specification"] = fluid
        projection[row["material"]]["generation_flags"].update(flags)
    for row in readiness["item_domain"]["material_catalog"]:
        item = {}
        flags = set()
        for gauge, values in row["specifications_by_gauge"].items():
            specification, flag = ITEM_FORMS[gauge]
            flags.add(flag)
            item[specification] = {
                "step_size": int(values["step_size"]),
                "stacks_per_second": int(values["stacks_per_second"]),
                "recipe": bool(values["recipe"]),
                "blocking": bool(values["blocking"]),
            }
        projection.setdefault(
            row["material"],
            {
                "fluid_by_specification": {},
                "item_by_specification": {},
                "generation_flags": set(),
            },
        )
        projection[row["material"]]["item_by_specification"] = item
        projection[row["material"]]["generation_flags"].update(flags)
    overlapping = sorted(
        material_id
        for material_id, values in projection.items()
        if values["fluid_by_specification"]
        and values["item_by_specification"]
    )
    if overlapping:
        raise ValueError(
            "GT6 pipe domains overlap for: " + ", ".join(overlapping)
        )
    return projection


def planned_documents() -> dict[Path, str]:
    projection = expected_projection()
    index = load(MATERIALS / "index.json")
    documents = {}
    seen = set()
    for filename in index:
        path = MATERIALS / filename
        document = load(path)
        material_id = str(document["id"])
        values = projection.get(material_id)
        metadata = document.get("gt6_metadata")
        if values is not None:
            if metadata is None:
                raise ValueError(
                    f"T8 pipe material lacks GT6 metadata: {material_id}"
                )
            metadata["pipe_properties"] = {
                "fluid_by_specification": values[
                    "fluid_by_specification"
                ],
                "item_by_specification": values[
                    "item_by_specification"
                ],
            }
            flags = set(values["generation_flags"])
            if values["fluid_by_specification"]:
                flags.update(COMBO_FLUID_FLAGS)
            if values["item_by_specification"]:
                flags.update(RESTRICTIVE_ITEM_FLAGS)
            document["generation_flags"] = sorted(
                (set(document.get("generation_flags") or [])
                 - ALL_PIPE_FLAGS)
                | flags
            )
            seen.add(material_id)
        elif metadata is not None:
            metadata.pop("pipe_properties", None)
            document["generation_flags"] = sorted(
                set(document.get("generation_flags") or [])
                - ALL_PIPE_FLAGS
            )
        documents[path] = stable_json(document)
    missing = sorted(set(projection) - seen)
    if missing:
        raise ValueError(
            "T8 pipe projection references absent materials: "
            + ", ".join(missing)
        )
    return documents


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write == args.check:
        parser.error("exactly one of --write or --check is required")
    documents = planned_documents()
    stale = [
        str(path.relative_to(ROOT)).replace("\\", "/")
        for path, content in documents.items()
        if not path.is_file()
        or path.read_text(encoding="utf-8") != content
    ]
    if args.write:
        for path, content in documents.items():
            path.write_text(content, encoding="utf-8", newline="\n")
        print(stable_json({
            "status": "WRITTEN",
            "material_documents": len(documents),
            "changed_documents": len(stale),
        }), end="")
        return 0
    print(stable_json({
        "status": "CURRENT" if not stale else "STALE",
        "stale_documents": stale,
    }), end="")
    return 0 if not stale else 1


if __name__ == "__main__":
    raise SystemExit(main())
