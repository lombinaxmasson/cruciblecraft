#!/usr/bin/env python3
"""Build the fixed-source ordinary-chemistry row denominator for T21."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

try:
    from tools import build_t5_source_projection as source_projection
except ModuleNotFoundError:
    import build_t5_source_projection as source_projection


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
T5_READINESS = TOOLS / "t5_chemical_readiness.json"
OUTPUT = TOOLS / "t21_source_denominator.json"
REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
PETROLEUM_MATERIALS = {
    "crude_oil",
    "natural_gas",
    "oil_sand",
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any, *, compact: bool = True) -> str:
    if compact:
        return json.dumps(
            value,
            ensure_ascii=False,
            separators=(",", ":"),
            sort_keys=True,
        ) + "\n"
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def value_sha256(value: Any) -> str:
    return hashlib.sha256(
        json.dumps(
            value,
            ensure_ascii=False,
            separators=(",", ":"),
            sort_keys=True,
        ).encode("utf-8")
    ).hexdigest()


def build() -> dict[str, Any]:
    readiness = load(T5_READINESS)
    candidates = readiness["chemical_materials"]
    if (
        len(candidates) != 224
        or readiness["counts"]["classified"] != 224
        or readiness["counts"]["unclassified"] != 0
    ):
        raise ValueError("T21 material candidate denominator drifted")
    candidate_by_id = {row["material"]: row for row in candidates}
    candidate_ids = set(candidate_by_id)
    materials = source_projection.material_documents()
    prefixes = source_projection.prefix_documents()
    source_by_id = source_projection.source_material_index(materials)
    registration = load(source_projection.REGISTRATION_GATE)["materials"]
    cross_reference = load(source_projection.CROSS_REFERENCE)
    gt_prefix_by_item = cross_reference["prefix_item_to_gt_prefix"]
    cc_prefix_by_gt = cross_reference["gt_prefix_to_cc"]
    fixed_item_document = load(source_projection.FIXED_ITEM_PROJECTION)
    fixed_items = {
        (row["source"]["item"], int(row["source"]["meta"])): row["cc_item"]
        for row in fixed_item_document["mappings"]
    }
    normalized_by_fluid = {
        row["fluid"]: row
        for row in load(source_projection.NORMALIZED_FLUIDS)["records"]
    }
    map_ids = list(source_projection.TARGET_MAPS)
    material_dictionary = sorted(candidate_ids)
    material_index = {
        material: index for index, material in enumerate(material_dictionary)
    }
    rows: list[list[Any]] = []
    map_summaries = []
    dispositions: Counter[str] = Counter()
    shapes: Counter[str] = Counter()
    translations: Counter[str] = Counter()
    rejections: Counter[str] = Counter()
    rejection_examples: dict[str, list[str]] = defaultdict(list)

    for map_index, map_id in enumerate(map_ids):
        path = source_projection.MAP_ROOT / f"{map_id}.json"
        document = load(path)
        map_counts: Counter[str] = Counter()
        for recipe_index, recipe in enumerate(document["recipes"]):
            targets = sorted(
                candidate_ids
                & source_projection.recipe_target_materials(
                    recipe,
                    source_by_id=source_by_id,
                    gt_prefix_by_item=gt_prefix_by_item,
                    normalized_by_fluid=normalized_by_fluid,
                )
            )
            if not targets:
                continue
            target_rows = [candidate_by_id[target] for target in targets]
            if set(targets) <= PETROLEUM_MATERIALS:
                disposition = "petroleum_t22"
            elif all(
                "ATOMIC.ACTINIDE" in set(row["material_tags"])
                for row in target_rows
            ):
                disposition = "post_1_0_nuclear"
            else:
                disposition = "ordinary_v1_required"
            if (
                map_id in {
                    "gt.recipe.electrolyzer",
                    "gt.recipe.centrifuge",
                }
                and any(row["source_loader_eligible"] for row in target_rows)
            ):
                shape = "composition_generated"
            else:
                shape = "named_reaction"
            try:
                projected, _ = source_projection.project_recipe(
                    recipe,
                    None,
                    source_by_id=source_by_id,
                    gt_prefix_by_item=gt_prefix_by_item,
                    cc_prefix_by_gt=cc_prefix_by_gt,
                    registration=registration,
                    prefixes=prefixes,
                    fixed_items=fixed_items,
                    normalized_by_fluid=normalized_by_fluid,
                    materials=materials,
                    map_id=map_id,
                )
                translation = "translatable"
                rejection = None
                projected_hash = value_sha256(projected)
            except ValueError as error:
                translation = "blocked_translation"
                rejection = str(error)
                projected_hash = None
                rejections[rejection] += 1
                if len(rejection_examples[rejection]) < 5:
                    rejection_examples[rejection].append(
                        f"{map_id}#recipes[{recipe_index}]"
                    )
            rows.append([
                map_index,
                recipe_index,
                [material_index[target] for target in targets],
                disposition,
                shape,
                translation,
                rejection,
                value_sha256(recipe),
                projected_hash,
            ])
            dispositions[disposition] += 1
            shapes[shape] += 1
            translations[translation] += 1
            map_counts[disposition] += 1
            map_counts[translation] += 1
        map_summaries.append({
            "map": map_id,
            "source_rows": len(document["recipes"]),
            "selected_rows": sum(
                1 for row in rows if row[0] == map_index
            ),
            "counts": dict(sorted(map_counts.items())),
            "source_sha256": sha256(path),
        })

    row_keys = [(row[0], row[1]) for row in rows]
    if len(row_keys) != len(set(row_keys)):
        raise ValueError("T21 source row denominator contains duplicate ids")
    ordinary = dispositions["ordinary_v1_required"]
    ordinary_translatable = sum(
        row[3] == "ordinary_v1_required"
        and row[5] == "translatable"
        for row in rows
    )
    ordinary_blocked = ordinary - ordinary_translatable
    return {
        "schema_version": 1,
        "status": "T21_SOURCE_DENOMINATOR_READY",
        "superseded": {
            "disposition": "ordinary_v1_required",
            "superseded_by": "tools/t21_template_denominator.json",
            "note": (
                "Input-touch diagnostic ledger; NOT a closure numerator. "
                "The authoritative template denominator is ledger-2 "
                "(t21_template_denominator.json, T21 rebaseline). "
                "T22.5 A0 marks this disposition superseded; all counts "
                "are historical values and remain unchanged."
            ),
        },
        "source": {
            "repository": "GregTech6/gregtech6",
            "revision": REVISION,
            "maps": map_ids,
            "selection": (
                "Every active or inactive fixed source row in the ten T5 "
                "ordinary maps whose item/fluid inputs reference at least one "
                "of the 224 authoritative chemical candidates."
            ),
        },
        "encoding": {
            "row": [
                "map_index",
                "recipe_index",
                "candidate_material_indexes",
                "disposition",
                "shape",
                "translation",
                "rejection",
                "source_row_sha256",
                "projected_row_sha256",
            ],
            "material_dictionary": material_dictionary,
        },
        "counts": {
            "material_candidates": len(material_dictionary),
            "source_rows": len(rows),
            "dispositions": dict(sorted(dispositions.items())),
            "shapes": {
                **dict(sorted(shapes.items())),
                "prefix_matrix": 0,
            },
            "translations": dict(sorted(translations.items())),
            "ordinary_v1_required": ordinary,
            "ordinary_translatable": ordinary_translatable,
            "ordinary_blocked_translation": ordinary_blocked,
            "unclassified": 0,
        },
        "map_summaries": map_summaries,
        "rejection_summary": [
            {
                "reason": reason,
                "rows": count,
                "examples": rejection_examples[reason],
            }
            for reason, count in sorted(rejections.items())
        ],
        "rows": rows,
        "inputs": {
            "tools/t5_chemical_readiness.json": sha256(T5_READINESS),
            "tools/build_t5_source_projection.py": sha256(
                Path(source_projection.__file__).resolve()
            ),
        },
    }


def semantic_document(document: dict[str, Any]) -> dict[str, Any]:
    from tools import semantic_projection as projection

    return projection.ledger_projection(document)["semantic_body"]


def verify_metadata_rebase(
    committed: dict[str, Any],
    candidate: dict[str, Any],
) -> list[str]:
    errors: list[str] = []
    if semantic_document(committed) != semantic_document(candidate):
        errors.append(
            "T21 source denominator metadata rebase changed semantic fields"
        )
    return errors


def check(document: dict[str, Any] | None = None) -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing {OUTPUT.relative_to(ROOT).as_posix()}"]
    from tools import currentness

    committed = load(OUTPUT)
    try:
        validate_compact(committed)
    except ValueError as error:
        return [str(error)]
    if document is None:
        return currentness.check_sidecar(OUTPUT)
    return currentness.check_rebuilt(OUTPUT, document)


def validate_compact(document: dict[str, Any]) -> None:
    counts = document.get("counts") or {}
    if (
        document.get("schema_version") != 1
        or document.get("status") != "T21_SOURCE_DENOMINATOR_READY"
        or document.get("source", {}).get("revision") != REVISION
        or counts.get("material_candidates") != 224
        or counts.get("source_rows") != 45_353
        or counts.get("ordinary_v1_required") != 45_044
        or counts.get("ordinary_translatable") != 2_638
        or counts.get("ordinary_blocked_translation") != 42_406
        or counts.get("unclassified") != 0
        or len(document.get("map_summaries") or []) != 10
        or len(document.get("rows") or []) != 45_353
    ):
        raise ValueError("T21 compact source denominator drifted")
    from tools import currentness

    errors = currentness.check_sidecar(OUTPUT)
    if errors:
        raise ValueError("; ".join(errors))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    replay = parser.add_mutually_exclusive_group()
    replay.add_argument("--reference-only", action="store_true")
    replay.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if args.write and not args.full_replay:
        parser.error("--write requires --full-replay")
    if args.check and not (args.reference_only or args.full_replay):
        parser.error("--check requires --reference-only or --full-replay")
    try:
        if args.reference_only:
            if not OUTPUT.is_file():
                raise ValueError("T21 compact source denominator is missing")
            validate_compact(load(OUTPUT))
            print("T21 compact ordinary source denominator is current.")
            return 0
        document = build()
        if args.write:
            OUTPUT.write_bytes(stable(document).encode("utf-8"))
            print(json.dumps(document["counts"], sort_keys=True))
            return 0
        errors = check(document)
        if errors:
            print("\n".join(errors))
            return 1
        print("T21 ordinary-chemistry source denominator is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T21 source denominator build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
