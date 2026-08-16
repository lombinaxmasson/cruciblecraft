"""Extract combinatorial GT6 recipe templates from dump maps.

A template is a recipe skeleton plus one or more independent material/fluid
axes whose cartesian product (with optional functional dependencies) expands
to the observed dump rows. Accounting can then count templates instead of
fully expanded rows.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
OUT_REPORT = TOOLS / "gt6_recipe_templates_report.json"
OUT_MATERIAL_HEAT = TOOLS / "gt6_recipe_template_material_heat.json"
OUT_INDEX = TOOLS / "gt6_recipe_templates_index.json"

DEFAULT_MAPS = (
    "gt.recipe.mixer",
    "gt.recipe.bath",
    "gt.recipe.shredder",
)


@dataclass
class TemplateAxis:
    path: str
    values: list[Any]
    value_count: int


@dataclass
class RecipeTemplate:
    template_id: str
    map: str
    expanded_count: int
    axis_count: int
    axes: list[TemplateAxis]
    skeleton: dict[str, Any]
    sample_recipe_index: int
    cartesian_ratio: float
    notes: list[str] = field(default_factory=list)


def meta_prefix(item_id: str) -> str | None:
    marker = "gregtech:gt.meta."
    if item_id.startswith(marker):
        return item_id[len(marker) :]
    return None


def item_skeleton_token(item: dict[str, Any]) -> tuple[Any, ...]:
    item_id = str(item.get("item") or "")
    count = item.get("count")
    prefix = meta_prefix(item_id)
    if prefix is not None:
        # Material-bearing meta items are template axes; molds/tools stay fixed.
        return ("MAT", item_id, count)
    return ("FIX", item_id, item.get("meta"), count)


def fluid_skeleton_token(fluid: dict[str, Any]) -> tuple[Any, ...]:
    # Names and amounts often participate in axes; keep only occupancy.
    return ("FLUID",)


def recipe_soft_key(recipe: dict[str, Any]) -> tuple[Any, ...]:
    return (
        recipe.get("euPerTick"),
        recipe.get("specialValue"),
        bool(recipe.get("enabled", True)),
        bool(recipe.get("hidden", False)),
        bool(recipe.get("fake", False)),
        tuple(
            item_skeleton_token(item)
            for item in (recipe.get("inputs") or [])
            if item
        ),
        tuple(
            item_skeleton_token(item)
            for item in (recipe.get("outputs") or [])
            if item
        ),
        tuple(
            fluid_skeleton_token(fluid)
            for fluid in (recipe.get("fluidInputs") or [])
            if fluid
        ),
        tuple(
            fluid_skeleton_token(fluid)
            for fluid in (recipe.get("fluidOutputs") or [])
            if fluid
        ),
        tuple(recipe.get("chances") or []),
        tuple(recipe.get("maxChances") or []),
    )


def recipe_axis_values(recipe: dict[str, Any]) -> dict[str, Any]:
    values: dict[str, Any] = {}
    for side in ("inputs", "outputs"):
        for index, item in enumerate(recipe.get(side) or []):
            if not item:
                continue
            item_id = str(item.get("item") or "")
            if meta_prefix(item_id) is not None:
                values[f"{side}[{index}].meta"] = item.get("meta")
                values[f"{side}[{index}].item"] = item_id
    for side in ("fluidInputs", "fluidOutputs"):
        for index, fluid in enumerate(recipe.get(side) or []):
            if not fluid:
                continue
            values[f"{side}[{index}].fluid"] = fluid.get("fluid")
            values[f"{side}[{index}].amount"] = fluid.get("amount")
    values["duration"] = recipe.get("duration")
    return values


def _stable_json(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def _functional_parent(
    rows: list[dict[str, Any]],
    child: str,
    candidates: list[str],
) -> str | None:
    """Return a candidate that uniquely determines child, if any."""
    for parent in candidates:
        if parent == child:
            continue
        mapping: dict[Any, set[Any]] = defaultdict(set)
        determined = True
        for row in rows:
            mapping[row[parent]].add(row[child])
            if len(mapping[row[parent]]) > 1:
                determined = False
                break
        if determined and all(len(values) == 1 for values in mapping.values()):
            return parent
    return None


def factor_axes(
    rows: list[dict[str, Any]],
) -> tuple[list[TemplateAxis], float, list[str]]:
    if not rows:
        return [], 1.0, []
    keys = sorted(rows[0])
    varying = [key for key in keys if len({row[key] for row in rows}) > 1]
    notes: list[str] = []
    free: list[str] = []
    for key in varying:
        parent = _functional_parent(rows, key, varying)
        if parent is None:
            free.append(key)
        else:
            notes.append(f"{key} determined by {parent}")

    if not free:
        return [], 1.0, notes

    domains = {
        key: sorted({row[key] for row in rows}, key=lambda value: _stable_json(value))
        for key in free
    }
    expected = 1
    for key in free:
        expected *= max(1, len(domains[key]))
    observed = len({_stable_json(tuple(row[key] for key in free)) for row in rows})
    ratio = (observed / expected) if expected else 1.0
    if expected > observed and expected > 0:
        notes.append(
            f"cartesian deficit: observed={observed} expected_product={expected}"
        )
    axes = [
        TemplateAxis(path=key, values=domains[key], value_count=len(domains[key]))
        for key in free
    ]
    return axes, ratio, notes


def skeleton_from_recipe(
    recipe: dict[str, Any], constants: dict[str, Any]
) -> dict[str, Any]:
    def render_item(side: str, index: int, item: dict[str, Any]) -> dict[str, Any]:
        item_id = str(item.get("item") or "")
        token: dict[str, Any] = {
            "item": item_id,
            "count": item.get("count"),
        }
        meta_path = f"{side}[{index}].meta"
        item_path = f"{side}[{index}].item"
        if meta_prefix(item_id) is not None:
            token["meta"] = (
                constants[meta_path] if meta_path in constants else f"${meta_path}"
            )
            if item_path in constants:
                token["item"] = constants[item_path]
            elif item_path not in constants:
                token["item"] = f"${item_path}"
        else:
            token["meta"] = item.get("meta")
        return token

    def render_fluid(side: str, index: int, fluid: dict[str, Any]) -> dict[str, Any]:
        fluid_path = f"{side}[{index}].fluid"
        amount_path = f"{side}[{index}].amount"
        return {
            "fluid": (
                constants[fluid_path] if fluid_path in constants else f"${fluid_path}"
            ),
            "amount": (
                constants[amount_path]
                if amount_path in constants
                else f"${amount_path}"
            ),
        }

    return {
        "euPerTick": recipe.get("euPerTick"),
        "specialValue": recipe.get("specialValue"),
        "duration": (
            constants["duration"] if "duration" in constants else "$duration"
        ),
        "inputs": [
            render_item("inputs", index, item)
            for index, item in enumerate(recipe.get("inputs") or [])
            if item
        ],
        "outputs": [
            render_item("outputs", index, item)
            for index, item in enumerate(recipe.get("outputs") or [])
            if item
        ],
        "fluidInputs": [
            render_fluid("fluidInputs", index, fluid)
            for index, fluid in enumerate(recipe.get("fluidInputs") or [])
            if fluid
        ],
        "fluidOutputs": [
            render_fluid("fluidOutputs", index, fluid)
            for index, fluid in enumerate(recipe.get("fluidOutputs") or [])
            if fluid
        ],
    }


def extract_map_templates(
    map_name: str,
    recipes: list[dict[str, Any]],
) -> list[RecipeTemplate]:
    groups: dict[tuple[Any, ...], list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    for index, recipe in enumerate(recipes):
        if recipe.get("enabled") is False:
            continue
        groups[recipe_soft_key(recipe)].append((index, recipe))

    templates: list[RecipeTemplate] = []
    sorted_groups = sorted(
        groups.items(),
        key=lambda item: (-len(item[1]), _stable_json(item[0])),
    )
    for group_index, (_key, members) in enumerate(sorted_groups):
        value_rows = [recipe_axis_values(recipe) for _, recipe in members]
        axes, ratio, notes = factor_axes(value_rows)
        free_paths = {axis.path for axis in axes}
        constants = {}
        for path in value_rows[0]:
            values = {row[path] for row in value_rows}
            if len(values) == 1 and path not in free_paths:
                constants[path] = next(iter(values))
        sample_index, sample = members[0]
        templates.append(
            RecipeTemplate(
                template_id=f"{map_name}#{group_index:04d}",
                map=map_name,
                expanded_count=len(members),
                axis_count=len(axes),
                axes=axes,
                skeleton=skeleton_from_recipe(sample, constants),
                sample_recipe_index=sample_index,
                cartesian_ratio=round(ratio, 6),
                notes=notes,
            )
        )
    templates.sort(key=lambda row: (-row.expanded_count, row.template_id))
    return templates


def material_ids_in_recipe(recipe: dict[str, Any]) -> set[int]:
    ids: set[int] = set()
    for side in ("inputs", "outputs"):
        for item in recipe.get(side) or []:
            if not item:
                continue
            if meta_prefix(str(item.get("item") or "")) is None:
                continue
            meta = item.get("meta")
            if isinstance(meta, int) and meta >= 0:
                ids.add(meta)
    return ids


def build_material_heat_and_index(
    map_name: str,
    recipes: list[dict[str, Any]],
) -> tuple[dict[int, dict[str, int]], list[dict[str, Any]]]:
    """Per-material heat and compact per-template material membership."""
    regroup: dict[tuple[Any, ...], list[dict[str, Any]]] = defaultdict(list)
    for recipe in recipes:
        if recipe.get("enabled") is False:
            continue
        regroup[recipe_soft_key(recipe)].append(recipe)
    sorted_keys = sorted(
        regroup.keys(),
        key=lambda key: (-len(regroup[key]), _stable_json(key)),
    )
    heat: dict[int, dict[str, int]] = defaultdict(
        lambda: {"expanded_appearances": 0, "template_appearances": 0}
    )
    index_rows: list[dict[str, Any]] = []
    for group_index, key in enumerate(sorted_keys):
        members = regroup[key]
        materials_in_template: set[int] = set()
        for recipe in members:
            mats = material_ids_in_recipe(recipe)
            for mat in mats:
                heat[mat]["expanded_appearances"] += 1
            materials_in_template |= mats
        for mat in materials_in_template:
            heat[mat]["template_appearances"] += 1
        index_rows.append(
            {
                "template_id": f"{map_name}#{group_index:04d}",
                "expanded_count": len(members),
                "material_ids": sorted(materials_in_template),
            }
        )
    return heat, index_rows


def build_material_heat(
    map_name: str,
    recipes: list[dict[str, Any]],
) -> dict[int, dict[str, int]]:
    heat, _index = build_material_heat_and_index(map_name, recipes)
    return heat


def summarize_templates(templates: list[RecipeTemplate]) -> dict[str, Any]:
    expanded = sum(row.expanded_count for row in templates)
    return {
        "template_count": len(templates),
        "expanded_recipe_count": expanded,
        "singleton_template_count": sum(
            1 for row in templates if row.expanded_count == 1
        ),
        "combinatorial_template_count": sum(
            1 for row in templates if row.expanded_count > 1
        ),
        "compression_ratio": (
            round(expanded / len(templates), 4) if templates else 1.0
        ),
        "largest_templates": [
            {
                "template_id": row.template_id,
                "expanded_count": row.expanded_count,
                "axis_count": row.axis_count,
                "axes": [
                    {"path": axis.path, "value_count": axis.value_count}
                    for axis in row.axes
                ],
                "cartesian_ratio": row.cartesian_ratio,
                "notes": row.notes,
            }
            for row in templates[:10]
        ],
    }


def load_map(map_name: str) -> list[dict[str, Any]]:
    path = DUMP / "maps" / f"{map_name}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("nameInternal") != map_name:
        raise SystemExit(f"map identity mismatch: {map_name}")
    return list(data.get("recipes") or [])


def process_maps(map_names: list[str]) -> dict[str, Any]:
    if "gt.recipe.extruder" in map_names:
        raise ValueError(
            "gt.recipe.extruder requires the lossless dedicated extractor: "
            "python tools/gt6_extruder_templates.py"
        )
    by_map: dict[str, Any] = {}
    material_heat: dict[int, dict[str, Any]] = {}
    all_templates: list[dict[str, Any]] = []
    compact_index: dict[str, list[dict[str, Any]]] = {}
    for map_name in map_names:
        print(f"Extracting templates from {map_name}...")
        recipes = load_map(map_name)
        templates = extract_map_templates(map_name, recipes)
        summary = summarize_templates(templates)
        print(
            f"  expanded={summary['expanded_recipe_count']} "
            f"templates={summary['template_count']} "
            f"compression={summary['compression_ratio']}"
        )
        by_map[map_name] = {
            **summary,
            "pinned_recipe_count": len(recipes),
        }
        heat, index_rows = build_material_heat_and_index(map_name, recipes)
        compact_index[map_name] = index_rows
        for mat, row in heat.items():
            acc = material_heat.setdefault(
                mat,
                {
                    "material_id": mat,
                    "expanded_appearances": 0,
                    "template_appearances": 0,
                    "maps": {},
                },
            )
            acc["expanded_appearances"] += row["expanded_appearances"]
            acc["template_appearances"] += row["template_appearances"]
            acc["maps"][map_name] = {
                "expanded_appearances": row["expanded_appearances"],
                "template_appearances": row["template_appearances"],
            }
        all_templates.extend(
            {
                "template_id": row.template_id,
                "map": row.map,
                "expanded_count": row.expanded_count,
                "axis_count": row.axis_count,
                "axes": [asdict(axis) for axis in row.axes],
                "skeleton": row.skeleton,
                "sample_recipe_index": row.sample_recipe_index,
                "cartesian_ratio": row.cartesian_ratio,
                "notes": row.notes,
            }
            for row in templates
            if row.expanded_count >= 8 or row.axis_count >= 2
        )
    total_expanded = sum(row["expanded_recipe_count"] for row in by_map.values())
    total_templates = sum(row["template_count"] for row in by_map.values())
    return {
        "schema_version": 1,
        "maps": by_map,
        "totals": {
            "maps": len(by_map),
            "expanded_recipe_count": total_expanded,
            "template_count": total_templates,
            "compression_ratio": (
                round(total_expanded / total_templates, 4) if total_templates else 1.0
            ),
        },
        "large_or_multi_axis_templates": all_templates,
        "accounting_note": (
            "Coverage and material heat should prefer template_count / "
            "template_appearances. expanded_* remain dump-literal diagnostics."
        ),
        "material_heat_path": str(OUT_MATERIAL_HEAT.relative_to(ROOT).as_posix()),
        "templates_index_path": str(OUT_INDEX.relative_to(ROOT).as_posix()),
        "material_heat_preview": sorted(
            (
                {
                    "material_id": mat,
                    "expanded_appearances": row["expanded_appearances"],
                    "template_appearances": row["template_appearances"],
                    "compression": round(
                        row["expanded_appearances"]
                        / max(1, row["template_appearances"]),
                        2,
                    ),
                }
                for mat, row in material_heat.items()
            ),
            key=lambda row: (-row["expanded_appearances"], row["material_id"]),
        )[:30],
        "_material_heat": material_heat,
        "_templates_index": compact_index,
    }


def attach_material_names(report: dict[str, Any]) -> None:
    materials = json.loads(
        (DUMP / "oredict" / "materials.json").read_text(encoding="utf-8")
    )
    names = {
        row["id"]: row["nameInternal"]
        for row in materials
        if isinstance(row.get("id"), int)
    }
    heat = report.pop("_material_heat")
    compact_index = report.pop("_templates_index")
    named = []
    for mat, row in heat.items():
        named.append(
            {
                **row,
                "nameInternal": names.get(mat),
                "compression": round(
                    row["expanded_appearances"]
                    / max(1, row["template_appearances"]),
                    2,
                ),
            }
        )
    named.sort(
        key=lambda row: (
            -row["expanded_appearances"],
            -row["template_appearances"],
            row["material_id"],
        )
    )
    OUT_MATERIAL_HEAT.write_text(
        json.dumps({"schema_version": 1, "records": named}, indent=2, ensure_ascii=False)
        + "\n",
        encoding="utf-8",
        newline="\n",
    )
    OUT_INDEX.write_text(
        json.dumps(
            {
                "schema_version": 1,
                "accounting_unit": "template",
                "maps": compact_index,
            },
            indent=2,
            ensure_ascii=False,
        )
        + "\n",
        encoding="utf-8",
        newline="\n",
    )
    for preview in report["material_heat_preview"]:
        preview["nameInternal"] = names.get(preview["material_id"])


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--maps",
        nargs="+",
        default=list(DEFAULT_MAPS),
        help="GT recipe maps to extract (default: mixer extruder bath shredder)",
    )
    args = parser.parse_args(argv)
    report = process_maps(args.maps)
    attach_material_names(report)
    OUT_REPORT.write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(f"Wrote {OUT_REPORT}")
    print(f"Wrote {OUT_MATERIAL_HEAT}")
    print(f"Wrote {OUT_INDEX}")
    print("totals", report["totals"])
    print("heat preview (expanded -> template):")
    for row in report["material_heat_preview"][:15]:
        print(
            f"  {row.get('nameInternal') or row['material_id']}: "
            f"{row['expanded_appearances']} -> {row['template_appearances']} "
            f"(x{row['compression']})"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
