#!/usr/bin/env python3
"""Build the compressed T42 family-to-operand snapshot."""
from __future__ import annotations

import argparse
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "tools"))

from tools import t35_common as t35
from tools import t42_common as common

OUTPUT = common.FAMILY_OPERAND_SNAPSHOT


def _catalog() -> dict[str, Any]:
    if common.REMAINING_CATALOG.is_file():
        return common.load_json(common.REMAINING_CATALOG)
    from tools import build_t42_partition_freeze as freeze

    return freeze.build_catalog()


def _side_operands(
    values: list[Any],
    *,
    side: str,
    fluid: bool,
    catalogs: dict[str, Any],
) -> list[dict[str, Any]]:
    operands: list[dict[str, Any]] = []
    for index, raw in enumerate(values or []):
        if fluid:
            if not isinstance(raw, dict) or not raw.get("fluid"):
                continue
            operands.append(
                {
                    "action": "consume" if side.endswith("inputs") else "produce",
                    "alias": None,
                    "amount": int(raw.get("amount") or 0),
                    "count": int(raw.get("amount") or 0),
                    "fluid": str(raw.get("fluid") or ""),
                    "form": None,
                    "item": None,
                    "material": None,
                    "meta": None,
                    "operand_index": index,
                    "side": side,
                }
            )
            continue
        if common.empty_item(raw):
            continue
        mapped = common.map_item_source(raw, catalogs)
        operands.append(
            {
                "action": common.operand_action(raw),
                "alias": mapped.get("alias"),
                "amount": int(raw.get("count") or 0),
                "count": int(raw.get("count") or 0),
                "fluid": None,
                "form": mapped.get("form"),
                "item": str(raw.get("item") or ""),
                "material": mapped.get("material"),
                "meta": raw.get("meta"),
                "operand_index": index,
                "side": side,
            }
        )
    return operands


def _compact_operand(
    operand: dict[str, Any],
    tables: dict[str, Any],
) -> list[Any]:
    return [
        common.intern(tables["sides"], tables["side_index"], operand["side"]),
        common.intern(tables["items"], tables["item_index"], operand.get("item")),
        common.intern(tables["fluids"], tables["fluid_index"], operand.get("fluid")),
        operand.get("meta"),
        operand.get("count"),
        common.intern(tables["actions"], tables["action_index"], operand.get("action")),
        common.intern(tables["materials"], tables["material_index"], operand.get("material")),
        common.intern(tables["forms"], tables["form_index"], operand.get("form")),
        common.intern(tables["aliases"], tables["alias_index"], operand.get("alias")),
    ]


