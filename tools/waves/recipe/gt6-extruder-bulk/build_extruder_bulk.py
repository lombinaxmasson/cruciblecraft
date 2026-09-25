#!/usr/bin/env python3
"""Select gt.recipe.extruder rows that translate, and group them into matrix holders."""
from __future__ import annotations

import hashlib
import json
import shutil
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(TOOLS) not in sys.path:
    sys.path.insert(1, str(TOOLS))

from tools import census_common as census
from tools.build_assembler_source import _source_row_hash
from tools.recipe_bulk import source_import
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.handlers import shape_transform

SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.extruder.json"
WAVE = ROOT / "tools" / "waves" / "recipe" / "gt6-extruder-bulk"
SLICE = WAVE / "source_pack" / "dump_slice.json"
WORK = WAVE / "source_pack" / "work_set.json"
# 4,096 exact-remainder rows exceed the 512 KiB compact wire ceiling (~150 B/row).
HOLDER = 2048


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _shape(relation: dict[str, Any]) -> tuple[Any, ...]:
    return (
        int(relation.get("duration") or 0),
        int(relation.get("eut") or 0),
        int(relation.get("special_value") or 0),
        tuple(int(value) for value in relation.get("item_input_counts") or []),
        tuple(str(action.get("kind") or "") for action in relation.get("item_input_actions") or []),
        tuple(int(value) for value in relation.get("output_chances") or []),
        len(relation.get("item_inputs") or []),
        len(relation.get("item_outputs") or []),
        bool(relation.get("can_be_buffered", True)),
    )


def select() -> None:
    if not SLICE.is_file():
        SLICE.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(DUMP, SLICE)
    document = json.loads(SLICE.read_text(encoding="utf-8"))
    recipes = document["recipes"]
    maps = gt6._maps()
    groups: dict[tuple[Any, ...], list[tuple[int, str, bool]]] = defaultdict(list)
    blocked: list[dict[str, str]] = []
    for index, recipe in enumerate(recipes):
        digest = _source_row_hash(recipe)
        try:
            relation, errors = gt6.compile_row(
                recipe,
                host="cruciblecraft:extruder",
                target_map="cruciblecraft:extruder",
                source_map="gt.recipe.extruder",
                family_id="gt.recipe.extruder#bulk",
                template_key="gt.recipe.extruder#bulk",
                recipe_index=index,
                shadow_order=0,
                source_revision=SOURCE_REVISION,
                source_row_sha256=digest,
                maps=maps,
            )
        except ValueError as failure:
            blocked.append(
                {
                    "source_recipe_index": str(index),
                    "source_row_sha256": digest,
                    "reason": str(failure),
                }
            )
            continue
        if errors or source_import._relation_unmapped(relation):
            blocked.append(
                {
                    "source_recipe_index": str(index),
                    "source_row_sha256": digest,
                    "reason": errors[0] if errors else "unmapped operand",
                }
            )
            continue
        groups[_shape(relation)].append(
            (index, digest, shape_transform.matches(relation, {}))
        )
    families: list[dict[str, Any]] = []
    published = 0
    shaped = 0
    family_index = 0
    for rows in groups.values():
        for start in range(0, len(rows), HOLDER):
            chunk = rows[start : start + HOLDER]
            family_id = f"gt.recipe.extruder#bulk_{family_index:04d}"
            families.append(
                {
                    "family_id": family_id,
                    "template_key": family_id,
                    "relations": [
                        {
                            "shadow_order": order,
                            "source_recipe_index": index,
                            "source_row_sha256": digest,
                        }
                        for order, (index, digest, _matched) in enumerate(chunk)
                    ],
                }
            )
            published += len(chunk)
            shaped += sum(1 for _index, _digest, matched in chunk if matched)
            family_index += 1
    work = {
        "accounting": {
            "blocked_rows": len(blocked),
            "published_rows": published,
            "selection_rule": "exact runtime operands only; missing forms and objects stay blocked",
            "shape_transform_rows": shaped,
            "source_rows": len(recipes),
        },
        "families": families,
    }
    WORK.parent.mkdir(parents=True, exist_ok=True)
    WORK.write_text(json.dumps(work, separators=(",", ":")), encoding="utf-8")
    census.write_stable(
        WAVE / "blocked.json",
        {
            "rows": blocked,
            "schema_version": 1,
            "source_rows": len(recipes),
        },
    )
    slice_hash = _sha256(SLICE)
    work_hash = _sha256(WORK)
    census.write_stable(
        WAVE / "source_pack_manifest.json",
        {
            "files": [
                {
                    "path": "tools/waves/recipe/gt6-extruder-bulk/source_pack/dump_slice.json",
                    "role": "dump_slice",
                    "sha256": slice_hash,
                },
                {
                    "path": "tools/waves/recipe/gt6-extruder-bulk/source_pack/work_set.json",
                    "role": "work_set",
                    "sha256": work_hash,
                },
            ],
            "full_replay": {
                "required_for_first_generation": True,
                "skip_is_not_pass": True,
            },
            "provenance_policy": {"append_only": True, "forbid_gt6u": True},
            "schema_version": 1,
            "source_dialect": "gt6",
            "source_pack_id": "recipe/gt6-extruder-bulk",
            "source_revision": SOURCE_REVISION,
            "source_system": "gt6",
        },
    )
    census.write_stable(
        WAVE / "recipe_import.json",
        {
            "family_membership_source": {
                "kind": "work_set",
                "path": "tools/waves/recipe/gt6-extruder-bulk/source_pack/work_set.json",
            },
            "host": "cruciblecraft:extruder",
            "import_slug": "recipe/gt6-extruder-bulk",
            "operand_authorities": [
                {"id": "identity_ledger_v3", "kind": "data"},
                {"id": "material_form_authority", "kind": "data"},
                {"adapter": "gt6", "kind": "source_dialect"},
            ],
            "output_paths": {
                "lock_candidate": "tools/waves/recipe/gt6-extruder-bulk/lock_candidate.json",
                "receipt": "tools/waves/recipe/gt6-extruder-bulk/source_receipt.json",
                "review": "tools/waves/recipe/gt6-extruder-bulk/source_review.json",
                "source": "tools/waves/recipe/gt6-extruder-bulk/source.json",
            },
            "representation_policy": {"allowed": ["exact", "exact_multi"]},
            "schema_version": 1,
            "selection_rule": {"kind": "work_set_members"},
            "source_maps": ["gt.recipe.extruder"],
            "source_pack": "tools/waves/recipe/gt6-extruder-bulk/source_pack_manifest.json",
            "stable_id_policy": {
                "algorithm": "source_system_revision_family_relation",
                "include_card_number": False,
            },
            "target_map": "cruciblecraft:extruder",
        },
    )
    print(
        f"published {published} blocked {len(blocked)} "
        f"shape_transform {shaped} families {len(families)}"
    )


if __name__ == "__main__":
    select()
