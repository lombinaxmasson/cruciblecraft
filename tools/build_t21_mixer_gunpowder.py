#!/usr/bin/env python3
"""Publish the first replay-verified T21 Mixer template family."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path
from typing import Any

try:
    from tools import build_t5_source_projection as source_projection
    from tools import gt6_mixer_templates as mixer_templates
except ModuleNotFoundError:
    import build_t5_source_projection as source_projection
    import gt6_mixer_templates as mixer_templates


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t21_template_denominator_policy.json"
EXPECTED = TOOLS / "t21_mixer_gunpowder_expected.json"
MANIFEST = TOOLS / "t21_mixer_gunpowder_manifest.json"
OUTPUT_ROOT = (
    ROOT
    / "src/chemical_recipe_generated/resources/data/cruciblecraft/"
    "recipe/chemical/mixer/gunpowder"
)
RAW_MIXER = (
    ROOT / "gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json"
)
TEMPLATE_ID = (
    "sha256:1b36aeb956cea348f8eaf5006a3e16c52c59c607b820d92941de6249d2efea66"
)
REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
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


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def slug(value: str) -> str:
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", value)
    return re.sub(r"[^a-z0-9_]+", "_", value.lower()).strip("_")


def projection_context() -> dict[str, Any]:
    materials = source_projection.material_documents()
    prefixes = source_projection.prefix_documents()
    source_by_id = source_projection.source_material_index(materials)
    cross = load(source_projection.CROSS_REFERENCE)
    fixed_item_document = load(source_projection.FIXED_ITEM_PROJECTION)
    return {
        "materials": materials,
        "prefixes": prefixes,
        "source_by_id": source_by_id,
        "registration": load(source_projection.REGISTRATION_GATE)[
            "materials"
        ],
        "gt_prefix_by_item": cross["prefix_item_to_gt_prefix"],
        "cc_prefix_by_gt": cross["gt_prefix_to_cc"],
        "fixed_items": {
            (row["source"]["item"], int(row["source"]["meta"])): row[
                "cc_item"
            ]
            for row in fixed_item_document["mappings"]
        },
        "normalized_by_fluid": {
            row["fluid"]: row
            for row in load(source_projection.NORMALIZED_FLUIDS)["records"]
        },
    }


def selected_template() -> dict[str, Any]:
    index = load(mixer_templates.INDEX_OUTPUT)
    rows = [
        row for row in index["templates"]
        if row["template_id"] == TEMPLATE_ID
    ]
    if len(rows) != 1:
        raise ValueError("selected gunpowder Mixer template is not unique")
    template = rows[0]
    if (
        template["shape"] != "material_matrix"
        or template["source_count"] != 4
        or not template["replay"]["replay_verified"]
    ):
        raise ValueError("selected gunpowder Mixer template drifted")
    policy = load(POLICY)
    forced = {
        row["template_id"]: row
        for row in policy["forced_v1_templates"]
    }
    if (
        TEMPLATE_ID not in forced
        or forced[TEMPLATE_ID]["expected_rows"] != 4
    ):
        raise ValueError("gunpowder template is not the reviewed v1 family")
    return template


def independent_expected(
    recipe: dict[str, Any],
    recipe_index: int,
    context: dict[str, Any],
) -> tuple[str, dict[str, Any]]:
    inputs = recipe.get("inputs") or []
    outputs = recipe.get("outputs") or []
    if (
        recipe.get("duration") != 64
        or recipe.get("euPerTick") != 16
        or recipe.get("specialValue") != 0
        or recipe.get("enabled") is not True
        or recipe.get("hidden") is not False
        or recipe.get("fake") is not False
        or recipe.get("canBeBuffered") is not True
        or len(inputs) != 3
        or len(outputs) != 1
        or recipe.get("fluidInputs") != []
        or recipe.get("fluidOutputs") != []
    ):
        raise ValueError(
            f"gunpowder source row shape drifted at {recipe_index}"
        )
    variable = inputs[0]
    niter = inputs[1]
    blaze = inputs[2]
    output = outputs[0]
    material = context["source_by_id"].get(variable.get("meta"))
    if (
        variable.get("item") != "gregtech:gt.meta.dust"
        or material not in {"charcoal", "coal_coke", "coal", "carbon"}
        or variable.get("count") not in {1, 2}
        or niter
        != {
            "item": "gregtech:gt.meta.dust",
            "meta": 8206,
            "count": 1,
            "displayName": "Niter Dust",
        }
        or blaze
        != {
            "item": "minecraft:blaze_powder",
            "meta": 0,
            "count": 1,
            "displayName": "Blaze Powder",
        }
        or output
        != {
            "item": "minecraft:gunpowder",
            "meta": 0,
            "count": 4,
            "displayName": "Gunpowder",
        }
    ):
        raise ValueError(
            f"gunpowder source row I/O drifted at {recipe_index}"
        )
    source_ref = (
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json"
        f"#recipes[{recipe_index}]"
    )
    return material, {
        "can_be_buffered": True,
        "duration": 64,
        "eut": 16,
        "item_input_counts": [int(variable["count"]), 1, 1],
        "item_inputs": [
            {"tag": f"c:dusts/{material}"},
            {"tag": "c:dusts/niter"},
            {"item": "minecraft:blaze_powder"},
        ],
        "item_outputs": [{
            "count": 4,
            "id": "minecraft:gunpowder",
        }],
        "map": "cruciblecraft:mixer",
        "output_chances": [10_000],
        "provenance": {
            "evidence_hashes": [REVISION],
            "selected_source_recipe": source_ref,
            "source_kind": "gt6_mixer_template_replay",
        },
        "type": "cruciblecraft:gt_recipe",
    }


def production_projection(
    recipe: dict[str, Any],
    recipe_index: int,
    context: dict[str, Any],
) -> dict[str, Any]:
    projected, _source = source_projection.project_recipe(
        recipe,
        None,
        source_by_id=context["source_by_id"],
        gt_prefix_by_item=context["gt_prefix_by_item"],
        cc_prefix_by_gt=context["cc_prefix_by_gt"],
        registration=context["registration"],
        prefixes=context["prefixes"],
        fixed_items=context["fixed_items"],
        normalized_by_fluid=context["normalized_by_fluid"],
        materials=context["materials"],
        map_id="gt.recipe.mixer",
    )
    projected["provenance"]["selected_source_recipe"] = (
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.mixer.json"
        f"#recipes[{recipe_index}]"
    )
    projected["provenance"]["source_kind"] = (
        "gt6_mixer_template_replay"
    )
    return projected


def build_documents() -> tuple[dict[str, Any], dict[str, Any], dict[Path, str]]:
    template = selected_template()
    raw = load(RAW_MIXER)
    recipes = raw["recipes"]
    context = projection_context()
    expected_rows = []
    manifest_rows = []
    files: dict[Path, str] = {}
    seen_materials = set()
    for recipe_index, values, source_digest in template["supports"]:
        recipe = mixer_templates.apply_support(
            template["skeleton"],
            template["binding_paths"],
            values,
        )
        if value_sha256(recipe) != source_digest:
            raise ValueError(
                f"template support digest drifted at {recipe_index}"
            )
        if mixer_templates.canonicalize(recipes[recipe_index]) != recipe:
            raise ValueError(
                f"template support differs from raw row {recipe_index}"
            )
        material, expected = independent_expected(
            recipes[recipe_index], recipe_index, context
        )
        production = production_projection(
            recipes[recipe_index], recipe_index, context
        )
        if expected != production:
            raise ValueError(
                f"{material}: independent expected differs from projection"
            )
        if material in seen_materials:
            raise ValueError(f"duplicate gunpowder family material {material}")
        seen_materials.add(material)
        path = OUTPUT_ROOT / f"{material}.json"
        encoded = stable(production)
        files[path] = encoded
        expected_rows.append({
            "material": material,
            "recipe_index": recipe_index,
            "source_row_sha256": source_digest,
            "recipe": expected,
        })
        manifest_rows.append({
            "id": f"cruciblecraft:chemical/mixer/gunpowder/{material}",
            "material": material,
            "recipe_index": recipe_index,
            "path": relative(path),
            "sha256": hashlib.sha256(
                encoded.encode("utf-8")
            ).hexdigest(),
        })
    expected_rows.sort(key=lambda row: row["material"])
    manifest_rows.sort(key=lambda row: row["material"])
    expected_document = {
        "schema_version": 1,
        "status": "T21_MIXER_GUNPOWDER_EXPECTED_READY",
        "template_id": TEMPLATE_ID,
        "source": {
            "revision": REVISION,
            "map": "gt.recipe.mixer",
            "map_sha256": sha256(RAW_MIXER),
            "template_multiset_sha256": template[
                "source_multiset_sha256"
            ],
        },
        "rows": expected_rows,
    }
    manifest_document = {
        "schema_version": 1,
        "status": "T21_MIXER_GUNPOWDER_READY",
        "family": "mixer/gunpowder",
        "template_id": TEMPLATE_ID,
        "counts": {
            "source_facts": 4,
            "authored_rules": 1,
            "datapack_files": 4,
            "logical_rows": 4,
            "eager_rows": 4,
            "lazy_rows": 0,
        },
        "publication_delta": {
            "logical": 4,
            "eager": 4,
            "lazy": 0,
        },
        "consumer": "minecraft:tnt and vanilla gunpowder consumers",
        "rows": manifest_rows,
        "inputs": {
            "tools/t21_template_denominator_policy.json": sha256(POLICY),
            "tools/gt6_mixer_templates_index.json": sha256(
                mixer_templates.INDEX_OUTPUT
            ),
            "tools/gt6_mixer_templates_membership.json": sha256(
                mixer_templates.MEMBERSHIP_OUTPUT
            ),
        },
    }
    return expected_document, manifest_document, files


def validate_compact(
    expected: dict[str, Any],
    manifest: dict[str, Any],
) -> None:
    if (
        expected.get("status")
        != "T21_MIXER_GUNPOWDER_EXPECTED_READY"
        or manifest.get("status") != "T21_MIXER_GUNPOWDER_READY"
        or expected.get("template_id") != TEMPLATE_ID
        or manifest.get("template_id") != TEMPLATE_ID
        or len(expected.get("rows") or []) != 4
        or manifest.get("counts", {}).get("logical_rows") != 4
        or manifest.get("publication_delta")
        != {"logical": 4, "eager": 4, "lazy": 0}
    ):
        raise ValueError("T21 gunpowder compact evidence drifted")
    for key, expected_hash in manifest["inputs"].items():
        if sha256(ROOT / key) != expected_hash:
            raise ValueError(f"T21 gunpowder input drifted: {key}")
    for row in manifest["rows"]:
        path = ROOT / row["path"]
        if not path.is_file() or sha256(path) != row["sha256"]:
            raise ValueError(
                f"T21 gunpowder runtime row drifted: {row['path']}"
            )


def check_outputs(
    full: tuple[dict[str, Any], dict[str, Any], dict[Path, str]] | None,
) -> list[str]:
    if not EXPECTED.is_file() or not MANIFEST.is_file():
        return ["missing T21 gunpowder compact evidence"]
    expected = load(EXPECTED)
    manifest = load(MANIFEST)
    validate_compact(expected, manifest)
    errors = []
    actual_files = set(OUTPUT_ROOT.glob("*.json")) if OUTPUT_ROOT.is_dir() else set()
    planned_files = {ROOT / row["path"] for row in manifest["rows"]}
    errors.extend(
        f"missing:{relative(path)}" for path in sorted(
            planned_files - actual_files
        )
    )
    errors.extend(
        f"stale:{relative(path)}" for path in sorted(
            actual_files - planned_files
        )
    )
    if full is not None:
        full_expected, full_manifest, full_files = full
        if expected != full_expected:
            errors.append(relative(EXPECTED))
        if manifest != full_manifest:
            errors.append(relative(MANIFEST))
        for path, content in full_files.items():
            if not path.is_file() or path.read_text(encoding="utf-8") != content:
                errors.append(relative(path))
    return errors


def write_outputs(
    documents: tuple[dict[str, Any], dict[str, Any], dict[Path, str]],
) -> None:
    expected, manifest, files = documents
    if OUTPUT_ROOT.exists():
        shutil.rmtree(OUTPUT_ROOT)
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    for path, content in files.items():
        path.write_text(content, encoding="utf-8", newline="\n")
    EXPECTED.write_text(
        stable(expected), encoding="utf-8", newline="\n"
    )
    MANIFEST.write_text(
        stable(manifest), encoding="utf-8", newline="\n"
    )


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
        full = build_documents() if args.full_replay else None
        if args.write:
            assert full is not None
            write_outputs(full)
            print("Wrote four T21 Mixer gunpowder family rows.")
            return 0
        errors = check_outputs(full)
        if errors:
            print(
                "T21 Mixer gunpowder family is stale:\n"
                + "\n".join(f"- {error}" for error in errors)
            )
            return 1
        tier = "full replay" if args.full_replay else "compact"
        print(f"T21 Mixer gunpowder {tier} evidence is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T21 Mixer gunpowder build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