def build_from_dump() -> dict[str, Any]:
    common.require_dump()
    catalog = _catalog()
    remaining = {
        row["family_id"]: row
        for row in catalog["families"]
    }
    by_template = {
        row["template_key"]: row["family_id"]
        for row in catalog["families"]
    }
    oo_rows = common.ordinary_optional_row_keys()
    maps = common.template_maps()
    catalogs = common.load_runtime_catalogs()
    tables = {
        "actions": [],
        "action_index": {},
        "aliases": [],
        "alias_index": {},
        "fluids": [],
        "fluid_index": {},
        "forms": [],
        "form_index": {},
        "items": [],
        "item_index": {},
        "materials": [],
        "material_index": {},
        "sides": [],
        "side_index": {},
    }
    family_relations: dict[str, list[dict[str, Any]]] = defaultdict(list)
    dump_hashes: dict[str, str] = {}

    for map_name in common.SHAPE_MAPS:
        path = common.DUMP_MAPS / f"{map_name}.json"
        dump_hashes[common.relative(path)] = common.sha256_file(path)
        recipes = common.load_map_recipes(map_name)
        membership = common.recipe_template_ids(map_name, recipes)
        map_index = maps.index(map_name)
        for recipe_index, template_id in membership.items():
            if (map_index, recipe_index) not in oo_rows:
                continue
            family_id = by_template.get(template_id)
            if family_id is None:
                continue
            recipe = recipes[recipe_index]
            operands = [
                *_side_operands(
                    recipe.get("inputs") or [],
                    side="item_inputs",
                    fluid=False,
                    catalogs=catalogs,
                ),
                *_side_operands(
                    recipe.get("outputs") or [],
                    side="item_outputs",
                    fluid=False,
                    catalogs=catalogs,
                ),
                *_side_operands(
                    recipe.get("fluidInputs") or [],
                    side="fluid_inputs",
                    fluid=True,
                    catalogs=catalogs,
                ),
                *_side_operands(
                    recipe.get("fluidOutputs") or [],
                    side="fluid_outputs",
                    fluid=True,
                    catalogs=catalogs,
                ),
            ]
            family_relations[family_id].append(
                {
                    "can_be_buffered": bool(recipe.get("canBeBuffered", True)),
                    "chances": list(recipe.get("chances") or []),
                    "duration": recipe.get("duration"),
                    "eut": recipe.get("euPerTick"),
                    "operands": [
                        _compact_operand(operand, tables) for operand in operands
                    ],
                    "recipe_index": recipe_index,
                    "row_sha256": common.row_sha256(recipe),
                    "source_map": map_name,
                    "special_value": recipe.get("specialValue") or 0,
                }
            )

    mixer_path = common.DUMP_MAPS / f"{common.MIXER_MAP}.json"
    dump_hashes[common.relative(mixer_path)] = common.sha256_file(mixer_path)
    mixer_recipes = common.load_map_recipes(common.MIXER_MAP)
    mixer_doc = common.load_json(common.MIXER_MEMBERSHIP)
    template_ids = list(mixer_doc.get("template_ids") or [])
    membership_list = list(mixer_doc.get("membership") or [])
    mixer_oo = common.mixer_ordinary_optional_recipe_indexes()
    if len(membership_list) != len(mixer_recipes):
        raise ValueError(
            "mixer membership length drifted from dump recipes: "
            f"{len(membership_list)} != {len(mixer_recipes)}"
        )
    for recipe_index, template_index in enumerate(membership_list):
        template_id = str(template_ids[int(template_index)])
        family_id = by_template.get(template_id)
        if family_id is None:
            continue
        if recipe_index not in mixer_oo.get(template_id, frozenset()):
            continue
        recipe = mixer_recipes[recipe_index]
        operands = [
            *_side_operands(
                recipe.get("inputs") or [],
                side="item_inputs",
                fluid=False,
                catalogs=catalogs,
            ),
            *_side_operands(
                recipe.get("outputs") or [],
                side="item_outputs",
                fluid=False,
                catalogs=catalogs,
            ),
            *_side_operands(
                recipe.get("fluidInputs") or [],
                side="fluid_inputs",
                fluid=True,
                catalogs=catalogs,
            ),
            *_side_operands(
                recipe.get("fluidOutputs") or [],
                side="fluid_outputs",
                fluid=True,
                catalogs=catalogs,
            ),
        ]
        family_relations[family_id].append(
            {
                "can_be_buffered": bool(recipe.get("canBeBuffered", True)),
                "chances": list(recipe.get("chances") or []),
                "duration": recipe.get("duration"),
                "eut": recipe.get("euPerTick"),
                "operands": [
                    _compact_operand(operand, tables) for operand in operands
                ],
                "recipe_index": recipe_index,
                "row_sha256": common.row_sha256(recipe),
                "source_map": common.MIXER_MAP,
                "special_value": recipe.get("specialValue") or 0,
            }
        )

    missing = sorted(set(remaining) - set(family_relations))
    extra = sorted(set(family_relations) - set(remaining))
    if missing or extra:
        raise ValueError(
            "snapshot family membership drifted "
            f"missing={len(missing)} extra={len(extra)}"
        )
    families = []
    relation_count = 0
    for family_id, row in remaining.items():
        relations = sorted(
            family_relations[family_id],
            key=lambda item: (item["source_map"], item["recipe_index"]),
        )
        expected = int(row["ordinary_source_rows"])
        if len(relations) != expected:
            raise ValueError(
                f"{family_id} snapshot relations {len(relations)} != {expected}"
            )
        relation_count += len(relations)
        families.append(
            {
                "cc_host_map": row["cc_host_map"],
                "family_id": family_id,
                "membership_kind": row["membership_kind"],
                "relation_count": len(relations),
                "relations": relations,
                "source_map": row["source_map"],
                "source_relation_membership_root": row["source_relation_membership_root"],
                "template_key": row["template_key"],
            }
        )
    families.sort(key=lambda row: row["family_id"])
    document = {
        "dictionaries": {
            "actions": tables["actions"],
            "aliases": tables["aliases"],
            "fluids": tables["fluids"],
            "forms": tables["forms"],
            "items": tables["items"],
            "materials": tables["materials"],
            "sides": tables["sides"],
        },
        "operand_layout": [
            "side",
            "item",
            "fluid",
            "meta",
            "count",
            "action",
            "material",
            "form",
            "alias",
        ],
        "dump_map_sha256": dump_hashes,
        "family_count": len(families),
        "families": families,
        "full_replay": True,
        "generated_by": "python tools/build_t42_family_operand_snapshot.py --full-replay",
        "l1b_operands_sha256": common.sha256_file(common.L1B_OPERANDS),
        "mixer_membership_sha256": common.sha256_file(common.MIXER_MEMBERSHIP),
        "remaining_catalog_family_count": catalog["family_count"],
        "relation_count": relation_count,
        "schema_version": 1,
        "semantic_root_sha256": None,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_FAMILY_OPERAND_SNAPSHOT",
    }
    document["semantic_root_sha256"] = common.sha256_stable(
        {
            "family_ids": [row["family_id"] for row in families],
            "membership_roots": [
                row["source_relation_membership_root"] for row in families
            ],
            "relation_count": relation_count,
            "row_sha256": [
                relation["row_sha256"]
                for family in families
                for relation in family["relations"]
            ],
        }
    )
    return document


def _unpack_action(action: str | None) -> dict[str, Any]:
    text = str(action or "consume")
    if text.startswith("wear:"):
        try:
            damage = int(text.split(":", 1)[1])
        except ValueError:
            damage = 0
        return {"kind": "wear", "damage": damage}
    if text == "wear":
        return {"kind": "wear"}
    return {"kind": text}


def expand_relation(family: dict[str, Any], relation: dict[str, Any], dictionaries: dict[str, Any]) -> dict[str, Any]:
    sides = dictionaries["sides"]
    items = dictionaries["items"]
    fluids = dictionaries["fluids"]
    actions = dictionaries["actions"]
    materials = dictionaries.get("materials") or []
    forms = dictionaries.get("forms") or []
    aliases = dictionaries.get("aliases") or []
    item_inputs: list[dict[str, Any]] = []
    item_outputs: list[dict[str, Any]] = []
    fluid_inputs: list[dict[str, Any]] = []
    fluid_outputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for packed in relation.get("operands") or []:
        side = sides[int(packed[0])]
        item = items[int(packed[1])] if packed[1] is not None else None
        fluid = fluids[int(packed[2])] if packed[2] is not None else None
        meta = packed[3]
        count = packed[4]
        action = actions[int(packed[5])] if packed[5] is not None else "consume"
        material = None
        form = None
        alias = None
        if len(packed) >= 9:
            material = materials[int(packed[6])] if packed[6] is not None else None
            form = forms[int(packed[7])] if packed[7] is not None else None
            alias = aliases[int(packed[8])] if packed[8] is not None else None
        unpacked_action = _unpack_action(action)
        if side == "item_inputs":
            item_inputs.append(
                {
                    "alias": alias,
                    "count": count,
                    "form": form,
                    "item": item,
                    "material": material,
                    "meta": meta,
                    "runtime_id": None,
                }
            )
            item_input_counts.append(int(count or 0))
            item_input_actions.append(unpacked_action)
        elif side == "item_outputs":
            item_outputs.append(
                {
                    "alias": alias,
                    "count": count,
                    "form": form,
                    "id": item,
                    "item": item,
                    "material": material,
                    "meta": meta,
                    "runtime_id": None,
                }
            )
        elif side == "fluid_inputs":
            fluid_inputs.append({"fluid": fluid, "amount": count, "runtime_id": None})
        else:
            fluid_outputs.append({"fluid": fluid, "amount": count, "runtime_id": None})
    return {
        "can_be_buffered": relation.get("can_be_buffered"),
        "chances": relation.get("chances") or [],
        "duration": relation.get("duration"),
        "eut": relation.get("eut"),
        "family_id": family["family_id"],
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "item_input_actions": item_input_actions,
        "item_input_counts": item_input_counts,
        "item_inputs": item_inputs,
        "item_outputs": item_outputs,
        "recipe_index": relation.get("recipe_index"),
        "row_sha256": relation.get("row_sha256"),
        "source_map": relation.get("source_map") or family.get("source_map"),
        "special_value": relation.get("special_value") or 0,
        "template_key": family["template_key"],
    }


def iter_snapshot_relations(document: dict[str, Any] | None = None):
    document = document if document is not None else common.load_json(OUTPUT)
    dictionaries = document.get("dictionaries") or {}
    for family in document.get("families") or []:
        for relation in family.get("relations") or []:
            yield family, expand_relation(family, relation, dictionaries)


def reference_only_document() -> dict[str, Any]:
    if not OUTPUT.is_file():
        raise FileNotFoundError(
            "committed T42 family operand snapshot is required for --check"
        )
    return common.load_json(OUTPUT)


def check_committed(document: dict[str, Any] | None = None) -> list[str]:
    snapshot = document if document is not None else reference_only_document()
    catalog = _catalog()
    errors: list[str] = []
    if snapshot.get("family_count") != catalog["family_count"]:
        errors.append("snapshot family_count drifted from remaining catalog")
    catalog_ids = [row["family_id"] for row in catalog["families"]]
    snapshot_ids = [row["family_id"] for row in snapshot.get("families") or []]
    if catalog_ids != snapshot_ids:
        errors.append("snapshot family ids drifted from remaining catalog")
    if int(snapshot.get("relation_count") or 0) != int(catalog.get("ordinary_source_rows") or 0):
        errors.append("snapshot relation_count drifted from remaining ordinary_source_rows")
    if snapshot.get("l1b_operands_sha256") != common.sha256_file(common.L1B_OPERANDS):
        errors.append("snapshot l1b operand digest drifted")
    catalog_roots = {
        row["family_id"]: row["source_relation_membership_root"]
        for row in catalog["families"]
    }
    for family in snapshot.get("families") or []:
        expected = catalog_roots.get(family["family_id"])
        if family.get("source_relation_membership_root") != expected:
            errors.append(
                f"membership root drifted for {family.get('family_id')}"
            )
            break
        if int(family.get("relation_count") or 0) != len(family.get("relations") or []):
            errors.append(f"relation_count mismatch for {family.get('family_id')}")
            break
    receipts = (
        "tools/t37_assembler_source_receipt.json",
        "tools/t38_roaster_source_receipt.json",
        "tools/t39_centrifuge_source_receipt.json",
        "tools/t40_electrolyzer_source_receipt.json",
        "tools/t41_assembler_source_receipt.json",
    )
    for relative_path in receipts:
        if not (common.ROOT / relative_path).is_file():
            errors.append(f"missing compact receipt: {relative_path}")
    return errors


def build(*, full_replay: bool) -> dict[str, Any]:
    if full_replay:
        return build_from_dump()
    snapshot = reference_only_document()
    errors = check_committed(snapshot)
    if errors:
        raise ValueError("; ".join(errors))
    return snapshot


def write(*, full_replay: bool) -> None:
    if not full_replay:
        raise ValueError("T42 snapshot --write requires --full-replay against the GT6 dump")
    t35.write_stable(OUTPUT, build_from_dump())


def check(*, full_replay: bool) -> list[str]:
    if full_replay:
        rebuilt = build_from_dump()
        return common.check_document(OUTPUT, rebuilt)
    return check_committed()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build T42 family operand snapshot")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--rebind-currentness-only", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    if args.rebind_currentness_only:
        from tools import currentness

        currentness.rebind_sidecar(OUTPUT)
        print(f"rebound currentness sidecar for {common.relative(OUTPUT)}")
        return 0
    try:
        if args.write:
            write(full_replay=args.full_replay)
            print("Wrote T42 family operand snapshot.")
            return 0
        errors = check(full_replay=args.full_replay)
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 family operand snapshot is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42 family operand snapshot failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
